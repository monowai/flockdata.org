/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.integration;

import org.flockdata.helper.JsonUtils;
import org.flockdata.search.QueryParams;

import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Support functions for interacting with ElasticSearch during integration testing
 *
 * @author mholdsworth
 * @since 31/05/2016
 */
public class SearchHelper {

    QueryParams getTagQuery(String label, String searchText) throws IOException {
        return new QueryParams()
                .searchTags()
                .setTypes(label.toLowerCase())
                .setQuery(JsonUtils.toMap("{\n" +
                        "    \"filtered\": {\n" +
                        "      \"query\": {\n" +
                        "        \"query_string\": {\n" +
                        "          \"query\": \""+searchText+"\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "}"));
    }

    QueryParams getTagMatchQuery(String label, String field, String searchText ) throws IOException {
        return new QueryParams()
                .searchTags()
                .setTypes(label.toLowerCase())
                .setQuery(JsonUtils.toMap("{\n" +
                        "    \"match\": {\n" +
                        "      \""+field+"\": {\n" +
                        "        \"query\": \""+searchText+"\",\n" +
                        "        \"type\": \"phrase\"\n" +
                        "      }\n" +
                        "  }\n" +
                        "}"));

    }

    void assertHitCount(String message, int expectedCount, Map<String, Object> esResult) {
        assertNotNull(esResult);
        int count = getHitCount(esResult);
        assertEquals(message + " got "+count, expectedCount, count);
    }

    Integer getHitCount( Map<String, Object> esResult){
        Map hits = (Map) esResult.get("hits");
        assertNotNull(hits);
        return (Integer) hits.get("total");
    }

    /**
     *
     * @param esResult Map of ES results
     * @return the hits as a Json string
     */
    String getHits(Map<String, Object> esResult) {
        assertNotNull(esResult);
        return JsonUtils.toJson(esResult.get("hits"));
    }
}
