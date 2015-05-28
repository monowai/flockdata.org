/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.elasticsearch.ElasticsearchException;
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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.model.SearchTag;
import org.flockdata.search.service.SearchAdmin;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.SearchChange;
import org.flockdata.track.model.TrackSearchDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * User: Mike Holdsworth
 * Date: 27/04/13
 * Time: 12:00 PM
 */
@Repository
public class TrackDaoES implements TrackSearchDao {


    @Autowired
    private Client esClient;

    @Autowired
    private SearchAdmin searchAdmin;

    private Logger logger = LoggerFactory.getLogger(TrackDaoES.class);

    @Override
    public boolean delete(SearchChange searchChange) {
        String indexName = searchChange.getIndexName();
        String recordType = searchChange.getDocumentType();

        String existingIndexKey = searchChange.getSearchKey();

        DeleteResponse dr = esClient.prepareDelete(indexName, recordType, existingIndexKey)
                //.setRouting(entity.getMetaKey())
                .execute()
                .actionGet();

        if (!dr.isFound()) {
            logger.debug("Didn't find the document to remove [{}] from {}/{}", existingIndexKey, indexName, recordType);
            return false;// Not found
        }
        logger.debug("Removed document [{}] from {}/{}", existingIndexKey, indexName, recordType);
        return true;
    }

