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
import com.auditbucket.bean.*;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
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
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    public static final String EMPTY = "";
    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    AuditEventService auditEventService;

    @Autowired
    SystemUserService sysUserService;

    @Autowired
    AuditTagService auditTagService;

    @Autowired
    WhatService whatService;

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

    public AuditWhat getWhat(AuditChange change) {
        return whatService.getWhat(change);
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
        // ToDo: Improve cypher query
        Fortress iFortress = companyService.getCompanyFortress(company, inputBean.getFortress());
        if (iFortress == null)
            throw new IllegalArgumentException("Unable to find the fortress [" + inputBean.getFortress() + "] for the company [" + company.getName() + "]");

        Future<AuditHeader> futureHeader = null;
        if (inputBean.getCallerRef() != null && !inputBean.getCallerRef().equals(EMPTY))
            futureHeader = findByCallerRefFuture(iFortress.getId(), inputBean.getDocumentType(), inputBean.getCallerRef());

        // Create fortressUser if missing
        FortressUser fu = fortressService.getFortressUser(iFortress, inputBean.getFortressUser(), true);
        fu.getFortress().setCompany(su.getCompany());

        AuditHeader ah = null;

        try {
            if (futureHeader != null)
                ah = futureHeader.get();
        } catch (InterruptedException e) {
            logger.error("waiting for future result", e);
            return null;
        } catch (ExecutionException e) {
            logger.error("waiting for future result", e);
            return null;
        }

        if (ah != null) {
            logger.debug("Existing auditHeader record found by Caller Ref [{}] found [{}]", inputBean.getCallerRef(), ah.getAuditKey());
            inputBean.setAuditKey(ah.getAuditKey());

            AuditResultBean arb = new AuditResultBean(ah);
            arb.setStatus("Existing audit record found and is being returned");
            return arb;
        }

        DocumentType documentType = tagService.resolveDocType(inputBean.getDocumentType());
        // Future from here on.....
        ah = auditDAO.create(fu, inputBean, documentType);
        handleTags(ah, inputBean.getTagValues());

        logger.debug("Audit Header created:{} key=[{}]", ah.getId(), ah.getAuditKey());
        inputBean.setWhen(new Date(ah.getFortressDateCreated()));
        inputBean.setAuditKey(ah.getAuditKey());
        return new AuditResultBean(ah);

    }

    @Async
    private Future<Void> handleTags(AuditHeader ah, Map<String, String> tagValues) {
        auditTagService.createTagValues(tagValues, ah);
        return null;
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
            throw new SecurityException(userName + " Not authorised to retrieve headers");

        AuditHeader ah = auditDAO.findHeader(key, inflate);
        if (ah == null)
            throw new IllegalArgumentException("Unable to find key [" + key + "]");

        if (!(ah.getFortress().getCompany().getId().equals(su.getCompany().getId())))
            throw new SecurityException("CompanyNode mismatch. [" + su.getName() + "] working for [" + su.getCompany().getName() + "] cannot write audit records for [" + ah.getFortress().getCompany().getName() + "]");
        return ah;
    }

    /**
     * Looks up the auditHeader from input and creates a log record
     * <p/>
     * Only public to support AOP transactions. You should be calling this via #AuditManagerService.createLog
     *
     * @param input log details
     * @return populated log information with any error messages
     */
    public AuditLogResultBean createLog(AuditLogInputBean input) {
        AuditHeader header;
        AuditLogResultBean resultBean = new AuditLogResultBean(input);
        String auditKey = input.getAuditKey();

        if (input.getAuditId() == null) {
            if (auditKey == null || auditKey.equals(EMPTY)) {
                header = findByCallerRef(input.getFortress(), input.getDocumentType(), input.getCallerRef());
                if (header != null)
                    input.setAuditKey(header.getAuditKey());
            } else
                header = getHeader(auditKey, true);
        } else
            header = getHeader(input.getAuditId());  // Only set internally by AuditBucket. Never rely on the caller

        if (header == null) {
            resultBean.setStatus(AuditLogInputBean.LogStatus.NOT_FOUND);
            resultBean.setMessage("Unable to locate requested header");
            return resultBean;
        }

        return createLog(header, input, header.getTagMap());
    }

    /**
     * Creates an audit log record for the supplied auditHeader from the supplied input
     *
     * @param auditHeader auditHeader the caller is authorised to work with
     * @param input       auditLog details containing the data to log
     * @param tagValues   audit header tag set
     * @return populated log information with any error messages
     */
    private AuditLogResultBean createLog(AuditHeader auditHeader, AuditLogInputBean input, Map<String, String> tagValues) {
        AuditLogResultBean resultBean = new AuditLogResultBean(input);
        if (input.getMapWhat() == null || input.getMapWhat().isEmpty()) {
            resultBean.setStatus(AuditLogInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No 'what' information provided. Ignoring this request");
            return resultBean;
        }

        try {
            // Normalise and JSON'ise the what argument that has probably just been
            //  placed in to the instance variable
            input.setWhat(input.getWhat());
        } catch (IOException e) {
            logger.error("Json parsing exception {}", input.getWhat());
            throw new IllegalArgumentException("Unable to pass What text as JSON object", e);
        }

        Fortress fortress = auditHeader.getFortress();
        FortressUser thisFortressUser = fortressService.getFortressUser(fortress, input.getFortressUser().toLowerCase(), true);

// Transactions checks
        TxRef txRef = handleTxRef(input);
        resultBean.setTxReference(txRef);

//ToDo: Look at spin the following off in to a separate thread?
        // https://github.com/monowai/auditbucket/issues/7
        AuditLog existingLog = null;
        if (auditHeader.getLastUpdated() != auditHeader.getWhenCreated()) // Will there even be a change to find
            existingLog = getLastAuditLog(auditHeader);//(auditHeader.getLastChange() != null ? auditHeader.getLastChange() : null);

        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(fortress.getTimeZone()) : new DateTime(input.getWhen()));

        if (existingLog != null) {
            // Neo4j won't store the map, so we store the raw escaped JSON text
            try {
                // KVStore.getWhat()
                if (whatService.isSame(existingLog.getAuditChange(), input.getWhat())) {
                    logger.debug("Ignoring a change we already have {}", input);
                    input.setStatus(AuditLogInputBean.LogStatus.IGNORE);
                    if (input.isForceReindex()) { // Caller is recreating the search index
                        prepareSearchDocument(auditHeader, input, existingLog.getAuditChange().getEvent(), searchActive, fortressWhen, existingLog);
                        resultBean.setMessage("Ignoring a change we already have. Honouring request to re-index");
                    } else
                        resultBean.setMessage("Ignoring a change we already have");
                    return resultBean;
                }
            } catch (IOException e) {
                input.setStatus(AuditLogInputBean.LogStatus.ILLEGAL_ARGUMENT);
                resultBean.setMessage("Error comparing JSON data: " + e.getMessage());
                logger.error("Error comparing JSON Data", e);
                return resultBean;
            }
            if (input.getEvent() == null) {
                input.setEvent(AuditChange.UPDATE);
            }
            if (searchActive)
                auditHeader = waitOnHeader(auditHeader);


        } else { // first ever log for the auditHeader
            if (input.getEvent() == null) {
                input.setEvent(AuditChange.CREATE);
            }
            auditHeader.setLastUser(thisFortressUser);
            auditHeader = auditDAO.save(auditHeader);
        }
        AuditChange existingChange = null;
        if (existingLog != null)
            existingChange = existingLog.getAuditChange();

        AuditEvent event = auditEventService.processEvent(input.getEvent());
        input.setAuditEvent(event);
        AuditChange thisChange = auditDAO.save(thisFortressUser, input, txRef, existingChange);

        whatService.logWhat(thisChange, input.getWhat());

        AuditLog newLog = auditDAO.addLog(auditHeader, thisChange, fortressWhen);
        boolean moreRecent = (existingChange == null || existingLog.getFortressWhen() < newLog.getFortressWhen());

        input.setStatus(AuditLogInputBean.LogStatus.OK);

        if (moreRecent) {
            if (!auditHeader.getLastUser().getId().equals(thisFortressUser.getId())) {
                auditHeader.setLastUser(thisFortressUser);
                auditDAO.save(auditHeader);
            }

            auditDAO.setLastChange(auditHeader, thisChange, existingChange);

            try {
                resultBean.setSearchDocument(prepareSearchDocument(auditHeader, input, event, searchActive, fortressWhen, newLog));
            } catch (JsonProcessingException e) {
                resultBean.setMessage("Error processing JSON document");
                resultBean.setStatus(AuditLogInputBean.LogStatus.ILLEGAL_ARGUMENT);
            }
        }
        return resultBean;

    }


    private SearchChange prepareSearchDocument(AuditHeader auditHeader, AuditLogInputBean logInput, AuditEvent event, Boolean searchActive, DateTime fortressWhen, AuditLog auditLog) throws JsonProcessingException {

        if (!searchActive || auditHeader.isSearchSuppressed())
            return null;
        SearchChange searchDocument;
        searchDocument = new AuditSearchChange(auditHeader, logInput.getMapWhat(), event.getCode(), fortressWhen);
        //searchDocument.setTags(getAuditTags(auditHeader.getId()));
        searchDocument.setWho(auditLog.getAuditChange().getWho().getName());

        try {
            logger.trace("JSON {}", om.writeValueAsString(searchDocument));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw (e);
        }
        searchDocument.setSysWhen(auditLog.getSysWhen());
        // Used to reconcile that the change was actually indexed
        searchDocument.setLogId(auditLog.getId());
        return searchDocument;
    }

    @Async
    @Transactional(propagation = Propagation.SUPPORTS)
    public void makeChangeSearchable(SearchChange searchDocument) {
        if (searchDocument == null)
            return;
        logger.debug("Indexing auditLog [{}]]", searchDocument);
        searchGateway.makeChangeSearchable(searchDocument);
    }

    private AuditHeader waitOnHeader(AuditHeader auditHeader) {

        if (auditHeader.isSearchSuppressed() || auditHeader.getSearchKey() != null)
            return auditHeader; // Nothing to wait for as we're suppressing searches for this auditHeader

        int timeOut = 100;
        int i = 0;

        while (auditHeader.getSearchKey() == null && i < timeOut) {
            i++;
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            auditHeader = getHeader(auditHeader.getId());
        }
        if (auditHeader.getSearchKey() == null)
            logger.error("Timeout waiting for the initial search document to be created [{}]", auditHeader.getAuditKey());
        return auditHeader;

    }

    //@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
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
    @Async
    @ServiceActivator(inputChannel = "searchResult")
    public void handleSearchResult(SearchResult searchResult) {

        logger.debug("Updating from search auditKey =[{}]", searchResult);
        AuditHeader header = auditDAO.getHeader(searchResult.getAuditId());

        if (header == null) {
            logger.error("Audit Key could not be found for [{}]", searchResult);
            return;
        }
        if (header.getSearchKey() == null) {
            header.setSearchKey(searchResult.getSearchKey());
            auditDAO.save(header);
            logger.trace("Updated from search auditKey =[{}]", searchResult);
        }

        if (searchResult.getLogId() == null) {
            // Indexing header meta data only
            return;
        }
        // The change has been indexed
        AuditLog when = auditDAO.getLog(searchResult.getLogId());
        if (when == null) {
            logger.error("Illegal node requested from handleSearchResult [{}]", searchResult.getLogId());
            return;
        }

        // Another thread may have processed this so save an update
        if (when != null && !when.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            logger.debug("Updating index status for {}", when);
            when.setIsIndexed();
            auditDAO.save(when);

        } else {
            logger.debug("Skipping {}", when);
        }
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
        AuditLog currentLog = getLastLog(auditHeader.getId());
        if (currentLog == null)
            return null;
        auditDAO.fetch(currentLog.getAuditChange());
        AuditChange currentChange = currentLog.getAuditChange();
        AuditChange priorChange = currentLog.getAuditChange().getPreviousChange();

        if (priorChange != null) {
            auditDAO.setLastChange(auditHeader, priorChange, currentChange);
            auditDAO.fetch(priorChange);
            auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), priorChange.getWho().getName()));
            auditHeader = auditDAO.save(auditHeader);
        } //else {
        // No changes left
        // What to to? Delete the auditHeader? Store the "canceled By" User? Assign the log to a Cancelled RLX?
        //}


        if (priorChange == null)
            // Nothing to index, no changes left so we're done
            return new AsyncResult<>(auditHeader);

        // Sync the update to ab-search.
        if (auditHeader.getFortress().isSearchActive() && !auditHeader.isSearchSuppressed()) {
            // Update against the Audit Header only by re-indexing the search document
            Map<String, Object> priorWhat = whatService.getWhat(priorChange).getWhatMap();
            searchGateway.makeChangeSearchable(new AuditSearchChange(auditHeader, priorWhat, priorChange.getEvent().getCode(), new DateTime(priorChange.getAuditLog().getFortressWhen())));
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

    private AuditHeader getValidHeader(String headerKey, boolean inflate) {
        AuditHeader header = auditDAO.findHeader(headerKey, inflate);
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
            //auditDAO.fetch(result.getLastChange());

        }
        return result;
    }

    @Async
    private Future<AuditHeader> findByCallerRefFuture(Long fortressId, String documentType, String callerRef) {
        AuditHeader auditHeader = findByCallerRef(fortressId, documentType, callerRef);
        return new AsyncResult<>(auditHeader);
    }

    /**
     * @param fortressID   fortress to search
     * @param documentType class of document
     * @param callerRef    fortress primary key
     * @return AuditHeader or NULL.
     */
    public AuditHeader findByCallerRef(Long fortressID, String documentType, String callerRef) {

        SystemUser su = sysUserService.findByName(securityHelper.getLoggedInUser());
        if (su == null)
            throw new SecurityException(securityHelper.getLoggedInUser() + " is not authorised");

        Fortress fortress = fortressService.getFortress(fortressID);
        if (!fortress.getCompany().getId().equals(su.getCompany().getId()))
            throw new SecurityException(securityHelper.getLoggedInUser() + " is not authorised to work with requested FortressNode");

        return auditDAO.findHeaderByCallerRef(fortress.getId(), documentType, callerRef.trim());
    }

    private AuditLog getLastLog(Long headerId) {
        return auditDAO.getLastLog(headerId);
    }

    public Set<AuditLog> getAuditLogs(Long headerId) {
        return auditDAO.getAuditLogs(headerId);
    }

    public AuditSummaryBean getAuditSummary(String auditKey) {
        AuditHeader header = getHeader(auditKey, true);
        Set<AuditLog> changes = getAuditLogs(header.getId());
        return new AuditSummaryBean(header, changes);
    }

    public Set<AuditTag> getAuditTags(Long id) {
        return auditTagService.findAuditTags(id);


    }

    @Async
    public void makeHeaderSearchable(AuditResultBean resultBean, String event, Date when) {
        AuditHeader header = getHeader(resultBean.getAuditId());
        if (header.isSearchSuppressed() || !header.getFortress().isSearchActive())
            return;

        auditDAO.fetch(header);
        if (when == null) {
            when = new DateTime(System.currentTimeMillis()).toDate();
        }

        SearchChange searchDocument = new AuditSearchChange(header, null, event, new DateTime(when));

        //Set<AuditTag>tagSet = getAuditTags(header.getId());
        //searchDocument.setTags(tagSet);
        makeChangeSearchable(searchDocument);

    }

    public AuditLog getLastLog(String auditKey) {
        AuditHeader audit = getValidHeader(auditKey);
        return getLastLog(audit.getId());

    }

    public AuditLogDetailBean getFullDetail(String auditKey, Long logId) {
        AuditHeader header = getHeader(auditKey, true);
        if (header == null)
            return null;

        AuditLog log = auditDAO.getLog(logId);
        auditDAO.fetch(log.getAuditChange());
        AuditWhat what = whatService.getWhat(log.getAuditChange());
        return new AuditLogDetailBean(log, what);
    }
}
