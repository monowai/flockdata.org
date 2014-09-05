/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.dao.TrackDao;
import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TxRef;
import com.auditbucket.track.service.SchemaService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 21/06/14
 * Time: 12:56 PM
 */
@Service
@Transactional
public class LogService {
    private Logger logger = LoggerFactory.getLogger(LogService.class);

    @Autowired
    private TxService txService;

    @Autowired
    private EngineConfig engineConfig;

    @Autowired
    private WhatService whatService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SearchServiceFacade searchService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    TrackDao trackDao;

    @Async
    public Future<Collection<TrackResultBean>> processLogs(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException, ExecutionException, InterruptedException {
        return new AsyncResult<>(processLogsSync(company, resultBeans));

    }

    public Collection<TrackResultBean> processLogsSync(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException, ExecutionException, InterruptedException {
        logger.debug("Process Logs {}", Thread.currentThread().getName());
        Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            logResults.add(processLogFromResult(resultBean));
        }
        logger.debug("Logs processed. Proceeding to distribute.");
        distributeChanges(company, logResults);
        return logResults;

    }

    public TrackResultBean processLogFromResult(TrackResultBean resultBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        //DAT-77 the header may still be committing in another thread
        TrackResultBean trackResult = null;
        if (resultBean.getLog() != null) {
            trackResult = writeTheLogAndDistributeChanges(resultBean);
        }
        return trackResult;
    }

    public TrackResultBean writeLog(MetaHeader metaHeader, LogInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException {
        TrackResultBean resultBean = new TrackResultBean(metaHeader);
        resultBean.setLogInput(input);
        return writeTheLogAndDistributeChanges(resultBean);
    }

    /**
     * Deadlock safe processor to creates a log
     *
     * @param resultBean details to write the log from. Will always contain a metaHeader
     * @return result details
     * @throws com.auditbucket.helper.DatagioException
     */
    public TrackResultBean writeTheLogAndDistributeChanges(final TrackResultBean resultBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        LogInputBean logInputBean = resultBean.getLog();
        logger.debug("writeLog {}", logInputBean);
        class DeadLockCommand implements Command {
            TrackResultBean result = null;

            @Override
            public Command execute() throws DatagioException, IOException, ExecutionException, InterruptedException {

                // ToDo: DAT-169 This needs to be dealt with via SpringIntegration and persistent messaging
                result = writeLog(resultBean);
                if (result.getLogResult().getStatus() == LogInputBean.LogStatus.NOT_FOUND)
                    throw new DatagioException("Unable to find MetaHeader ");
                whatService.doKvWrite(result); //ToDo: Consider KV not available. How to write the logs
                //      need to think of a way to recognize that the header has unprocessed work
                return this;
            }
        }
        DeadLockCommand c = new DeadLockCommand();
        DeadlockRetry.execute(c, "processing log for header", 20);
        return c.result;

    }


    /**
     *
     * @param trackResultBean input data to process
     * @return result of the operation
     * @throws DatagioException
     * @throws IOException
     */
    public TrackResultBean writeLog(TrackResultBean trackResultBean) throws DatagioException, IOException {
        LogInputBean input = trackResultBean.getLog();

        MetaHeader metaHeader = trackResultBean.getMetaHeader();

        logger.debug("writeLog - Received log request for header=[{}]", metaHeader);

        LogResultBean resultBean = new LogResultBean(input, metaHeader);
        if (metaHeader == null) {
            resultBean.setStatus(LogInputBean.LogStatus.NOT_FOUND);
            resultBean.setMessage("Unable to locate requested header");
            logger.debug(resultBean.getMessage());
            trackResultBean.setLogResult(resultBean);
            return trackResultBean;
        }
        logger.trace("looking for fortress user {}", metaHeader.getFortress());
        String fortressUser = (input.getFortressUser()!=null?input.getFortressUser():trackResultBean.getMetaInputBean().getFortressUser());
        FortressUser thisFortressUser = fortressService.getFortressUser(metaHeader.getFortress(), fortressUser, true);
        trackResultBean.setLogResult(createLog(metaHeader, input, thisFortressUser));
        return trackResultBean;
    }

    /**
     * Event log record for the supplied metaHeader from the supplied input
     *
     *
     * @param authorisedHeader metaHeader the caller is authorised to work with
     * @param input            trackLog details containing the data to log
     * @param thisFortressUser User name in calling system that is making the change
     * @return populated log information with any error messages
     */
    public LogResultBean createLog(MetaHeader authorisedHeader, LogInputBean input, FortressUser thisFortressUser) throws DatagioException, IOException {
        // Warning - making this private means it doesn't get a transaction!

        Fortress fortress = authorisedHeader.getFortress();

        // Transactions checks
        TxRef txRef = txService.handleTxRef(input, fortress.getCompany());
        LogResultBean resultBean = new LogResultBean(input, authorisedHeader);
        //ToDo: May want to track a "View" event which would not change the What data.
        if (input.getWhat() == null || input.getWhat().isEmpty()) {
            resultBean.setStatus(LogInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No 'what' information provided. Ignoring this request");
            return resultBean;
        }

        resultBean.setTxReference(txRef);

        TrackLog existingLog;
        existingLog = getLastLog(authorisedHeader);

        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(input.getWhen()));

        if (existingLog != null) {
            logger.debug("createLog, existing log found {}", existingLog);
            boolean unchanged = whatService.isSame(authorisedHeader, existingLog.getLog(), input.getWhat());
            if (unchanged) {
                logger.trace("Ignoring a change we already have {}", input);
                resultBean.setStatus(LogInputBean.LogStatus.IGNORE);
                if (input.isForceReindex()) { // Caller is recreating the search index
                    resultBean.setStatus((LogInputBean.LogStatus.REINDEX));
                    resultBean.setLogToIndex(existingLog);
                    resultBean.setMessage("Ignoring a change we already have. Honouring request to re-index");
                } else
                    resultBean.setMessage("Ignoring a change we already have");
                return resultBean;
            }
            if (input.getEvent() == null) {
                input.setEvent(Log.UPDATE);
            }
//            if (searchActive)
//                authorisedHeader = waitOnInitialSearchResult(authorisedHeader);


        } else { // first ever log for the metaHeader
            logger.debug("createLog - first log created for the header");
            if (input.getEvent() == null) {
                input.setEvent(Log.CREATE);
            }
            //if (!metaHeader.getLastUser().getId().equals(thisFortressUser.getId())){
            authorisedHeader.setLastUser(thisFortressUser);
            authorisedHeader.setCreatedBy(thisFortressUser);
        }

        Log thisLog = trackDao.prepareLog(thisFortressUser, input, txRef, (existingLog != null ? existingLog.getLog() : null));
        // Prepares the change
        input.setChangeEvent(thisLog.getEvent());
        resultBean.setWhatLog(thisLog);
        resultBean.setMetaHeader(authorisedHeader);

        if (authorisedHeader.getId() == null)
            input.setStatus(LogInputBean.LogStatus.TRACK_ONLY);
        else
            input.setStatus(LogInputBean.LogStatus.OK);

        // This call also saves the header
        TrackLog newLog = trackDao.addLog(authorisedHeader, thisLog, fortressWhen, existingLog);

        resultBean.setSysWhen(newLog.getSysWhen());

        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen().compareTo(newLog.getFortressWhen())<=0 );

        if (moreRecent && searchActive)
            resultBean.setLogToIndex(newLog);  // Notional log to index.

        return resultBean;

    }


    public TrackLog getLastLog(MetaHeader metaHeader) throws DatagioException {
        if (metaHeader == null || metaHeader.getId() == null)
            return null;
        logger.trace("Getting lastLog MetaID [{}]", metaHeader.getId());
        return trackDao.getLastLog(metaHeader.getId());
    }

//    private MetaHeader waitOnInitialSearchResult(MetaHeader metaHeader) {
//
//        if (metaHeader.isSearchSuppressed() || metaHeader.getSearchKey() != null)
//            return metaHeader; // Nothing to wait for as we're suppressing searches for this metaHeader
//
//        int timeOut = 100;
//        int i = 0;
//
//        while (metaHeader.getSearchKey() == null && i < timeOut) {
//            i++;
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                logger.error(e.getMessage());
//            }
//            metaHeader = getHeader(metaHeader.getId());
//        }
//        if (metaHeader.getSearchKey() == null)
//            logger.error("Timeout waiting for the initial search document to be created [{}]", metaHeader.getMetaKey());
//        return metaHeader;
//
//    }


    public TrackResultBean distributeChange(Company company, TrackResultBean trackResultBean) throws IOException {
        ArrayList<TrackResultBean> results = new ArrayList<>();
        results.add(trackResultBean);
        distributeChanges(company, results);
        return trackResultBean;
    }

    public void distributeChanges(Company company, Iterable<TrackResultBean> resultBeans) throws IOException {
        logger.debug("Distributing changes to sub-services");
        if (engineConfig.isConceptsEnabled()) {
            logger.debug("Distributing concepts");
            schemaService.registerConcepts(company, resultBeans);
        }
        //whatService.doKvWrite(resultBeans);
        searchService.makeChangesSearchable(resultBeans);
        logger.debug("Distributed changes to search service");
    }


}
