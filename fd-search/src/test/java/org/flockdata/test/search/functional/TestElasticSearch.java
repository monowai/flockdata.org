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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchSchema;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Raw ES client functionality. Establishes a local node
 */
@RunWith(SpringRunner.class)
public class TestElasticSearch extends ESBase {

    ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
    private Logger logger = LoggerFactory.getLogger(TestElasticSearch.class);

    @Test
    public void testMappingJson() throws Exception {
        String escWhat = "{\"house\": \"house\"}";
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        Map<String, Object> indexMe = new HashMap<>(40);
        indexMe.put("auditKey", "abc");
        Map what = om.readValue(escWhat, Map.class);
        indexMe.put(SearchSchema.DATA, what);
        logger.info(indexMe.get(SearchSchema.DATA).toString());
    }

    @Test
    public void testJson() throws Exception {
        // Basic JSON/ES tests to figure our what is going on

        EntitySearchChange change = new EntitySearchChange();
        change.setSysWhen(new DateTime().getMillis());

        // Add Who Parameter because it's used in creating the Document in ES as a Type .
        change.setWho("Who");

        HashMap<String, Object> name = new HashMap<>();
        name.put("first", "Joe");
        name.put("last", "Sixpack");
        change.setData(name);

        GetResponse response = writeSimple(change);
        assertNotNull(response);

        EntitySearchChange found = om.readValue(response.getSourceAsBytes(), EntitySearchChange.class);
        assertNotNull(found);
        assertEquals(0, change.getSysWhen().compareTo(found.getSysWhen()));


    }

    private GetResponse writeSimple(EntitySearchChange change) throws Exception {

        // Elasticsearch
        RestHighLevelClient client = searchConfig.getRestHighLevelClient();

        String indexKey = change.getIndexName() == null ? "indexkey" : change.getIndexName();
        IndexRequest indexRequest = new IndexRequest(indexKey, change.getWho())
            .source(om.writeValueAsString(change));

        IndexResponse indexResponse = client.index(indexRequest);

        assertNotNull(indexResponse);
        logger.info(indexResponse.getId());

        return client.get
            (new GetRequest(indexKey, change.getWho(), indexResponse.getId())
            );
    }


}
