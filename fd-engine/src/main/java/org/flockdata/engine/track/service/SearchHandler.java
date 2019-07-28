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
import org.flockdata.helper.FlockException;
import org.flockdata.search.SearchResult;
import org.flockdata.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Invoked when a results are returned from fd-search. Extracted to support unit testing
 *
 * @author mholdsworth
 * @since 12/02/2016
 */
@Service
public class SearchHandler {

  private EntityService entityService;
  private Logger logger = LoggerFactory.getLogger(SearchHandler.class);

  public void handleResults(SearchResults searchResults) {
    Collection<SearchResult> theResults = searchResults.getSearchResults();
    int count = 0;
    int size = theResults.size();
    logger.debug("searchDocSyncResult processing {} incoming search results", size);
    for (SearchResult searchResult : theResults) {
      count++;
      logger.debug("Updating {}/{} from search key =[{}]", count, size, searchResult);
      Long entityId = searchResult.getEntityId();
      if (entityId == null) {
        return;
      }

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

  @Autowired
  public void setEntityService(EntityService entityService) {
    this.entityService = entityService;
  }

}
