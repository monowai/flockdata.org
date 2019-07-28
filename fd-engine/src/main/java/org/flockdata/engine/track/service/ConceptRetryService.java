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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.transaction.HeuristicRollbackException;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @since 20/09/2014
 */
@Service
public class ConceptRetryService {

  private final ConceptService conceptService;

  private final PlatformConfig engineConfig;

  private Logger logger = LoggerFactory.getLogger(ConceptRetryService.class);

  @Autowired
  public ConceptRetryService(ConceptService conceptService, PlatformConfig engineConfig) {
    this.conceptService = conceptService;
    this.engineConfig = engineConfig;
  }

  @Retryable(include = {HeuristicRollbackException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class}, maxAttempts = 20,
      backoff = @Backoff(maxDelay = 200, multiplier = 5, random = true))
  @Async("fd-tag")
  public Future<Void> trackConcepts(Iterable<TrackResultBean> resultBeans)
      throws InterruptedException, ExecutionException, FlockException {
    doRegister(resultBeans);
    return new AsyncResult<>(null);
  }

  @Transactional
  void doRegister(Iterable<TrackResultBean> resultBeans) throws InterruptedException, FlockException, ExecutionException {
    if (!engineConfig.isConceptsEnabled()) {
      return;
    }

    logger.debug("Register concepts");
    conceptService.registerConcepts(resultBeans);
    logger.debug("Completed concept registrations");

  }


}
