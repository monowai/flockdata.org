package org.flockdata.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.search.IndexHelper;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by mike on 10/09/15.
 */
@Service
public class IndexMappingServiceEs implements IndexMappingService {

    @Autowired
    private Client esClient;

    @Autowired
    private SearchAdmin searchAdmin;

    private Logger logger = LoggerFactory.getLogger(IndexMappingServiceEs.class);

    @Override
    public boolean ensureIndexMapping(SearchChange change) {

        final String indexName = change.getIndexName();
        String documentType = IndexHelper.parseType(change.getDocumentType());

        if (hasIndex(change)) {
            // Need to be able to allow for a "per document" mapping
            putMapping(change);
            return true;
        }

        logger.debug("Ensuring index {}, {}", indexName, documentType);
        //if (hasIndex(indexName)) return true;
        XContentBuilder esMapping = getMapping(change);
        // create Index  and Set Mapping
        if (esMapping != null) {
            //Settings settings = Builder
            logger.debug("Creating new index {} for document type {}", indexName, documentType);
            Map<String, Object> esSettings = getSettings();
            try {
                if (esSettings != null) {
                    esClient.admin()
                            .indices()
                            .prepareCreate(IndexHelper.parseIndex(indexName))
                            .addMapping(documentType, esMapping)
                            .setSettings(esSettings)
                            .execute()
                            .actionGet();
                } else {
                    esClient.admin()
                            .indices()
                            .prepareCreate(IndexHelper.parseIndex(indexName))
                            .addMapping(documentType, esMapping)
                            .execute()
                            .actionGet();
                }
            } catch (ElasticsearchException esx) {
                if (!(esx instanceof IndexAlreadyExistsException)) {
                    logger.error("Error while ensuring index.... " + indexName, esx);
                    throw esx;
                }
            }
        }
        return true;
    }


    private void putMapping(SearchChange change) {
        // Mappings are on a per Index basis. We need to ensure the mapping exists for the
        //    same index but every document type
        logger.debug("Checking mapping for {}, {}", change.getIndexName(), change.getDocumentType());

        // Test if Type exist
        String[] documentTypes = new String[1];
        documentTypes[0] = change.getDocumentType();

        String[] indexNames = new String[1];
        indexNames[0] = change.getIndexName();

        boolean hasIndexMapping = esClient.admin()
                .indices()
                        //.exists( new IndicesExistsRequest(indexNames))
                .typesExists(new TypesExistsRequest(indexNames,change.getDocumentType()))
                .actionGet()
                .isExists();
        if (!hasIndexMapping) {
            XContentBuilder mapping = getMapping(change);
            esClient.admin().indices()
                    .preparePutMapping(indexNames[0])
                    .setType(change.getDocumentType())
                    .setSource(mapping)
                    .execute().actionGet();
            logger.debug("Created default mapping and applied settings for {}, {}", indexNames[0], change.getDocumentType());
        }
    }

    private boolean hasIndex(SearchChange change) {
        String indexName = change.getIndexName();
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

    private Map<String, Object> defaultSettings = null;

    private Map<String, Object> getSettings() {
        InputStream file;
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
                file.close();
                logger.debug("Initialised settings {} with {} keys", settings, defaultSettings.keySet().size());
            }
        } catch (IOException e) {
            logger.error("Error in building settings for the ES index", e);
        }
        return defaultSettings;
    }

    private Map<String, Object> getMapFromStream(InputStream file) throws IOException {
        ObjectMapper mapper = FlockDataJsonFactory.getObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, HashMap.class);
        return mapper.readValue(file, mapType);

    }

    private XContentBuilder getMapping(SearchChange change) {

        XContentBuilder xbMapping = null;
        try {
            Map<String, Object> map = getDefaultMapping(change);
            Map<String, Object> docMap = new HashMap<>();
            Map<String,Object>theMapping = (Map<String, Object>) map.get("mapping");
            if ( change.getParent() != null ){
                HashMap<String,Object>parentMap = new HashMap<> ();
                parentMap.put ("type", IndexHelper.parseType(change.getParent().getDocumentType()));
                theMapping.put("_parent", parentMap);
            }
            docMap.put(change.getDocumentType(), theMapping);
            xbMapping = jsonBuilder().map(docMap);
        } catch (IOException e) {
            logger.error("Problem getting the search mapping", e);
        }

        return xbMapping;
    }

    private String getKeyName(SearchChange change) {
        return change.getIndexName() + "/" + change.getDocumentType() + ".json";
    }

    private Map<String, Object> getDefaultMapping(SearchChange change) throws IOException {
        String keyName = getKeyName(change) ;
        EntityService.TAG_STRUCTURE tagStructure = change.getTagStructure();
        Map<String, Object> found;

        // Locate file on disk
        try {
            found = getMapping(searchAdmin.getEsMappingPath() + "/" + keyName);
            if (found != null) {
                logger.debug("Found custom mapping for {}", keyName);
                return found;
            }
        } catch (IOException ioe) {
            logger.debug("Custom mapping does not exists for {} - reverting to default", keyName);
        }

        String esDefault = searchAdmin.getEsDefaultMapping(tagStructure);
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
