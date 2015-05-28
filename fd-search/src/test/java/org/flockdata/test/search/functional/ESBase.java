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
import io.searchbox.indices.mapping.GetMapping;
import junit.framework.TestCase;
import org.flockdata.search.endpoint.TrackServiceEs;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.service.QueryServiceEs;
import org.flockdata.track.model.TrackSearchDao;
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

    private static JestClient esClient;

    @Autowired
    TrackSearchDao searchRepo;

    @Qualifier("trackServiceEs")
    @Autowired
    TrackServiceEs trackService;

    @Autowired
    QueryServiceEs queryServiceEs;

    static void deleteEsIndex(String indexName) throws Exception {
        logger.info("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName.toLowerCase()).build());
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

    String doFacetQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        return doFacetQuery(index, field, queryString, expectedHitCount, null);
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
    String doFacetQuery(String index, String field, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
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
            Search search = new Search.Builder(query)
                    .addIndex(index)
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
     * @param index
     * @param field
     * @param queryString
     * @param expectedHitCount
     * @param exceptionMessage
     * @return
     * @throws Exception
     */
    String doFieldQuery(String index, String field, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
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
                    .addIndex(index)
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

    String doCompletionQuery(String index, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
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
                addIndex(index).
                build();
        result = esClient.execute(search);
        TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

        List<SuggestResult.Suggestion> suggestions = result.getSuggestions("tags");

        for (SuggestResult.Suggestion suggestion : suggestions) {
            assertEquals(exceptionMessage, expectedHitCount, suggestion.options.size());
        }


        return result.getJsonString();
    }

    String doQuery(String index, String queryString, int expectedHitCount) throws Exception {
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
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + index + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
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

    String doFacetQuery(String index, String type, String field, String queryString, int expectedHitCount) throws Exception {
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
                    .addIndex(index)
                    .addType(type)
                    .build();

            result = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < 5);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        Assert.assertEquals("Unexpected hit count searching '" + index + "' for {" + queryString + "} in field {" + field + "}", expectedHitCount, nbrResult);
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

    String doDefaultFieldQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        return doDefaultFieldQuery(index, null, field, queryString, expectedHitCount);
    }

    String doDefaultFieldQuery(String index, String type, String field, String queryString, int expectedHitCount) throws Exception {
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
                    .addIndex(index)
                    .addType(type)
                    .build();

            result = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < 5);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        Assert.assertEquals("Unexpected hit count searching '" + index + "' for [" + queryString + "] in field [" + field + "]", expectedHitCount, nbrResult);
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

    String getMapping(String indexName) throws Exception {
        return getMapping(indexName, null);
    }

    String getMapping(String indexName, String type) throws Exception {

        GetMapping mapping = new GetMapping.Builder()
                .addIndex(indexName)
                .addType(type)
                .build();


        JestResult jResult = esClient.execute(mapping);
        if (jResult == null)
            return null;
        return jResult.getJsonString();
    }

}
