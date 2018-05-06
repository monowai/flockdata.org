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

package org.flockdata.test.search.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.searchbox.action.Action;
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
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.base.IndexMappingService;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.base.TagChangeWriter;
import org.flockdata.search.configure.SearchConfig;
import org.flockdata.search.service.ContentService;
import org.flockdata.search.service.QueryServiceEs;
import org.flockdata.search.service.SearchAdmin;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.test.search.EsContainer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.Rollback;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * @author mholdsworth
 * @tag Test, ElasticSearch, Search
 * @since 15/08/2014
 */
@Component
public class ESBase {
    static JestClient esClient;
    private static Logger logger = LoggerFactory.getLogger(TestMappings.class);

    private static EsContainer esContainer = EsContainer.getInstance();

    @Autowired
    SearchConfig searchConfig;

    @Autowired
    EntityChangeWriter entityWriter;

    @Autowired
    TagChangeWriter tagWriter;

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
        int httpPort = esContainer.esContainer().getMappedPort(9200);

        //properties.get("es.http.port")
        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + httpPort).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();
        System.setProperty("es.http.port", esContainer.esContainer().getMappedPort(9200).toString());
        System.setProperty("es.nodes", "localhost:" + esContainer.esContainer().getMappedPort(9300));
        System.setProperty("es.tcp.port", esContainer.esContainer().getMappedPort(9300).toString());
        logger.info("Set properties - http: {}, tcp {}", System.getProperty("es.http.port"), System.getProperty("es.tcp.port"));
    }

