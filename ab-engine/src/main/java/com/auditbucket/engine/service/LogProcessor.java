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

import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 21/06/14
 * Time: 12:56 PM
 */
@Service
@Transactional (propagation = Propagation.SUPPORTS)
public class LogProcessor {
    private Logger logger = LoggerFactory.getLogger(LogProcessor.class);

    @Autowired
    private TrackService trackService;

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

    @Async
    public Future<Collection<TrackResultBean>> processLogs(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException {
//      public Collection<TrackResultBean> processLogs(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException {
        Thread.yield();
        logger.debug("Process Logs {}", Thread.currentThread().getName());
        Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            logResults.add(processLogFromResult(company, resultBean));
        }
        logger.debug("Logs processed. Proceeding to distribute.");
        distributeChanges(company, logResults);
        return new AsyncResult<>(logResults);
//          return logResults;
    }
    protected TrackResultBean processLogFromResult(Company company, TrackResultBean resultBean) throws DatagioException, IOException {
        LogInputBean logBean = resultBean.getLog();
        MetaHeader header = resultBean.getMetaHeader();

        //DAT-77 the header may still be committing in another thread
//        if ( header!=null && header.getId()>0)
//            header = trackService.getHeader(company, resultBean.getMetaHeader().getMetaKey());

        if (resultBean.getLog() != null) {
            // Secret back door so that the log result can quickly get the auditid
            logBean.setMetaId(resultBean.getAuditId());
            logBean.setMetaKey(resultBean.getMetaKey());
            logBean.setCallerRef(resultBean.getCallerRef());

            LogResultBean logResult;
            if (header != null)
                logResult = writeLog(header, logBean);
            else
                logResult = processCompanyLog(company, resultBean);

            logResult.setMetaKey(null);// Don't duplicate the text as it's in the header
            logResult.setFortressUser(null);
            resultBean.setLogResult(logResult);

        }
        return resultBean;
    }

    private LogResultBean processCompanyLog(Company company, TrackResultBean resultBean) throws DatagioException, IOException {
        MetaHeader header = resultBean.getMetaHeader();
        if (header == null)
            header = trackService.getHeader(company, resultBean.getMetaKey());
        return writeLog(header, resultBean.getLog());
    }

    /**
     * Deadlock safe processor to creates a log
     *
     * @param header       Header that the caller is authorised to work with
     * @param logInputBean log details to apply to the authorised header
     * @return result details
     * @throws com.auditbucket.helper.DatagioException
     *
     */
    public LogResultBean writeLog(final MetaHeader header, final LogInputBean logInputBean) throws DatagioException, IOException {
        logger.debug("writeLog {}", logInputBean);
        logInputBean.setWhat(logInputBean.getWhat());
        class DeadLockCommand implements Command {
            LogResultBean result = null;

            @Override
            public Command execute() throws DatagioException, IOException {
                result = trackService.writeLog(header, logInputBean);
                return this;
            }
        }
        DeadLockCommand c = new DeadLockCommand();
        DeadlockRetry.execute(c, "processing log for header", 20);
        return c.result;

    }
    public TrackResultBean distributeChange(Company company, TrackResultBean trackResultBean) throws IOException {
        ArrayList<TrackResultBean>results = new ArrayList<>();
        results.add(trackResultBean);
        distributeChanges(company, results);
        return trackResultBean;
    }

    public void distributeChanges(Company company, Iterable<TrackResultBean> resultBeans) throws IOException {
        logger.debug("Distributing changes to KV and Search");
        if (engineConfig.isConceptsEnabled())
            schemaService.registerConcepts(company, resultBeans);
        whatService.doKvWrite(resultBeans);
        Collection<SearchChange> changes = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            SearchChange change = getSearchChange(resultBean);
            if (change!=null )
                changes.add(change);
        }
        searchService.makeChangesSearchable(changes);
    }

    private SearchChange getSearchChange(TrackResultBean trackResultBean) {
        if (trackResultBean.getMetaInputBean()!=null && trackResultBean.getMetaInputBean().isMetaOnly()){
            return getMetaSearchChange(trackResultBean);
        }
        if ( !trackResultBean.getMetaHeader().getFortress().isSearchActive())
            return null;

        LogResultBean logResultBean = trackResultBean.getLogResult();
        LogInputBean input = trackResultBean.getLog();

        if ( !trackResultBean.processLog())
            return null;

        if (logResultBean != null && logResultBean.getLogToIndex() != null && logResultBean.getStatus() == LogInputBean.LogStatus.OK) {
            try {
                DateTime fWhen = new DateTime(logResultBean.getLogToIndex().getFortressWhen());
                return searchService.prepareSearchDocument(logResultBean.getLogToIndex().getMetaHeader(), input, input.getChangeEvent(), fWhen, logResultBean.getLogToIndex());
            } catch (JsonProcessingException e) {
                logResultBean.setMessage("Error processing JSON document");
                logResultBean.setStatus(LogInputBean.LogStatus.ILLEGAL_ARGUMENT);
            }
        }
        return null;
    }

    private SearchChange getMetaSearchChange(TrackResultBean trackResultBean) {
        return searchService.getSearchChange(trackResultBean.getMetaHeader().getFortress().getCompany(), trackResultBean);
    }

}
