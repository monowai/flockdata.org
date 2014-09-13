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

import com.auditbucket.engine.repo.neo4j.EntityDaoNeo;
import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TxRef;
import com.auditbucket.track.service.LogService;
import com.auditbucket.track.service.SchemaService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 21/06/14
 * Time: 12:56 PM
 */
@Service
@Transactional
public class LogServiceNeo4j implements LogService {
    private Logger logger = LoggerFactory.getLogger(LogServiceNeo4j.class);

    @Autowired
    private TxService txService;

    @Autowired
    private KvService kvService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SearchServiceFacade searchService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    EntityDaoNeo trackDao;

    @Override
    public Collection<TrackResultBean> processLogsSync(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException, ExecutionException, InterruptedException {
        logger.debug("Process Logs {}", Thread.currentThread().getName());
        Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            logResults.add(processLogFromResult(resultBean));
        }
        logger.debug("Logs processed. Proceeding to distribute changes");
        distributeChanges(company, resultBeans);

        return logResults;

    }

    TrackResultBean processLogFromResult(TrackResultBean resultBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        if (resultBean.getLog() == null)
            return resultBean;

        return writeTheLogAndDistributeChanges(resultBean);
    }

    @Override
    public TrackResultBean writeLog(Entity entity, LogInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException {

        TrackResultBean resultBean = new TrackResultBean(entity);
        resultBean.setLogInput(input);
        ArrayList<TrackResultBean> logs = new ArrayList<>();
        logs.add(resultBean);
        return processLogsSync(entity.getFortress().getCompany(), logs).iterator().next();
    }

    /**
     * Deadlock safe processor to creates a log
     *
     * @param resultBean details to write the log from. Will always contain an entity
     * @return result details
     * @throws com.auditbucket.helper.DatagioException
     */
    TrackResultBean writeTheLogAndDistributeChanges(final TrackResultBean resultBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        LogInputBean logInputBean = resultBean.getLog();
        logger.debug("writeLog {}", logInputBean);
        class DeadLockCommand implements Command {
            TrackResultBean result = null;

            @Override
            public Command execute() throws DatagioException, IOException, ExecutionException, InterruptedException {

                // ToDo: DAT-169 This needs to be dealt with via SpringIntegration and persistent messaging
                result = writeLog(resultBean);
                if (result.getLogResult().getStatus() == LogInputBean.LogStatus.NOT_FOUND)
                    throw new DatagioException("Unable to find Entity ");
                kvService.doKvWrite(result); //ToDo: Consider KV not available. How to write the logs
                //      need to think of a way to recognize that the entity has unprocessed work
                return this;
            }
        }
        DeadLockCommand c = new DeadLockCommand();
        DeadlockRetry.execute(c, "processing log for entity", 20);
        return c.result;

    }


    /**
     * @param trackResultBean input data to process
     * @return result of the operation
     * @throws DatagioException
     * @throws IOException
     */
    TrackResultBean writeLog(TrackResultBean trackResultBean) throws DatagioException, IOException {
        LogInputBean input = trackResultBean.getLog();

        Entity entity = trackResultBean.getEntity();

        logger.debug("writeLog - Received log request for entity [{}]", entity);

        LogResultBean resultBean = new LogResultBean(input, entity);
        if (entity == null) {
            resultBean.setStatus(LogInputBean.LogStatus.NOT_FOUND);
            resultBean.setMessage("Unable to locate requested entity");
            logger.debug(resultBean.getMessage());
            trackResultBean.setLogResult(resultBean);
            return trackResultBean;
        }
        logger.trace("looking for fortress user {}", entity.getFortress());
        String fortressUser = (input.getFortressUser() != null ? input.getFortressUser() : trackResultBean.getEntityInputBean().getFortressUser());
        FortressUser thisFortressUser = fortressService.getFortressUser(entity.getFortress(), fortressUser, true);
        trackResultBean.setLogResult(createLog(entity, input, thisFortressUser));
        return trackResultBean;
    }

    /**
     * Event log record for the supplied entity from the supplied input
     *
     * @param entity entity the caller is authorised to work with
     * @param input            trackLog details containing the data to log
     * @param thisFortressUser User name in calling system that is making the change
     * @return populated log information with any error messages
     */
    LogResultBean createLog(Entity entity, LogInputBean input, FortressUser thisFortressUser) throws DatagioException, IOException {
        // Warning - making this private means it doesn't get a transaction!

        Fortress fortress = entity.getFortress();

        // Transactions checks
        TxRef txRef = txService.handleTxRef(input, fortress.getCompany());
        LogResultBean resultBean = new LogResultBean(input, entity);
        //ToDo: May want to track a "View" event which would not change the What data.
        if (input.getWhat() == null || input.getWhat().isEmpty()) {
            resultBean.setStatus(LogInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No 'what' information provided. Ignoring this request");
            return resultBean;
        }

        resultBean.setTxReference(txRef);

        TrackLog existingLog;
        existingLog = getLastLog(entity);

        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (input.getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(input.getWhen()));

        if (existingLog != null) {
            logger.debug("createLog, existing log found {}", existingLog);
            boolean unchanged = kvService.isSame(entity, existingLog.getLog(), input.getWhat());
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
//                entity = waitOnInitialSearchResult(entity);


        } else { // first ever log for the entity
            logger.debug("createLog - first log created for the entity");
            if (input.getEvent() == null) {
                input.setEvent(Log.CREATE);
            }
            //if (!entity.getLastUser().getId().equals(thisFortressUser.getId())){
            entity.setLastUser(thisFortressUser);
            entity.setCreatedBy(thisFortressUser);
        }

        Log thisLog = trackDao.prepareLog(thisFortressUser, input, txRef, (existingLog != null ? existingLog.getLog() : null));
        // Prepares the change
        input.setChangeEvent(thisLog.getEvent());
        resultBean.setWhatLog(thisLog);
        resultBean.setEntity(entity);

        if (entity.getId() == null)
            input.setStatus(LogInputBean.LogStatus.TRACK_ONLY);
        else
            input.setStatus(LogInputBean.LogStatus.OK);

        // This call also saves the entity
        TrackLog newLog = trackDao.addLog(entity, thisLog, fortressWhen, existingLog);

        resultBean.setSysWhen(newLog.getSysWhen());

        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen().compareTo(newLog.getFortressWhen()) <= 0);

        if (moreRecent && searchActive)
            resultBean.setLogToIndex(newLog);  // Notional log to index.

        return resultBean;

    }

    @Override
    public TrackLog getLastLog(Entity entity) throws DatagioException {
        if (entity == null || entity.getId() == null)
            return null;
        logger.trace("Getting lastLog MetaID [{}]", entity.getId());
        return trackDao.getLastLog(entity.getId());
    }

    void distributeChanges(Company company, Iterable<TrackResultBean> resultBeans) throws IOException {
        logger.debug("Distributing changes to sub-services");
        schemaService.registerConcepts(company, resultBeans);
        searchService.makeChangesSearchable(resultBeans);
        logger.debug("Distributed changes to search service");
    }

//    private Entity waitOnInitialSearchResult(Entity entity) {
//
//        if (entity.isSearchSuppressed() || entity.getSearchKey() != null)
//            return entity; // Nothing to wait for as we're suppressing searches for this entity
//
//        int timeOut = 100;
//        int i = 0;
//
//        while (entity.getSearchKey() == null && i < timeOut) {
//            i++;
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                logger.error(e.getMessage());
//            }
//            entity = getHeader(entity.getId());
//        }
//        if (entity.getSearchKey() == null)
//            logger.error("Timeout waiting for the initial search document to be created [{}]", entity.getMetaKey());
//        return entity;
//
//    }


}
