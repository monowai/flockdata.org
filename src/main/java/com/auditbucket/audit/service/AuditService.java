package com.auditbucket.audit.service;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.dao.IAuditDao;
import com.auditbucket.audit.dao.IAuditQueryDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.audit.repo.es.model.AuditChange;
import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import com.auditbucket.audit.repo.neo4j.model.AuditLog;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.SystemUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 8/04/13
 * To change this template use File | Settings | File Templates.
 */

@Service
public class AuditService {
    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SystemUserService sysUserService;

    @Autowired
    IAuditDao auditDAO;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private IAuditChangeDao auditChange;

    @Autowired
    private IAuditQueryDao auditQuery;

    private Log log = LogFactory.getLog(AuditService.class);

    private ObjectMapper om = new ObjectMapper();

    @Transactional
    public ITagRef beginTransaction() {
        return beginTransaction(UUID.randomUUID().toString());
    }

    @Transactional
    private ITagRef beginTransaction(String id) {
        String userName = securityHelper.getLoggedInUser();
        ISystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        ICompany company = su.getCompany();
        return auditDAO.beginTransaction(id, company);

    }

    /**
     * Creates a fortress specific header for the caller. FortressUser is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    @Transactional
    public AuditHeaderInputBean createHeader(AuditHeaderInputBean inputBean) {
        String userName = securityHelper.getLoggedInUser();
        ISystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        ICompany company = su.getCompany();
        String fortress = inputBean.getFortress();
        // ToDo: Improve cypher query
        IFortress iFortress = companyService.getCompanyFortress(company, fortress);
        if (iFortress == null)
            throw new IllegalArgumentException("Unable to find the fortress [" + fortress + "] for the company [" + su.getCompany().getName() + "]");

        IAuditHeader ah = null;

        // Idempotent check
        if (inputBean.getCallerRef() != null)
            ah = findByName(iFortress.getId(), inputBean.getRecordType(), inputBean.getCallerRef());

        if (ah != null) {
            if (log.isDebugEnabled())
                log.debug("Existing header record found by Caller Ref [" + inputBean.getCallerRef() + "] found [" + ah.getAuditKey() + "]");
            inputBean.setAuditKey(ah.getAuditKey());
            return inputBean;
        }

        // Create fortressUser if missing
        IFortressUser fu = fortressService.getFortressUser(iFortress, inputBean.getFortressUser(), true);
        ITagRef txTag = null;
        // Prefer the user supplied, though the transaction will have to belong to the callers company
        if (inputBean.getTxRef() != null)
            // Joining an existing transaction
            txTag = beginTransaction(inputBean.getTxRef());
        else if (inputBean.isTransactional())
            // New transaction
            txTag = beginTransaction();

        ah = new AuditHeader(fu, inputBean);
        ah.addTxTag(txTag);
        ah = auditDAO.save(ah, inputBean);

        if (txTag != null)
            inputBean.setTxRef(txTag.getName());

        if (log.isDebugEnabled())
            log.debug("Audit Header created:" + ah.getId() + " key=[" + ah.getAuditKey() + "]");
        inputBean.setAuditKey(ah.getAuditKey());
        return inputBean;

    }

    public IAuditHeader getHeader(@NotNull @NotEmpty String key) {
        return getHeader(key, false);
    }

    public IAuditHeader getHeader(@NotNull @NotEmpty String key, boolean inflate) {
        String userName = securityHelper.getLoggedInUser();

        ISystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException("Not authorised");


        IAuditHeader ah = auditDAO.findHeader(key, inflate);
        if (ah == null)
            throw new IllegalArgumentException("Unable to find key [" + key + "]");

        if (!(ah.getFortress().getCompany().getId().equals(su.getCompany().getId())))
            throw new SecurityException("Company mismatch. [" + su.getName() + "] working for [" + su.getCompany().getName() + "] cannot write audit records for [" + ah.getFortress().getCompany().getName() + "]");
        return ah;
    }

    public IAuditHeader findByName(Long fortressID, String recordType, String clientRef) {
        String userName = securityHelper.getLoggedInUser();

        ISystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException("Not authorised");

        IFortress fortress = fortressService.getFortress(fortressID);
        if (!fortress.getCompany().getId().equals(su.getCompany().getId()))
            throw new SecurityException("User is not authorised to work with requested Fortress");

        String key = new StringBuilder().append(recordType).append(".").append(clientRef).toString();
        return auditDAO.findHeaderByClientRef(key.toLowerCase(), fortress.getName(), fortress.getCompany().getName());
    }

    @Transactional
    public LogStatus createLog(AuditLogInputBean input) {
        IAuditHeader header = getHeader(input.getAuditKey(), true);
        if (header == null)
            return LogStatus.NOT_FOUND;

        if (input.getWhat() == null || input.getWhat().isEmpty())
            return LogStatus.IGNORE;

        String user = securityHelper.getUserName(false, false);
        if (user == null)
            return LogStatus.FORBIDDEN;

        if (input.getFortressUser() == null) {
            input.setMessage("Fortress User not supplied");
            return LogStatus.ILLEGAL_ARGUMENT;
        }

        boolean setHeader = false;
        IFortress fortress = header.getFortress();

        IFortressUser fUser = fortressService.getFortressUser(fortress, input.getFortressUser().toLowerCase(), true);
        // Spin the following off in to a separate thread?
        String childKey = null, parentKey = null;
        IAuditLog lastChange = auditDAO.getLastChange(header.getId());
        String event = input.getEventType();
        DateTime dateWhen;
        if (input.getWhen() == null)
            dateWhen = new DateTime();
        else
            dateWhen = new DateTime(input.getWhen());

        if (lastChange != null) {
            // Find the change data
            if (lastChange.getWhat().equals(input.getWhat())) {
                if (log.isDebugEnabled())
                    log.debug("Ignoring a change we already have");
                return LogStatus.IGNORE;
            }
            if (event == null)
                event = IAuditLog.UPDATE;

            // Graph who did this for future analysis
            if (header.getLastUser() != null && !header.getLastUser().getId().equals(fUser.getId())) {
                setHeader = true;
                auditDAO.removeLastChange(header);
            }

            if (fortress.isAccumulatingChanges()) {
                IAuditChange change = createSearchableChange(header, dateWhen, input.getWhat(), event);
                if (change != null)
                    childKey = change.getChild();
            } else {
                // Update instead of Create
                childKey = lastChange.getKey(); // Key does not change in this mode
                updateSearchableChange(header, childKey, dateWhen, input.getWhat());
            }
        } else { // Creating a new log
            if (event == null)
                event = IAuditLog.CREATE;
            setHeader = true;
            IAuditChange change = createSearchableChange(header, dateWhen, input.getWhat(), event);
            if (change != null) {
                childKey = change.getChild();
                parentKey = change.getParent();
            }
        }

        if (setHeader) {
            header.setLastUser(fUser);
            if (parentKey != null)
                header.setSearchKey(parentKey);
            header = auditDAO.save(header, input.getTxRef());
        }

        AuditLog al = new AuditLog(header, fUser, dateWhen, event, input.getWhat());
        if (input.getTxRef() != null)
            al.setTxRef(input.getTxRef());
        al.setKey(childKey);
        auditDAO.save(al);
        return LogStatus.OK;

    }


    public void getAuditLogs(String key, String txRef) {
        IAuditHeader header = getValidHeader(key);

    }

    public enum LogStatus {
        IGNORE, OK, FORBIDDEN, NOT_FOUND, ILLEGAL_ARGUMENT
    }


    private void updateSearchableChange(IAuditHeader header, String existingKey, DateTime dateWhen, String what) {
        if (header.getFortress().isIgnoreSearchEngine())
            return;
        if (existingKey != null)
            auditChange.update(header, existingKey, what);
        else
            // Only happen if the fortress was previously not creating searchable key values
            createSearchableChange(header, dateWhen, what, "Create");
    }

    private IAuditChange createSearchableChange(IAuditHeader header, DateTime dateWhen, String what, String event) {
        if (header.getFortress().isIgnoreSearchEngine())
            return null;
        IAuditChange thisChange = new AuditChange(header);
        thisChange.setEvent(event);
        thisChange.setWhat(what);
        if (dateWhen != null)
            thisChange.setWhen(dateWhen.toDate());
        thisChange = auditChange.save(thisChange);
        return thisChange;
    }

    public IAuditLog getLastChange(String headerKey) {
        IAuditHeader ah = getValidHeader(headerKey);
        return getLastChange(ah);
    }

    public IAuditLog getLastChange(IAuditHeader auditHeader) {
        return auditDAO.getLastChange(auditHeader.getId());
    }

    public Set<IAuditLog> getAuditLogs(String headerKey) {
        securityHelper.isValidUser();
        IAuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getAuditLogs(auditHeader.getId());

    }

    public Set<IAuditLog> getAuditLogs(String headerKey, Date from, Date to) {
        securityHelper.isValidUser();
        IAuditHeader auditHeader = getValidHeader(headerKey);
        return getAuditLogs(auditHeader, from, to);
    }

    protected Set<IAuditLog> getAuditLogs(IAuditHeader auditHeader, Date from, Date to) {
        return auditDAO.getAuditLogs(auditHeader.getId(), from, to);
    }


    /**
     * This can be used in compensating transactions to roll back the last change if the caller decides a rollback
     * is required after the log has been written. If there are no IAuditLog records left, then the header will also
     * be removed and the headerKey will be invalid hence forth.
     *
     * @param headerKey UID of the header
     * @return the modified header record or null if no header exists.
     */
    public IAuditHeader cancelLastLog(String headerKey) {
        IAuditHeader auditHeader = getValidHeader(headerKey);
        IAuditLog auditLog = getLastChange(auditHeader);
        if (auditHeader.getFortress().isAccumulatingChanges())
            // If adding, then we need to remove the ES document
            auditChange.delete(auditHeader, auditLog.getKey());

        auditDAO.delete(auditLog);

        auditLog = getLastChange(auditHeader);
        if (auditLog == null)
            return null;

        auditHeader = auditDAO.fetch(auditHeader);
        auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), auditLog.getWho().getName()));
        auditHeader = auditDAO.save(auditHeader);

        // Sync the update to elastic search.
        if (auditHeader.getFortress().isAccumulatingChanges())
            auditChange.update(auditHeader, auditLog.getKey(), auditLog.getWhat());

        return auditHeader;
    }


    protected String calcDelta(String what) {
        return null;
    }

    public int getAuditLogCount(String headerKey) {
        IAuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getLogCount(auditHeader.getId());

    }

    private IAuditHeader getValidHeader(String headerKey) {
        IAuditHeader header = auditDAO.findHeader(headerKey);
        if (header == null) {
            throw new IllegalArgumentException("No audit header for [" + headerKey + "]");
        }
        String userName = securityHelper.getLoggedInUser();
        ISystemUser sysUser = sysUserService.findByName(userName);

        if (!header.getFortress().getCompany().getId().equals(sysUser.getCompany().getId())) {
            throw new SecurityException("Not authorised to retrieve change records");
        }
        return header;

    }

    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }
}
