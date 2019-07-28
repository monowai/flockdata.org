/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.service;

import static org.flockdata.store.StoreHelper.resolveStore;

import java.io.IOException;
import java.util.Set;
import javax.transaction.HeuristicRollbackException;
import org.flockdata.data.ChangeEvent;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Log;
import org.flockdata.data.TxRef;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.concept.service.TxService;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.data.dao.EntityDaoNeo;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.engine.data.graph.TxRefNode;
import org.flockdata.helper.FlockException;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.LogResultBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @since 20/09/2014
 */
@Service
public class LogRetryService {
  private final StorageProxy storage;
  private final FortressService fortressService;
  private final TxService txService;
  private final EntityDaoNeo entityDao;
  private EngineConfig engineConfig;
  private TrackEventService trackEventService;

  private Logger logger = LoggerFactory.getLogger(LogRetryService.class);

  @Autowired
  public LogRetryService(StorageProxy storage, FortressService fortressService, TxService txService, EntityDaoNeo entityDao) {
    this.storage = storage;
    this.fortressService = fortressService;
    this.txService = txService;
    this.entityDao = entityDao;
  }

  @Autowired
  void setAuxServices(EngineConfig engineConfig, TrackEventService trackEventService) {
    this.trackEventService = trackEventService;
    this.engineConfig = engineConfig;
  }

  /**
   * Attempts to gracefully handle deadlock conditions
   *
   * @param trackResultBean input data to process
   * @return result of the operation
   * @throws org.flockdata.helper.FlockException data error
   * @throws IOException                         Unrecoverable
   */
  @Retryable(include = {HeuristicRollbackException.class, DeadlockDetectedException.class, ConcurrencyFailureException.class, InvalidDataAccessResourceUsageException.class}, maxAttempts = 12,
      backoff = @Backoff(maxDelay = 200, multiplier = 5, random = true))
  @Transactional
  TrackResultBean writeLog(FortressNode fortress, TrackResultBean trackResultBean) throws FlockException {
    ContentInputBean content = trackResultBean.getContentInput();

    boolean entityExists = (trackResultBean.getEntityInputBean() != null && !trackResultBean.getEntityInputBean().isTrackSuppressed());

    Entity entity = trackResultBean.getEntity();

    assert entity != null;
    //assert entity.getKey()!=null;

    logger.debug("writeLog entityExists [{}]  entity [{}], [{}]", entityExists, entity.getId(), new DateTime(entity.getFortressUpdatedTz()));

//        LogResultBean resultBean = new LogResultBean(content);
    logger.trace("looking for fortress user {}", fortress);
    String fortressUser = (content.getFortressUser() != null ? content.getFortressUser() : trackResultBean.getEntityInputBean().getFortressUser());

    FortressUserNode thisFortressUser = (FortressUserNode) entity.getCreatedBy();
    if (fortressUser != null) {
      if (thisFortressUser == null || !(thisFortressUser.getCode() != null && thisFortressUser.getCode().equals(fortressUser))) {
        // Different user creating the Entity than is creating the log
        thisFortressUser = fortressService.getFortressUser(fortress, fortressUser, true);
      }
    }
    //resultBean.setEntity(entity);
    //trackResultBean.setCurrentLog(
    createLog(trackResultBean, thisFortressUser);
    //);
    return trackResultBean;

  }

