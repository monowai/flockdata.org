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

import static org.junit.Assert.assertNotNull;

import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import org.flockdata.data.Entity;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.SearchSchema;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * ES Parent/Child structures
 *
 * @author mholdsworth
 * @since 10/09/2015
 */
@RunWith(SpringRunner.class)
public class TestParentChild extends ESBase {

    @Test
    public void linkedEntities() throws Exception {

        String company = "xRef_FromInputBeans";
        String fortress = "timesheet";
        Entity entity = getEntity(company, fortress, "mike", "work");

        deleteEsIndex(entity);

        // Comes from TestEntityLinks.testLinkedToSearch
        String json = "{\n" +
            "  \"documentType\": \"work\",\n" +
            "  \"description\": null,\n" +
            "  \"" + SearchSchema.DATA + "\": null,\n" +
            "  \"props\": {},\n" +
            "  \"attachment\": null,\n" +
            "  \"fortressName\": \"timesheet\",\n" +
            "  \"who\": null,\n" +
            "  \"event\": null,\n" +
            "  \"key\": \"81Rpuh8WQw6xD4pDwijZPQ\",\n" +
            "  \"code\": \"ABC321\",\n" +
            "  \"logId\": null,\n" +
            "  \"tagValues\": {},\n" +
            "  \"id\": 10,\n" +
            "  \"indexName\": \"" + searchConfig.getIndexManager().toIndex(entity) + "\",\n" +
            "  \"sysWhen\": 1450644354230,\n" +
            "  \"replyRequired\": true,\n" +
            "  \"forceReindex\": false,\n" +
            "  \"delete\": false,\n" +
            "  \"createdDate\": 1450644354202,\n" +
            "  \"updatedDate\": null,\n" +
            "  \"contentType\": null,\n" +
            "  \"fileName\": null,\n" +
            "  \"tagStructure\": \"DEFAULT\",\n" +
            "  \"parent\": null,\n" +
            "  \"segment\": null,\n" +
            "  \"entityLinks\": [\n" +
            "    {\n" +
            "      \"fortress\": \"timesheet\",\n" +
            "      \"documentName\": \"Staff\",\n" +
            "      \"key\": \"ZnU0EpMaQHKFtUT2eZE9LQ\",\n" +
            "      \"code\": \"ABC123\",\n" +
            "      \"index\": \"" + searchConfig.getIndexManager().getPrefix() + "xref_frominputbeans.timesheet\",\n" +
            "      \"searchTags\": {\n" +
            "        \"role\": {\n" +
            "          \"position\": [\n" +
            "            {\n" +
            "              \"code\": \"Cleaner\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"relationshipName\": \"\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"searchKey\": \"ABC321\"\n" +
            "}";
        EntitySearchChange change = JsonUtils.toObject(json.getBytes(), EntitySearchChange.class);
        esSearchWriter.createSearchableChange(new SearchChanges(change));
        Thread.sleep(2000);

        //ToDo: Needs the relationship
        System.out.println(doQuery(entity, "*"));
        // ToDo - why is e.staff.code only matching on lowercase?
        doTermQuery(entity, "e.staff.code", "ABC123", 1, "Unable to locate by staff field");
        doTermQuery(entity, "e.staff.tag.role.position.code", "Cleaner", 1, "Unable to locate by staff tag code");
    }

    @Test
    public void testParentChildStructure() throws Exception {


        String fortress = "ParentChild";
        String company = "Anyco";
        String type = "Parent";
        String child = "Child";
        String user = "mike";

        Entity parentEntity = getEntity(company, fortress, user, type, "123");
        Entity childEntity = getEntity(company, fortress, user, child);

        deleteEsIndex(searchConfig.getIndexManager().toIndex(parentEntity));
        deleteEsIndex(searchConfig.getIndexManager().toIndex(childEntity));

        EntitySearchChange parent = new EntitySearchChange(parentEntity, searchConfig.getIndexManager().toIndex(parentEntity));

        // Children have to be in the same company/fortress.
        // ES connects the Child to a Parent. Parents don't need to know about children
        EntitySearchChange childChange =
            new EntitySearchChange(childEntity, searchConfig.getIndexManager().toIndex(parentEntity))
                .setParent(new EntityKeyBean(parentEntity, searchConfig.getIndexManager().toIndex(parentEntity)))
                .setData(ContentDataHelper.getSimpleMap("childKey", "childValue"));

        esSearchWriter.createSearchableChange(new SearchChanges(childChange));
        // I'm calling Parent/Child mapping broken for the time being. This test fails if the parent already exists
        // because the _hasChild is in the parent mapping, not hte child.
        //     https://github.com/elastic/elasticsearch/issues/9448
        esSearchWriter.createSearchableChange(new SearchChanges(parent));

        Thread.sleep(2000);
        // One document of parent type
        doQuery(parentEntity, "*");

        if (!searchConfig.getIndexManager().isSuffixed()) {
            doQuery(childEntity, "*");
        }

        // Should find both the parent and the child when searching just the index
        doQuery(searchConfig.getIndexManager().getIndexRoot(parentEntity.getFortress()) + "*", "*", "*", 2);
        // Both entities are in the same index but are of different types
        doQuery(searchConfig.getIndexManager().getIndexRoot(childEntity.getFortress()) + "*" + "*", "*", "*", 2);

//        String result = doHasChild(parentEntity, indexManager.parseType(childEntity), "childValue");
//        assertTrue(result.contains("123"));

    }

    String doHasChild(Entity entity, String childType, String queryString) throws Exception {

        int runCount = 0, nbrResult;
        JestResult jResult;

        int expectedHitCount = 1;
        do {

            String query = "{\n" +
                "  \"query\": {\n" +
                "    \"has_child\": {\n" +
                "      \"type\": \"" + childType + "\", \n" +
                "      \"query\": {\n" +
                "          query_string : {\n" +
                "              \"query\" : \"" + queryString + "\"" +
                "           }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

            //
            Search search = new Search.Builder(query)
                .addIndex(searchConfig.getIndexManager().toIndex(entity))
                .addType(searchConfig.getIndexManager().parseType(entity))
                .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            nbrResult = getNbrResult(jResult);
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < 6);

        assertNotNull(jResult);
        Assert.assertEquals(searchConfig.getIndexManager().toIndex(entity) + "\r\n" + queryString + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);

        return jResult.getJsonString();

    }
}
