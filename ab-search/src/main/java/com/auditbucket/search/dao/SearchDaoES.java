/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.search.dao;

import com.auditbucket.audit.model.AuditSearchDao;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.audit.model.SearchChange;
import com.auditbucket.search.model.MetaSearchSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * User: Mike Holdsworth
 * Date: 27/04/13
 * Time: 12:00 PM
 */
@Repository("esAuditChange")
public class SearchDaoES implements AuditSearchDao {

    private static final String NOT_ANALYZED = "not_analyzed";
    @Autowired
    private Client esClient;

    private Logger logger = LoggerFactory.getLogger(SearchDaoES.class);

    @Override
    public void delete(MetaHeader header, String existingIndexKey) {
        String indexName = header.getIndexName();
        String recordType = header.getDocumentType();

        if (existingIndexKey == null)
            existingIndexKey = header.getSearchKey();

        DeleteResponse dr = esClient.prepareDelete(indexName, recordType, existingIndexKey)
                //.setRouting(header.getMetaKey())
                .execute()
                .actionGet();

        if (logger.isDebugEnabled()) {
            if (!dr.isFound())
                logger.debug("Didn't find the document to remove [" + existingIndexKey + "] from " + indexName + "/" + recordType);
            else
                logger.debug("Removed document [" + existingIndexKey + "] from " + indexName + "/" + recordType);
        }

    }

    /**
     * @param searchChange object containing changes
     * @return key value of the child document
     */
    private SearchChange save(SearchChange searchChange) {
        String indexName = searchChange.getIndexName();
        String documentType = searchChange.getDocumentType();
        logger.debug("Received request to Save [{}]", searchChange.getMetaKey());

        ensureIndex(indexName, documentType);
        ensureMapping(indexName, documentType);

        String source = makeIndexJson(searchChange);
        IndexRequestBuilder irb = esClient.prepareIndex(indexName, documentType)
                .setSource(source);
        //.setRouting(searchChange.getMetaKey());

        String searchKey = searchChange.getSearchKey();
        if (searchKey == null && searchChange.getCallerRef() != null)
            searchKey = searchChange.getCallerRef().toLowerCase();

        // Rebuilding a document after a reindex - preserving the unique key.
        if (searchKey != null) {
            irb.setId(searchKey);
        }


        try {
            IndexResponse ir = irb.execute().actionGet();
            searchChange.setSearchKey(ir.getId());

            if (logger.isDebugEnabled())
                logger.debug("Added Document [" + searchChange.getMetaKey() + "], logId=" + searchChange.getLogId() + " searchId [" + ir.getId() + "] to " + indexName + "/" + documentType);
            return searchChange;
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return searchChange;
        }

    }

    private void ensureMapping(String indexName, String documentType) {
        XContentBuilder mappingEs = mapping(documentType);
        // Test if Type exist
        String[] indexNames = new String[1];
        indexNames[0] = indexName;
        String[] documentTypes = new String[1];
        documentTypes[0] = documentType;

        boolean hasType = esClient.admin().indices().typesExists(new TypesExistsRequest(indexNames, documentTypes)).actionGet().isExists();
        if (!hasType) {
            // Type Don't exist ==> Insert Mapping
            if (mappingEs != null) {
                esClient.admin().indices()
                        .preparePutMapping(indexName)
                        .setType(documentType)
                        .setSource(mappingEs)
                        .execute().actionGet();
            }
        }
    }

    private synchronized void ensureIndex(String indexName, String documentType) {
        boolean hasIndex = esClient.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet().isExists();
        if (hasIndex)
            return;
        XContentBuilder mappingEs = mapping(documentType);
        // create Index  and Set Mapping
        if (mappingEs != null) {
            //Settings settings = Builder
            logger.debug("Creating new index {} for document type {}", indexName, documentType);
            String settingDefinition = settingDefinition();
            if (settingDefinition != null) {
                Settings settings = ImmutableSettings.settingsBuilder().loadFromSource(settingDefinition).build();
                esClient.admin().indices().prepareCreate(indexName).addMapping(documentType, mappingEs).setSettings(settings).execute().actionGet();
            } else {
                esClient.admin().indices().prepareCreate(indexName).addMapping(documentType, mappingEs).execute().actionGet();
            }
        }
    }