  /**
   * Event log record for the supplied entity from the supplied input
   *
   * @param trackResult      trackLog details containing the data to log
   * @param thisFortressUser User name in calling system that is making the change
   * @return populated log information with any error messages
   */
  private LogResultBean createLog(TrackResultBean trackResult, FortressUserNode thisFortressUser) throws FlockException {
    FortressNode fortress = (FortressNode) trackResult.getEntity().getFortress();
    // ToDo: ??? noticed during tracking over AMQP
    if (thisFortressUser != null) {
      if (thisFortressUser.getFortress() == null) {
        thisFortressUser.setFortress(fortress);
      }
    }

    LogResultBean resultBean = new LogResultBean(trackResult.getContentInput());
    //ToDo: May want to track a "View" event which would not change the What data.
    if (!trackResult.getContentInput().hasData()) {
      trackResult.setLogStatus(ContentInputBean.LogStatus.IGNORE);
      trackResult.addServiceMessage("No content information provided. Ignoring this request");
      //logger.debug(trackResult.getServiceMessages());
      return resultBean;
    }

    // Transactions checks
    final TxRef txRef = txService.handleTxRef(trackResult.getContentInput(), (CompanyNode) fortress.getCompany());
    trackResult.setTxReference(txRef);

    EntityLog lastLog = getLastLog(trackResult.getEntity());

    logger.debug("createLog key {}, ContentWhen {}, lastLogWhen {}, log {}", trackResult.getEntity().getKey(), new DateTime(trackResult.getContentInput().getWhen()),
        (lastLog == null ? "[null]" : new DateTime(lastLog.getFortressWhen()))
        , lastLog);

    DateTime contentWhen = (trackResult.getContentInput().getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(trackResult.getContentInput().getWhen()));

    // Is this content historic relative to what we know?
    lastLog = resolveHistoricLog(trackResult.getEntity(), lastLog, contentWhen);

    if (trackResult.getContentInput().getEvent() == null) {
      trackResult.getContentInput().setEvent(lastLog == null ? LogNode.CREATE : LogNode.UPDATE);
    }

    Log preparedLog = null;
    if (trackResult.getCurrentLog() != null) {
      preparedLog = trackResult.getCurrentLog().getLog();
    }

    if (preparedLog == null) // log is prepared during the entity process and stashed here ONLY if it is a brand new entity
    {
      preparedLog = prepareLog(fortress.getCompany(), thisFortressUser, trackResult, txRef, (lastLog != null ? (LogNode) lastLog.getLog() : null));
    } else {
      trackResult.setTxReference(txRef);
    }

    if (lastLog != null) {
      logger.debug("createLog, existing log found {}", lastLog);
      boolean unchanged = storage.compare(trackResult.getEntity(), lastLog.getLog(), preparedLog);
      if (unchanged) {
        logger.debug("Ignoring a change we already have {}", trackResult);
        if (trackResult.getContentInput().isForceReindex()) { // Caller is recreating the search index
          trackResult.setLogStatus((ContentInputBean.LogStatus.REINDEX));
//                    resultBean.setLogToIndex(lastLog);
          trackResult.addServiceMessage("Ignoring a change we already have. Honouring request to re-index");
        } else {
          trackResult.setLogStatus((ContentInputBean.LogStatus.IGNORE));
          trackResult.addServiceMessage("Ignoring a change we already have");
          trackResult.setLogIgnored();
        }

        return resultBean;
      }

    } else { // first ever log for the entity
      logger.debug("createLog - first log created {}", contentWhen);
      //if (!entity.getLastUser().getId().equals(thisFortressUser.getId())){
      EntityNode entity = (EntityNode) trackResult.getEntity();
      entity.setLastUser(thisFortressUser);
      entity.setCreatedBy(thisFortressUser);
      if (entity.getCreatedBy() == null) {
        entity.setCreatedBy(thisFortressUser);
      }
    }

    // Prepares the change
    trackResult.getContentInput().setChangeEvent(preparedLog.getEvent());
    //resultBean.setLog(preparedLog);

    if (trackResult.getEntity().getId() == null) {
      trackResult.setLogStatus(ContentInputBean.LogStatus.TRACK_ONLY);
    } else {
      trackResult.setLogStatus(ContentInputBean.LogStatus.OK);
    }

    // This call also saves the entity
    EntityLog entityLog = entityDao.writeLog(trackResult, preparedLog, contentWhen);

    resultBean.setSysWhen(entityLog.getSysWhen());

    boolean moreRecent = (lastLog == null || lastLog.getFortressWhen().compareTo(contentWhen.getMillis()) <= 0);

    if (moreRecent) {
      trackResult.setCurrentLog(entityLog);  // Notional log to index.
    }

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

    if (incomingLog == null || incomingLog.isMocked()) {
      return null;
    }

    boolean historicIncomingLog = (contentWhen.isBefore(incomingLog.getFortressWhen()));

    logger.debug("Historic {}, {}, log {}, contentWhen {}",
        new DateTime(entity.getFortressUpdatedTz()),
        historicIncomingLog,
        new DateTime(incomingLog.getFortressWhen()),
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
          if (closestLog == null) {
            closestLog = entityLog;
          } else if (entityLog.getFortressWhen() < closestLog.getFortressWhen()) {
            closestLog = entityLog;
          }
          if (entityLog.getFortressWhen().equals(contentWhen.getMillis())) {
            return entityLog; // Exact match to the millis
          }
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
    if (entity == null || entity.getId() == null || entity.isNewEntity()) {
      return null;
    }
    logger.trace("Getting lastLog MetaID [{}]", entity.getId());
    return entityDao.getLastEntityLog((EntityNode) entity);
  }

  LogNode prepareLog(Company company, FortressUser fUser, TrackResultBean payLoad, TxRef txRef, LogNode previousChange) throws FlockException {
    ChangeEvent event = trackEventService.processEvent(company, payLoad.getContentInput().getEvent());
    LogNode changeLog = new LogNode((FortressUserNode) fUser, payLoad.getContentInput(), (TxRefNode) txRef);

    changeLog.setEvent(event);
    changeLog.setPreviousLog(previousChange);
    return prepareLog(engineConfig.store(), payLoad, changeLog);
  }

  public LogNode prepareLog(Store defaultStore, TrackResultBean trackResult, LogNode log) {
    Store storage = resolveStore(trackResult, defaultStore);
    StoredContent storedContent = new StorageBean(trackResult);
    storedContent.setStore(storage.name());
    log.setStorage(storage.name());
    log.setContent(storedContent);
    return log;
  }


}
