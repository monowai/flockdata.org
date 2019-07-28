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

package org.flockdata.engine.admin.service;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.configure.CacheConfiguration;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.services.SchemaService;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.bean.TrackResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

/**
 * General maintenance activities across fortress entities. Async in nature
 *
 * @author mholdsworth
 * @since 25/03/2015
 */
@Service
public class AdminService implements EngineAdminService {

  private final EntityService entityService;
  private final SchemaService schemaService;
  private final ConceptService conceptService;
  private final FortressService fortressService;
  private final IndexManager indexManager;

  private CacheConfiguration cacheConfiguration;
  private SearchServiceFacade searchService;

  private Logger logger = LoggerFactory.getLogger(AdminService.class);

  @Autowired
  public AdminService(SchemaService schemaService, EntityService entityService, IndexManager indexManager, ConceptService conceptService, FortressService fortressService) {
    this.schemaService = schemaService;
    this.entityService = entityService;
    this.indexManager = indexManager;
    this.conceptService = conceptService;
    this.fortressService = fortressService;
  }

  @Autowired(required = false) // Functional tests don't require it
  private void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
    this.cacheConfiguration = cacheConfiguration;
  }

  @Autowired(required = false) // Functional tests don't require it
  private void setSearchService(SearchServiceFacade searchService) {
    this.searchService = searchService;
  }

  @Async("fd-engine")
  public Future<Boolean> purge(Company company, Fortress fortress, Document documentType, String segmentToDelete) {
    NumberFormat nf = NumberFormat.getInstance();

    StopWatch watch = new StopWatch("Purge Fortress " + fortress);
    watch.start();
    boolean keepRunning;

    Collection<Segment> segments = fortressService.getSegments(fortress);
    Collection<Segment> segmentsToDelete = new ArrayList<>();

//        if (segmentToDelete != null) {
    for (Segment segment : segments) {
      if (segmentToDelete == null || segment.getCode().equalsIgnoreCase(segmentToDelete.toLowerCase())) {
        segmentsToDelete.add(segment);
      }
    }
//        }

    long total = 0;
    String searchIndexToDelete = null;

    if (cacheConfiguration != null) {
      cacheConfiguration.resetCache();
    }

    for (Segment segment : segmentsToDelete) {

      do {
        Collection<String> entities = entityService.getEntityBatch(fortress, documentType, segment, 2000);

        if (searchService != null && searchIndexToDelete == null && segment.getFortress().isSearchEnabled()) {
          if (entities.size() > 0) {
            EntityNode entity = entityService.getEntity(company, entities.iterator().next());
            // We need to get an entity to figure out which search index it is in
            searchIndexToDelete = indexManager.toIndex(entity);
          } else {
            searchIndexToDelete = indexManager.getIndexRoot(segment.getFortress(), documentType);
          }
        }
        entityService.purge(fortress, entities);

        keepRunning = entities.size() > 0;
        total = total + entities.size();
        if (total % 100000 == 0) {
          logger.info("Progress update - {} entities purged ... ", nf.format(total));
        }

      } while (keepRunning);
      if (searchIndexToDelete != null) {
        searchService.purge(searchIndexToDelete);
      }
      logger.info("Purged {}, {}, {}", fortress.getName(), documentType.getName(), segment.getCode());
      searchIndexToDelete = null;
      if (!segment.isDefault()) {
        conceptService.delete(documentType, segment);
      }
    }
    if (segmentToDelete == null && total == 0) // First purge is data only. Delete again to remove the docType
    // Removing the DocumentType
    {
      conceptService.delete(documentType);
    }

    if (cacheConfiguration != null) {
      cacheConfiguration.resetCache();
    }

    return new AsyncResult<>(true);
  }

  @Async("fd-engine")
  public Future<Boolean> purge(Fortress fortress) throws FlockException {
    // Rename the exiting fortress and flag it as deleted.
    // Batch the entities for deletion. Log Content could be stored across multiple KVstores for a
    // single fortress
    NumberFormat nf = NumberFormat.getInstance();

    StopWatch watch = new StopWatch("Purge Fortress " + fortress);
    watch.start();
    boolean keepRunning;
    schemaService.purge(fortress);

    long total = 0;
    do {
      Collection<String> entities = entityService.getEntityBatch(fortress, 2000);
      entityService.purge(fortress, entities);
      keepRunning = entities.size() > 0;
      total = total + entities.size();
      if (total % 100000 == 0) {
        logger.info("Progress update - {} entities purged ... ", nf.format(total));
      }

    } while (keepRunning);
    entityService.purgeFortressDocs(fortress);
    searchService.purge(fortress);
    fortressService.purge(fortress);
    if (cacheConfiguration != null) {
      cacheConfiguration.resetCache();
    }
    watch.stop();
    logger.info("Completed purge. Removed " + nf.format(total) + " entities for fortress " + fortress);

    return new AsyncResult<>(true);

  }

  @Async("fd-engine")
  public Future<Collection<String>> validateFromSearch(Company company, Fortress fortress, String docType) throws FlockException {
    Collection<String> errors = new ArrayList<>();
    int start = 0;
    int size = 1000;
    boolean continueSearch;

    QueryParams qp;
    try {
      qp = getPagedQueryParams(company, size, docType);
    } catch (IOException e) {
      logger.error("Admin Query Issue", e);
      errors.add(e.getMessage());
      return new AsyncResult<>(errors);
    }

    Collection<Map<String, Object>> searchResults;
    int rowsAnalyzed = 0;
    NumberFormat nf = NumberFormat.getInstance();
    String totalHits = null;
    StopWatch watch = new StopWatch("Validate Search Docs " + company.getName() + "/" + docType);
    watch.start();
    do {
      qp.setFrom(start);

      EsSearchRequestResult searchResult = new EsSearchRequestResult();//queryService.search(company, qp);
      Map<String, Object> results = searchResult.getRawResults();

      Map<String, Object> hits = (Map<String, Object>) results.get("hits");
      if (totalHits == null) {
        totalHits = hits.get("total").toString();
        logger.info("Analyzing " + nf.format(Integer.parseInt(totalHits)) + " Entities");
      }
      searchResults = (Collection<Map<String, Object>>) hits.get("hits");

      Map<String, String> searchKeys = getKeys(searchResults);

      if (!searchKeys.isEmpty()) {
        validateEntities(company, errors, searchKeys);
      }

      start = start + size;

      if (start % 100000 == 0) {
        logger.info("Progress update - {} entities analyzed ...", nf.format(start));
      }

      rowsAnalyzed = rowsAnalyzed + searchResults.size();
      continueSearch = searchResults.size() != 0;

    } while (continueSearch);
    watch.stop();
    logger.info("Finished  - validated " + nf.format(rowsAnalyzed) + " entities out of a reported hit count of " + nf.format(Integer.parseInt(totalHits)) + (errors.isEmpty() ? ". No problems found" : " " + nf.format(errors.size()) + ". errors found"));

    if (errors.isEmpty()) {
      errors.add("No problems found!");
    }

    return new AsyncResult<>(errors);

  }

  private QueryParams getPagedQueryParams(Company company, int size, String docType) throws IOException {
    String esQuery = "{\n" +
        "  \"query\": {\n" +
        "    \"query_string\": {\n" +
        "      \"query\": \"*\"\n" +
        "    }\n" +
        "  }\n" +
        "}";
    QueryParams qp;
    qp = JsonUtils.toObject(esQuery.getBytes(), QueryParams.class);

    qp.setCompany(company.getName().toLowerCase());
    qp.setTypes(docType.toLowerCase());
    qp.setSearchText("*");
    ArrayList<String> fields = new ArrayList<>();
    fields.add("key");
    fields.add("_id");
    qp.setFields(fields);
    qp.setSize(size);
    return qp;
  }

  // Extracts the searchKey and the key in to a map for analysis
  private Map<String, String> getKeys(Collection<Map<String, Object>> searchResults) {
    Map<String, String> searchKeys = new HashMap<>();
    for (Map<String, Object> result : searchResults) {
      String searchKey = result.get("_id").toString();
      Map fieldResult = (Map) result.get("fields");
      Collection keyResult = (Collection) fieldResult.get("key");
      String key = keyResult.iterator().next().toString();
      searchKeys.put(key, searchKey);
    }
    return searchKeys;
  }

  private void validateEntities(Company company, Collection<String> errors, Map<String, String> searchKeys) {
    Map<String, EntityNode> entities = entityService.getEntities(company, searchKeys.keySet());
    if (entities.size() != searchKeys.size()) {
      for (String key : entities.keySet()) {
        EntityNode e = entities.get(key);
        if (e == null) {
          String message = "Didn't find key " + key;
          errors.add(message);
          logger.error(message);
        }
        String searchKey = searchKeys.get(key);
        if (e != null && !e.getSearchKey().equals(searchKey)) {
          String message = "SearchKey mismatch for key " + key + ". Found " + searchKey + " in ES, but the entity expected it as " + e.getSearchKey();
          errors.add(message);
          logger.error(message);
        }

      }
    }
  }

  @Override
  @Async("fd-track")
  @Transactional
  public Future<Long> doReindex(Fortress fortress) throws FlockException {
    long reindexCount = reindex(fortress);
    logger.info("Reindex Search request completed. Processed [" + reindexCount + "] entities for [" + fortress.getName() + "]");
    return new AsyncResult<>(reindexCount);
  }

  @Override
  @Async("fd-engine")
  @Transactional
  public Future<Long> doReindex(Fortress fortress, String docType) {
    long result = reindexByDocType(fortress, docType);
    logger.info("Reindex Search request completed. Processed [" + result + "] entities for [" + fortress.getName() + "]");
    return new AsyncResult<>(result);

  }

  @Override
  public Future<Long> doReindex(Fortress fortress, Entity entity) throws FlockException {
    Collection<Entity> entities = new ArrayList<>();
    entities.add(entity);
    reindexEntities(entities, 0L);
    return new AsyncResult<>(1L);
  }

  public void doReindex(Entity entity) throws FlockException {
    try {
      doReindex(entity.getFortress(), entity).get(3000L, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      logger.error(e.getMessage());
      throw new FlockException(e.getMessage());
    }
  }

  long reindex(Fortress fortress) {
    Long lastEntityId = 0L;
    Long processed = 0L;
    Collection<Entity> entities;
    do {
      entities = entityService.getEntities(fortress, lastEntityId);
      processed = processed + entities.size();
      if (entities.isEmpty()) {
        return processed;
      }
      lastEntityId = reindexEntities(entities, lastEntityId);

    } while (!entities.isEmpty());
    return processed;
  }

  long reindexByDocType(Fortress fortress, String docType) {
    Long lastEntityId = 0L;
    Long processed = 0L;

    Collection<Entity> entities;
    do {
      entities = entityService.getEntities(fortress, docType, lastEntityId);
      processed = processed + entities.size();
      if (entities.isEmpty()) {
        return lastEntityId;
      }
      reindexEntities(entities, lastEntityId);

    } while (!entities.isEmpty());
    return processed;
  }

  @Async("fd-track")
  @Transactional
  Long reindexEntities(Collection<Entity> entities, Long lastEntityId) {

    Collection<SearchChange> searchDocuments = new ArrayList<>(entities.size());
    for (Entity entity : entities) {
      EntityLog lastLog = entityService.getLastEntityLog(entity.getId());
      DocumentNode documentType = conceptService.findDocumentType(entity.getFortress(), entity.getType());
      TrackResultBean trackResultBean = new TrackResultBean(entity, documentType);
      // How to get the Linked Entities? From the model
      //Map<String, List<EntityKeyBean>> linkedEntities = entityService.getLinkedEntities(entity);

//            searchService.getEntityChange(trackResultBean,lastLog);
      EntitySearchChange searchDoc = searchService.rebuild(entity, lastLog);
      lastEntityId = entity.getId();
      if (searchDoc != null && entity.getFortress().isSearchEnabled() && !entity.isSearchSuppressed()) {
        searchDocuments.add(searchDoc);
      }
    }
    searchService.makeChangesSearchable(searchDocuments);
    return lastEntityId;
  }
}
