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

package org.flockdata.search.service;

import org.flockdata.search.*;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.base.IndexMappingService;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.base.TagChangeWriter;
import org.flockdata.search.integration.WriteSearchChanges;
import org.flockdata.track.bean.SearchChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Service endpoint to write incoming search requests via an ElasticSearch implementation
 *
 * @author mholdsworth
 * @since 15/02/2016
 * @tag ElasticSearch, Entity, Search
 */
@Service
@Qualifier("esSearchWriter")
@DependsOn("searchConfig")
@Primary
public class EsSearchWriter implements SearchWriter {

    private final IndexMappingService indexMappingService;

    private final EntityChangeWriter entityWriter;

    private final TagChangeWriter tagWriter;

    private boolean fdServer =false;

    private WriteSearchChanges.EngineResultGateway engineResultGateway;

    private Logger logger = LoggerFactory.getLogger(EsSearchWriter.class);

    @Autowired
    public EsSearchWriter(TagChangeWriter tagWriter, EntityChangeWriter entityWriter, IndexMappingService indexMappingService, Environment environment) {
        this.tagWriter = tagWriter;
        this.entityWriter = entityWriter;
        this.indexMappingService = indexMappingService;
        this.fdServer = environment.acceptsProfiles("fd-server");
    }

    @Autowired(required = false)
    void setEngineResultGateway (WriteSearchChanges.EngineResultGateway engineResultGateway){
        this.engineResultGateway = engineResultGateway;
    }

    /**
     * Triggered by the Engine, this is the payload that is required to be indexed
     *
     * Handles scenarios where the content exists or doesn't
     *
     * @param changes to process
     * @throws java.io.IOException if there is a problem with mapping files. This exception will keep
     *                             the message on the queue until the mapping is fixed
     */
    public SearchResults createSearchableChange(SearchChanges changes) throws IOException {
        Iterable<SearchChange> thisChange = changes.getChanges();
        logger.debug("Received request to index Batch {}", changes.getChanges().size());

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
//                } else if (searchChange.isType(SearchChange.Type.TAG)) {
//                    logger.debug("Delete Tag request");
//                    tagWriter.delete((TagSearchChange) searchChange);
                }
                return results;
            }

            indexMappingService.ensureIndexMapping(searchChange);

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
        if ( results.isEmpty()){
            logger.debug("No results to return");
        } else if ( !fdServer) {
            // Manually checking as @Profile does not seem to work with an @MessageGateway
            logger.debug( "Engine Result Gateway is not enabled. ");
        } else {
            engineResultGateway.writeEntitySearchResult(results);
            logger.debug("Processed {} requests. Returning [{}] SearchResults", results.getSearchResults().size(), results.getSearchResults().size());

        }
        return results;

    }
    @PostConstruct
    void logStatus() {
        logger.debug("**** Deployed EsSearchWriter.  EngineResultGateway {}" , engineResultGateway!=null);
    }

}
