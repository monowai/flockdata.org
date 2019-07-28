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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.base.IndexMappingService;
import org.flockdata.search.configure.SearchConfig;
import org.flockdata.track.bean.SearchChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Maintenance routines for FD
 *
 * @author mholdsworth
 * @tag ElasticSearch, Search
 * @since 10/09/2015
 */
@Service
@Scope("singleton")
public class IndexMappingServiceEs implements IndexMappingService {

  private SearchConfig searchConfig;

  private Logger logger = LoggerFactory.getLogger(IndexMappingServiceEs.class);
  private Collection<String> knownIndexes = new ArrayList<>();
  private Map<String, Object> defaultSettings = null;

  @Autowired
  public IndexMappingServiceEs(SearchConfig searchConfig) {
    this.searchConfig = searchConfig;
  }

  @Override
  public boolean ensureIndexMapping(SearchChange change) {

    final String indexName = change.getIndexName();
    String documentType = searchConfig.getIndexManager().parseType(change.getDocumentType());

    if (hasIndex(change)) {
      // Need to be able to allow for a "per document" mapping
      ensureMapping(change);
      return true;
    }

    makeIndex(indexName, documentType, change);
    return true;
  }

  @Override
  public void deleteIndexes(Collection<String> indexesToDelete) {
    knownIndexes.clear();
    indexesToDelete.forEach(this::deleteIndex);

  }

  private void deleteIndex(String index) {
    try {
      searchConfig.getClient().admin().indices().delete(new DeleteIndexRequest(index)).get();
      logger.info("deleted [{}]", index);
    } catch (ExecutionException | InterruptedException e) {
      logger.error(e.getMessage());
    }
  }

  private synchronized void makeIndex(String indexName, String documentType, SearchChange change) {
    logger.debug("Ensuring index {}, {}", indexName, documentType);
    String key = change.getIndexName() + "/" + change.getDocumentType();
    if (knownIndexes.contains(key)) {
      return;
    }

    XContentBuilder esMapping = getMapping(change);
    // create Index  and Set Mapping
    if (esMapping != null) {
      //Settings settings = Builder
      logger.debug("Creating new index {} for document type {}", indexName, documentType);
      Map<String, Object> esSettings = getSettings();
      try {
        if (esSettings != null) {
          searchConfig.getClient().admin()
              .indices()
              .prepareCreate(indexName)
              .addMapping(documentType, esMapping)
              .setSettings(esSettings)
              .execute()
              .actionGet();
        } else {
          searchConfig.getClient().admin()
              .indices()
              .prepareCreate(indexName)
              .addMapping(documentType, esMapping)
              .execute()
              .actionGet();
        }
      } catch (ElasticsearchException esx) {
//                if (!(esx instanceof IndexAlreadyExistsException)) {
        logger.error("Error while ensuring index.... " + indexName, esx);
//                    throw esx;
//                }
      }
    }
    knownIndexes.add(key);

  }

  private void ensureMapping(SearchChange change) {
    // Mappings are on a per Index basis. We need to ensure the mapping exists for the
    //    same index but every document type
    logger.debug("Checking mapping for {}, {}", change.getIndexName(), change.getDocumentType());

    // Test if Type exist
    String[] documentTypes = new String[1];
    String documentType = searchConfig.getIndexManager().parseType(change.getDocumentType());
    documentTypes[0] = documentType;

    String[] indexNames = new String[1];
    indexNames[0] = change.getIndexName();

    boolean hasIndexMapping = searchConfig.getClient().admin()
        .indices()
        //.exists( new IndicesExistsRequest(indexNames))
        .typesExists(new TypesExistsRequest(indexNames, documentType))
        .actionGet()
        .isExists();

    if (!hasIndexMapping) {
      XContentBuilder mapping = getMapping(change);
      makeMapping(documentType, indexNames, mapping);
      logger.debug("Created default mapping and applied settings for {}, {}", indexNames[0], change.getDocumentType());
    }
  }

