package org.flockdata.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.model.Company;
import org.flockdata.model.Entity;
import org.flockdata.search.IndexHelper;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.model.QueryParams;
import org.flockdata.track.service.EntityService;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * ElasticSearch plumbing stuff to support running and validating integration tests
 *
 * Created by mike on 1/01/16.
 */
public class EsIntegrationHelper {
    static int esTimeout = 10; // Max attempts to find the result in ES
    private static JestClient esClient;
    private static final Logger logger = LoggerFactory.getLogger(EsIntegrationHelper.class);



    public static void cleanupElasticSearch() throws Exception {
        Properties properties = TestFdIntegration.getProperties(null);
        String abDebug = System.getProperty("fd.debug");

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "suppress"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "testfortress"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "ngram"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "rebuildtest"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "audittest"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "suppress"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "entitywithtagsprocess"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "trackgraph"));
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "111"));

        for (int i = 1; i < TestFdIntegration.fortressMax + 1; i++) {
            deleteEsIndex(IndexHelper.PREFIX + "monowai.bulkloada" + i);
        }

    }

    static void deleteEsIndex(String indexName) throws Exception {
        String deleteMe = IndexHelper.parseIndex(indexName);
        logger.info("%% Delete Index {}", deleteMe);
        esClient.execute(new DeleteIndex.Builder(deleteMe).build());
    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        if (esClient != null)
            esClient.shutdownClient();
    }

    static String doEsNestedQuery(Entity entity, String path, String field, String term, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        logger.debug("doEsQuery {}", term);
        JestResult jResult;
        do {
            if (runCount > 0)
                Helper.waitAWhile("Sleep {} for fd-search to catch up");
            String query = "{\n" +
                    "  \"query\": {\n" +
                    "    \"match_all\": {}\n" +
                    "  },\n" +
                    "  \"filter\": {\n" +
                    "    \"nested\": {\n" +
                    "      \"path\": \"" + path + "\",\n" +
                    "      \"filter\": {\n" +
                    "        \"bool\": {\n" +
                    "          \"must\": [\n" +
                    "            {\n" +
                    "              \"term\": {\n" +
                    "                \"" + field + "\": \"" + term + "\"\n" +
                    "              }\n" +
                    "            }\n" +
                    "          ]\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            Search search = new Search.Builder(query)
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(IndexHelper.parseType(entity))
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + entity + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", entity);
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
        } while (nbrResult != expectedHitCount && runCount < esTimeout);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        Object json = FdJsonObjectMapper.getObjectMapper().readValue(jResult.getJsonString(), Object.class);

        assertEquals(entity + "\r\n" + FdJsonObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json),
                expectedHitCount, nbrResult);
        return jResult.getJsonString();
    }

    static String doEsQuery(String index, String type, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        logger.debug("doEsQuery {}", queryString);
        JestResult jResult;
        do {
            if (runCount > 0)
                Helper.waitAWhile("Sleep {} for fd-search to catch up");
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"query\" : \"" + queryString + "\"" +
                    "           }\n" +
                    "      }\n" +
                    "}";

            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .addType(IndexHelper.parseType(type))
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
        } while (nbrResult != expectedHitCount && runCount < esTimeout);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        Object json = FdJsonObjectMapper.getObjectMapper().readValue(jResult.getJsonString(), Object.class);

        assertEquals(index + "\r\n" + FdJsonObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json),
                expectedHitCount, nbrResult);
        return jResult.getJsonString();
    }

    static String getMapping(String indexName) throws Exception {
        GetMapping mapping = new GetMapping.Builder()
                .addIndex(indexName)
                .build();

        JestResult jResult = esClient.execute(mapping);
        return jResult.getJsonString();
    }

    static String doEsTermQuery(Entity entity, String metaKey, String metaKey1, int i) throws Exception {
        return doEsTermQuery(entity, metaKey, metaKey1, i, false);
    }

    static String doEsTermQuery(Entity entity, String field, String queryString, int expectedHitCount, boolean suppressLog) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        JestResult jResult;

        do {
            if (runCount > 0)
                Helper.waitAWhile("Sleep {} for ES Query to work");
            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          term : {\n" +
                    "              \"" + field + "\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(entity.getType().toLowerCase())
                    .build();

            jResult = esClient.execute(search);
            String message = entity + " - " + field + " - " + queryString + (jResult == null ? "[noresult]" : "\r\n" + jResult.getJsonString());
            assertNotNull(message, jResult);
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        if (!suppressLog) {
            logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
            logger.trace("searching index [{}] field [{}] for [{}]", entity, field, queryString);
        }

        Object json = FdJsonObjectMapper.getObjectMapper().readValue(jResult.getJsonString(), Object.class);
        assertEquals(FdJsonObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json), expectedHitCount, nbrResult);

        if (nbrResult != 0) {
            return jResult.getJsonObject()
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

    static String doEsFieldQuery(Entity entity, String field, String queryString, int expectedHitCount) throws Exception {
        return doEsFieldQuery(IndexHelper.parseIndex(entity), entity.getType(), field, queryString, expectedHitCount);
    }

    /**
     * Use this carefully. Due to ranked search results, you can get more results than you expect. If
     * you are looking for an exact match then consider doEsTermQuery
     *
     * @param index            to search
     * @param field            field containing queryString
     * @param queryString      text to search for
     * @param expectedHitCount result count
     * @return query _source
     * @throws Exception if expectedHitCount != actual hit count
     */
    private static String doEsFieldQuery(String index, String type, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;

        JestResult jResult;
        do {
            if (runCount > 0)
                Helper.waitAWhile("Sleep {} for ES Query to work");

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "            default_field:   \"" + field + "\", query: \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .addType(IndexHelper.parseType(type))
                    .build();

            jResult = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (jResult == null ? "[noresult]" : "\r\n" + jResult.getJsonString());
            assertNotNull(message, jResult);
            assertNotNull(message, jResult.getJsonObject());
            assertNotNull(message, jResult.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, jResult.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        Object json = FdJsonObjectMapper.getObjectMapper().readValue(jResult.getJsonString(), Object.class);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        assertEquals("Unexpected hit count searching '" + index + "' for {" + queryString + "} in field {" + field + "}\n\r" + FdJsonObjectMapper.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json),
                expectedHitCount, nbrResult);
        if (nbrResult == 0)
            return "";
        else
            return jResult.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
    }

    static String doCompletionQuery(String index, String type, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        // There should only ever be one document for a given Entity.
        // Let's assert that
        int runCount = 0, nbrResult;
        SuggestResult result;
        int esTimeout = 5;

        runCount++;
        String query = "{" +
                "    \"result\" : {\n" +
                "        \"text\" : \"" + queryString + "\",\n" +
                "        \"completion\" : {\n" +
                "            \"field\" : \"" + EntitySearchSchema.ALL_TAGS + "\"\n" +
                "        }\n" +
                "    }" +
                "}";


        Suggest search = new Suggest.Builder(query)
                .addIndex(index)
                .addType(IndexHelper.parseType(type))
                .build();
        result = esClient.execute(search);
        TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

        List<SuggestResult.Suggestion> suggestions = result.getSuggestions("result");

        for (SuggestResult.Suggestion suggestion : suggestions) {
            assertEquals(expectedHitCount, suggestion.options.size());
        }


        return result.getJsonString();
    }

    static String doEsQuery(Entity entity, String queryString) throws Exception {
        return doEsQuery(entity, queryString, 1);
    }

    static String doEsQuery(Entity entity, String queryString, int expectedHitCount) throws Exception {
        return doEsQuery(IndexHelper.parseIndex(entity), entity.getType(), queryString, expectedHitCount);
    }

    static void validateResultFieds(String result) throws Exception {
        JsonNode node = FdJsonObjectMapper.getObjectMapper().readTree(result);

        assertNotNull(node.get(EntitySearchSchema.CREATED));
        assertNotNull(node.get(EntitySearchSchema.WHO));
        assertNotNull(node.get(EntitySearchSchema.UPDATED));
        assertNotNull(node.get(EntitySearchSchema.META_KEY));
        assertNotNull(node.get(EntitySearchSchema.DOC_TYPE));
        assertNotNull(node.get(EntitySearchSchema.FORTRESS));

    }

    static Entity waitForFirstSearchResult(Company company, Entity entity, EntityService entityService) throws Exception {
       // Looking for the first searchKey to be logged against the entity
       int i = 1;

       Thread.yield();
//        Entity entity = entityService.getEntity(company, metaKey);
       if (entity == null)
           return null;

       int timeout = 10;

       while (entity.getSearch() == null && i <= timeout) {

           entity = entityService.getEntity(company, entity.getMetaKey());
           //logger.debug("Entity {}, searchKey {}", entity.getId(), entity.getSearchKey());
           if (i > 5) // All this yielding is not letting other threads complete, so we will sleep
               Helper.waitAWhile("Sleeping {} secs for entity [" + entity.getId() + "] to update ");
           else if (entity.getSearch() == null)
               Thread.yield(); // Small pause to let things happen

           i++;
       }

       if (entity.getSearch() == null) {
           logger.debug("!!! Search not working after [{}] attempts for entityId [{}]. SearchKey [{}]", i, entity.getId(), entity.getSearchKey());
           fail("Search reply not received from fd-search");
       }
       return entity;
   }

    static String runQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = Helper.getHttpHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange(TestFdIntegration.FD_SEARCH + "/fd-search/v1/query/", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Client tracking error {}", e.getMessage());
        }
        return null;
    }
}
