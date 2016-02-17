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

package org.flockdata.engine.schema;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.service.SchemaService;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * User: mike
 * Date: 2/12/14
 * Time: 7:48 AM
 */
@EnableRetry
@Service
public class IndexRetryService {

    @Autowired
    private SchemaService schemaService;

    private Logger logger = LoggerFactory.getLogger(IndexRetryService.class);

    @Retryable(include =  {FlockException.class, DeadlockDetectedException.class, InvalidDataAccessResourceUsageException.class},
            maxAttempts = 12, backoff = @Backoff(maxDelay = 300, delay = 20, random = true))
    public Boolean ensureUniqueIndexes(Collection<TagInputBean> tagInputs){
            return schemaService.ensureUniqueIndexes(tagInputs);
    }

}
