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

package org.flockdata.test.search.functional;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.Suggest;
import io.searchbox.core.SuggestResult;
import io.searchbox.indices.DeleteIndex;
import junit.framework.TestCase;
import org.flockdata.model.Entity;
import org.flockdata.search.IndexHelper;
import org.flockdata.search.endpoint.TrackServiceEs;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.service.IndexMappingService;
import org.flockdata.search.service.QueryServiceEs;
import org.flockdata.search.service.TrackSearchDao;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.Rollback;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:55 PM
 */
@Component
public class ESBase {
    private static Logger logger = LoggerFactory.getLogger(TestMappings.class);

    static Properties properties = new Properties();

    static JestClient esClient;

    @Autowired
    TrackSearchDao searchRepo;

    @Autowired
    IndexMappingService indexMappingService;


    @Qualifier("trackServiceEs")
    @Autowired
    TrackServiceEs trackService;

    @Autowired
    QueryServiceEs queryServiceEs;

    static void deleteEsIndex(Entity entity) throws Exception{
        deleteEsIndex(entity.getFortress().getIndexName(), entity.getType());
    }

    static void deleteEsIndex(String indexName, String documentType) throws Exception {
        String deleteMe = IndexHelper.parseIndex(indexName, documentType);
        logger.info("%% Delete Index {}", deleteMe);
        esClient.execute(new DeleteIndex.Builder(deleteMe).build());
    }

    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        logger.info("You have to be running this with a working directory of fd-search for this to work");
        FileInputStream f = new FileInputStream("./src/test/resources/fd-search-config.properties");
        properties.load(f);

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

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

            if (result.getErrorMessage() == null) {
                assertNotNull(result.getErrorMessage(), result.getJsonObject());
                assertNotNull(result.getErrorMessage(), result.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(result.getErrorMessage(), result.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again

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
                    "            default_field:   \"" + field + "\", query: \"" + queryString.toLowerCase() + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(IndexHelper.parseIndex(entity))
                    .build();

            result = esClient.execute(search);
            TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

            if (result.getErrorMessage() == null) {
                assertNotNull(result.getErrorMessage(), result.getJsonObject());
                assertNotNull(result.getErrorMessage(), result.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(result.getErrorMessage(), result.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
        logger.trace("searching index [{}] field [{}] for [{}]", IndexHelper.parseIndex(entity), field, queryString);
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
                "            \"field\" : \"" + EntitySearchSchema.ALL_TAGS + "\"\n" +
                "        }\n" +
                "    }" +
                "}";


        Suggest search = new Suggest.Builder(query).
                addIndex(IndexHelper.parseIndex(entity)).
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
        return doQuery(IndexHelper.parseIndex(entity), IndexHelper.parseType(entity), queryString, expectedHitCount);
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
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else {
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again
            }
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < 6);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        Assert.assertEquals(index + "\r\n" + queryString + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);

        return jResult.getJsonString();

    }

    String doFacetQuery(Entity entity, String type, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
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
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(type)
                    .build();

            result = esClient.execute(search);
            String message = IndexHelper.parseIndex(entity) + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < 5);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        Assert.assertEquals("Unexpected hit count searching '" + IndexHelper.parseIndex(entity) + "' for {" + queryString + "} in field {" + field + "}", expectedHitCount, nbrResult);
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
                    .addIndex(IndexHelper.parseIndex(entity))
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


}