    /**
     * @param searchChange object containing changes
     * @param source       Json to save
     * @return key value of the child document
     */
    private SearchChange save(SearchChange searchChange, String source) throws IOException {
        String indexName = searchChange.getIndexName();
        String documentType = searchChange.getDocumentType();
        logger.debug("Received request to Save [{}]", searchChange.getMetaKey());

        logger.debug("Resolved SearchKey to [{}]", searchChange.getSearchKey());
        // Rebuilding a document after a reindex - preserving the unique key.
        IndexRequestBuilder irb = esClient.prepareIndex(indexName, documentType)
                .setSource(source);

        //if (searchChange.getSearchKey() != null) {
        irb.setId(searchChange.getSearchKey());
        //}

        try {
            IndexResponse ir = irb.execute().actionGet();
            //searchChange.setSearchKey(ir.getId());

            if (logger.isDebugEnabled())
                logger.debug("Save:Document entityId [{}], [{}], logId= [{}] searchKey [{}] index [{}/{}]",
                        searchChange.getEntityId(),
                        searchChange.getMetaKey(),
                        searchChange.getLogId(),
                        ir.getId(),
                        indexName,
                        documentType);

            return searchChange;
        } catch (MapperParsingException e) {
            // DAT-359
            logger.error("Parsing error - callerRef [" + searchChange.getCallerRef() + "], metaKey [" + searchChange.getMetaKey() + "], " + e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Parsing error - callerRef [" + searchChange.getCallerRef() + "], metaKey [" + searchChange.getMetaKey() + "], " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return searchChange;
        }

    }

    // ToDo: Fix this. Caching is not resetting after the index is deleted
    //@Cacheable(value = "mappedIndexes", key = "#indexName +'/'+ #documentType")
    public boolean ensureIndex(String indexName, String documentType) throws IOException {

        if (hasIndex(indexName)) {
            // Need to be able to allow for a "per document" mapping
            ensureMapping(indexName, documentType);
            return true;
        }

        logger.debug("Ensuring index {}, {}", indexName, documentType);
        //if (hasIndex(indexName)) return true;
        XContentBuilder mappingEs = getMapping(indexName, documentType);
        // create Index  and Set Mapping
        if (mappingEs != null) {
            //Settings settings = Builder
            logger.debug("Creating new index {} for document type {}", indexName, documentType);
            Map<String, Object> settings = getSettings();
            try {
                if (settings != null) {
                    esClient.admin()
                            .indices()
                            .prepareCreate(indexName)
                            .addMapping(documentType, mappingEs)
                            .setSettings(settings)
                            .execute()
                            .actionGet();
                } else {
                    esClient.admin()
                            .indices()
                            .prepareCreate(indexName)
                            .addMapping(documentType, mappingEs)
                            .execute()
                            .actionGet();
                }
            } catch (ElasticsearchException esx) {
                if (! (esx instanceof IndexAlreadyExistsException)) {
                    logger.error("Error while ensuring index.... " + indexName, esx);
                    throw esx;
                }
            }
        }
        return true;
    }

    private void ensureMapping(String indexName, String documentType) throws IOException {
        // Mappings are on a per Document basis. We need to ensure the mapping exists for the
        //    same index but every document type
        logger.debug("Checking mapping for {}, {}", indexName, documentType);

        // Test if Type exist
        String[] indexNames = new String[1];
        indexNames[0] = indexName;
        String[] documentTypes = new String[1];
        documentTypes[0] = documentType;

        boolean hasTypeMapping = esClient.admin()
                .indices()
                .typesExists(new TypesExistsRequest(indexNames, documentTypes))
                .actionGet()
                .isExists();
        if (!hasTypeMapping) {
            XContentBuilder mapping = getMapping(indexName, documentType);
            esClient.admin().indices()
                    .preparePutMapping(indexName)
                    .setType(documentType)
                    .setSource(mapping)
                    .execute().actionGet();
            logger.debug("Created default mapping and applied settings for {}, {}", indexName, documentType);
        }
    }

    private boolean hasIndex(String indexName) {
        boolean hasIndex = esClient
                .admin()
                .indices()
                .exists(new IndicesExistsRequest(indexName))
                .actionGet().isExists();
        if (hasIndex) {
            logger.trace("Index {} ", indexName);
            return true;
        }
        return false;
    }

    @Override
    public SearchChange handle(SearchChange searchChange) throws IOException {
        String source = getJsonToIndex(searchChange);

        if (searchChange.getSearchKey() == null || searchChange.getSearchKey().equals("")) {
            searchChange.setSearchKey((searchChange.getCallerRef() == null ? searchChange.getMetaKey() : searchChange.getCallerRef()));
            logger.debug("No search key, creating as a new document [{}]", searchChange.getMetaKey());
            return save(searchChange, source);
        }

        try {
            logger.debug("Update request for searchKey [{}], metaKey[{}]", searchChange.getSearchKey(), searchChange.getMetaKey());

            GetResponse response =
                    esClient.prepareGet(searchChange.getIndexName(),
                            searchChange.getDocumentType(),
                            searchChange.getSearchKey())
                            .execute()
                            .actionGet();
            logger.debug("executed get request for {}", searchChange.toString());
            if (response.isExists() && !response.isSourceEmpty()) {
                logger.debug("Document exists!");
                // Messages can be received out of sequence
                // Check to ensure we don't accidentally overwrite a more current
                // document with an older one. We assume the calling fortress understands
                // what the most recent doc is.
                Object o = response.getSource().get(EntitySearchSchema.WHEN); // fortress view of WHEN, not FlockDatas!
                if (o != null) {

                    Long existingWhen = Long.decode(o.toString());
                    logger.debug("Comparing searchChange when {} with stored when {}", searchChange.getWhen(), existingWhen);
                    if (!searchChange.isForceReindex()) {
                        if (existingWhen.compareTo(searchChange.getWhen().getTime()) > 0) {
                            logger.debug("ignoring a request to update as the existing document dated [{}] is newer than the searchChange document dated [{}]", new Date(existingWhen), searchChange.getWhen());
                            return searchChange; // Don't overwrite the most current doc!
                        } else if (searchChange.getWhen().getTime() == 0l && !searchChange.isReplyRequired()) {
                            // Meta Change - not indexed in FD, so ignore something we already have.
                            // Likely scenario is a batch is being reprocessed
                            return searchChange;
                        }
                        logger.debug("Document is more recent. Proceeding with update");
                    } else {
                        logger.debug("Forcing an update of the document.");
                    }
                }
            } else {
                // No response, to a search key we expect to exist. Create a new one
                // Likely to be in response to rebuilding an ES index from Graph data.
                logger.debug("About to create in response to an update request for {}", searchChange.toString());
                return save(searchChange, source);
            }

            // Update the existing document with the searchChange change
            IndexRequestBuilder update = esClient
                    .prepareIndex(searchChange.getIndexName(), searchChange.getDocumentType(), searchChange.getSearchKey());
            //.setRouting(searchChange.getMetaKey());

            ListenableActionFuture<IndexResponse> ur = update.setSource(source).
                    execute();

            if (logger.isDebugEnabled()) {
                IndexResponse indexResponse = ur.actionGet();
                logger.debug("Updated [{}] logId=[{}] for [{}] to version [{}]", searchChange.getSearchKey(), searchChange.getLogId(), searchChange, indexResponse.getVersion());
            }
        } catch (IndexMissingException e) { // administrator must have deleted it, but we think it still exists
            logger.info("Attempt to update non-existent index [{}]. Moving to create it", searchChange.getIndexName());
            purgeCache();
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
        String indexName = entity.getFortress().getIndexName();
        String documentType = entity.getDocumentType();
        if (id == null)
            id = entity.getSearchKey();
        logger.debug("Looking for [{}] in {}", id, indexName + documentType);

        GetResponse response = esClient.prepareGet(indexName, documentType, id)
                //.setRouting(entity.getMetaKey())
                .execute()
                .actionGet();

        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSource();

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
        results.put("Status", "ok");
        results.put("health", response.getStatus().name());
        results.put("dataNodes", response.getNumberOfDataNodes());
        results.put("nodes", response.getNumberOfNodes());
        results.put("clusterName", response.getClusterName());
        results.put("nodeName", esClient.settings().get("name"));

        return results;
    }

    private String getJsonToIndex(SearchChange searchChange) {
        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
        Map<String, Object> index = getMapFromChange(searchChange);
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
     * @param searchChange searchChange
     * @return document to index
     */
    private Map<String, Object> getMapFromChange(SearchChange searchChange) {
        Map<String, Object> indexMe = new HashMap<>();
        if (searchChange.getWhat() != null)
            indexMe.put(EntitySearchSchema.WHAT, searchChange.getWhat());

        if (searchChange.getProps() != null)
            indexMe.put(EntitySearchSchema.PROPS, searchChange.getProps());
        if (searchChange.getMetaKey() != null) //DAT-83 No need to track NULL metaKey
            // This occurs if the search doc is not being tracked in fd-engine's graph
            indexMe.put(EntitySearchSchema.META_KEY, searchChange.getMetaKey());
        if (searchChange.getWho() != null)
            indexMe.put(EntitySearchSchema.WHO, searchChange.getWho());
        if (searchChange.getEvent() != null)
            indexMe.put(EntitySearchSchema.LAST_EVENT, searchChange.getEvent());

        indexMe.put(EntitySearchSchema.WHEN, searchChange.getWhen());

        if (searchChange.hasAttachment()) { // DAT-159
            indexMe.put(EntitySearchSchema.ATTACHMENT, searchChange.getAttachment());
            indexMe.put(EntitySearchSchema.FILENAME, searchChange.getFileName());
            indexMe.put(EntitySearchSchema.CONTENT_TYPE, searchChange.getContentType());
        }

        // When the Entity was created in the fortress
        indexMe.put(EntitySearchSchema.CREATED, searchChange.getCreatedDate());

        // Time that this change was indexed by fd-engine
        indexMe.put(EntitySearchSchema.TIMESTAMP, new Date(searchChange.getSysWhen()));

        indexMe.put(EntitySearchSchema.FORTRESS, searchChange.getFortressName());
        indexMe.put(EntitySearchSchema.DOC_TYPE, searchChange.getDocumentType());
        if (searchChange.getCallerRef() != null)
            indexMe.put(EntitySearchSchema.CALLER_REF, searchChange.getCallerRef());

        if (searchChange.getDescription() != null)
            indexMe.put(EntitySearchSchema.DESCRIPTION, searchChange.getDescription());

        if (!searchChange.getTagValues().isEmpty())
            setTags(indexMe, searchChange.getTagValues());

        return indexMe;
    }

    private void setTags(Map<String, Object> indexMe, HashMap<String, Map<String, ArrayList<SearchTag>>> tagValues) {

        Collection<String> uniqueTags = new ArrayList<>();
        Collection<String> outputs = new ArrayList<>();

        Map<String, Object> byRelationship = new HashMap<>();
        Map<String, Object> squash = new HashMap<>();
        boolean enableSquash = true; // If tag and rlx names are the same, store just the values
        // means sold.sold.name will appear as sold.name

        for (String relationship : tagValues.keySet()) {
            if (enableSquash && tagValues.get(relationship).containsKey(relationship)) {
                // DAT-328 - the relationship and label have the same name
                ArrayList<SearchTag> values = tagValues.get(relationship).get(relationship);
                if (values.size() == 1) {
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
                    if (mapValues.get(label).size() == 1) {
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
            indexMe.put(EntitySearchSchema.TAG, byRelationship);
        }
        if (!uniqueTags.isEmpty()) {
//            Map<String,Object>suggestInput = new HashMap<>();
            //suggestInput.put("input", uniqueTags);

            indexMe.put(EntitySearchSchema.ALL_TAGS, uniqueTags);
        }


    }

    /**
     * adds a unique tag to the list of tags to store with this document
     * These tags can be search by autocomplete
     *
     * @param tagCodes modified by reference
     * @param fromTags tags to analyze
     */
    private void gatherTags(Collection<String> tagCodes, ArrayList<SearchTag> fromTags) {
        for (SearchTag value : fromTags) {
            gatherTag(tagCodes, value);
        }
    }

    private void gatherTag(Collection<String> tagCodes, SearchTag value) {
        final int minTagLength = 2;
        if (!tagCodes.contains(value.getCode())) {
            if (value.getCode().length() >= minTagLength) {
                tagCodes.add(value.getCode() );
            }
            if ( value.getName()!=null ){
                tagCodes.add(value.getName()  + " : "+value.getCode());
            }
        }
        if (value.getGeo() != null) {
            Map<String, Object> o = value.getGeo();
            for (String s : o.keySet()) {
                String geoCode = o.get(s).toString();
                if (!o.containsKey(geoCode)) {
                    if (s.endsWith(".code")) {
                        String nameCol = s.substring(0, s.indexOf('.')) + ".name";
                        String geoName = null;
                        if (o.containsKey(nameCol))
                            geoName = o.get(nameCol).toString();

                        tagCodes.add(geoCode + (geoName != null ? " - " + geoName : ""));
                        tagCodes.add(geoName + " - " + geoCode);
                    }
                }
            }
        }
    }

    private Map<String, Object> defaultSettings = null;

    private Map<String, Object> getSettings() throws IOException {
        InputStream file = null;
        try {

            if (defaultSettings == null) {
                String settings = searchAdmin.getEsDefaultSettings();
                // Look for a file in a configuration folder
                file = getClass().getClassLoader().getResourceAsStream(settings);
                if (file == null) {
                    // Read it from inside the WAR
                    file = getClass().getClassLoader().getResourceAsStream("/fd-default-settings.json");
                    logger.info("No default settings exists. Using FD defaults /fd-default-settings.json");

                    if (file == null) // for JUnit tests
                        file = new FileInputStream(settings);
                } else
                    logger.debug("Overriding default settings with file on disk {}", settings);
                defaultSettings = getMapFromStream(file);
                logger.debug("Initialised settings {} with {} keys", settings, defaultSettings.keySet().size());
            }
        } catch (IOException e) {
            logger.error("Error in building settings for the ES index", e);
        } finally {
            if (file != null) {
                file.close();
            }
        }
        return defaultSettings;
    }

    private Map<String, Object> getMapFromStream(InputStream file) throws IOException {
        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, HashMap.class);
        return mapper.readValue(file, mapType);

    }

    private XContentBuilder getMapping(String indexName, String documentType) throws IOException {

        XContentBuilder xbMapping;
        Map<String, Object> map = getDefaultMapping(getKeyName(indexName, documentType));
        Map<String, Object> docMap = new HashMap<>();
        docMap.put(documentType, map.get("mapping"));
        xbMapping = jsonBuilder().map(docMap);

        return xbMapping;
    }

    private String getKeyName(String indexName, String documentType) {
        return indexName + "/" + documentType + ".json";
    }


    private Map<String, Object> getDefaultMapping(String key) throws IOException {
        Map<String, Object> found;

        // Locate file on disk
        try {
            found = getMapping(searchAdmin.getEsMappingPath() + "/" + key);
            if (found != null) {
                logger.debug("Found custom mapping for {}", key);
                return found;
            }
        } catch (IOException ioe) {
            logger.debug("Custom mapping does not exists for {} - reverting to default", key);
        }

        String esDefault = searchAdmin.getEsDefaultMapping();
        try {
            // Chance to find it on disk
            found = getMapping(esDefault);
            logger.debug("Overriding packaged mapping with local default of [{}]. {} keys", esDefault, found.keySet().size());
        } catch (IOException ioe) {
            // Extract it from the WAR
            logger.debug("Reading default mapping from the package");
            found = getMapping("/fd-default-mapping.json");
        }
        return found;
    }

    private Map<String, Object> getMapping(String fileName) throws IOException {
        logger.debug("Looking for {}", fileName);
        InputStream file = null;
        try {
            file = getClass().getClassLoader().getResourceAsStream(fileName);
            if (file == null)
                // running from JUnit can only read this as a file input stream
                file = new FileInputStream(fileName);
            return getMapFromStream(file);
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

}
