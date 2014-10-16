package com.auditbucket.engine.service;

import com.auditbucket.engine.repo.neo4j.EntityDaoNeo;
import com.auditbucket.helper.FlockException;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.TxRef;
import com.auditbucket.track.service.LogService;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.service.TrackService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * User: mike
 * Date: 20/09/14
 * Time: 3:38 PM
 */
//@Configuration
@EnableRetry
@Service
@Transactional
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

    @Retryable(include = ConcurrencyFailureException.class, maxAttempts = 12, backoff = @Backoff(delay = 100, maxDelay = 500))
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

        FortressUser thisFortressUser ;
        if (!fortressUser.equals( trackResultBean.getEntity().getCreatedBy().getCode())){
            // Different user creating the Entity than is creating the log
            thisFortressUser = fortressService.getFortressUser(entity.getFortress(), fortressUser, true);
        } else {
            thisFortressUser = trackResultBean.getEntity().getCreatedBy();
        }

        trackResultBean.setLogResult(createLog(entity, content, thisFortressUser));
        return trackResultBean;
    }

    /**
     * Event log record for the supplied entity from the supplied input
     *
     * @param entity           entity the caller is authorised to work with
     * @param content          trackLog details containing the data to log
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

        if (content.getEvent() == null) {
            content.setEvent(existingLog == null ? Log.CREATE : Log.UPDATE);
        }

        Log preparedLog = entityDao.prepareLog(thisFortressUser, content, txRef, (existingLog != null ? existingLog.getLog() : null));

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
                } else {
                    resultBean.setMessage("Ignoring a change we already have");
                    resultBean.setLogIgnored();
                }

                return resultBean;
            }
//            if (searchActive)
//                entity = waitOnInitialSearchResult(entity);


        } else { // first ever log for the entity
            logger.debug("createLog - first log created for the entity");
            //if (!entity.getLastUser().getId().equals(thisFortressUser.getId())){
            entity.setLastUser(thisFortressUser);
            if (entity.getCreatedBy() == null)
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
        EntityLog newLog = entityDao.addLog(entity, preparedLog, fortressWhen, existingLog);

        resultBean.setSysWhen(newLog.getSysWhen());

        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen().compareTo(newLog.getFortressWhen()) <= 0);

        if (moreRecent && searchActive)
            resultBean.setLogToIndex(newLog);  // Notional log to index.

        return resultBean;

    }

    public EntityLog getLastLog(Entity entity) throws FlockException {
        if (entity == null || entity.getId() == null)
            return null;
        logger.trace("Getting lastLog MetaID [{}]", entity.getId());
        return entityDao.getLastLog(entity.getId());
    }


}
