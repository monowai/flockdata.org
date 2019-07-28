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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
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
public class EntityRetryService {

  private final EntityService entityService;

  private final LogService logService;

  @Autowired
  public EntityRetryService(EntityService entityService, LogService logService) {
    this.entityService = entityService;
    this.logService = logService;
  }

  @Retryable(include = {NotFoundException.class, InvalidDataAccessResourceUsageException.class, DataIntegrityViolationException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class, ConstraintViolationException.class},
      maxAttempts = 20,
      backoff = @Backoff(delay = 600, multiplier = 5, random = true))
  @Transactional(timeout = 4000)
  public Iterable<TrackResultBean> track(DocumentNode documentType, Segment segment, List<EntityInputBean> entityInputs, Future<Collection<FdTagResultBean>> tags)
      throws InterruptedException, ExecutionException, FlockException {

    Collection<TrackResultBean>
        resultBeans = entityService.trackEntities(documentType, segment, entityInputs, tags);

    if (resultBeans.size() > 1) {
      // Could have a mix of new and existing entities, so we need to
      // Split the batch between new and existing entities
      Collection<TrackResultBean> newEntities = TrackBatchSplitter.getNewEntities(resultBeans);
      Collection<TrackResultBean> existingEntities = TrackBatchSplitter.getExistingEntities(resultBeans);


      if (!newEntities.isEmpty()) { // New can be processed async
        logService.processLogs(segment.getFortress(), newEntities);
        if (existingEntities.isEmpty()) {
          return newEntities;
        }

      }
      // Process updates synchronously
      logService.processLogs(segment.getFortress(), existingEntities).get();
      return resultBeans;
    }
    return logService.processLogsSync(segment.getFortress(), resultBeans);

  }


}
