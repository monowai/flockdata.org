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

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.PostConstruct;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.EsSearchResult;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.SearchResult;
import org.flockdata.search.SearchResults;
import org.flockdata.search.TagSearchChange;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.base.IndexMappingService;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.base.TagChangeWriter;
import org.flockdata.search.integration.GraphResultGateway;
import org.flockdata.track.bean.SearchChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Service endpoint to write incoming search requests via an ElasticSearch implementation
 *
 * @author mholdsworth
 * @tag ElasticSearch, Entity, Search
 * @since 15/02/2016
 */
@Service
@Qualifier("esSearchWriter")
@DependsOn("searchConfig")
@Primary
public class EsSearchWriter implements SearchWriter {

  private final IndexMappingService indexMappingService;

  private final EntityChangeWriter entityWriter;

  private final TagChangeWriter tagWriter;

  private boolean fdServer = false;

  private GraphResultGateway graphGateway;

  private Logger logger = LoggerFactory.getLogger(EsSearchWriter.class);

  @Autowired
  public EsSearchWriter(TagChangeWriter tagWriter, EntityChangeWriter entityWriter, IndexMappingService indexMappingService, Environment environment) {
    this.tagWriter = tagWriter;
    this.entityWriter = entityWriter;
    this.indexMappingService = indexMappingService;
    this.fdServer = environment.acceptsProfiles("fd-server");
  }

  @Autowired(required = false)
  void setGraphGateway(GraphResultGateway graphResultGateway) {
    this.graphGateway = graphResultGateway;
  }

  /**
   * Triggered by the Coordinator, this is the payload that is required to be indexed
   * <p>
   * Handles scenarios where the content exists or doesn't
   *
   * @param changes to process
   */
  public SearchResults createSearchableChange(SearchChanges changes) {
    Iterable<SearchChange> thisChange = changes.getChanges();
    logger.debug("Received request to index Batch {}", changes.getChanges().size());

    SearchResults results = new SearchResults();
    Collection<SearchResult> searchResults = new ArrayList<>(changes.getChanges().size());

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
        }
        return results;
      }

      indexMappingService.ensureIndexMapping(searchChange);
//    public EsSearchResult(SearchChange thisChange) {
//        this();
//        this.entityId = thisChange.getId();
//        this.fortress = thisChange.getFortressName();
//        this.searchKey = thisChange.getSearchKey();
//        this.documentType = thisChange.getDocumentType();
//        this.key = thisChange.getKey();
//        this.indexName = thisChange.getIndexName();
//
//        setLogId(thisChange.getLogId());
//        setEntityId(thisChange.getId());
//    }
      EsSearchResult result;
      if (searchChange.isType(SearchChange.Type.ENTITY)) {
        EntitySearchChange esc = entityWriter.handle((EntitySearchChange) searchChange);
        result = EsSearchResult.builder()
            .entityId(esc.getId())
            .fortress(esc.getFortressName())
            .searchKey(esc.getSearchKey())
            .documentType(esc.getDocumentType())
            .key(esc.getKey())
            .indexName(esc.getIndexName())
            .logId(esc.getLogId())
            .build();
      } else {
        TagSearchChange esc = tagWriter.handle((TagSearchChange) searchChange);
        result = EsSearchResult.builder()
            .entityId(esc.getId())
            .fortress(esc.getFortressName())
            .searchKey(esc.getSearchKey())
            .documentType(esc.getDocumentType())
            .key(esc.getKey())
            .indexName(esc.getIndexName())
            .logId(esc.getLogId())
            .build();
      }


      // Used to tie the fact that the doc was updated back to the engine
      if (searchChange.isReplyRequired()) {
        searchResults.add(result);
        logger.trace("Dispatching searchResult to fd-engine {}", result);
      } else {
        logger.trace("No reply required");
      }
    }
    results = new SearchResults();
    results.setSearchResults(searchResults);

    if (results.isEmpty()) {
      logger.debug("No results to return");
    } else if (!fdServer) {
      // Manually checking as @Profile does not seem to work with an @MessageGateway
      logger.debug("Engine Result Gateway is not enabled. ");
    } else {
      graphGateway.writeSearchResult(results);
      logger.debug("Processed {} requests. Returning [{}] SearchResults", results.getSearchResults().size(), results.getSearchResults().size());

    }
    return results;

  }

  @PostConstruct
  void logStatus() {
    logger.debug("**** Deployed EsSearchWriter.  EngineResultGateway {}", graphGateway != null);
  }

}
