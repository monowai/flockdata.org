/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import org.flockdata.helper.FlockException;
import org.flockdata.search.model.SearchResult;
import org.flockdata.search.model.SearchResults;
import org.flockdata.track.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Invoked when a results are returned from fd-search. Extracted to support unit testing
 *
 * Created by mike on 12/02/16.
 */
@Service
public class SearchHandler {

    private Logger logger = LoggerFactory.getLogger(SearchHandler.class);

    @Autowired
    EntityService entityService;

    public void handlResults(SearchResults searchResults) {
        Collection<SearchResult> theResults = searchResults.getSearchResults();
        int count = 0;
        int size = theResults.size();
        logger.debug("searchDocSyncResult processing {} incoming search results", size);
        for (SearchResult searchResult : theResults) {
            count++;
            logger.debug("Updating {}/{} from search key =[{}]", count, size, searchResult);
            Long entityId = searchResult.getEntityId();
            if (entityId == null)
                return;

            try {
                entityService.recordSearchResult(searchResult, entityId);
            } catch (FlockException e) {
                logger.error("Unexpected error recording searchResult for entityId " + entityId, e);
            }
        }
        logger.trace("Finished processing search results");
    }

    public EntityService getEntityService() {
        return entityService;
    }

    public void setEntityService(EntityService entityService) {
        this.entityService = entityService;
    }
}
