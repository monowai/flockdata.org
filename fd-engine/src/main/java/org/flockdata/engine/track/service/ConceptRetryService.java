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

import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.LogService;
import org.flockdata.track.service.SchemaService;
import org.flockdata.track.service.TrackService;
import org.neo4j.kernel.DeadlockDetectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.transaction.HeuristicRollbackException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 20/09/14
 * Time: 3:38 PM
 */
@Configuration
@EnableRetry
@Service
//@Transactional
public class ConceptRetryService {

    @Autowired
    TrackService trackService;

    @Autowired
    LogService logService;

    @Autowired
    LogRetryService logRetryService;

    @Autowired
    SchemaService schemaService;

    @Retryable(include = {HeuristicRollbackException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class}, maxAttempts = 20, backoff = @Backoff(delay = 150, maxDelay = 500))
    public Iterable<TrackResultBean> trackConcepts(Fortress fortress, Iterable<TrackResultBean> resultBeans)
            throws InterruptedException, ExecutionException, FlockException, IOException {
        return doTrack(fortress, resultBeans);
    }

    @Transactional
    Iterable<TrackResultBean> doTrack(Fortress fortress, Iterable<TrackResultBean> resultBeans) throws InterruptedException, FlockException, ExecutionException, IOException {
        schemaService.registerConcepts(fortress, resultBeans);

//        resultBeans = logService.processLogsSync(fortress, resultBeans);
//        Collection<TrackResultBean> trackResultBeans = new ArrayList<>();
//        for (TrackResultBean resultBean : resultBeans) {
//            trackResultBeans.add(logRetryService.writeLogTx(fortress, resultBean));
//        }
        //return trackResultBeans;
        return resultBeans;

    }



}