    @Override
    public SearchChange update(SearchChange incoming) {

        String source = makeIndexJson(incoming);
        if (incoming.getSearchKey() == null)
            return save(incoming);

        try {
            logger.debug("Received request to Update [{}]", incoming.getMetaKey());
            ensureIndex(incoming.getIndexName(), incoming.getDocumentType());
            ensureMapping(incoming.getIndexName(), incoming.getDocumentType());
            GetResponse response =
                    esClient.prepareGet(incoming.getIndexName(),
                            incoming.getDocumentType(),
                            incoming.getSearchKey())
                            //.setRouting(incoming.getMetaKey())
                            .execute()
                            .actionGet();
            if (response.isExists() && !response.isSourceEmpty()) {
                // Messages can be received out of sequence
                // Check to ensure we don't accidentally overwrite a more current
                // document with an older one. We assume the calling fortress understands
                // what the most recent doc is.
                Object o = response.getSource().get(MetaSearchSchema.WHEN); // fortress view of WHEN, not AuditBuckets!
                if (o != null) {
                    Long existingWhen = Long.decode(o.toString());
                    if (existingWhen > incoming.getWhen()) {
                        logger.debug("ignoring a request to update as the existing document dated [{}] is newer than the incoming document dated [{}]", new Date(existingWhen), new Date(incoming.getWhen()));
                        return incoming; // Don't overwrite the most current doc!
                    } else if (incoming.getWhen() == 0l && !incoming.isReplyRequired()) {
                        // Meta Change - not indexed in AB, so ignore something we already have.
                        // Likely scenario is a batch is being reprocessed
                        return incoming;
                    }
                }
            } else {
                // No response, to a search key we expect to exist. Create a new one
                // Likely to be in response to rebuilding an ES index from Graph data.
                return save(incoming);
            }

            // Update the existing document with the incoming change
            IndexRequestBuilder update = esClient
                    .prepareIndex(incoming.getIndexName(), incoming.getDocumentType(), incoming.getSearchKey());
            //.setRouting(incoming.getMetaKey());

            ListenableActionFuture<IndexResponse> ur = update.setSource(source).
                    execute();

            if (logger.isDebugEnabled()) {
                IndexResponse indexResponse = ur.actionGet();
                logger.debug("Updated [{}] logId=[{}] for [{}] to version [{}]" , incoming.getSearchKey(), incoming.getLogId(), incoming,  indexResponse.getVersion());
            }
        } catch (IndexMissingException e) { // administrator must have deleted it, but we think it still exists
            logger.info("Attempt to update non-existent index [{}]. Moving to create it", incoming.getIndexName());
            return save(incoming);
        }
        return incoming;
    }

    public byte[] findOne(MetaHeader header) {
        return findOne(header, null);
    }

    public byte[] findOne(MetaHeader header, String id) {
        String indexName = header.getIndexName();
        String documentType = header.getDocumentType();
        if (id == null)
            id = header.getSearchKey();
        logger.debug("Looking for [{}] in {}", id, indexName + documentType);

        GetResponse response = esClient.prepareGet(indexName, documentType, id)
                //.setRouting(header.getMetaKey())
                .execute()
                .actionGet();

        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSourceAsBytes();

        logger.info("Unable to find response data for [" + id + "] in " + indexName + "/" + documentType);
        return null;
    }

    @Override
    public Map<String, Object> ping() {
        Map<String, Object> results = new HashMap<>();
        ClusterHealthRequest request = new ClusterHealthRequest();
        ClusterHealthResponse response = esClient.admin().cluster().health(request).actionGet();
        if (response == null) {
            results.put("status", "error!");
            return results;
        }
        results.put("abStatus", "ok");
        results.put("health", response.getStatus().name());
        results.put("dataNodes", response.getNumberOfDataNodes());
        results.put("nodes", response.getNumberOfNodes());
        results.put("clusterName", response.getClusterName());

        return results;
    }

