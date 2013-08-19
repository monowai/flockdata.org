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
import com.auditbucket.dao.AuditDao;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Mike Holdsworth
 * Date: 8/04/13
 * To change this template use File | Settings | File Templates.
 */

@Service(value = "ab.AuditService")
@Transactional
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
    AuditDao auditDAO;

    @Autowired
    TagService tagService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private AbSearchGateway searchGateway;

    private Logger logger = LoggerFactory.getLogger(AuditService.class);
    static final ObjectMapper om = new ObjectMapper();

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Map<String, String> getHealth() {
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("ab-engine", auditDAO.ping());
        return healthResults;

    }

    public TxRef beginTransaction() {
        return beginTransaction(UUID.randomUUID().toString());
    }

    TxRef beginTransaction(String id) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByName(userName);

        if (su == null)
            throw new SecurityException("Not authorised");

        Company company = su.getCompany();
        return auditDAO.beginTransaction(id, company);

    }

    public Map<String, Object> findByTXRef(String txRef) {
        TxRef tx = findTx(txRef);
        return (tx == null ? null : auditDAO.findByTransaction(tx));
    }

    /**
     * Creates a fortress specific auditHeader for the caller. FortressUserNode is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    public AuditResultBean createHeader(AuditHeaderInputBean inputBean) {
        SystemUser su = sysUserService.findByName(securityHelper.getLoggedInUser());

        if (su == null)
            throw new SecurityException("Not authorised");

        Company company = su.getCompany();
        String fortress = inputBean.getFortress();
        // ToDo: Improve cypher query
        Fortress iFortress = companyService.getCompanyFortress(company, fortress);
        if (iFortress == null)
            throw new IllegalArgumentException("Unable to find the fortress [" + fortress + "] for the company [" + company.getName() + "]");

        AuditHeader ah = null;

        // Idempotent check
        if (inputBean.getCallerRef() != null)
            ah = findByCallerRef(iFortress.getId(), inputBean.getDocumentType(), inputBean.getCallerRef());

        if (ah != null) {
            if (logger.isDebugEnabled())
                logger.debug("Existing auditHeader record found by Caller Ref [" + inputBean.getCallerRef() + "] found [" + ah.getAuditKey() + "]");
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

        if (logger.isDebugEnabled())
            logger.debug("Audit Header created:" + ah.getId() + " key=[" + ah.getAuditKey() + "]");


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

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public AuditHeader getHeader(@NotEmpty String key) {
        return getHeader(key, false);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public AuditHeader getHeader(@NotEmpty String key, boolean inflate) {
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
     * Looks up the auditHeader from input and creates a log record
     *
     * @param input log details
     * @return populated log information with any error messages
     */
    public AuditLogInputBean createLog(AuditLogInputBean input) {
        String auditKey = input.getAuditKey();
        AuditHeader header;

        if (auditKey == null || auditKey.equals("")) {
            header = findByCallerRef(input.getFortress(), input.getDocumentType(), input.getCallerRef());
            if (header != null)
                input.setAuditKey(header.getAuditKey());
        } else
            header = getHeader(input.getAuditKey(), true);

        if (header == null) {
            input.setStatus(AuditLogInputBean.LogStatus.NOT_FOUND);
            return input;
        }

        return createLog(header, input, header.getTagMap());
    }

    /**
     * Creates an audit log record for the supplied auditHeader from the supplied input
     *
     * @param auditHeader auditHeader the caller is authorised to work with
     * @param input       auditLog details containing the data to log
     * @return populated log information with any error messages
     */
    private AuditLogInputBean createLog(AuditHeader auditHeader, AuditLogInputBean input, Map<String, Object> tagValues) {
        if (input.getMapWhat() == null || input.getMapWhat().isEmpty()) {
            input.setStatus(AuditLogInputBean.LogStatus.IGNORE);
            return input;
        }

        try {
            // Normalise and JSON'ise the what argument that has probably just been
            //  placed in to the instance variable
            input.setWhat(input.getWhat());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to pass what text as JSON object", e);
        }

        Fortress fortress = auditHeader.getFortress();
        FortressUser fUser = fortressService.getFortressUser(fortress, input.getFortressUser().toLowerCase(), true);

// Transactions checks
        TxRef txRef = handleTxRef(input);

//ToDo: Look at spin the following off in to a separate thread?
        // https://github.com/monowai/auditbucket/issues/7
        if (auditHeader.getLastChange() != null) {
            auditDAO.fetch(auditHeader.getLastChange());
        }
        AuditChange lastChange = (auditHeader.getLastChange() != null ? auditHeader.getLastChange() : null);
        String event = input.getEvent();
        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(DateTimeZone.UTC) : new DateTime(input.getWhen(), DateTimeZone.UTC));

        if (lastChange != null) {
            // Neo4j won't store the map, so we store the raw escaped JSON text
            try {
                // KVStore.getWhat()
                if (isSame(lastChange.getJsonWhat(), input.getWhat())) {
                    if (logger.isDebugEnabled())
                        logger.debug("Ignoring a change we already have");
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
            if (searchActive)
                auditHeader = waitOnHeader(auditHeader);

        } else { // first ever log for the auditHeader
            if (event == null) {
                event = AuditChange.CREATE;
                input.setEvent(event);
            }
        }

        AuditChange change = auditDAO.save(fUser, input, txRef, lastChange);

        auditHeader.setLastChange(change);
        auditHeader.setLastUser(fUser);
        auditHeader = auditDAO.save(auditHeader);

        AuditLog auditLog = auditDAO.addLog(auditHeader, change, fortressWhen);
        input.setStatus(AuditLogInputBean.LogStatus.OK);

        handleSearch(auditHeader, input, tagValues, searchActive, fortressWhen, auditLog);

        return input;

    }

    @Async
    private void handleSearch(AuditHeader auditHeader, AuditLogInputBean input, Map<String, Object> tagValues, Boolean searchActive, DateTime fortressWhen, AuditLog auditLog) {

        SearchChange searchDocument;
        searchDocument = new AuditSearchChange(auditHeader, input.getMapWhat(), input.getEvent(), fortressWhen);
        searchDocument.setTagValues(tagValues);

        if (logger.isTraceEnabled()) {
            try {
                logger.trace(om.writeValueAsString(searchDocument));
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage());
            }
        }

        if (searchActive && !auditHeader.isSearchSuppressed())
            makeChangeSearchable(searchDocument, auditLog);
    }

    private void makeChangeSearchable(SearchChange sd, AuditLog log) {
        sd.setSysWhen(log.getSysWhen());
        // Used to reconcile that the change was actually indexed
        sd.setLogId(log.getId());
        searchGateway.makeChangeSearchable(sd);
    }

    private AuditHeader waitOnHeader(AuditHeader auditHeader) {

        int timeOut = 100;
        int i = 0;
        if (auditHeader.isSearchSuppressed() || auditHeader.getSearchKey() != null)
            return auditHeader; // Nothing to wait for as we're suppressing searches for this auditHeader

        while (auditHeader.getSearchKey() == null && i < timeOut) {
            i++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            auditHeader = getHeader(auditHeader.getId());
        }
        if (auditHeader.getSearchKey() == null)
            logger.error("Timeout waiting for the initial search document to be created " + auditHeader.getAuditKey());
        return auditHeader;

    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    private AuditHeader getHeader(Long id) {
        return auditDAO.getHeader(id);
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
    public Future<Void> handleSearchResult(SearchResult searchResult) {
        String auditKey = searchResult.getAuditKey();
        if (logger.isTraceEnabled())
            logger.trace("Updating from search auditKey =[" + searchResult + "]");
        AuditHeader header = auditDAO.findHeader(auditKey);

        if (header == null) {
            logger.error("Audit Key could not be found for [" + searchResult + "]");
            return null;
        }
        if (header.getSearchKey() == null) {
            header.setSearchKey(searchResult.getSearchKey());
            auditDAO.save(header);
            if (logger.isTraceEnabled())
                logger.trace("Updated from search auditKey =[" + searchResult + "]");
        }
        AuditLog when = auditDAO.getChange(searchResult.getLogId());
        // Another thread may have processed this so save an update
        if (when != null && !when.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            if (logger.isDebugEnabled())
                logger.debug("Updating index status for " + when);
            when.setIsIndexed();
            auditDAO.save(when);

        } else {
            logger.info("Skipping " + when);
        }
        return null;
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

    public void updateHeader(AuditHeader auditHeader) {
        auditDAO.save(auditHeader);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public AuditLog getLastAuditLog(String headerKey) {
        AuditHeader ah = getValidHeader(headerKey);
        return getLastAuditLog(ah);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public AuditLog getLastAuditLog(AuditHeader auditHeader) {
        return auditDAO.getLastAuditLog(auditHeader.getId());
    }

    public Set<AuditLog> getAuditLogs(String headerKey) {
        securityHelper.isValidUser();
        AuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getAuditLogs(auditHeader.getId());
    }

    public Set<AuditLog> getAuditLogs(String headerKey, Date from, Date to) {
        securityHelper.isValidUser();
        AuditHeader auditHeader = getValidHeader(headerKey);
        return getAuditLogs(auditHeader, from, to);
    }

    protected Set<AuditLog> getAuditLogs(AuditHeader auditHeader, Date from, Date to) {
        return auditDAO.getAuditLogs(auditHeader.getId(), from, to);
    }

    /**
     * blocks until the header has been cancelled
     *
     * @param headerKey UID of the Header
     * @return AuditHeader
     * @throws IOException
     */
    public AuditHeader cancelLastLogSync(String headerKey) throws IOException {
        Future<AuditHeader> futureHeader = cancelLastLog(headerKey);
        try {
            return futureHeader.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * This could be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no AuditChange records left, then the auditHeader will also be removed and the
     * AB headerKey will be forever invalid.
     *
     * @param headerKey UID of the auditHeader
     * @return Future<AuditHeader> record or null if no auditHeader exists.
     */
    @Async
    public Future<AuditHeader> cancelLastLog(String headerKey) throws IOException {
        AuditHeader auditHeader = getValidHeader(headerKey, true);
        AuditChange lastChange = auditHeader.getLastChange();
        if (lastChange == null)
            return null;
        auditDAO.fetch(lastChange);
        AuditChange previousChange = lastChange.getPreviousChange();
        auditHeader.setLastChange(previousChange);

        if (previousChange != null) {
            auditDAO.fetch(previousChange);
            auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), previousChange.getWho().getName()));
        } //else {
        // No changes left
        // What to to? Delete the auditHeader? Store the "canceled By" User? Assign the log to a Cancelled RLX?
        //}
        auditHeader = auditDAO.save(auditHeader);

        if (previousChange == null)
            // Nothing to index, no changes left so we're done
            return new AsyncResult<>(auditHeader);

        // Sync the update to ab-search.
        if (auditHeader.getFortress().isSearchActive() && !auditHeader.isSearchSuppressed()) {
            // Update against the Audit Header only by re-indexing the search document
            searchGateway.makeChangeSearchable(new AuditSearchChange(auditHeader, previousChange.getWhat(), previousChange.getEvent(), new DateTime(previousChange.getAuditLog().getFortressWhen())));
        }
        return new AsyncResult<>(auditHeader);
    }

    /**
     * counts the number of audit logs that exist for the given auditHeader
     *
     * @param headerKey GUID
     * @return count
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public int getAuditLogCount(String headerKey) {
        AuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getLogCount(auditHeader.getId());

    }

    private AuditHeader getValidHeader(String headerKey) {
        return getValidHeader(headerKey, false);
    }

    private AuditHeader getValidHeader(String headerKey, boolean infalte) {
        AuditHeader header = auditDAO.findHeader(headerKey, infalte);
        if (header == null) {
            throw new IllegalArgumentException("No audit auditHeader for [" + headerKey + "]");
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

    /**
     * inflates the search result with dependencies populated
     *
     * @param fortressID   PK
     * @param documentType Class of doc
     * @param callerRef    fortress PK
     * @return inflated header
     */
    public AuditHeader findByCallerRefFull(Long fortressID, String documentType, String callerRef) {
        AuditHeader result = findByCallerRef(fortressID, documentType, callerRef);
        if (result != null) {
            auditDAO.fetch(result);
            auditDAO.fetch(result.getLastChange());

        }
        return result;
    }

    /**
     * @param fortressID   fortress to search
     * @param documentType class of document
     * @param callerRef    fortress primary key
     * @return AuditHeader or NULL.
     */
    public AuditHeader findByCallerRef(Long fortressID, String documentType, String callerRef) {
        String userName = securityHelper.getLoggedInUser();

        SystemUser su = sysUserService.findByName(userName);
        if (su == null)
            throw new SecurityException(userName + " is not authorised");

        Fortress fortress = fortressService.getFortress(fortressID);
        if (!fortress.getCompany().getId().equals(su.getCompany().getId()))
            throw new SecurityException(userName + " is not authorised to work with requested FortressNode");

        return auditDAO.findHeaderByCallerRef(fortress.getId(), documentType, callerRef.trim());
    }
}
