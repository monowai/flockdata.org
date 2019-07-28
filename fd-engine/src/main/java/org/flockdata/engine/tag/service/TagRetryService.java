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

package org.flockdata.engine.tag.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.transaction.HeuristicRollbackException;
import org.flockdata.data.Company;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.schema.IndexRetryService;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Tag, Track
 * @since 26/09/2014
 */

@Service
public class TagRetryService {

  private final TagService tagService;

  private final IndexRetryService indexRetryService;

  private SearchServiceFacade searchService;
  private Logger logger = LoggerFactory.getLogger(TagRetryService.class);

  @Autowired
  public TagRetryService(TagService tagService, IndexRetryService indexRetryService) {
    this.tagService = tagService;
    this.indexRetryService = indexRetryService;
  }

  @Autowired(required = false)
  void setSearchServiceFacade(SearchServiceFacade searchService) {
    this.searchService = searchService;
  }

  @Retryable(include = {FlockException.class,
      HeuristicRollbackException.class,
      DataIntegrityViolationException.class,
      EntityNotFoundException.class,
      IllegalStateException.class,
      ConcurrencyFailureException.class,
      DeadlockDetectedException.class,
      ConstraintViolationException.class,
      TransactionFailureException.class},
      maxAttempts = 15,
      backoff = @Backoff(delay = 300, multiplier = 3, random = true))

  @Async("fd-tag")
  public Future<Collection<FdTagResultBean>> createTags(Company company, Collection<TagInputBean> tagInputBeans) throws FlockException, ExecutionException, InterruptedException {
    logger.trace("!!! Create Tags");
    if (tagInputBeans == null || tagInputBeans.isEmpty()) {
      return new AsyncResult<>(new ArrayList<>());
    }

    boolean schemaReady;
    do {
      schemaReady = indexRetryService.ensureUniqueIndexes(tagInputBeans);
    } while (!schemaReady);


    if (tagInputBeans.isEmpty()) {
      return new AsyncResult<>(new ArrayList<>());
    }
    Collection<FdTagResultBean> tagResults = tagService.createTags(company, tagInputBeans);
    if (searchService != null) {
      searchService.makeTagsSearchable(company, tagResults);
    }
    return new AsyncResult<>(tagResults);
  }

}
