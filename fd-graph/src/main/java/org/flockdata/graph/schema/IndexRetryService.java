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

package org.flockdata.graph.schema;

import java.util.Collection;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Administration, Neo4j, Index
 * @since 2/12/2014
 */
@EnableRetry
@Service
public class IndexRetryService {

  private final SchemaService schemaService;

  @Autowired
  public IndexRetryService(SchemaService schemaService) {
    this.schemaService = schemaService;
  }

  @Retryable(include = {FlockException.class,},
      maxAttempts = 12, backoff = @Backoff(maxDelay = 300, delay = 20, random = true))
  public Boolean ensureUniqueIndexes(Collection<TagInputBean> tagInputs) {
    return schemaService.ensureUniqueIndexes(tagInputs);
  }

}
