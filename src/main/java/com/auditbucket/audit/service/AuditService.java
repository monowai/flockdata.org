package com.auditbucket.audit.service;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.bean.SearchDocumentBean;
import com.auditbucket.audit.dao.IAuditDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITxRef;
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
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
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
    public static final String DOT = ".";
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
    private AuditSearchService searchService;

    private Logger log = LoggerFactory.getLogger(AuditService.class);

    @Transactional
    public ITxRef beginTransaction() {
        return beginTransaction(UUID.randomUUID().toString());
    }

    @Transactional
    ITxRef beginTransaction(String id) {
        String userName = securityHelper.getLoggedInUser();
        ISystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        ICompany company = su.getCompany();
        return auditDAO.beginTransaction(id, company);

    }

    @Transactional
    public Map<String, Object> findByTXRef(String txRef) {
        ITxRef tx = findTx(txRef);
        return (tx == null ? null : auditDAO.findByTransaction(tx));
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

        ah = new AuditHeader(fu, inputBean);
        ah = auditDAO.save(ah, inputBean);

        if (log.isDebugEnabled())
            log.debug("Audit Header created:" + ah.getId() + " key=[" + ah.getAuditKey() + "]");
        inputBean.setAuditKey(ah.getAuditKey());
        if (inputBean.getAuditLog() != null) {
            AuditLogInputBean logBean = inputBean.getAuditLog();
            logBean.setAuditKey(ah.getAuditKey());
            inputBean.setAuditLog(createLog(logBean));
        }

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

    public IAuditHeader findByName(Long fortressID, @NotEmpty @NotNull String recordType, @NotEmpty @NotNull String clientRef) {
        String userName = securityHelper.getLoggedInUser();

        ISystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException("Not authorised");

        IFortress fortress = fortressService.getFortress(fortressID);
        if (!fortress.getCompany().getId().equals(su.getCompany().getId()))
            throw new SecurityException("User is not authorised to work with requested Fortress");

        String key = (recordType.trim() + DOT + clientRef.trim());
        return auditDAO.findHeaderByClientRef(key, fortress.getName(), fortress.getCompany().getName());
    }

    @Transactional
    public AuditLogInputBean createLog(AuditLogInputBean input) {
        IAuditHeader header = getHeader(input.getAuditKey());
        if (header == null) {
            input.setStatus(LogStatus.NOT_FOUND);
            return input;
        }

        if (input.getWhat() == null || input.getWhat().isEmpty()) {
            input.setStatus(LogStatus.IGNORE);
            return input;
        }

        String user = securityHelper.getUserName(false, false);
        if (user == null) {
            input.setStatus(LogStatus.FORBIDDEN);
            return input;
        }

        if (input.getFortressUser() == null) {
            input.setMessage("Fortress User not supplied");
            input.setStatus(LogStatus.ILLEGAL_ARGUMENT);
            return input;
        }

        boolean setHeader = false;

        IFortress fortress = header.getFortress();
        IFortressUser fUser = fortressService.getFortressUser(fortress, input.getFortressUser().toLowerCase(), true);

        // Spin the following off in to a separate thread?
        String searchKey = null;
        IAuditLog lastChange = auditDAO.getLastChange(header.getId());
        String event = input.getEventType();

        DateTime dateWhen;
        if (input.getWhen() == null)
            dateWhen = new DateTime();
        else
            dateWhen = new DateTime(input.getWhen());

        if (lastChange != null) {
            // Neo4j won't store the map, so we store the raw escaped JSON text
            if (lastChange.getWhat().equals(input.getWhatAsText())) {
                if (log.isDebugEnabled())
                    log.debug("Ignoring a change we already have");
                input.setStatus(LogStatus.IGNORE);
                return input;
            }
            if (event == null)
                event = IAuditLog.UPDATE;

            // Graph who did this for future analysis
            if (header.getLastUser() != null && !header.getLastUser().getId().equals(fUser.getId())) {
                setHeader = true;
                auditDAO.removeLastChange(header);
            }

            if (fortress.isAccumulatingChanges()) {
                IAuditChange change = searchService.createSearchableChange(header, dateWhen, input.getWhat(), event);
                if (change != null)
                    searchKey = change.getSearchKey();
            } else {
                // Update instead of Create
                searchKey = lastChange.getSearchKey(); // Key does not change in this mode

                IAuditChange change = searchService.updateSearchableChange(header, searchKey, dateWhen, input.getWhat(), event);
                if (change != null) {
                    setHeader = true;
                    searchKey = change.getSearchKey();
                }
            }
        } else { // Creating a new log
            if (event == null)
                event = IAuditLog.CREATE;
            setHeader = true;
            IAuditChange change = searchService.createSearchableChange(header, dateWhen, input.getWhat(), event);
            if (change != null) {
                searchKey = change.getSearchKey();
            }
        }
        ITxRef txRef = null;
        if (input.isTransactional()) {
            if (input.getTxRef() == null) {
                txRef = beginTransaction();
                input.setTxRef(txRef.getName());
            } else {
                txRef = beginTransaction(input.getTxRef());
            }
        }

        if (setHeader) {
            header.setLastUser(fUser);
            if (searchKey != null)
                header.setSearchKey(searchKey);
            header = auditDAO.save(header);
        }

        AuditLog al = new AuditLog(fUser, dateWhen, event, input.getWhatAsText());
        if (input.getTxRef() != null)
            al.setTxRef(txRef);
        al.setKey(searchKey);
        auditDAO.save(al);
        auditDAO.addChange(header, al, dateWhen);
        input.setStatus(LogStatus.OK);
        return input;

    }

    public ITxRef findTx(String txRef) {
        return findTx(txRef, false);
    }

    public ITxRef findTx(String txRef, boolean fetchHeaders) {
        String userName = securityHelper.getLoggedInUser();
        ISystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");
        ITxRef tx = auditDAO.findTxTag(txRef, su.getCompany(), fetchHeaders);
        if (tx == null)
            return null;
        return tx;
    }

    public Set<IAuditHeader> findTxHeaders(String txName) {
        ITxRef txRef = findTx(txName, true);
        return txRef.getHeaders();
    }

    public enum LogStatus {
        IGNORE, OK, FORBIDDEN, NOT_FOUND, ILLEGAL_ARGUMENT
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
    @Transactional
    public IAuditHeader cancelLastLog(String headerKey) throws IOException {
        IAuditHeader auditHeader = getValidHeader(headerKey);
        IAuditLog auditLog = getLastChange(auditHeader);
        if (auditHeader.getFortress().isAccumulatingChanges())
            // If adding, then we need to remove the ES document
            searchService.delete(auditHeader, auditLog.getSearchKey());

        auditDAO.delete(auditLog);
        String searchKey = auditLog.getSearchKey();
        if (searchKey != null)
            searchService.delete(auditHeader, searchKey);

        IAuditLog lastChange = getLastChange(auditHeader);
        if (lastChange == null)
            return null;

        auditHeader = auditDAO.fetch(auditHeader);

        auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), lastChange.getWho().getName()));
        auditHeader = auditDAO.save(auditHeader);

        // Sync the update to elastic search.
        if (!auditHeader.getFortress().isIgnoreSearchEngine()) {
            IAuditChange change = searchService.createSearchableChange(new SearchDocumentBean(auditHeader, new DateTime(lastChange.getWhen()), lastChange.getWhat(), lastChange.getEvent()));
            if (change != null)
                auditHeader.setSearchKey(change.getSearchKey());
        }

        return auditHeader;
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
            throw new SecurityException("Not authorised to work with this audit record");
        }
        return header;

    }

}
