/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.IDocumentType;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.dao.IAuditDao;
import com.auditbucket.engine.repo.neo4j.model.AuditHeader;
import com.auditbucket.engine.repo.neo4j.model.AuditLog;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.registration.service.TagService;
import com.auditbucket.search.AuditChange;
import com.auditbucket.search.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
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
 * User: Mike Holdsworth
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
    AuditTagService auditTagService;

    @Autowired
    IAuditDao auditDAO;

    @Autowired
    TagService tagService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private IAuditSearchService searchService;

    private Logger log = LoggerFactory.getLogger(AuditService.class);
    static final ObjectMapper om = new ObjectMapper();

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
    public AuditResultBean createHeader(AuditHeaderInputBean inputBean) {
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
            ah = findByCallerRef(iFortress.getId(), inputBean.getDocumentType(), inputBean.getCallerRef());

        if (ah != null) {
            if (log.isDebugEnabled())
                log.debug("Existing header record found by Caller Ref [" + inputBean.getCallerRef() + "] found [" + ah.getAuditKey() + "]");
            inputBean.setAuditKey(ah.getAuditKey());

            AuditResultBean arb = new AuditResultBean(ah, null);
            arb.setStatus("Existing audit record found and is being returned");
            return arb;
        }

        // Create fortressUser if missing
        IFortressUser fu = fortressService.getFortressUser(iFortress, inputBean.getFortressUser(), true);
        IDocumentType documentType = tagService.resolveDocType(inputBean.getDocumentType());
        ah = new AuditHeader(fu, inputBean, documentType);
        inputBean.setAuditKey(ah.getAuditKey());

        // Future from here on.....
        ah = auditDAO.save(ah);

        Map<String, Object> userTags = inputBean.getTagValues();
        auditTagService.createTagValues(userTags, ah);


        if (log.isDebugEnabled())
            log.debug("Audit Header created:" + ah.getId() + " key=[" + ah.getAuditKey() + "]");


        AuditLogInputBean logBean = inputBean.getAuditLog();
        if (logBean != null) {
            logBean.setAuditKey(ah.getAuditKey());
            logBean.setFortressUser(inputBean.getFortressUser());
            logBean.setCallerRef(ah.getCallerRef());
            // Creating an initial change record
            logBean = createLog(ah, logBean, userTags);
            AuditResultBean arb = new AuditResultBean(ah, logBean.getTxRef());
            return arb;
        }
        return new AuditResultBean(ah, null);

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

    /**
     * Looks up the header from input and creates a log record
     *
     * @param input log details
     * @return populated log information with any error messages
     */
    @Transactional
    public AuditLogInputBean createLog(AuditLogInputBean input) {
        String auditKey = input.getAuditKey();
        IAuditHeader header;

        if (auditKey == null || auditKey.equals("")) {
            header = findByCallerRef(input.getFortress(), input.getDocumentType(), input.getCallerRef());
            if (header != null)
                input.setAuditKey(header.getAuditKey());
        } else
            header = getHeader(input.getAuditKey());

        if (header == null) {
            input.setStatus(AuditLogInputBean.LogStatus.NOT_FOUND);
            return input;
        }

        return createLog(header, input, header.getTagMap());
    }

    /**
     * Creates an audit log record for the supplied header from the supplied input
     *
     * @param header auditHeader the caller is authorised to work with
     * @param input  auditLog details containing the data to log
     * @return populated log information with any error messages
     */
    @Transactional
    AuditLogInputBean createLog(IAuditHeader header, AuditLogInputBean input, Map<String, Object> tagValues) {
        if (input.getMapWhat() == null || input.getMapWhat().isEmpty()) {
            input.setStatus(AuditLogInputBean.LogStatus.IGNORE);
            return input;
        }

        String user = securityHelper.getUserName(false, false);
        if (user == null) {
            input.setStatus(AuditLogInputBean.LogStatus.FORBIDDEN);
            return input;
        }

        if (input.getFortressUser() == null) {
            input.setAbMessage("Fortress User not supplied");
            input.setStatus(AuditLogInputBean.LogStatus.ILLEGAL_ARGUMENT);
            return input;
        }

        boolean updateHeader = false;

        IFortress fortress = header.getFortress();
        IFortressUser fUser = fortressService.getFortressUser(fortress, input.getFortressUser().toLowerCase(), true);

        // Spin the following off in to a separate thread?
        // https://github.com/monowai/auditbucket/issues/7
        IAuditLog lastChange = auditDAO.getLastChange(header.getId());
        String event = input.getEvent();
        Boolean searchActive = fortress.isSearchActive();
        DateTime dateWhen;
        if (input.getWhen() == null)
            dateWhen = new DateTime();
        else
            dateWhen = new DateTime(input.getWhen());

        if (lastChange != null) {
            // Neo4j won't store the map, so we store the raw escaped JSON text
            try {
                if (isSame(lastChange.getJsonWhat(), input.getWhat())) {
                    if (log.isDebugEnabled())
                        log.debug("Ignoring a change we already have");
                    input.setStatus(AuditLogInputBean.LogStatus.IGNORE);
                    return input;
                }
            } catch (IOException e) {
                input.setStatus(AuditLogInputBean.LogStatus.ILLEGAL_ARGUMENT);
                input.setAbMessage("Error comparing JSON data: " + e.getMessage());
                return input;
            }
            if (event == null)
                event = IAuditLog.UPDATE;

            // Graph who did this for future analysis
            if (header.getLastUser() != null && !header.getLastUser().getId().equals(fUser.getId())) {
                updateHeader = true;
                auditDAO.removeLastChange(header);
            }
            header.setLastUser(fUser);
            AuditChange sd = new AuditChange(header, input.getMapWhat(), event, dateWhen);
            sd.setTagValues(tagValues);
            if (searchActive)
                searchService.makeChangeSearchable(sd);
        } else { // first ever log for the header
            if (event == null)
                event = IAuditLog.CREATE;
            updateHeader = true;
            AuditChange sd = new AuditChange(header, input.getMapWhat(), event, dateWhen);
            sd.setTagValues(tagValues);
            if (log.isTraceEnabled()) {
                try {
                    log.trace(om.writeValueAsString(sd));
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage());
                }
            }
            if (searchActive)
                searchService.makeChangeSearchable(sd);
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

        if (updateHeader) {
            header.setLastUser(fUser);
            header = auditDAO.save(header);
        }

        IAuditLog al = new AuditLog(fUser, dateWhen, event, input.getWhat());
        if (input.getTxRef() != null)
            al.setTxRef(txRef);
//        if (accumulatingChanges)
//            al.setSearchKey(searchKey);

        al = auditDAO.save(al);
        auditDAO.addChange(header, al, dateWhen);
        input.setStatus(AuditLogInputBean.LogStatus.OK);
        return input;

    }

    /**
     * Callback handler that is invoked from ab-search. This routine ties the generated search document ID
     * with the auditHeader
     * <p/>
     * On completion of this, an outbound message should be posted so that the caller can be made aware(?)
     *
     * @param searchResult contains keys to tie the search to the audit
     */
    @ServiceActivator(inputChannel = "searchResult")
    public void handleSearchResult(SearchResult searchResult) {
        String auditKey = searchResult.getAuditKey();
        if (log.isDebugEnabled())
            log.debug("Updating from search record =[" + searchResult + "]");
        IAuditHeader header = auditDAO.findHeader(auditKey);

        if (header == null)
            throw new IllegalArgumentException("Audit Key could not be found for [" + searchResult + "]");
        header.setSearchKey(searchResult.getSearchKey());
        auditDAO.save(header);
    }

    private boolean isSame(String jsonWhat, String jsonOther) throws IOException {
        if (jsonWhat == null || jsonOther == null)
            return false;

        if (jsonWhat.length() != jsonOther.length())
            return false;

        // Compare values
        JsonNode compareTo = om.readTree(jsonWhat);
        JsonNode other = om.readTree(jsonOther);
        return compareTo.equals(other);
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

    @Transactional
    public void updateHeader(IAuditHeader auditHeader) {
        auditDAO.save(auditHeader);
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
     * This could be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no IAuditLog records left, then the header will also be removed and the
     * AB headerKey will be forever invalid.
     *
     * @param headerKey UID of the header
     * @return the modified header record or null if no header exists.
     */
    @Transactional
    public IAuditHeader cancelLastLog(String headerKey) throws IOException {
        IAuditHeader auditHeader = getValidHeader(headerKey);
        IAuditLog logToDelete = getLastChange(auditHeader);
        auditDAO.delete(logToDelete);

        IAuditLog newLastChange = getLastChange(auditHeader);
        if (newLastChange == null)
            // No Log records exist. Delete the header??
            return null;

        auditHeader = auditDAO.fetch(auditHeader);
        auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), newLastChange.getWho().getName()));
        auditHeader = auditDAO.save(auditHeader);
        // MKH Removed accumulating fortress. Can't see a need to accumulate old versions of a document
        //     in the search engine.

        //boolean accumulatingFortress = auditHeader.getFortress().isAccumulatingChanges();
        // Sync the update to elastic search.
        if (auditHeader.getFortress().isSearchActive()) {
            //String searchKey = (accumulatingFortress ? logToDelete.getSearchKey() : auditHeader.getSearchKey());
//            if (accumulatingFortress) {
            //searchService.delete(auditHeader, searchKey);

            // Rebuild the search record
//                SearchResult change = searchService.makeChangeSearchable(new AuditChange(auditHeader, newLastChange.getWhat(), newLastChange.getName(), new DateTime(newLastChange.getWhen())));
//                if (change != null) {
//                    // When accumulating the searchkey is against the logs
//                    newLastChange.setSearchKey(change.getSearchKey());
//                    auditDAO.save(newLastChange);
//                }
//            } else {
            // Update against the Audit Header only by reindexing the search document
            searchService.makeChangeSearchable(new AuditChange(auditHeader, newLastChange.getWhat(), newLastChange.getEvent(), new DateTime(newLastChange.getWhen())));

            //          }
        }

        return auditHeader;
    }

    /**
     * counts the number of audit logs that exist for the given header
     *
     * @param headerKey GUID
     * @return count
     */
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

    private IAuditHeader findByCallerRef(String fortress, String documentType, String callerRef) {
        IFortress iFortress = fortressService.find(fortress);
        if (iFortress == null)
            return null;

        return findByCallerRef(iFortress.getId(), documentType, callerRef);
    }

    public IAuditHeader findByCallerRef(Long fortressID, String documentType, String callerRef) {
        String userName = securityHelper.getLoggedInUser();

        ISystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException("Not authorised");

        IFortress fortress = fortressService.getFortress(fortressID);
        if (!fortress.getCompany().getId().equals(su.getCompany().getId()))
            throw new SecurityException("User is not authorised to work with requested Fortress");

        return auditDAO.findHeaderByCallerRef(fortress.getId(), documentType, callerRef.trim());
    }


}
