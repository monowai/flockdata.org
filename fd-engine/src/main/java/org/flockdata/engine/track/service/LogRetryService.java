/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.service;

import org.flockdata.engine.schema.service.TxService;
import org.flockdata.engine.track.EntityDaoNeo;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.LogResultBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.Log;
import org.flockdata.track.model.TxRef;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.LogService;
import org.flockdata.track.service.TrackService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.transaction.HeuristicRollbackException;
import java.io.IOException;
import java.util.Set;

/**
 * User: mike
 * Date: 20/09/14
 * Time: 3:38 PM
 */
//@Configuration
@EnableRetry
@Service
public class LogRetryService {
    private Logger logger = LoggerFactory.getLogger(LogRetryService.class);
    @Autowired
    TrackService trackService;

    @Autowired
    FortressService fortressService;

    @Autowired
    KvService kvService;

    @Autowired
    TxService txService;

    @Autowired
    EntityDaoNeo entityDao;

    @Autowired
    LogService logService;

    /**
     * Attempts to gracefully handle deadlock conditions
     *
     * @param trackResultBean input data to process
     * @return result of the operation
     * @throws org.flockdata.helper.FlockException
     * @throws IOException
     */
    @Retryable(include = {HeuristicRollbackException.class, DeadlockDetectedException.class, ConcurrencyFailureException.class, InvalidDataAccessResourceUsageException.class}, maxAttempts = 12, backoff = @Backoff(delay = 100, maxDelay = 500))
    TrackResultBean writeLog(Fortress fortress, TrackResultBean trackResultBean) throws FlockException, IOException {
        return writeLogTx(fortress, trackResultBean);
    }

    @Transactional
    TrackResultBean writeLogTx(Fortress fortress, TrackResultBean trackResultBean) throws FlockException, IOException {
        ContentInputBean content = trackResultBean.getContentInput();

        boolean entityExists = (trackResultBean.getEntityInputBean() != null && !trackResultBean.getEntityInputBean().isTrackSuppressed());

        Entity entity = trackResultBean.getEntity();

        assert entity != null;

        logger.debug("writeLog entityExists [{}]  entity [{}], [{}]", entityExists, entity.getId(), new DateTime(entity.getFortressDateUpdated()));

//        LogResultBean resultBean = new LogResultBean(content);
        logger.trace("looking for fortress user {}", fortress);
        String fortressUser = (content.getFortressUser() != null ? content.getFortressUser() : trackResultBean.getEntityInputBean().getFortressUser());

        FortressUser thisFortressUser = trackResultBean.getEntity().getCreatedBy();
        if (thisFortressUser == null || !(thisFortressUser.getCode()!=null && thisFortressUser.getCode().equals(fortressUser))) {
            // Different user creating the Entity than is creating the log
            thisFortressUser = fortressService.getFortressUser(fortress, fortressUser, true);
        }
        //resultBean.setEntity(entity);
        trackResultBean.setLogResult(
                createLog(trackResultBean, thisFortressUser)
        );
        return trackResultBean;

    }

