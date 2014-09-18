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
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;
import com.auditbucket.track.model.Log;
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
    public Collection<TrackResultBean> processLogsSync(Company company, Iterable<TrackResultBean> resultBeans) throws FlockException, IOException, ExecutionException, InterruptedException {
        logger.debug("Process Logs {}", Thread.currentThread().getName());
        Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            logResults.add(processLogFromResult(resultBean));
        }
        logger.debug("Logs processed. Proceeding to distribute changes");
        distributeChanges(company, logResults);

        return logResults;

    }

    TrackResultBean processLogFromResult(TrackResultBean resultBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        if (resultBean.getContentInput() == null)
            return resultBean;

        return writeTheLogAndDistributeChanges(resultBean);
    }

    @Override
    public TrackResultBean writeLog(Entity entity, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {

        TrackResultBean resultBean = new TrackResultBean(entity);
        resultBean.setContentInput(input);
        ArrayList<TrackResultBean> logs = new ArrayList<>();
        logs.add(resultBean);
        return processLogsSync(entity.getFortress().getCompany(), logs).iterator().next();
    }

    /**
     * Deadlock safe processor to creates a log
     *
     * @param resultBean details to write the log from. Will always contain an entity
     * @return result details
     * @throws com.auditbucket.helper.FlockException
     */
    TrackResultBean writeTheLogAndDistributeChanges(final TrackResultBean resultBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        ContentInputBean contentInputBean = resultBean.getContentInput();
        logger.debug("writeLog {}", contentInputBean);
        class DeadLockCommand implements Command {
            TrackResultBean result = null;

            @Override
            public Command execute() throws FlockException, IOException, ExecutionException, InterruptedException {

                // ToDo: DAT-169 This needs to be dealt with via SpringIntegration and persistent messaging
                result = writeLog(resultBean);
                if (result.getLogResult().getStatus() == ContentInputBean.LogStatus.NOT_FOUND)
                    throw new FlockException("Unable to find Entity ");
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
     * @throws com.auditbucket.helper.FlockException
     * @throws IOException
     */
    TrackResultBean writeLog(TrackResultBean trackResultBean) throws FlockException, IOException {
        ContentInputBean content = trackResultBean.getContentInput();

        Entity entity = trackResultBean.getEntity();

        logger.debug("writeLog - Received log request for entity [{}]", entity);

        LogResultBean resultBean = new LogResultBean(content, entity);
        if (entity == null) {
            resultBean.setStatus(ContentInputBean.LogStatus.NOT_FOUND);
            resultBean.setMessage("Unable to locate requested entity");
            logger.debug(resultBean.getMessage());
            trackResultBean.setLogResult(resultBean);
            return trackResultBean;
        }
        logger.trace("looking for fortress user {}", entity.getFortress());
        String fortressUser = (content.getFortressUser() != null ? content.getFortressUser() : trackResultBean.getEntityInputBean().getFortressUser());
        FortressUser thisFortressUser = fortressService.getFortressUser(entity.getFortress(), fortressUser, true);
        trackResultBean.setLogResult(createLog(entity, content, thisFortressUser));
        return trackResultBean;
    }

    /**
     * Event log record for the supplied entity from the supplied input
     *
     * @param entity entity the caller is authorised to work with
     * @param content            trackLog details containing the data to log
     * @param thisFortressUser User name in calling system that is making the change
     * @return populated log information with any error messages
     */
    LogResultBean createLog(Entity entity, ContentInputBean content, FortressUser thisFortressUser) throws FlockException, IOException {
        // Warning - making this private means it doesn't get a transaction!

        Fortress fortress = entity.getFortress();

        // Transactions checks
        TxRef txRef = txService.handleTxRef(content, fortress.getCompany());
        LogResultBean resultBean = new LogResultBean(content, entity);
        //ToDo: May want to track a "View" event which would not change the What data.
        if (!content.hasData()) {
            resultBean.setStatus(ContentInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No 'what' information provided. Ignoring this request");
            return resultBean;
        }

        resultBean.setTxReference(txRef);

        EntityLog existingLog;
        existingLog = getLastLog(entity);

        Boolean searchActive = fortress.isSearchActive();
        DateTime fortressWhen = (content.getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(content.getWhen()));

        if (content.getEvent() == null ) {
            content.setEvent(existingLog == null ? Log.CREATE : Log.UPDATE);
        }

        Log preparedLog = trackDao.prepareLog(thisFortressUser, content, txRef, (existingLog != null ? existingLog.getLog() : null));

        if (existingLog != null) {
            logger.debug("createLog, existing log found {}", existingLog);
            boolean unchanged = kvService.isSame(entity, existingLog.getLog(), preparedLog);
            if (unchanged) {
                logger.trace("Ignoring a change we already have {}", content);
                resultBean.setStatus(ContentInputBean.LogStatus.IGNORE);
                if (content.isForceReindex()) { // Caller is recreating the search index
                    resultBean.setStatus((ContentInputBean.LogStatus.REINDEX));
                    resultBean.setLogToIndex(existingLog);
                    resultBean.setMessage("Ignoring a change we already have. Honouring request to re-index");
                } else
                    resultBean.setMessage("Ignoring a change we already have");
                return resultBean;
            }
//            if (searchActive)
//                entity = waitOnInitialSearchResult(entity);


        } else { // first ever log for the entity
            logger.debug("createLog - first log created for the entity");
            //if (!entity.getLastUser().getId().equals(thisFortressUser.getId())){
            entity.setLastUser(thisFortressUser);
            entity.setCreatedBy(thisFortressUser);
        }

        // Prepares the change
        content.setChangeEvent(preparedLog.getEvent());
        resultBean.setWhatLog(preparedLog);
        resultBean.setEntity(entity);

        if (entity.getId() == null)
            content.setStatus(ContentInputBean.LogStatus.TRACK_ONLY);
        else
            content.setStatus(ContentInputBean.LogStatus.OK);

        // This call also saves the entity
        EntityLog newLog = trackDao.addLog(entity, preparedLog, fortressWhen, existingLog);

        resultBean.setSysWhen(newLog.getSysWhen());

        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen().compareTo(newLog.getFortressWhen()) <= 0);

        if (moreRecent && searchActive)
            resultBean.setLogToIndex(newLog);  // Notional log to index.

        return resultBean;

    }

    @Override
    public EntityLog getLastLog(Entity entity) throws FlockException {
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
