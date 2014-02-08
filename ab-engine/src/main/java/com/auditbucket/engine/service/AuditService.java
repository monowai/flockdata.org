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

import com.auditbucket.audit.bean.*;
import com.auditbucket.audit.model.*;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.helper.AuditException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.*;
import com.auditbucket.search.model.AuditSearchChange;
import com.auditbucket.search.model.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: Mike Holdsworth
 * Date: 8/04/13
 * To change this template use File | Settings | File Templates.
 */

@Service(value = "ab.AuditService")
@Transactional
public class AuditService {
    private static final String EMPTY = "";
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
    private static final ObjectMapper om = new ObjectMapper();

    @Autowired
    private KeyGenService keyGenService;

    public AuditWhat getWhat(AuditHeader auditHeader, AuditChange change) {
        return whatService.getWhat(auditHeader, change);
    }


    TxRef beginTransaction() {
        return beginTransaction(keyGenService.getUniqueKey());
    }

    TxRef beginTransaction(String id) {
        Company company = securityHelper.getCompany();
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
    public AuditResultBean createHeader(AuditHeaderInputBean inputBean, Company company, Fortress fortress) throws AuditException {
        DocumentType documentType;
        documentType = tagService.resolveDocType(company, inputBean.getDocumentType());

        // Create thisFortressUser if missing
        FortressUser fu = fortressService.getFortressUser(fortress, inputBean.getFortressUser(), true);
        fu.setFortress(fortress);// Save fetching it twice

        AuditHeader ah = null;
        if (inputBean.getAuditKey() == null && inputBean.getCallerRef() != null && !inputBean.getCallerRef().equals(EMPTY))
            ah = findByCallerRef(fortress, documentType, inputBean.getCallerRef());

        if (ah != null) {
            logger.debug("Existing auditHeader record found by Caller Ref [{}] found [{}]", inputBean.getCallerRef(), ah.getAuditKey());
            inputBean.setAuditKey(ah.getAuditKey());

            AuditResultBean arb = new AuditResultBean(ah);
            arb.setWasDuplicate();

            return arb;
        }

        ah = makeAuditHeader(inputBean, fu, documentType);
        return new AuditResultBean(ah);

    }

    private AuditHeader makeAuditHeader(AuditHeaderInputBean inputBean, FortressUser fu, DocumentType documentType) throws AuditException {
        inputBean.setAuditKey(keyGenService.getUniqueKey());
        AuditHeader ah = auditDAO.create(inputBean, fu, documentType);
        if (ah.getId() == null)
            inputBean.setAuditKey("NT " + fu.getFortress().getFortressKey()); // Audit ain't tracking this
        logger.debug("Audit Header created:{} key=[{}]", ah.getId(), ah.getAuditKey());
        return ah;
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
            throw new IllegalArgumentException("Unable to resolve requested audit key [" + key + "]");

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
    public AuditLogResultBean createLog(AuditHeader header, AuditLogInputBean input) {
        AuditLogResultBean resultBean = new AuditLogResultBean(input);

        if (header == null) {
            String auditKey = input.getAuditKey();
            if (input.getAuditId() == null) {
                if (auditKey == null || auditKey.equals(EMPTY)) {
                    header = findByCallerRef(input.getFortress(), input.getDocumentType(), input.getCallerRef());
                    if (header != null)
                        input.setAuditKey(header.getAuditKey());
                } else
                    header = getHeader(auditKey); // true??
            } else
                header = getHeader(input.getAuditId());  // Only set internally by AuditBucket. Never rely on the caller
        }

        if (header == null) {
            resultBean.setStatus(AuditLogInputBean.LogStatus.NOT_FOUND);
            resultBean.setMessage("Unable to locate requested header");
            return resultBean;
        }
        FortressUser thisFortressUser = fortressService.getFortressUser(header.getFortress(), input.getFortressUser(), true);
        return createLog(header, input, thisFortressUser);
    }

    /**
     * Creates an audit log record for the supplied auditHeader from the supplied input
     *
     * @param auditHeader      auditHeader the caller is authorised to work with
     * @param input            auditLog details containing the data to log
     * @param thisFortressUser audit header tag set
     * @return populated log information with any error messages
     */
    public AuditLogResultBean createLog(AuditHeader auditHeader, AuditLogInputBean input, FortressUser thisFortressUser) {
        // Warning - making this private means it doesn't get a transaction!
        AuditLogResultBean resultBean = new AuditLogResultBean(input);
        //ToDo: May want to track a "View" event which would not change the What data.
        if (input.getMapWhat() == null || input.getMapWhat().isEmpty()) {
            resultBean.setStatus(AuditLogInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No 'what' information provided. Ignoring this request");
            return resultBean;
        }

        Fortress fortress = auditHeader.getFortress();

        // Transactions checks
        TxRef txRef = handleTxRef(input);
        resultBean.setTxReference(txRef);

        //ToDo: Look at spin the following off in to a separate thread?
        // https://github.com/monowai/auditbucket/issues/7
        AuditLog existingLog = null;
        if (auditHeader.getLastUpdated() != auditHeader.getWhenCreated()) // Will there even be a change to find
            existingLog = getLastAuditLog(auditHeader);//(auditHeader.getLastChange() != null ? auditHeader.getLastChange() : null);

        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(input.getWhen()));

        if (existingLog != null) {
            try {
                if (whatService.isSame(auditHeader, existingLog.getAuditChange(), input.getWhat())) {
                    logger.trace("Ignoring a change we already have {}", input);
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

        AuditEvent event = auditEventService.processEvent(fortress.getCompany(), input.getEvent());
        input.setAuditEvent(event);
        AuditChange thisChange = auditDAO.save(thisFortressUser, input, txRef, existingChange);
        int version = 0;
//        if (existingChange != null) {
//            version = whatService.getWhat(auditHeader, existingChange).getVersion();
//        }

        whatService.logWhat(auditHeader, thisChange, input.getWhat(), version);

        AuditLog newLog = auditDAO.addLog(auditHeader, thisChange, fortressWhen);
        boolean moreRecent = (existingChange == null || existingLog.getFortressWhen() <= newLog.getFortressWhen());

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

    public Set<AuditHeader> getAuditHeaders(Fortress fortress, Long skipTo) {
        return auditDAO.findHeaders(fortress.getId(), skipTo);
    }

    public Set<AuditHeader> getAuditHeaders(Fortress fortress, String docTypeName, Long skipTo) {
        DocumentType docType = tagService.resolveDocType(fortress.getCompany(), docTypeName);
        return auditDAO.findHeaders(fortress.getId(), docType.getId(), skipTo);
    }

    private SearchChange prepareSearchDocument(AuditHeader auditHeader, AuditLogInputBean logInput, AuditEvent event, Boolean searchActive, DateTime fortressWhen, AuditLog auditLog) throws JsonProcessingException {

        if (!searchActive || auditHeader.isSearchSuppressed())
            return null;
        SearchChange searchDocument;
        searchDocument = new AuditSearchChange(auditHeader, logInput.getMapWhat(), event.getCode(), fortressWhen);
        searchDocument.setWho(auditLog.getAuditChange().getWho().getCode());
        searchDocument.setTags(auditTagService.findAuditTags(auditHeader.getFortress().getCompany(), auditHeader));
        searchDocument.setDescription(auditHeader.getName());
        try {
            logger.trace("JSON {}", om.writeValueAsString(searchDocument));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw (e);
        }
        if (auditLog != null && auditLog.getSysWhen() != 0)
            searchDocument.setSysWhen(auditLog.getSysWhen());
        else
            searchDocument.setSysWhen(auditHeader.getWhenCreated());

        // Used to reconcile that the change was actually indexed
        searchDocument.setLogId(auditLog.getId());
        return searchDocument;
    }

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

    @Cacheable(value = "auditHeaderId")
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
    public void handleSearchResult(SearchResult searchResult) {

        logger.debug("Updating from search auditKey =[{}]", searchResult);
        Long auditId = searchResult.getAuditId();
        if (auditId == null)
            return;
        AuditHeader header = auditDAO.getHeader(auditId);

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

    TxRef findTx(String txRef, boolean fetchHeaders) {
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
        TxRef txRef = findTx(txName);
        if (txRef == null)
            return null;
        return auditDAO.findHeadersByTxRef(txRef.getId());
    }

    public void updateHeader(AuditHeader auditHeader) {
        auditDAO.save(auditHeader);
    }

    public AuditLog getLastAuditLog(String headerKey) throws AuditException {
        AuditHeader ah = getValidHeader(headerKey);
        return getLastAuditLog(ah);
    }

    public AuditLog getLastAuditLog(AuditHeader auditHeader) {
        return auditDAO.getLastAuditLog(auditHeader.getId());
    }

    public Set<AuditLog> getAuditLogs(String headerKey) throws AuditException {
        securityHelper.isValidUser();
        AuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getAuditLogs(auditHeader.getId());
    }

    public Set<AuditLog> getAuditLogs(String headerKey, Date from, Date to) throws AuditException {
        securityHelper.isValidUser();
        AuditHeader auditHeader = getValidHeader(headerKey);
        return getAuditLogs(auditHeader, from, to);
    }

    Set<AuditLog> getAuditLogs(AuditHeader auditHeader, Date from, Date to) {
        return auditDAO.getAuditLogs(auditHeader.getId(), from, to);
    }

    /**
     * blocks until the header has been cancelled
     *
     * @param headerKey UID of the Header
     * @return AuditLogResultBean
     * @throws IOException
     */
    public AuditHeader cancelLastLogSync(String headerKey) throws IOException, AuditException {
        Future<AuditHeader> futureHeader = cancelLastLog(headerKey);
        try {
            return futureHeader.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            throw new AuditException("This is bad - Interrupted Exception ", e);
        }
    }

    /**
     * This could be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no AuditChange records left, then the auditHeader will also be removed and the
     * AB headerKey will be forever invalid.
     *
     * @param headerKey UID of the auditHeader
     * @return Future<AuditLogResultBean> record or null if no auditHeader exists.
     */
    @Async
    Future<AuditHeader> cancelLastLog(String headerKey) throws IOException, AuditException {
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
            auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), priorChange.getWho().getCode()));
            auditHeader = auditDAO.save(auditHeader);
            whatService.delete(auditHeader, currentChange);
            auditDAO.delete(currentChange);
        } else if (currentChange != null) {
            // No changes left, there is now just a header
            // What to to? Delete the auditHeader? Store the "canceled By" User? Assign the log to a Cancelled RLX?
            // ToDo: Delete from ElasticSearch??
            auditHeader.setLastUser(fortressService.getFortressUser(auditHeader.getFortress(), auditHeader.getCreatedBy().getCode()));
            auditHeader = auditDAO.save(auditHeader);
            whatService.delete(auditHeader, currentChange);
            auditDAO.delete(currentChange);
        }

        if (priorChange == null)
            // Nothing to index, no changes left so we're done
            return new AsyncResult<>(auditHeader);

        // Sync the update to ab-search.
        if (auditHeader.getFortress().isSearchActive() && !auditHeader.isSearchSuppressed()) {
            // Update against the Audit Header only by re-indexing the search document
            Map<String, Object> priorWhat = whatService.getWhat(auditHeader, priorChange).getWhatMap();
            AuditSearchChange searchDocument = new AuditSearchChange(auditHeader, priorWhat, priorChange.getEvent().getCode(), new DateTime(priorChange.getAuditLog().getFortressWhen()));
            searchDocument.setTags(auditTagService.findAuditTags(auditHeader));
            searchGateway.makeChangeSearchable(searchDocument);
        }
        return new AsyncResult<>(auditHeader);
    }

    public void rebuild(AuditHeader auditHeader) {
        try {
            AuditLog lastLog = getLastLog(auditHeader.getId());
            AuditChange lastChange = null;
            if (lastLog != null)
                lastChange = lastLog.getAuditChange();
            else {
                // ToDo: This will not work for meta-data index headers. Work loop also needs looking at
                logger.info("No last change for {}, ignoring the re-index request for this record" + auditHeader.getCallerRef());
            }

            if (auditHeader.getFortress().isSearchActive() && !auditHeader.isSearchSuppressed()) {
                // Update against the Audit Header only by re-indexing the search document
                Map<String, Object> lastWhat;
                if (lastChange != null)
                    lastWhat = whatService.getWhat(auditHeader, lastChange).getWhatMap();
                else
                    return; // ToDo: fix reindex header only scenario, i.e. no "change/what"

                AuditSearchChange searchDocument = new AuditSearchChange(auditHeader, lastWhat, lastChange.getEvent().getCode(), new DateTime(lastChange.getAuditLog().getFortressWhen()));
                searchDocument.setTags(auditTagService.findAuditTags(auditHeader));
                searchDocument.setReplyRequired(false);
                searchGateway.makeChangeSearchable(searchDocument);
            }
        } catch (Exception e) {
            logger.error("error", e);
        }

    }

    /**
     * counts the number of audit logs that exist for the given auditHeader
     *
     * @param headerKey GUID
     * @return count
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public int getAuditLogCount(String headerKey) throws AuditException {
        AuditHeader auditHeader = getValidHeader(headerKey);
        return auditDAO.getLogCount(auditHeader.getId());

    }

    private AuditHeader getValidHeader(String headerKey) throws AuditException {
        return getValidHeader(headerKey, false);
    }

    private AuditHeader getValidHeader(String headerKey, boolean inflate) throws AuditException {
        AuditHeader header = auditDAO.findHeader(headerKey, inflate);
        if (header == null) {
            throw new AuditException("No audit auditHeader for [" + headerKey + "]");
        }
        String userName = securityHelper.getLoggedInUser();
        SystemUser sysUser = sysUserService.findByName(userName);

        if (!header.getFortress().getCompany().getId().equals(sysUser.getCompany().getId())) {
            throw new SecurityException("Not authorised to work with this audit record");
        }
        return header;

    }

    public AuditHeader findByCallerRef(String fortress, String documentType, String callerRef) {
        Fortress iFortress = fortressService.findByName(fortress);
        if (iFortress == null)
            return null;

        return findByCallerRef(iFortress, documentType, callerRef);
    }

    public AuditHeader findByCallerRefFull(Long fortressId, String documentType, String callerRef) {
        Fortress fortress = fortressService.getFortress(fortressId);
        return findByCallerRefFull(fortress, documentType, callerRef);

    }

    /**
     * \
     * inflates the search result with dependencies populated
     *
     * @param fortress
     * @param documentType Class of doc
     * @param callerRef    fortress PK
     * @return inflated header
     */
    public AuditHeader findByCallerRefFull(Fortress fortress, String documentType, String callerRef) {
        return findByCallerRef(fortress, documentType, callerRef);
    }

    @Async
    private Future<AuditHeader> findByCallerRefFuture(Fortress fortress, String documentType, String callerRef) {
        try {
            AuditHeader auditHeader = findByCallerRef(fortress, documentType, callerRef);
            return new AsyncResult<>(auditHeader);
        } catch (Exception e) {
            logger.error("Caller Reference ", e);
        }
        return new AsyncResult<>(null);
    }

    public AuditHeader findByCallerRef(Fortress fortress, String documentType, String callerRef) {
        DocumentType doc = tagService.resolveDocType(fortress.getCompany(), documentType, false);
        if (doc == null)
            return null;
        return findByCallerRef(fortress, doc, callerRef);

    }

    /**
     * @param fortress
     * @param documentType class of document
     * @param callerRef    fortress primary key
     * @return AuditLogResultBean or NULL.
     */
    public AuditHeader findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef) {
        return auditDAO.findHeaderByCallerRef(fortress.getId(), documentType.getId(), callerRef.toLowerCase().trim());
    }

    private AuditLog getLastLog(Long headerId) {
        return auditDAO.getLastLog(headerId);
    }

    public Set<AuditLog> getAuditLogs(Long headerId) {
        return auditDAO.getAuditLogs(headerId);
    }

    public AuditSummaryBean getAuditSummary(String auditKey) throws AuditException {
        AuditHeader header = getHeader(auditKey, true);
        if ( header == null )
            throw new AuditException("Invalid Audit Key ["+auditKey+"]");
        Set<AuditLog> changes = getAuditLogs(header.getId());
        Set<AuditTag> tags = auditTagService.findAuditTags(header);
        return new AuditSummaryBean(header, changes, tags);
    }

    @Async
    public void makeHeaderSearchable(AuditResultBean resultBean, String event, Date when, Company company) {
        AuditHeader header = resultBean.getAuditHeader();
        if (header.isSearchSuppressed() || !header.getFortress().isSearchActive())
            return ;

        SearchChange searchDocument = getSearchChange(resultBean, event, when, company);
        if (searchDocument == null) return;
        makeChangeSearchable(searchDocument);

    }

    public SearchChange getSearchChange(AuditResultBean resultBean, String event, Date when, Company company) {
        AuditHeader header = resultBean.getAuditHeader();

        fortressService.fetch(header.getLastUser());
        SearchChange searchDocument = new AuditSearchChange(header, null, event, new DateTime(when));
        if (resultBean.getTags() != null) {
            searchDocument.setTags(resultBean.getTags());
            searchDocument.setReplyRequired(false);
            searchDocument.setSearchKey(header.getCallerRef());
            if (header.getId() == null)
                searchDocument.setWhen(null);
            searchDocument.setSysWhen(header.getWhenCreated());

        } else {
            searchDocument.setTags(auditTagService.findAuditTags(company, header));
        }
        return searchDocument;
    }

    public AuditLog getLastLog(String auditKey) throws AuditException {
        AuditHeader audit = getValidHeader(auditKey);
        return getLastLog(audit.getId());

    }

    public AuditLog getLastLog(AuditHeader audit) throws AuditException {
//        AuditHeader audit = getValidHeader(auditKey);
        return getLastLog(audit.getId());

    }

    public AuditLogDetailBean getFullDetail(String auditKey, Long logId) {
        AuditHeader auditHeader = getHeader(auditKey, true);
        if (auditHeader == null)
            return null;

        AuditLog log = auditDAO.getLog(logId);
        auditDAO.fetch(log.getAuditChange());
        AuditWhat what = whatService.getWhat(auditHeader, log.getAuditChange());
        log.getAuditChange().setWhat(what);
        return new AuditLogDetailBean(log, what);
    }

    public AuditLog getAuditLog(AuditHeader header, Long logId) {
        if (header != null) {

            AuditLog log = auditDAO.getLog(logId);
            if (!log.getAuditHeader().getId().equals(header.getId()))
                return null;

            auditDAO.fetch(log.getAuditChange());
            return log;
        }
        return null;
    }

    /**
     * Typically called only for regression test purposes
     *
     * @param resultBean Audit to work with
     * @param event      descriptor of last event
     * @param when       date fortress is saying this took place
     * @return           populated search doc
     */
    public SearchChange getSearchChange(AuditResultBean resultBean, String event, Date when) {
        Company company = securityHelper.getCompany();
        return getSearchChange(resultBean, event, when, company)   ;
    }
}
