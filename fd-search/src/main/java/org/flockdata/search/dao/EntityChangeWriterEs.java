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

package org.flockdata.search.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.rest.RestStatus;
import org.flockdata.data.Entity;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.configure.SearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @since 27/04/2013
 */
@Service
public class EntityChangeWriterEs implements EntityChangeWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityChangeWriterEs.class);
  private final SearchConfig searchConfig;

  @Autowired
  public EntityChangeWriterEs(SearchConfig searchConfig) {
    this.searchConfig = searchConfig;
  }

  @Override
  public boolean delete(EntitySearchChange searchChange) {
    String indexName = searchChange.getIndexName();
    String recordType = searchChange.getDocumentType();

    String existingIndexKey = searchChange.getSearchKey();

    DeleteResponse dr = searchConfig.getClient().prepareDelete(searchChange.getIndexName(), recordType, existingIndexKey)
//                .setRouting(searchChange.getCode())
        .execute()
        .actionGet();

    if (dr.status() == RestStatus.NOT_FOUND) {
      LOGGER.debug("Didn't find the document to remove [{}] from {}/{}", existingIndexKey, indexName, recordType);
      return false;// Not found
    }
    LOGGER.debug("Removed document [{}] from {}/{}", existingIndexKey, indexName, recordType);
    return true;
  }

  /**
   * @param searchChange object containing changes
   * @param source       Json to save
   * @return key value of the child document
   */
  private EntitySearchChange save(EntitySearchChange searchChange, String source) {
    String indexName = searchChange.getIndexName();
    String documentType = searchChange.getDocumentType();
    LOGGER.debug("Received request to Save [{}] SearchKey [{}]", searchChange.getKey(), searchChange.getSearchKey());

    // Rebuilding a document after a reindex - preserving the unique key.
    IndexRequestBuilder irb =
        searchConfig.getClient()
            .prepareIndex(searchChange.getIndexName(), documentType)
            .setSource(source, XContentType.JSON);

    irb.setId(searchChange.getSearchKey());

    try {
      IndexResponse ir = irb.execute().actionGet();

      LOGGER.debug("Save:Document entityId [{}], [{}], logId= [{}] searchKey [{}] index [{}/{}]",
          searchChange.getId(),
          searchChange.getKey(),
          searchChange.getLogId(),
          ir.getId(),
          indexName,
          documentType);

      return searchChange;
    } catch (MapperParsingException e) {
      // DAT-359
      LOGGER.error("Parsing error {} - code [{}], key [{}], [{}]", indexName, searchChange.getCode(), searchChange.getKey(), e.getMessage());
      throw new AmqpRejectAndDontRequeueException("Parsing error - callerRef [" + searchChange.getCode() + "], key [" + searchChange.getKey() + "], " + e.getMessage(), e);
    } catch (Exception e) {
      LOGGER.error("Writing to index {} produced an error [{}]", indexName, e.getMessage());
      throw new AmqpRejectAndDontRequeueException("Parsing error - callerRef [" + searchChange.getCode() + "], key [" + searchChange.getKey() + "], " + e.getMessage(), e);
    }

  }


  @Override
  public EntitySearchChange handle(EntitySearchChange searchChange) {
    String source = getJsonToIndex(searchChange);

    if (searchChange.getSearchKey() == null || searchChange.getSearchKey().equals("")) {
      searchChange.setSearchKey((searchChange.getCode() == null ? searchChange.getKey() : searchChange.getCode()));
      LOGGER.debug("No search key, creating as a new document [{}]", searchChange.getKey());
      return save(searchChange, source);
    }

    try {
      LOGGER.debug("Update request for searchKey [{}], key[{}]", searchChange.getSearchKey(), searchChange.getKey());

      GetRequestBuilder request =
          searchConfig.getClient().prepareGet(searchChange.getIndexName(),
              searchChange.getDocumentType(),
              searchChange.getSearchKey());

//            if (searchChange.getParent() != null) {
//                request.setRouting(searchChange.getParent().getCode());
//            }


      GetResponse response = request.execute()
          .actionGet();

      LOGGER.debug("executed get request for {}", searchChange.toString());
      if (response.isExists() && !response.isSourceEmpty()) {
        LOGGER.debug("Document exists!");
        // Messages can be received out of sequence
        // Check to ensure we don't accidentally overwrite a more current
        // document with an older one. We assume the calling fortress understands
        // what the most recent doc is.
        Object o = response.getSource().get(SearchSchema.UPDATED); // fortress view of UPDATED, not FlockDatas!
        if (o != null) {

          Long existingWhen = Long.decode(o.toString());
          LOGGER.debug("Comparing searchChange when {} with stored when {}", searchChange.getUpdatedDate(), existingWhen);
          if (!searchChange.isForceReindex()) {
            if (existingWhen.compareTo(searchChange.getUpdatedDate().getTime()) > 0) {
              LOGGER.debug("ignoring a request to update as the existing document dated [{}] is newer than the searchChange document dated [{}]", new Date(existingWhen), searchChange.getUpdatedDate());
              return searchChange; // Don't overwrite the most current doc!
            } else if (searchChange.getUpdatedDate().getTime() == 0L && !searchChange.isReplyRequired()) {
              // Meta Change - not indexed in FD, so ignore something we already have.
              // Likely scenario is a batch is being reprocessed
              return searchChange;
            }
            LOGGER.debug("Document is more recent. Proceeding with update");
          } else {
            LOGGER.debug("Forcing an update of the document.");
          }
        }
      } else {
        // No response, to a search key we expect to exist. Create a new one
        // Likely to be in response to rebuilding an ES index from Graph data.
        LOGGER.debug("About to create in response to an update request for {}", searchChange.toString());
        return save(searchChange, source);
      }

      // Update the existing document with the searchChange change
      IndexRequestBuilder update = searchConfig.getClient()
          .prepareIndex(searchChange.getIndexName(), searchChange.getDocumentType(), searchChange.getSearchKey());
//            update.setRouting(searchChange.getCode());

      ListenableActionFuture<IndexResponse> ur = update.setSource(source).
          execute();

      if (LOGGER.isDebugEnabled()) {
        IndexResponse indexResponse = ur.actionGet();
        LOGGER.debug("Updated [{}] logId=[{}] for [{}] to version [{}]", searchChange.getSearchKey(), searchChange.getLogId(), searchChange, indexResponse.getVersion());
      }
//        } catch (IndexMissingException e) { // administrator must have deleted it, but we think it still exists
//            LOGGER.info("Attempt to update non-existent index [{}]. Creating it..", searchChange.getRootIndex());
//            purgeCache();
//            return save(searchChange, source);
    } catch (NoShardAvailableActionException e) {
      return save(searchChange, source);
    }
    return searchChange;
  }


  //@CacheEvict(value = {"mappedIndexes"}, allEntries = true)
  public void purgeCache() {

  }

  public Map<String, Object> findOne(Entity entity) {
    return findOne(entity, null);
  }

  public Map<String, Object> findOne(Entity entity, String id) {
    String indexName = searchConfig.getIndexManager().toIndex(entity);//entity.getFortress().getRootIndex();
    String documentType = entity.getType();
    if (id == null) {
      id = entity.getSearchKey();
    }
    LOGGER.debug("Looking for [{}] in {}", id, indexName + documentType);

    GetResponse response = searchConfig.getClient().prepareGet(indexName, documentType, id)
        //.setRouting(entity.getKey())
        .execute()
        .actionGet();

    if (response != null && response.isExists() && !response.isSourceEmpty()) {
      return response.getSource();
    }

    LOGGER.info("Unable to find response data for [" + id + "] in " + indexName + "/" + documentType);
    return null;
  }

  @Override
  public Map<String, Object> ping() {
    Map<String, Object> results = new HashMap<>();
    ClusterHealthResponse response;
    try {
      response = searchConfig.getClient().admin()
          .cluster()
          .health(new ClusterHealthRequest())
          .actionGet();
    } catch (NoNodeAvailableException e) {
      // Node may become available, so we will not stop the service
      results.put("status", e.getMessage());
      LOGGER.error(e.getMessage());
      return results;
    }

    results.put("health", response.getStatus().name());
    results.put("dataNodes", response.getNumberOfDataNodes());
    results.put("nodes", response.getNumberOfNodes());
    results.put("clusterName", response.getClusterName());
    results.put("Status", "ok");
    results.put("nodeName", searchConfig.getClient().settings().get("name"));

    return results;
  }

  private String getJsonToIndex(EntitySearchChange searchChange) {
    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
    Map<String, Object> index = EntityChangeMapper.getMapFromChange(searchChange);
    try {
      return mapper.writeValueAsString(index);
    } catch (JsonProcessingException e) {

      LOGGER.error(e.getMessage());
    }
    return null;
  }


}