    /**
     * Event log record for the supplied entity from the supplied input
     *
     * @param payLoad          trackLog details containing the data to log
     * @param thisFortressUser User name in calling system that is making the change
     * @return populated log information with any error messages
     */
    @Transactional
    LogResultBean createLog(TrackResultBean payLoad, FortressUser thisFortressUser) throws FlockException, IOException {
        // Warning - making this private means it doesn't get a transaction!
        //entity = trackService.getEntity(entity);
        Fortress fortress = payLoad.getEntity().getFortress();
        // ToDo: ??? noticed during tracking over AMQP
        if (thisFortressUser != null) {
            if (thisFortressUser.getFortress() == null)
                thisFortressUser.setFortress(payLoad.getEntity().getFortress());
        }

        LogResultBean resultBean = new LogResultBean(payLoad.getContentInput());
        //ToDo: May want to track a "View" event which would not change the What data.
        if (!payLoad.getContentInput().hasData()) {
            resultBean.setStatus(ContentInputBean.LogStatus.IGNORE);
            resultBean.setMessage("No content information provided. Ignoring this request");
            logger.debug(resultBean.getMessage());
            return resultBean;
        }

        // Transactions checks
        TxRef txRef = txService.handleTxRef(payLoad.getContentInput(), fortress.getCompany());
        resultBean.setTxReference(txRef);

        EntityLog lastLog = getLastLog(payLoad.getEntity());
        logger.debug("createLog ContentWhen {}, lastLogWhen {}, log {}", new DateTime(payLoad.getContentInput().getWhen()),
                (lastLog == null ? "[null]" : new DateTime(lastLog.getFortressWhen()))
                , lastLog);

        DateTime contentWhen = (payLoad.getContentInput().getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(payLoad.getContentInput().getWhen()));

        // Is this content historic relative to what we know?
        lastLog = resolveHistoricLog(payLoad.getEntity(), lastLog, contentWhen);

        if (payLoad.getContentInput().getEvent() == null) {
            payLoad.getContentInput().setEvent(lastLog == null ? Log.CREATE : Log.UPDATE);
        }

        Log preparedLog = entityDao.prepareLog( fortress.getCompany(), thisFortressUser, payLoad, txRef, (lastLog != null ? lastLog.getLog() : null));

        if (lastLog != null) {
            logger.debug("createLog, existing log found {}", lastLog);
            boolean unchanged = kvService.isSame(payLoad.getEntity(), lastLog.getLog(), preparedLog);
            if (unchanged) {
                logger.debug("Ignoring a change we already have {}", payLoad);
                resultBean.setStatus(ContentInputBean.LogStatus.IGNORE);
                if (payLoad.getContentInput().isForceReindex()) { // Caller is recreating the search index
                    resultBean.setStatus((ContentInputBean.LogStatus.REINDEX));
                    resultBean.setLogToIndex(lastLog);
                    resultBean.setMessage("Ignoring a change we already have. Honouring request to re-index");
                } else {
                    resultBean.setMessage("Ignoring a change we already have");
                    resultBean.setLogIgnored();
                }

                return resultBean;
            }

        } else { // first ever log for the entity
            logger.debug("createLog - first log created {}", contentWhen);
            //if (!entity.getLastUser().getId().equals(thisFortressUser.getId())){
            payLoad.getEntity().setLastUser(thisFortressUser);
            payLoad.getEntity().setCreatedBy(thisFortressUser);
            if (payLoad.getEntity().getCreatedBy() == null)
                payLoad.getEntity().setCreatedBy(thisFortressUser);
        }

        // Prepares the change
        payLoad.getContentInput().setChangeEvent(preparedLog.getEvent());
        resultBean.setLog(preparedLog);

        if (payLoad.getEntity().getId() == null)
            payLoad.getContentInput().setStatus(ContentInputBean.LogStatus.TRACK_ONLY);
        else
            payLoad.getContentInput().setStatus(ContentInputBean.LogStatus.OK);

        // This call also saves the entity
        EntityLog newLog = entityDao.addLog(payLoad.getEntity(), preparedLog, contentWhen);

        resultBean.setFdWhen(newLog.getSysWhen());

        boolean moreRecent = (lastLog == null || lastLog.getFortressWhen().compareTo(contentWhen.getMillis()) <= 0);

        if (moreRecent)
            resultBean.setLogToIndex(newLog);  // Notional log to index.

        return resultBean;

    }

    /**
     * Evaluates which log is the one for the fortress whenDate. It will either be the "current"
     * or is the log for the contentWhen date
     *
     * @param entity      entity owning the logs
     * @param incomingLog defaults to the last log found
     * @param contentWhen date range to consider
     * @return entityLog to compare against
     */
    private EntityLog resolveHistoricLog(Entity entity, EntityLog incomingLog, DateTime contentWhen) {

        boolean historicIncomingLog = (incomingLog != null && contentWhen.isBefore(incomingLog.getFortressWhen()));

        logger.debug("Historic {}, {}, log {}, contentWhen {}",
                new DateTime(entity.getFortressDateUpdated()),
                historicIncomingLog,
                incomingLog != null ? new DateTime(incomingLog.getFortressWhen()) : "[no existing]",
                contentWhen);

        if (historicIncomingLog) {
            Set<EntityLog> entityLogs = entityDao.getLogs(entity.getId(), contentWhen.toDate());
            if (entityLogs.isEmpty()) {
                logger.debug("No logs prior to {}. Returning existing log", contentWhen);
                return incomingLog;
            } else {
                logger.debug("Found {} historic logs", entityLogs.size());
                EntityLog closestLog = null;

                for (EntityLog entityLog : entityLogs) {
                    if (closestLog == null)
                        closestLog = entityLog;
                    else if (entityLog.getFortressWhen() < closestLog.getFortressWhen())
                        closestLog = entityLog;
                    if (entityLog.getFortressWhen().equals(contentWhen.getMillis()))
                        return entityLog; // Exact match to the millis
                }

                logger.debug("return closestLog {}", closestLog == null ? "[null]" : closestLog.getFortressWhen());
                return closestLog;
            }

        }
        logger.debug("return incomingLog");
        return incomingLog;
    }

    @Transactional
    public EntityLog getLastLog(Entity entity) throws FlockException {
        if (entity == null || entity.getId() == null)
            return null;
        logger.trace("Getting lastLog MetaID [{}]", entity.getId());
        return entityDao.getLastEntityLog(entity);
    }


}
