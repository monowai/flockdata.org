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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchSchema;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Raw ES client functionality. Establishes a local node
 */
public class TestElasticSearch extends ESBase {

    private Logger logger = LoggerFactory.getLogger(TestElasticSearch.class);
    ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

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

    private String getComputerName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else if (env.containsKey("HOSTNAME"))
            return env.get("HOSTNAME");
        else
            return "Unknown Computer";
    }


    private GetResponse writeSimple(EntitySearchChange change) throws Exception {
        File tempDir = File.createTempFile("elasticsearch-temp", Long.toString(System.nanoTime()));
        tempDir.delete();
        tempDir.mkdir();

        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", getComputerName())
                .put("node.name", getComputerName())
                .put("path.home", new File(tempDir, "./").getAbsolutePath())
                .put("path.data", new File(tempDir, "data").getAbsolutePath())
                .put("path.logs", new File(tempDir, "logs").getAbsolutePath())
                .put("path.work", new File(tempDir, "work").getAbsolutePath())

                .put("node.data", true)
                .put("node.local", true).build();

        // Elasticsearch
        Node node = org.elasticsearch.node.NodeBuilder
                .nodeBuilder()
                .settings(settings)
                .clusterName(getComputerName())
                .local(true)
                .node();

        Client client = node.client();
        String indexKey = change.getIndexName() == null ? "indexkey" : change.getIndexName();

        // Write the object to Lucene
        IndexResponse ir =
                client.prepareIndex(indexKey, change.getWho())
                        .setSource(om.writeValueAsString(change))
                        .setRouting(change.getKey())
                        .execute()
                        .actionGet();

        assertNotNull(ir);
        logger.info(ir.getId());

        // Retrieve from Lucene
        GetResponse response = client.prepareGet(indexKey, change.getWho(), ir.getId())
                .setRouting(change.getKey())
                .execute()
                .actionGet();
        node.close();
        return response;
    }

//    private  GetResponse writeSimpleV2(EntitySearchChange change) throws Exception {
//
//        // Elasticsearch v2
//
//        Client client = getNode().client();
//        String indexKey = change.getIndexName() == null ? "indexkey" : change.getIndexName();
//
//        // Write the object to Lucene
//        IndexResponse ir =
//                client.prepareIndex(indexKey, change.getWho())
//                        .setSource(om.writeValueAsString(change))
//                        .setRouting(change.getKey())
//                        .execute()
//                        .actionGet();
//
//        assertNotNull(ir);
//        logger.info(ir.getId());
//
//        // Retrieve from Lucene
//        GetResponse response = client.prepareGet(indexKey, change.getWho(), ir.getId())
//                .setRouting(change.getKey())
//                .execute()
//                .actionGet();
//        client.close();
//        return response;
//    }

    //    public  Node getNode() throws Exception {
//        File tempDir = File.createTempFile("elasticsearch-temp", Long.toString(System.nanoTime()));
//        tempDir.delete();
//        tempDir.mkdir();
//        //LOGGER.info("writing to: %s", tempDir);
//
//        String clusterName = UUID.randomUUID().toString();
//        Node esNode = NodeBuilder
//                .nodeBuilder()
//                .local(false)
//                .clusterName(clusterName)
//                .settings(  Settings.builder()
//                                .put("index.number_of_shards", "1")
//                                .put("index.number_of_replicas", "0")
//                                .put("path.home", new File(tempDir, "./").getAbsolutePath())
//                                .put("path.data", new File(tempDir, "data").getAbsolutePath())
//                                .put("path.logs", new File(tempDir, "logs").getAbsolutePath())
//                                .put("path.work", new File(tempDir, "work").getAbsolutePath())
//                ).node();
//        esNode.start();
//        return esNode;
//    }

}
