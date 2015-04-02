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

package org.flockdata.engine.tag.service;

import org.flockdata.engine.schema.service.IndexRetryService;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.service.TagService;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.transaction.HeuristicRollbackException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 26/09/14
 * Time: 6:43 PM
 */

@EnableRetry
@Service
public class TagRetryService {

    @Autowired
    private TagService tagService;

    @Autowired
    private IndexRetryService indexRetryService;

    private Logger logger = LoggerFactory.getLogger(TagRetryService.class);

    @Retryable(include =  {HeuristicRollbackException.class, ConstraintViolationException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class},
            maxAttempts = 12,
            backoff =@Backoff(maxDelay = 200, multiplier = 2, random = true))
    public Collection<Tag> createTags(Company company, List<TagInputBean> tagInputBeans) throws InterruptedException, FlockException, ExecutionException, IOException {
        return tagService.createTags(company, tagInputBeans);
    }

    @Async("fd-track")
    public Future<Collection<Tag>> createTagsFuture(Company company, List<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {

        if ( tagInputs.isEmpty())
            return null;
        boolean schemaReady ;
        do {
            schemaReady = indexRetryService.ensureUniqueIndexes(company, tagInputs);
        } while (!schemaReady);
        logger.debug("Schema Indexes appear to be in place");

        Collection<Tag> results;
        try {
            results = createTags(company, tagInputs);
        } catch (IOException e) {
            logger.error("Unexpected", e);
            throw new FlockException("IO Exception", e);
        }
        return new AsyncResult<>(results);
    }


}
