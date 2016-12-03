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

package org.flockdata.test.search.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.Suggest;
import io.searchbox.core.SuggestResult;
import io.searchbox.indices.DeleteIndex;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.IndexManager;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.base.IndexMappingService;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.base.TagChangeWriter;
import org.flockdata.search.model.SearchSchema;
import org.flockdata.search.service.ContentService;
import org.flockdata.search.service.QueryServiceEs;
import org.flockdata.search.service.SearchAdmin;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileInputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @tag Test, ElasticSearch, Search
 * @author mholdsworth
 * @since 15/08/2014
 */
@Component
@ActiveProfiles({"dev"})
public class ESBase {
    static JestClient esClient;
    private static Logger logger = LoggerFactory.getLogger(TestMappings.class);
    @Autowired
    EntityChangeWriter entityWriter;

    @Autowired
    TagChangeWriter tagWriter;


    @Autowired
    IndexManager indexManager;

    @Autowired
    IndexMappingService indexMappingService;

    @Autowired
    SearchAdmin searchAdmin;

    @Autowired
    ContentService contentService;

    @Qualifier("esSearchWriter")
    @Autowired
    SearchWriter esSearchWriter;

    @Autowired
    QueryServiceEs queryServiceEs;

    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        logger.info("You have to be running this with a working directory of fd-search for this to work");

        FileInputStream f = new FileInputStream("./src/test/resources/application.yml");


        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode tree = mapper.reader().readTree(f);
        JsonNode esNode = tree.get("es");
        JsonNode http = esNode.get("http");
        Object port = http.get("port").asText();

