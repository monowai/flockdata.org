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
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.MetaHeader;
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
        TrackResultBean trackResult = null;
        if (resultBean.getLog() != null) {
            // Secret back door so that the log result can quickly get the pk

            if (header != null)
                trackResult = writeLog(company, header.getMetaKey(), resultBean);
            else
                trackResult = writeLog(company, logBean.getMetaKey(), resultBean);
        }
        return trackResult;
    }

    public TrackResultBean writeLog(Company company, String metaKey, LogInputBean input) throws DatagioException, IOException {
        TrackResultBean resultBean = new TrackResultBean(input.getFortress(), input.getDocumentType(), input.getCallerRef(), metaKey);
        resultBean.setLogInput(input);
        return writeLog(company, metaKey, resultBean);
    }

    /**
     * Deadlock safe processor to creates a log
     *
     *
     * @param company
     * @param metaKey      Header that the caller is authorised to work with
     * @param resultBean   log details to apply to the authorised header
     * @return result details
     * @throws com.auditbucket.helper.DatagioException
     *
     */
    public TrackResultBean writeLog(final Company company, final String metaKey, final TrackResultBean resultBean) throws DatagioException, IOException {
        LogInputBean logInputBean = resultBean.getLog();
        logger.debug("writeLog {}", logInputBean);
        class DeadLockCommand implements Command {
            TrackResultBean result = null;
            @Override
            public Command execute() throws DatagioException, IOException {

                // ToDo: DAT-169 This needs to be dealt with via SpringIntegration and persistent messaging
                LogResultBean logResult = trackService.writeLog(company, metaKey, resultBean);
                result = new TrackResultBean(logResult, resultBean.getLog());
                result.setMetaInputBean(resultBean.getMetaInputBean());
                if ( result.getLogResult().getStatus()== LogInputBean.LogStatus.NOT_FOUND)
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
    public TrackResultBean distributeChange(Company company, TrackResultBean trackResultBean) throws IOException {
        ArrayList<TrackResultBean>results = new ArrayList<>();
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
