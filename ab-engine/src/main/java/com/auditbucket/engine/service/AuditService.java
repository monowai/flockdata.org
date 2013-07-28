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

import com.auditbucket.audit.model.*;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.dao.IAuditDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.registration.service.TagService;
import com.auditbucket.search.AuditSearchChange;
import com.auditbucket.search.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;

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
    private AbSearchGateway searchGateway;

    private Logger log = LoggerFactory.getLogger(AuditService.class);
    static final ObjectMapper om = new ObjectMapper();

    public Map<String, String> getHealth() {
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("ab-engine", auditDAO.ping());
        return healthResults;

    }

    @Transactional
    public TxRef beginTransaction() {
        return beginTransaction(UUID.randomUUID().toString());
    }

    @Transactional
    TxRef beginTransaction(String id) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        Company company = su.getCompany();
        return auditDAO.beginTransaction(id, company);

    }

    @Transactional
    public Map<String, Object> findByTXRef(String txRef) {
        TxRef tx = findTx(txRef);
        return (tx == null ? null : auditDAO.findByTransaction(tx));
    }

    /**
     * Creates a fortress specific header for the caller. FortressUserNode is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    @Transactional
    public AuditResultBean createHeader(AuditHeaderInputBean inputBean) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        Company company = su.getCompany();
        String fortress = inputBean.getFortress();
        // ToDo: Improve cypher query
        Fortress iFortress = companyService.getCompanyFortress(company, fortress);
        if (iFortress == null)
            throw new IllegalArgumentException("Unable to find the fortress [" + fortress + "] for the company [" + su.getCompany().getName() + "]");

        AuditHeader ah = null;

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
        FortressUser fu = fortressService.getFortressUser(iFortress, inputBean.getFortressUser(), true);
        DocumentType documentType = tagService.resolveDocType(inputBean.getDocumentType());

        // Future from here on.....
        ah = auditDAO.save(fu, inputBean, documentType);
        inputBean.setAuditKey(ah.getAuditKey());

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
            return new AuditResultBean(ah, logBean.getTxRef());
        }
        return new AuditResultBean(ah, null);

    }

    public AuditHeader getHeader(@NotNull @NotEmpty String key) {
        return getHeader(key, false);
    }

    public AuditHeader getHeader(@NotNull @NotEmpty String key, boolean inflate) {
        String userName = securityHelper.getLoggedInUser();

        SystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException("Not authorised");


        AuditHeader ah = auditDAO.findHeader(key, inflate);
        if (ah == null)
            throw new IllegalArgumentException("Unable to find key [" + key + "]");

        if (!(ah.getFortress().getCompany().getId().equals(su.getCompany().getId())))
            throw new SecurityException("CompanyNode mismatch. [" + su.getName() + "] working for [" + su.getCompany().getName() + "] cannot write audit records for [" + ah.getFortress().getCompany().getName() + "]");
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
        AuditHeader header;

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
    AuditLogInputBean createLog(AuditHeader header, AuditLogInputBean input, Map<String, Object> tagValues) {
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
            input.setAbMessage("FortressNode User not supplied");
            input.setStatus(AuditLogInputBean.LogStatus.ILLEGAL_ARGUMENT);
            return input;
        }
        try {
            // Normalise and JSON'ise the what argument that has probably just been
            //  placed in to the instance variable
            input.setWhat(input.getWhat());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to pass what text as JSON object", e);
        }

        boolean headerModified = false;

        Fortress fortress = header.getFortress();
        FortressUser fUser = fortressService.getFortressUser(fortress, input.getFortressUser().toLowerCase(), true);

// Transactions checks
        TxRef txRef = handleTxRef(input);

//ToDo: Look at spin the following off in to a separate thread?
        // https://github.com/monowai/auditbucket/issues/7
        AuditLog lastWhen = auditDAO.getLastChange(header.getId());
        AuditChange lastChange = (lastWhen != null ? lastWhen.getAuditChange() : null);
        String event = input.getEvent();
        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(DateTimeZone.UTC) : new DateTime(input.getWhen(), DateTimeZone.UTC));

        SearchChange sd; // Document that will be indexed

        if (lastChange != null) {
            // Neo4j won't store the map, so we store the raw escaped JSON text
            try {
                // KVStore.getWhat()
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
            if (event == null) {
                event = AuditChange.UPDATE;
                input.setEvent(event);
            }

            // Graph who did this for future analysis
            if (header.getLastUser() != null && !header.getLastUser().getId().equals(fUser.getId())) {
                headerModified = true;
                auditDAO.removeLastChange(header);
            }
            header.setLastUser(fUser);
            sd = new AuditSearchChange(header, input.getMapWhat(), event, fortressWhen);
            sd.setTagValues(tagValues);
        } else { // first ever log for the header
            if (event == null) {
                event = AuditChange.CREATE;
                input.setEvent(event);
            }

            headerModified = true;
            header.setLastUser(fUser);
            sd = new AuditSearchChange(header, input.getMapWhat(), event, fortressWhen);
            sd.setTagValues(tagValues);
            if (log.isTraceEnabled()) {
                try {
                    log.trace(om.writeValueAsString(sd));
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage());
                }
            }
        }

        if (headerModified) {
            header.setLastUser(fUser);
            header = auditDAO.save(header);
        }

        AuditChange change = auditDAO.save(fUser, input);
        AuditLog log = auditDAO.addChange(header, change, fortressWhen);

        if (searchActive) {
            // Used to reconcile that the change was actually indexed
            sd.setSysWhen(log.getSysWhen());
            searchGateway.makeChangeSearchable(sd);
        }
        input.setStatus(AuditLogInputBean.LogStatus.OK);
        return input;

    }

    private TxRef handleTxRef(AuditLogInputBean input) {
        TxRef txRef = null;
        if (input.isTransactional()) {
            if (input.getTxRef() == null) {
                txRef = beginTransaction();
                input.setTxRef(txRef.getName());
            } else {
                txRef = beginTransaction(input.getTxRef());
            }
        }
        return txRef;
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
            log.debug("Updating from search auditKey =[" + searchResult + "]");
        AuditHeader header = auditDAO.findHeader(auditKey);

        if (header == null) {
            log.error("Audit Key could not be found for [" + searchResult + "]");
            return;
        }
        header.setSearchKey(searchResult.getSearchKey());
        auditDAO.save(header);
        if (log.isDebugEnabled())
            log.debug("Updated from search auditKey =[" + searchResult + "]");

        AuditLog when = auditDAO.getChange(header.getId(), searchResult.getSysWhen());
        // Another thread may have processed this so save an update
        if (when != null && !when.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            when.setIsIndexed();
            auditDAO.save(when);
        }
    }

    private boolean isSame(String jsonThis, String jsonThat) throws IOException {
        if (jsonThis == null || jsonThat == null)
            return false;

        if (jsonThis.length() != jsonThat.length())
            return false;

        // Compare values
        JsonNode compareTo = om.readTree(jsonThis);
        JsonNode other = om.readTree(jsonThat);
        return compareTo.equals(other);
    }

    public TxRef findTx(String txRef) {
        return findTx(txRef, false);
    }

    public TxRef findTx(String txRef, boolean fetchHeaders) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");
        TxRef tx = auditDAO.findTxTag(txRef, su.getCompany(), fetchHeaders);
        if (tx == null)
            return null;
        return tx;
    }

    public Set<AuditHeader> findTxHeaders(String txName) {
        TxRef txRef = findTx(txName, true);
        return txRef.getHeaders();
    }

    @Transactional
    public void updateHeader(AuditHeader auditHeader) {
        auditDAO.save(auditHeader);
    }


    public AuditLog getLastChange(String headerKey) {
        AuditHeader ah = getValidHeader(headerKey);
        return getLastChange(ah);
    }

    public AuditLog getLastChange(AuditHeader auditHeader) {
        return auditDAO.getLastChange(auditHeader.getId());
    }

    public Set<AuditChange> getAuditLogs(String headerKey) {
        securityHelper.isValidUser();
        AuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getAuditLogs(auditHeader.getId());
    }

    public Set<AuditChange> getAuditLogs(String headerKey, Date from, Date to) {
        securityHelper.isValidUser();
        AuditHeader auditHeader = getValidHeader(headerKey);
        return getAuditLogs(auditHeader, from, to);
    }

    protected Set<AuditChange> getAuditLogs(AuditHeader auditHeader, Date from, Date to) {
        return auditDAO.getAuditLogs(auditHeader.getId(), from, to);
    }


    /**
     * This could be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no AuditChange records left, then the header will also be removed and the
     * AB headerKey will be forever invalid.
     *
     * @param headerKey UID of the header
     * @return the modified header record or null if no header exists.
     */
    @Transactional
    public AuditHeader cancelLastLog(String headerKey) throws IOException {
        AuditHeader auditHeader = getValidHeader(headerKey);
        AuditLog whenToDelete = getLastChange(auditHeader);
        if (whenToDelete == null)
            return null;

        auditDAO.delete(whenToDelete.getAuditChange());

        AuditLog auditLog = getLastChange(auditHeader);
        if (auditLog == null)
            // No Log records exist. Delete the header??
            return null;
        AuditChange newLastChange = auditLog.getAuditChange();
        auditHeader = auditDAO.fetch(auditHeader);
        auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), newLastChange.getWho().getName()));
        auditHeader = auditDAO.save(auditHeader);
        // MKH Removed accumulating fortress. Can't see a need to accumulate old versions of a document
        //     in the search engine.

        // Sync the update to elastic search.
        if (auditHeader.getFortress().isSearchActive()) {
            // Update against the Audit Header only by reindexing the search document
            searchGateway.makeChangeSearchable(new AuditSearchChange(auditHeader, newLastChange.getWhat(), newLastChange.getEvent(), new DateTime(auditLog.getFortressWhen())));
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
        AuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getLogCount(auditHeader.getId());

    }

    private AuditHeader getValidHeader(String headerKey) {
        AuditHeader header = auditDAO.findHeader(headerKey);
        if (header == null) {
            throw new IllegalArgumentException("No audit header for [" + headerKey + "]");
        }
        String userName = securityHelper.getLoggedInUser();
        SystemUser sysUser = sysUserService.findByName(userName);

        if (!header.getFortress().getCompany().getId().equals(sysUser.getCompany().getId())) {
            throw new SecurityException("Not authorised to work with this audit record");
        }
        return header;

    }

    private AuditHeader findByCallerRef(String fortress, String documentType, String callerRef) {
        Fortress iFortress = fortressService.find(fortress);
        if (iFortress == null)
            return null;

        return findByCallerRef(iFortress.getId(), documentType, callerRef);
    }

    public AuditHeader findByCallerRef(Long fortressID, String documentType, String callerRef) {
        String userName = securityHelper.getLoggedInUser();

        SystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException("Not authorised");

        Fortress fortress = fortressService.getFortress(fortressID);
        if (!fortress.getCompany().getId().equals(su.getCompany().getId()))
            throw new SecurityException("User is not authorised to work with requested FortressNode");

        return auditDAO.findHeaderByCallerRef(fortress.getId(), documentType, callerRef.trim());
    }


}
