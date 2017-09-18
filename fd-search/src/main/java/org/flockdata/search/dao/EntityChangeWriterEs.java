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
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.rest.RestStatus;
import org.flockdata.data.Entity;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.SearchTag;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.track.bean.EntityKeyBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author mholdsworth
 * @since 27/04/2013
 */
@Service
public class EntityChangeWriterEs implements EntityChangeWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityChangeWriterEs.class);
    private final Client elasticSearchClient;
    private final IndexManager indexManager;

    @Autowired
    public EntityChangeWriterEs(Client elasticSearchClient, IndexManager indexManager) {
        this.elasticSearchClient = elasticSearchClient;
        this.indexManager = indexManager;
    }

    @Override
    public boolean delete(EntitySearchChange searchChange) {
        String indexName = searchChange.getIndexName();
        String recordType = searchChange.getDocumentType();

        String existingIndexKey = searchChange.getSearchKey();

        DeleteResponse dr = elasticSearchClient.prepareDelete(searchChange.getIndexName(), recordType, existingIndexKey)
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
     * @param source Json to save
     * @return key value of the child document
     */
    private EntitySearchChange save(EntitySearchChange searchChange, String source) {
        String indexName = searchChange.getIndexName();
        String documentType = searchChange.getDocumentType();
        LOGGER.debug("Received request to Save [{}] SearchKey [{}]", searchChange.getKey(), searchChange.getSearchKey());

        // Rebuilding a document after a reindex - preserving the unique key.
        IndexRequestBuilder irb =
                elasticSearchClient
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
                    elasticSearchClient.prepareGet(searchChange.getIndexName(),
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
            IndexRequestBuilder update = elasticSearchClient
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
        String indexName = indexManager.toIndex(entity);//entity.getFortress().getRootIndex();
        String documentType = entity.getType();
        if (id == null)
            id = entity.getSearchKey();
        LOGGER.debug("Looking for [{}] in {}", id, indexName + documentType);

        GetResponse response = elasticSearchClient.prepareGet(indexName, documentType, id)
                //.setRouting(entity.getKey())
                .execute()
                .actionGet();

        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSource();

        LOGGER.info("Unable to find response data for [" + id + "] in " + indexName + "/" + documentType);
        return null;
    }

    @Override
    public Map<String, Object> ping() {
        Map<String, Object> results = new HashMap<>();
        ClusterHealthResponse response = null;
        try {
            response = elasticSearchClient.admin()
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
        results.put("nodeName", elasticSearchClient.settings().get("name"));

        return results;
    }

    private String getJsonToIndex(EntitySearchChange searchChange) {
        ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
        Map<String, Object> index = getMapFromChange(searchChange);
        try {
            return mapper.writeValueAsString(index);
        } catch (JsonProcessingException e) {

            LOGGER.error(e.getMessage());
        }
        return null;
    }

    /**
     * Converts a user requested searchChange in to a standardised document to index
     *
     * @param searchChange searchChange
     * @return document to index
     */
    public Map<String, Object> getMapFromChange(EntitySearchChange searchChange) {
        Map<String, Object> indexMe = new HashMap<>();
        indexMe.put(SearchSchema.FORTRESS, searchChange.getFortressName());
        // MKH - let elasticSearch manage this
//        indexMe.put(SearchSchema.DOC_TYPE, searchChange.getType());
        if (searchChange.getKey() != null) //DAT-83 No need to track NULL key
            // This occurs if the search doc is not being tracked in fd-engine's graph
            indexMe.put(SearchSchema.KEY, searchChange.getKey());

        if (searchChange.getData() != null)
            indexMe.put(SearchSchema.DATA, searchChange.getData());
        if (searchChange.getProps() != null && !searchChange.getProps().isEmpty())
            indexMe.put(SearchSchema.PROPS, searchChange.getProps());
        if (searchChange.getWho() != null)
            indexMe.put(SearchSchema.WHO, searchChange.getWho());
        if (searchChange.getEvent() != null)
            indexMe.put(SearchSchema.LAST_EVENT, searchChange.getEvent());

        // When the Entity was created in the fortress
        indexMe.put(SearchSchema.CREATED, searchChange.getCreatedDate());
        if (searchChange.getUpdatedDate() != null)
            indexMe.put(SearchSchema.UPDATED, searchChange.getUpdatedDate());

        if (searchChange.hasAttachment()) { // DAT-159
            indexMe.put(SearchSchema.ATTACHMENT, searchChange.getAttachment());
            indexMe.put(SearchSchema.FILENAME, searchChange.getFileName());
            indexMe.put(SearchSchema.CONTENT_TYPE, searchChange.getContentType());
        }

        // Time that this change was indexed by fd-engine
        indexMe.put(SearchSchema.TIMESTAMP, new Date(searchChange.getSysWhen()));

        if (searchChange.getCode() != null)
            indexMe.put(SearchSchema.CODE, searchChange.getCode());

        String name = searchChange.getName();
        String description = searchChange.getDescription();

        if (name != null) {
            // We prefer storing the description in the search doc
            if (description == null || StringUtils.equals(name, description))
                description = name;
            else
                indexMe.put(SearchSchema.NAME, searchChange.getName());
        }
        if (description != null) // Search description - generally always returned
            indexMe.put(SearchSchema.DESCRIPTION, searchChange.getDescription());

        if (!searchChange.getTagValues().isEmpty())
            setTags("", indexMe, searchChange.getTagValues());

        if (!searchChange.getEntityLinks().isEmpty())
            setEntityLinks(indexMe, searchChange.getEntityLinks());

        return indexMe;
    }

    private void setEntityLinks(Map<String, Object> indexMe, Collection<EntityKeyBean> entityLinks) {

        for (EntityKeyBean linkedEntity : entityLinks) {
            //String prefix;

            Map<String, Map<String, Object>> entity = (Map<String, Map<String, Object>>) indexMe.get("e");
            if (entity == null) {
                entity = new HashMap<>();//
                indexMe.put("e", entity);

            }
            String docType = linkedEntity.getDocumentType().toLowerCase();
            Map<String, Object> docEntry = entity.get(docType);
            if (docEntry == null) {
                docEntry = new HashMap<>();
            }
            entity.put(docType, docEntry);

            Map<String, Object> leaf;
            if (linkedEntity.getRelationshipName() == null || linkedEntity.getRelationshipName().equals("") || linkedEntity.getRelationshipName().equalsIgnoreCase(linkedEntity.getDocumentType())) {
                leaf = docEntry;
                //prefix = "e" +QueryDaoES.ES_FIELD_SEP + linkedEntity.getType().toLowerCase() + QueryDaoES.ES_FIELD_SEP;
            } else {
                leaf = new HashMap<>();
                docEntry.put(linkedEntity.getRelationshipName().toLowerCase(), leaf);

                //prefix = "e" +QueryDaoES.ES_FIELD_SEP+ linkedEntity.getType().toLowerCase() + QueryDaoES.ES_FIELD_SEP + linkedEntity.getRelationship() + QueryDaoES.ES_FIELD_SEP;
            }
            setNonEmptyValue(SearchSchema.CODE, linkedEntity.getCode(), leaf);
            setNonEmptyValue(SearchSchema.INDEX, linkedEntity.getIndex(), leaf);
            setNonEmptyValue(SearchSchema.DESCRIPTION, linkedEntity.getDescription(), leaf);
            setNonEmptyValue("name", linkedEntity.getName(), leaf);
            setTags("", leaf, linkedEntity.getSearchTags());
        }
    }

    private void setNonEmptyValue(String key, Object value, Map<String, Object> values) {
        if (value != null && !value.toString().equals("")) {
            values.put(key, value);
        }
    }

    private void setTags(String prefix, Map<String, Object> indexMe, HashMap<String, Map<String, ArrayList<SearchTag>>> tagValues) {

        Collection<String> uniqueTags = new ArrayList<>();
        Collection<String> outputs = new ArrayList<>();
        boolean oneToMany = true;   // Kibana presentation only. ToDo: Set from TagInputBean->SearchTag
        // If false, makes the presentation look better in Kibana.
        // Consider one to one then one to many. One to one displays flat
        // while one to many assumes the tags to be an []. Presentation should strive to be consistent
        // but you can still query consistently either way.

        Map<String, Object> byRelationship = new HashMap<>();
        Map<String, Object> squash = new HashMap<>();
        boolean enableSquash = true; // If tag and rlx names are the same, store just the values
        // means sold.sold.name will appear as sold.name

        for (String relationship : tagValues.keySet()) {
            if (enableSquash && tagValues.get(relationship).containsKey(relationship)) {
                // DAT-328 - the relationship and label have the same name
                ArrayList<SearchTag> values = tagValues.get(relationship).get(relationship);
                if (values.size() == 1 & !oneToMany) {
                    // DAT-329
                    SearchTag searchTag = values.iterator().next();
                    squash.put(relationship, searchTag);
                    gatherTag(uniqueTags, searchTag);

                } else {
                    ArrayList<SearchTag> searchTags = tagValues.get(relationship).get(relationship);
                    squash.put(relationship, searchTags);
                    gatherTags(uniqueTags, searchTags);

                }
            } else {
                Map<String, ArrayList<SearchTag>> mapValues = tagValues.get(relationship);
                Map<String, Object> newValues = new HashMap<>();
                for (String label : mapValues.keySet()) {
                    if (mapValues.get(label).size() == 1 & !oneToMany) {
                        // DAT-329 if only one value, don't store as a collection
                        SearchTag searchTag = mapValues.get(label).iterator().next();
                        // Store the tag
                        newValues.put(label, searchTag);
                        gatherTag(uniqueTags, searchTag);
                    } else {
                        ArrayList<SearchTag> searchTags = mapValues.get(label);
                        newValues.put(label, searchTags);
                        gatherTags(uniqueTags, searchTags);

                    }
                }
                byRelationship.put(relationship, newValues);

            }
        }
        if (!squash.isEmpty())
            byRelationship.putAll(squash);
        if (!byRelationship.isEmpty()) {
            indexMe.put(prefix + SearchSchema.TAG, byRelationship);
        }
        //
        if (prefix.equals("") && !uniqueTags.isEmpty()) {
            // ALL_TAGS contains autocomplete searchable tags.
            // ToDo: Prefix == null check stops linked entity tags being written to this list
            indexMe.put(SearchSchema.ALL_TAGS, uniqueTags);
        }


    }

    /**
     * adds a unique tag to the list of tags to store with this document
     * These tags can be search by autocomplete
     *
     * @param tagCodes modified by reference
     * @param fromTags tags to analyze
     */
    private void gatherTags(Collection<String> tagCodes, Collection<SearchTag> fromTags) {
        for (SearchTag value : fromTags) {
            gatherTag(tagCodes, value);
        }
    }

    private void gatherTag(Collection<String> tagCodes, SearchTag tag) {
        // ToDo: externalise config
        final int minTagLength = 2;
        if (!tagCodes.contains(tag.getCode())) {
            if (tag.getCode() != null && tag.getCode().length() >= minTagLength) {
                // DAT-446 - Favour the description over a numeric tag code
                // MKH - let fd-engine decide this
                //boolean isAlphaNumeric = true;//!NumberUtils.isNumber(tag.getCode());
                // Always store the code if there is no description or it is Alpha Numeric
                //if ( tag.getName() == null || isAlphaNumeric )
                tagCodes.add(tag.getCode());
            }
            if (tag.getName() != null) {
                String key = tag.getName();
                if (!tagCodes.contains(key))
                    tagCodes.add(key);
            }
        }
        if (tag.getGeo() != null) {
            Map<String, Object> geoBeans = tag.getGeo();
            for (String key : geoBeans.keySet()) {
//                GeoDataBean geoBean = geoBeans.get(key);
                String code = geoBeans.get(key).toString();
                if (!tagCodes.contains(code)) {
                    // ToDo: Figure out autocomplete across ngrams
                    // DAT-501 ES 2.0 does not support field names with a .
                    if (key.endsWith("Code")) {
                        String nameKey = key.substring(0, key.indexOf("Code")) + "Name";
                        String name = null;
                        if (geoBeans.containsKey(nameKey))
                            name = geoBeans.get(nameKey).toString();

                        tagCodes.add(code + (name != null ? " - " + name : ""));
                        if (name != null)
                            tagCodes.add(name + " - " + code);
                    }
                }
            }
        }
        if (!tag.getParent().isEmpty()) {
            for (String key : tag.getParent().keySet()) {
                Collection<SearchTag> nestedSearchTags = tag.getParent().get(key);
                gatherTags(tagCodes, nestedSearchTags);
//                LOGGER.info(key);
            }
        }
    }


}
