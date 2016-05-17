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
import org.flockdata.search.base.TagChangeWriter;
import org.flockdata.search.integration.WriteEntityChange;
import org.flockdata.search.model.*;
import org.flockdata.track.bean.SearchChange;
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
 * Service endpoint to write incoming search requests via an ElasticSaerch implementation
 * <p>
 * Created by mike on 15/02/16.
 */
@Service
@Qualifier("esSearchWriter")
@DependsOn("searchConfig")
public class EsSearchWriter implements SearchWriter {

    @Autowired
    private IndexMappingService indexMappingService;

    @Autowired
    private EntityChangeWriter entityWriter;

    @Autowired
    private TagChangeWriter tagWriter;

    @Autowired(required = false)
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
    public SearchResults createSearchableChange(SearchChanges changes) throws IOException {
        Iterable<SearchChange> thisChange = changes.getChanges();
        logger.debug("Received request to index Batch {}", changes.getChanges().size());
        Map<String, Boolean> checked = new HashMap<>();
        SearchResults results = new SearchResults();

        for (SearchChange searchChange : thisChange) {
            if (searchChange == null) {
                logger.error("Null search change received. Retry your operation with data!");
                return results;
            }
            logger.debug("searchRequest received for {}", searchChange);

            if (searchChange.isDelete()) {
                if (searchChange.isType(SearchChange.Type.ENTITY)) {
                    logger.debug("Delete Entity request");
                    entityWriter.delete((EntitySearchChange) searchChange);
                } else if (searchChange.isType(SearchChange.Type.TAG)) {
                    logger.debug("Delete Tag request");
                    tagWriter.delete((TagSearchChange) searchChange);
                }
                return results;
            }
            if (checked.isEmpty() && !checked.containsKey(searchChange.getIndexName() + "/" + searchChange.getDocumentType())) {
                entityWriter.purgeCache();
                // Batches must be for the same fortress/doctype combo
                indexMappingService.ensureIndexMapping(searchChange);
                String key = searchChange.getIndexName() + "/" + searchChange.getDocumentType();
                checked.put(key, true);
            }
            SearchResult result;
            if (searchChange.isType(SearchChange.Type.ENTITY))
                result = new SearchResult(
                        entityWriter.handle((EntitySearchChange) searchChange));
            else
                result = new SearchResult(
                        tagWriter.handle((TagSearchChange) searchChange));


            // Used to tie the fact that the doc was updated back to the engine
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
