package com.auditbucket.engine.service;

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.service.LogService;
import com.auditbucket.track.service.TrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 20/09/14
 * Time: 3:38 PM
 */
//@Configuration
@EnableRetry
@Service
@Transactional
public class EntityRetryService {

    @Autowired
    TrackService trackService;

    @Autowired
    LogService logService;

    @Retryable(include = ConcurrencyFailureException.class, maxAttempts = 12, backoff = @Backoff(delay = 100, maxDelay = 500))
    public Iterable<TrackResultBean> track(Fortress fortress, List<EntityInputBean> entities)
            throws InterruptedException, ExecutionException, FlockException, IOException {

        Iterable<TrackResultBean> resultBeans = trackService.trackEntities(fortress, entities);

        return logService.processLogsSync(fortress.getCompany(), resultBeans);

    }



}