        //properties.get("es.http.port")
        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + port).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

    }

    static int getNbrResult(JestResult jResult) {
        int nbrResult;
        if (jResult.getErrorMessage() == null) {
            assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
            assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } else {
            nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again
        }
        return nbrResult;
    }

    void deleteEsIndex(Entity entity) throws Exception {
        deleteEsIndex(indexManager.parseIndex(entity));
    }

    void deleteEsIndex(String indexName) throws Exception {
        logger.info("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName).build());
    }

    /**
     * Term query on a non-analyzed field
     *
     * @param index
     * @param field
     * @param queryString
     * @param expectedHitCount
     * @param exceptionMessage
     * @return
     * @throws Exception
     */
    String doFacetQuery(String index, String type, String field, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        // There should only ever be one document for a given Entity.
        // Let's assert that
        int runCount = 0, nbrResult;
        JestResult result;
        int esTimeout = 5;

        do {

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          term : {\n" +
                    "              \"" + field + "\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            if ( type!=null && type.equals("*"))
                type = null;
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .addType(type)
                    .build();

            result = esClient.execute(search);
            TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

            nbrResult = getNbrResult(result);

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
        logger.trace("searching index [{}] field [{}] for [{}]", index, field, queryString);
        if (exceptionMessage == null)
            exceptionMessage = result.getJsonString();
        Assert.assertEquals(exceptionMessage, expectedHitCount, nbrResult);
        if (nbrResult != 0) {
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        } else {
            return null;
        }
    }

    /**
     * Scans an analyzed field looking for the queryString
     * @param entity
     * @param field
     * @param queryString
     * @param expectedHitCount
     * @param exceptionMessage
     * @return
     * @throws Exception
     */
    String doFieldQuery(Entity entity, String field, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        int runCount = 0, nbrResult;
        JestResult result;
        int esTimeout = 5;

        do {

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "            default_field:   \"" + field + "\", query: \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(indexManager.parseIndex(entity))
                    .build();

            result = esClient.execute(search);
            TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

            nbrResult = getNbrResult(result);

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
        logger.trace("searching index [{}] field [{}] for [{}]", indexManager.parseIndex(entity), field, queryString);
        if (exceptionMessage == null)
            exceptionMessage = result.getJsonString();
        Assert.assertEquals(exceptionMessage, expectedHitCount, nbrResult);
        if (nbrResult != 0) {
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        } else {
            return null;
        }
    }

    String doCompletionQuery(Entity entity, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        // There should only ever be one document for a given Entity.
        // Let's assert that
        SuggestResult result;

        String query = "{" +
                "    \"tags\" : {\n" +
                "        \"text\" : \"" + queryString + "\",\n" +
                "        \"completion\" : {\n" +
                "            \"field\" : \"" + SearchSchema.ALL_TAGS + "\"\n" +
                "        }\n" +
                "    }" +
                "}";


        Suggest search = new Suggest.Builder(query).
                addIndex(indexManager.parseIndex(entity)).
                build();
        result = esClient.execute(search);
        TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

        List<SuggestResult.Suggestion> suggestions = result.getSuggestions("tags");

        for (SuggestResult.Suggestion suggestion : suggestions) {
            assertEquals(exceptionMessage, expectedHitCount, suggestion.options.size());
        }


        return result.getJsonString();
    }

    String doQuery(Entity entity, String queryString, int expectedHitCount) throws Exception {
        return doQuery(indexManager.parseIndex(entity), indexManager.parseType(entity), queryString, expectedHitCount);
    }

    String doQuery(String index, String type, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        //waitAWhile();
        int runCount = 0, nbrResult;
        JestResult jResult;
        do {

            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"query\" : \"" + queryString + "\"" +
                    "           }\n" +
                    "      }\n" +
                    "}";

            //
            Search.Builder builder= new Search.Builder(query)
                    .addIndex(index);

            if ( type !=null && !type.equals("*"))
                    builder.addType(type)  ;

            Search search = builder.build();
            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" +index + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", index);
                return null;
            }
            nbrResult = getNbrResult(jResult);
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < 6);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        Assert.assertEquals(index + "\r\n" + queryString + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);

        return jResult.getJsonString();

    }

    String doFacetQuery(Entity entity, String field, String queryString, int expectedHitCount) throws Exception {
        int runCount = 0, nbrResult;

        JestResult result;
        do {

            runCount++;
            String query = "{\n" +
                    "    \"query\" : {\n" +
                    "        \"filtered\" : {\n" +
                    "            \"filter\" : {\n" +
                    "                \"term\" : {\n" +
                    "                    \"" + field + "\" : \"" + queryString + "\"\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(indexManager.parseIndex(entity))
                    .addType(indexManager.parseType(entity))
                    .build();

            result = esClient.execute(search);
            String message = indexManager.parseIndex(entity) + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < 5);

        Assert.assertEquals("Unexpected hit count searching '" + indexManager.parseIndex(entity) + "' for {" + queryString + "} in field {" + field + "}", expectedHitCount, nbrResult);
        if (nbrResult != 0)
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        else
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray().toString();
    }

    String doDefaultFieldQuery(Entity entity, String field, String queryString, int expectedHitCount) throws Exception {
        return doDefaultFieldQuery(entity, null, field, queryString, expectedHitCount);
    }

    String doDefaultFieldQuery(Entity entity, String type, String field, String queryString, int expectedHitCount) throws Exception {
        int runCount = 0, nbrResult;
        JestResult result;
        do {

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"default_field\" : \"" + field + "\",\n" +
                    "              \"query\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(indexManager.parseIndex(entity))
                    .addType(type)
                    .build();

            result = esClient.execute(search);
            String message = entity + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < 5);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        Assert.assertEquals("Unexpected hit count searching '" + entity + "' for [" + queryString + "] in field [" + field + "]", expectedHitCount, nbrResult);
        if (nbrResult != 0)
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        else
            return result.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray().toString();
    }

    public Entity getEntity(String comp, String fort, String userName, String docType) throws FlockException {
        String callerRef = new DateTime().toString();
        return getEntity(comp, fort, userName, docType, callerRef);
    }

    public Entity getEntity(String comp, String fort, String userName, String docType, String code) throws FlockException {
        // These are the minimum objects necessary to create Entity data

        Company mockCompany = new Company(comp);
        mockCompany.setName(comp);

        FortressInputBean fib = new FortressInputBean(fort, false);
        Fortress fortress = new Fortress(fib, mockCompany);
        String index = indexManager.getIndexRoot(fortress);
        fortress.setRootIndex(index);
        DateTime now = new DateTime();
        EntityInputBean entityInput = EntityContentHelper.getEntityInputBean(docType, fortress, userName, code, now);

        DocumentType doc = new DocumentType(fortress, docType);
        return new Entity(Long.toString(System.currentTimeMillis()), fortress.getDefaultSegment(), entityInput, doc)
                .setIndexName(index);

    }
}