    private String makeIndexJson(SearchChange searchChange) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> index = makeIndexDocument(searchChange);
        try {
            return mapper.writeValueAsString(index);
        } catch (JsonProcessingException e) {

            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Converts a user requested searchChange in to a standardised document to index
     *
     * @param searchChange incoming
     * @return document to index
     */
    private Map<String, Object> makeIndexDocument(SearchChange searchChange) {
        Map<String, Object> indexMe = new HashMap<>();
        if (searchChange.getWhat() != null)
            indexMe.put(MetaSearchSchema.WHAT, searchChange.getWhat());

        indexMe.put(MetaSearchSchema.META_KEY, searchChange.getMetaKey());
        indexMe.put(MetaSearchSchema.WHO, searchChange.getWho());
        if (searchChange.getEvent() != null)
            indexMe.put(MetaSearchSchema.LAST_EVENT, searchChange.getEvent());

        indexMe.put(MetaSearchSchema.WHEN, searchChange.getWhen());
        // When the MetaHeader was created
        indexMe.put(MetaSearchSchema.CREATED, searchChange.getCreatedDate());
        // When the log was created
        indexMe.put(MetaSearchSchema.TIMESTAMP, new Date(searchChange.getSysWhen()));
        // https://github.com/monowai/auditbucket/issues/21

        indexMe.put(MetaSearchSchema.FORTRESS, searchChange.getFortressName());
        indexMe.put(MetaSearchSchema.DOC_TYPE, searchChange.getDocumentType());
        indexMe.put(MetaSearchSchema.CALLER_REF, searchChange.getCallerRef());
        indexMe.put(MetaSearchSchema.DESCRIPTION, searchChange.getDescription());

        if (!searchChange.getTagValues().isEmpty())
            indexMe.put(MetaSearchSchema.TAG, searchChange.getTagValues());
        // ToDo: Force the index mapping to handle the tags

        return indexMe;
    }

    private String settingDefinition() {
        try {
            XContentBuilder setting = setting();
            return setting.string();
        } catch (IOException e) {
            logger.error("Error in building settings for the ES index", e);
        }
        return null;
    }

    private XContentBuilder setting() throws IOException {
        return jsonBuilder()
                .startObject()
                .startObject("analysis")
                .startObject("analyzer")
                .startObject(MetaSearchSchema.NGRM_WHAT_CODE)
                .field("tokenizer", MetaSearchSchema.NGRM_WHAT_CODE)
                .endObject()
                .startObject(MetaSearchSchema.NGRM_WHAT_NAME)
                .field("tokenizer", MetaSearchSchema.NGRM_WHAT_NAME)
                .endObject()
                .startObject(MetaSearchSchema.NGRM_WHAT_DESCRIPTION)
                .field("tokenizer", MetaSearchSchema.NGRM_WHAT_DESCRIPTION)
                .endObject()
                .endObject()
                .startObject("tokenizer")
                .startObject(MetaSearchSchema.NGRM_WHAT_CODE)
                .field("type", "nGram")
                .field("min_gram", MetaSearchSchema.NGRM_WHAT_CODE_MIN)
                .field("max_gram", MetaSearchSchema.NGRM_WHAT_CODE_MAX)
                .endObject()
                .startObject(MetaSearchSchema.NGRM_WHAT_NAME)
                .field("type", "nGram")
                .field("min_gram", MetaSearchSchema.NGRM_WHAT_NAME_MIN)
                .field("max_gram", MetaSearchSchema.NGRM_WHAT_NAME_MAX)
                .endObject()
                .startObject(MetaSearchSchema.NGRM_WHAT_DESCRIPTION)
                .field("type", "nGram")
                .field("min_gram", MetaSearchSchema.NGRM_WHAT_DESCRIPTION_MIN)
                .field("max_gram", MetaSearchSchema.NGRM_WHAT_DESCRIPTION_MAX)
                .endObject()
                .endObject()
                .endObject()
                .endObject();
    }

    private XContentBuilder mapping(String documentType) {
        XContentBuilder xbMapping;
        try {
            xbMapping = jsonBuilder()
                    .startObject()
                    .startObject(documentType)
                    .startObject("properties")
                    .startObject(MetaSearchSchema.META_KEY) // keyword
                    .field("type", "string")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .startObject(MetaSearchSchema.CALLER_REF) // keyword
                    .field("type", "string")
                    .field("boost", "2.0")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .startObject(MetaSearchSchema.DOC_TYPE)  // keyword
                    .field("type", "string")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .startObject(MetaSearchSchema.FORTRESS)   // keyword
                    .field("type", "string")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .startObject(MetaSearchSchema.LAST_EVENT)  //@lastEvent
                    .field("type", "string")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .startObject(MetaSearchSchema.TAG + ".lead.key")
                    .field("type", "string")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .startObject(MetaSearchSchema.TIMESTAMP)
                    .field("type", "date")
                    .endObject()
                    .startObject(MetaSearchSchema.CREATED)
                    .field("type", "date")
                    .endObject()
                    .startObject(MetaSearchSchema.WHAT)    //@what is dynamic so we don't init his mapping we choose the convention
                    .startObject("properties")
                    .startObject(MetaSearchSchema.WHAT_CODE)
                    .field("type", "string")
                    .field("boost", 2d)
                    .field("analyzer", MetaSearchSchema.NGRM_WHAT_CODE)
                    .endObject()
                    .startObject(MetaSearchSchema.WHAT_NAME)
                    .field("type", "string")
                    .field("boost", 2d)
                    .field("analyzer", MetaSearchSchema.NGRM_WHAT_NAME)
                    .endObject()
                    .startObject(MetaSearchSchema.WHAT_DESCRIPTION)
                    .field("type", "string")
                    .field("analyzer", MetaSearchSchema.NGRM_WHAT_DESCRIPTION)
                    .endObject()
                    .endObject()
                    .endObject()
                    .startObject(MetaSearchSchema.WHEN)      //@when
                    .field("type", "date")
                    .endObject()
                    .startObject(MetaSearchSchema.WHO)       //@who
                    .field("type", "string")
                    .field("index", NOT_ANALYZED)
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject();
        } catch (IOException e) {
            return null;
        }
        return xbMapping;
    }
}
