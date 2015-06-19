/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Company;
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
    IndexRetryService indexRetryService;

    private Logger logger = LoggerFactory.getLogger(TagRetryService.class);

    @Retryable(include = {FlockException.class, HeuristicRollbackException.class, DataIntegrityViolationException.class, EntityNotFoundException.class, IllegalStateException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class, ConstraintViolationException.class},
            maxAttempts = 15,
            backoff = @Backoff( delay = 300,  multiplier = 3, random = true))

    public Collection<TagResultBean> createTags(Company company, List<TagInputBean> tagInputBeans, boolean suppressRelationships) throws FlockException, ExecutionException, InterruptedException {
        logger.trace("!!! Create Tags");
        boolean schemaReady;
        do {
            schemaReady = indexRetryService.ensureUniqueIndexes(company, tagInputBeans);
        } while (!schemaReady);


        if (tagInputBeans.isEmpty())
            return new ArrayList<>();
        try {
            return tagService.createTags(company, tagInputBeans);
        } catch (FlockException e) {
            throw (e);
        }
    }

    @Async("fd-tag")
    public Future<Collection<TagResultBean>> createTagsFuture(Company company, List<TagInputBean> tags) throws InterruptedException, ExecutionException, FlockException {
        return new AsyncResult<>(createTags(company, tags, false));
    }
}
