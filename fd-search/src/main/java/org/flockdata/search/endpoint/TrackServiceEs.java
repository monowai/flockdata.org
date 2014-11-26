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

package org.flockdata.search.endpoint;

import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.search.model.SearchResult;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.TrackSearchDao;
import org.flockdata.search.model.SearchResults;
import org.flockdata.search.service.EngineGateway;
import org.flockdata.search.service.TrackService;
import org.flockdata.track.model.SearchChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Services TRACK requests from the Engine
 * User: mike
 * Date: 12/04/14
 * Time: 6:23 AM
 */
@Service
@Transactional
@MessageEndpoint
public class TrackServiceEs implements TrackService {
    @Autowired
    private TrackSearchDao trackSearch;

    private Logger logger = LoggerFactory.getLogger(TrackServiceEs.class);

    @Autowired(required = false)
    private EngineGateway engineGateway;

    static final ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    /**
     * Triggered by the Engine, this is the payload that is required to be indexed
     * <p/>
     * It may or may not already exist.
     *
     * @param changes to process
     * @throws java.io.IOException if there is a problem with mapping files. This exception will keep
     * the message on the queue until the mapping is fixed
     */
    @Override
    public SearchResults createSearchableChange(EntitySearchChanges changes) throws IOException {
        Iterable<EntitySearchChange> thisChange = changes.getChanges();
        logger.debug("Received request to index Batch {}", changes.getChanges().size());
        SearchResults results = new SearchResults();
        int processed = 0;
        for (SearchChange searchChange : thisChange) {
            processed++;
            logger.trace("searchRequest received for {}", searchChange);

            if (searchChange.isDelete()) {
                logger.debug("Delete request");
                trackSearch.delete(searchChange);
                return results;
            }
            SearchResult result = new SearchResult(trackSearch.update(searchChange));

            // Used to tie the fact that the doc was updated back to the engine
            result.setLogId(searchChange.getLogId());
            result.setMetaId(searchChange.getEntityId());
            if (searchChange.isReplyRequired()) {
                results.addSearchResult(result);
                logger.trace("Dispatching searchResult to fd-engine {}", result);
            } else {
                logger.trace("No reply required");
            }

        }

        if (!results.isEmpty()) {
            logger.debug("Processed {} requests. Sending back {} SearchChanges", processed, results.getSearchResults().size());
            engineGateway.handleSearchResult(results);
        }
        return results;

    }
    @Override
    @ServiceActivator(inputChannel = "syncSearchDocs", requiresReply = "false") // Subscriber
    public Boolean createSearchableChange(byte[] bytes) throws IOException {
        SearchResults results = createSearchableChange(objectMapper.readValue(bytes, EntitySearchChanges.class));
        return true;
        //return results;

    }


        @Override
    public void delete(Entity entity) {
        //trackDao.delete(entity, null);
    }

    @Override
    public byte[] findOne(Entity entity) {
        return null;
        //return trackDao.findOne(entity);
    }

    @Override
    public byte[] findOne(Entity entity, String id) {
        return null;
        //return trackDao.findOne(entity, id);
    }

}