  private synchronized void makeMapping(String documentType, String[] indexNames, XContentBuilder mapping) {
    if (!searchConfig.getClient().admin()
        .indices()
        //.exists( new IndicesExistsRequest(indexNames))
        .typesExists(new TypesExistsRequest(indexNames, documentType))
        .actionGet()
        .isExists()) {
      searchConfig.getClient().admin().indices()
          .preparePutMapping(indexNames[0])
          .setType(documentType)
          .setSource(mapping)
          .execute().actionGet();
    }

  }

  private boolean hasIndex(SearchChange change) {
    String indexName = change.getIndexName();
    boolean hasIndex = searchConfig.getClient()
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

  private Map<String, Object> getSettings() {
    InputStream file;
    try {

      if (defaultSettings == null) {
        String settings = searchConfig.getEsDefaultSettings();
        // Look for a file in a configuration folder
        file = getClass().getClassLoader().getResourceAsStream(settings);
        if (file == null) {
          // Read it from inside the WAR
          file = getClass().getClassLoader().getResourceAsStream("/fd-default-settings.json");
          logger.info("No default settings exists. Using FD defaults /fd-default-settings.json");

          if (file == null) // for JUnit tests
          {
            file = new FileInputStream(settings);
          }
        } else {
          logger.debug("Overriding default settings with file on disk {}", settings);
        }
        defaultSettings = getMap(file);
        file.close();
        logger.debug("Initialised settings {} with {} keys", settings, defaultSettings.keySet().size());
      }
    } catch (IOException e) {
      logger.error("Error in building settings for the ES index", e);
    }
    return defaultSettings;
  }

  private Map<String, Object> getMap(InputStream file) throws IOException {
    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
    TypeFactory typeFactory = mapper.getTypeFactory();
    MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, HashMap.class);
    return mapper.readValue(file, mapType);
  }

  private Map<String, Object> getMap(URL url) throws IOException {
    ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
    TypeFactory typeFactory = mapper.getTypeFactory();
    MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, HashMap.class);
    return mapper.readValue(url, mapType);
  }


  private XContentBuilder getMapping(SearchChange change) {

    XContentBuilder xbMapping = null;
    try {
      Map<String, Object> map = getDefaultMapping(change);
      Map<String, Object> docMap = new HashMap<>();
      Map<String, Object> theMapping = (Map<String, Object>) map.get("mapping");

      docMap.put(searchConfig.getIndexManager().parseType(change.getDocumentType()), theMapping);
      xbMapping = jsonBuilder().map(docMap);
    } catch (IOException e) {
      logger.error("Problem getting the search mapping", e);
    }

    return xbMapping;
  }

  private String getKeyName(SearchChange change) {
    String fileName = change.getIndexName();
    if (fileName.startsWith(".")) {
      fileName = fileName.substring(1);
    }
    return fileName + "/" + change.getDocumentType() + ".json";
  }

  private Map<String, Object> getDefaultMapping(SearchChange change) throws IOException {
    String esDefault;

    String keyName = getKeyName(change);
    Map<String, Object> found;

    // Locate file on disk
    try {
      found = getMapping(searchConfig.getEsMappingPath() + "/" + keyName);
      if (found != null) {
        logger.debug("Found custom mapping for {}", keyName);
        return found;
      }
    } catch (IOException ioe) {
      logger.debug("Custom mapping does not exists for {} - reverting to default", keyName);
    }

    esDefault = searchConfig.getEsPathedMapping(change);
    try {
      // Chance to find it on disk
      found = getMapping(esDefault);
      logger.debug("Overriding packaged mapping with local default of [{}]. {} keys", esDefault, found.keySet().size());
    } catch (IOException ioe) {
      // Extract it from the WAR
      logger.debug("Reading default mapping from the package");
      found = getMapping(searchConfig.getEsMapping(change));
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
      {
        file = new FileInputStream(fileName);
      }
      return getMap(file);
      //return getMap(new URL(fileName));
    } finally {
      if (file != null) {
        file.close();
      }
    }
  }
}