//    @PostConstruct
//    void resetPorts() throws Exception {
//        GenericContainer container = esContainer.esContainer();
//        searchConfig.resetPorts(
//            container.getMappedPort(9200),
//            container.getMappedPort(9300)
//        );
////        logger.info("Running {}, http {}", container.isRunning(), container.getMappedPort(9200));
//    }

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
        deleteEsIndex(searchConfig.getIndexManager().toIndex(entity));
    }

    void deleteEsIndex(String indexName) throws Exception {
        logger.info("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName).build());
    }

    /**
     * Term query on a non-analyzed field
     *
     * @param index to search
     * @param field to query
     * @param queryString for
     * @param expectedHitCount how many?
     * @param exceptionMessage display this error
     * @return ES JSON result
     * @throws Exception anything goes wrong
     */
    String doTermQuery(String index, String type, String field, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        // There should only ever be one document for a given Entity.
        // Let's assert that
        int runCount = 0, nbrResult;
        JestResult result;
        int esTimeout = 5;

        do {

            runCount++;
            String query = getTermQuery(field, queryString);
            if (type != null && type.equals("*"))
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

    private String getTermQuery(String field, String queryString) {
        return "{\n" +
                        "    \"query\": {\n" +
                        "          \"term\" : {\n" +
                        "              \"" + field + "\" : \"" + queryString + "\"\n" +
                        "           }\n" +
                        "      }\n" +
                        "}";
    }

    /**
     * Scans an analyzed field looking for the queryString
     *
     * @param entity calculate the index from this
     * @param field to query
     * @param queryString for
     * @param expectedHitCount how many?
     * @param exceptionMessage display this error
     * @return ES JSON result
     * @throws Exception anything goes wrong
     */
    String doTermQuery(Entity entity, String field, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        int runCount = 0, nbrResult;
        JestResult result;
        int esTimeout = 5;

        do {

            runCount++;
            String query = getTermQuery(field, queryString);
            Search search = new Search.Builder(query)
                .addIndex(searchConfig.getIndexManager().toIndex(entity))
                    .build();

            result = esClient.execute(search);
            TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

            nbrResult = getNbrResult(result);

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
        logger.trace("searching index [{}] field [{}] for [{}]", searchConfig.getIndexManager().toIndex(entity), field, queryString);
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
                "            \"field\" : \"" + SearchSchema.ALL_TAGS +".suggest"+ "\"\n" +
                "        }\n" +
                "    }" +
                "}";


        Suggest search = new Suggest.Builder(query).
            addIndex(searchConfig.getIndexManager().toIndex(entity)).
                build();
        result = esClient.execute(search);
        TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

        List<SuggestResult.Suggestion> suggestions = result.getSuggestions("tags");

        for (SuggestResult.Suggestion suggestion : suggestions) {
            assertEquals(exceptionMessage, expectedHitCount, suggestion.options.size());
        }


        return result.getJsonString();
    }

    String doQuery(Entity entity, String queryString) throws Exception {
        return doQuery(searchConfig.getIndexManager().toIndex(entity),
            searchConfig.getIndexManager().parseType(entity), queryString, 1);
    }

    String doQuery(String index, String type, String queryString, int expectedHitCount) throws Exception {

        int runCount = 0, nbrResult;
        JestResult jResult;
        do {

            String query = "{\n" +
                    "    \"query\": {\n" +
                    "          \"query_string\" : {\n" +
                    "              \"query\" : \"" + queryString + "\"" +
                    "           }\n" +
                    "      }\n" +
                    "}";

            //
            Search.Builder builder = new Search.Builder(query)
                    .addIndex(index);

            if (type != null && !type.equals("*"))
                builder.addType(type);

            Search search = builder.build();
            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + index + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
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

    String getMapping(Entity entity) throws Exception {

        //"/fd.cust.fortmapping.doctype/_mapping/doctype";
        String indexName = searchConfig.getIndexManager().toIndex(entity);
        Action getMapping = new GetMapping.Builder().addIndex(indexName).build();
        JestResult result = esClient.execute(getMapping);
        if ( result == null )
            return null;
        return result.getJsonString();
    }

    String doTermQuery(Entity entity, String field, String queryString) throws Exception {
        int runCount = 0, nbrResult;

        JestResult result;
        do {

            runCount++;
            String query = "{\n" +
                    "    \"query\" : {\n" +
                    "                \"term\" : {\n" +
                    "                    \"" + field + "\" : \"" + queryString + "\"\n" +
                    "                }\n" +
                    "    }\n" +
                    "}\n";
            Search search = new Search.Builder(query)
                .addIndex(searchConfig.getIndexManager().toIndex(entity))
                .addType(searchConfig.getIndexManager().parseType(entity))
                    .build();

            result = esClient.execute(search);
            String message = searchConfig.getIndexManager().toIndex(entity) + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != 1 && runCount < 5);

        Assert.assertEquals("Unexpected hit count searching '" + searchConfig.getIndexManager().toIndex(entity) + "' for {" + queryString + "} in field {" + field + "}", 1, nbrResult);
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

    String doDescriptionQuery(Entity entity, String queryString, int expectedHitCount) throws Exception {

        int runCount = 0, nbrResult;
        JestResult result;
        String field = "description";
        do {

            runCount++;
            String query = "{\n" +
                    "    \"query\": {\n" +
                    "          \"query_string\" : {\n" +
                    "              \"default_field\" : \"" + field + "\",\n" +
                    "              \"query\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                .addIndex(searchConfig.getIndexManager().toIndex(entity))
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
        return getEntity(comp, fort, userName, docType, null);
    }

    protected Entity getEntity(String company, String fortress, String user, String invoice, String code) throws FlockException {
        return getEntity(company, fortress, user, invoice, code, null);
    }

    public Entity getEntity(String comp, String fort, String userName, String docType, String code, String segment) throws FlockException {
        // These are the minimum objects necessary to create Entity data
        Entity entity = MockDataFactory.getEntity(comp, fort, userName, docType, code);
        boolean defaultSegment = segment == null || segment.equals(Fortress.DEFAULT);

        when(entity.getSegment().isDefault()).thenReturn(defaultSegment);
        if (!defaultSegment) {
            when(entity.getSegment().getCode()).thenReturn(segment);
        }
        assertEquals(searchConfig.getIndexManager().getIndexRoot(entity.getFortress()), entity.getFortress().getRootIndex());

        return entity;

    }

    /**
     * Convenience helper to return the hits from an ES result
     *
     * @param result ES Json result
     * @return Map of hits
     * @throws IOException conversion error
     */
    Collection<Map<String, Object>> getHits(String result) throws IOException {
        Map<String, Object> rez = JsonUtils.toMap(result);
        TestCase.assertNotNull(rez);
        assertTrue("No hits found", rez.containsKey("hits"));
        Map<String, Object> hits = (Map<String, Object>) rez.get("hits");
        return (Collection<Map<String, Object>>) hits.get("hits");
    }

}
