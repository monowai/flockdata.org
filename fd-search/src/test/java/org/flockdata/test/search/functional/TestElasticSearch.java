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

import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.endpoint.ElasticSearchEP;
import org.flockdata.search.service.TrackSearchDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestElasticSearch {

    private Logger logger = LoggerFactory.getLogger(TestElasticSearch.class);
    ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

    @Autowired
    TrackSearchDao alRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Test
    public void testMappingJson() throws Exception {
        String escWhat = "{\"house\": \"house\"}";
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

        Map<String, Object> indexMe = new HashMap<>(40);
        indexMe.put("auditKey", "abc");
        Map what = om.readValue(escWhat, Map.class);
        indexMe.put("what", what);
        logger.info(indexMe.get("what").toString());
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
        change.setWhat(name);

        GetResponse response = writeSimple(change);
        assertNotNull(response);

        EntitySearchChange found = om.readValue(response.getSourceAsBytes(), EntitySearchChange.class);
        assertNotNull(found);
        assertEquals(0, change.getSysWhen().compareTo(found.getSysWhen()));


    }


    private  GetResponse writeSimple(EntitySearchChange change) throws JsonProcessingException {

        // Elasticsearch
        Node node = nodeBuilder().local(true).node();
        Client client = node.client();
        String indexKey = change.getIndexName() == null ? "indexkey" : change.getIndexName();

        // Write the object to Lucene
        IndexResponse ir =
                client.prepareIndex(indexKey, change.getWho())
                        .setSource(om.writeValueAsString(change))
                        .setRouting(change.getMetaKey())
                        .execute()
                        .actionGet();

        assertNotNull(ir);
        logger.info(ir.getId());

        // Retrieve from Lucene
        GetResponse response = client.prepareGet(indexKey, change.getWho(), ir.getId())
                .setRouting(change.getMetaKey())
                .execute()
                .actionGet();
        node.close();
        return response;
    }

}
