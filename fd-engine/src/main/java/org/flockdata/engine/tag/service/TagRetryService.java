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
import org.flockdata.track.service.SchemaService;
import org.flockdata.track.service.TagService;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
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

import javax.transaction.HeuristicRollbackException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 26/09/14
 * Time: 6:43 PM
 */

@Service
public class TagRetryService {

    @Autowired
    private TagService tagService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    IndexRetryService indexRetryService;

    private Logger logger = LoggerFactory.getLogger(TagRetryService.class);

    @Async("fd-track")
    @Retryable(include = {FlockException.class, HeuristicRollbackException.class, DataIntegrityViolationException.class, EntityNotFoundException.class, IllegalStateException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class, ConstraintViolationException.class},
            maxAttempts = 15,
            backoff = @Backoff( delay = 100,  maxDelay = 500, random = true))
    public Future<Collection<Tag>> createTagsFuture(Company company, List<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {
        logger.trace("!!! Create Tags");
        return new AsyncResult<>(createTags(company, tagInputs));
    }

    public Collection<Tag> createTags(Company company, List<TagInputBean> tagInputBeans) throws FlockException {
        if (tagInputBeans.isEmpty())
            return new ArrayList<>();
        boolean schemaReady;
        do {
            schemaReady = indexRetryService.ensureUniqueIndexes(company, tagInputBeans);
        } while (!schemaReady);
        logger.debug("Schema Indexes appear to be in place");

        try {
            return tagService.createTags(company, tagInputBeans);
        } catch (FlockException e) {
            throw (e);
        } catch (IOException | ExecutionException | InterruptedException e) {
            logger.error("Track Error", e);
        }
        return new ArrayList<>();
    }

}
