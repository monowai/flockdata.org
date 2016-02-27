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

package org.flockdata.search.service;

import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.integration.WriteEntityChange;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.search.model.SearchResult;
import org.flockdata.search.model.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 15/02/16.
 */
@Service
@Qualifier("esSearchWriter")
@DependsOn("searchConfig")
public class EsSearchWriter implements SearchWriter {

    @Autowired
    private IndexMappingService indexMappingService;

    @Autowired
    private EntityChangeWriter trackSearch;

    @Autowired (required = false)
    WriteEntityChange.EngineResultGateway engineResultGateway;

    private Logger logger = LoggerFactory.getLogger(EsSearchWriter.class);

    /**
     * Triggered by the Engine, this is the payload that is required to be indexed
     * <p/>
     * Handles scenarios where the content exists or doesn't
     *
     * @param changes to process
     * @throws java.io.IOException if there is a problem with mapping files. This exception will keep
     *                             the message on the queue until the mapping is fixed
     */
    public SearchResults createSearchableChange(EntitySearchChanges changes) throws IOException {
        Iterable<EntitySearchChange> thisChange = changes.getChanges();
        logger.debug("Received request to index Batch {}", changes.getChanges().size());
        Map<String, Boolean> checked = new HashMap<>();
        SearchResults results = new SearchResults();
//        boolean mappingChecked = false;
        for (EntitySearchChange searchChange : thisChange) {
            if (searchChange == null) {
                logger.error("Null search change received. Retry your operation with data!");
                return results;
            }
            logger.debug("searchRequest received for {}", searchChange);

            if (searchChange.isDelete()) {
                logger.debug("Delete request");
                trackSearch.delete(searchChange);
                return results;
            }
            if (checked.isEmpty() && !checked.containsKey(searchChange.getIndexName() + "/" + searchChange.getDocumentType())) {
                trackSearch.purgeCache();
                // Batches must be for the same fortress/doctype combo
                indexMappingService.ensureIndexMapping(searchChange);
                String key = searchChange.getIndexName() + "/" + searchChange.getDocumentType();
                checked.put(key, true);
            }

            SearchResult result = new SearchResult(
                    trackSearch.handle(searchChange)
            );

            // Used to tie the fact that the doc was updated back to the engine
            result.setLogId(searchChange.getLogId());
            result.setEntityId(searchChange.getEntityId());
            if (searchChange.isReplyRequired()) {
                results.addSearchResult(result);
                logger.trace("Dispatching searchResult to fd-engine {}", result);
            } else {
                logger.trace("No reply required");
            }

        }
        if (!results.isEmpty() && engineResultGateway != null) {
            logger.debug("Processed {} requests. Returning [{}] SearchResults", results.getSearchResults().size(), results.getSearchResults().size());
            engineResultGateway.writeEntitySearchResult(results);
        }
        return results;

    }
}
