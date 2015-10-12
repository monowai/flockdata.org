package org.flockdata.test.search.functional;

import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import org.flockdata.model.Entity;
import org.flockdata.search.IndexHelper;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mike on 10/09/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestParentChild extends ESBase {
    @Test
    public void testParentChildStructure() throws Exception {


        String fortress = "Common";
        String company = "Anyco";
        String parent = "Parent";
        String child = "Child";
        String user = "mike";

        deleteEsIndex("fd." + company.toLowerCase() + "." + fortress.toLowerCase(), "parent");
        deleteEsIndex("fd." + company.toLowerCase() + "." + fortress.toLowerCase(), "child");

        Entity parentEntity = Helper.getEntity(company, fortress, user, parent, "123");
        EntityKeyBean parentKey = new EntityKeyBean(parentEntity);

        EntitySearchChange change = new EntitySearchChange(parentEntity);


        trackService.createSearchableChange(new EntitySearchChanges(change));

        // Children have to be in the same company/fortress.
        // ES connects the Child to a Parent. Parents don't need to know about children
        Entity childEntity = Helper.getEntity(company, fortress, user, child);
        EntitySearchChange childChange = new EntitySearchChange(childEntity);
        childChange.setParent(parentKey);
        childChange.setWhat(Helper.getSimpleMap("childKey", "childValue"));

        trackService.createSearchableChange(new EntitySearchChanges(childChange));
        Thread.sleep(2000);
        // One document of parent type
        doQuery(parentEntity, "*", 1);
        // One document of child type
        doQuery(childEntity, "*", 1);

        // Should find both the parent and the child when searching just the index
        doQuery(IndexHelper.parseIndex(parentEntity), "*", "*", 2);
        // Both entities are in the same index but are of different types
        doQuery(IndexHelper.parseIndex(childEntity), "*", "*", 2);

        String result = doHasChild(parentEntity, childEntity.getType().toLowerCase(), "childValue");
        assertTrue ( result.contains("123"))   ;

    }

    String doHasChild(Entity entity, String childType, String queryString) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        //waitAWhile();
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
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(IndexHelper.parseType(entity))
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);

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

        assertNotNull(jResult);
        Assert.assertEquals(IndexHelper.parseIndex(entity) + "\r\n" + queryString + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);

        return jResult.getJsonString();

    }
}
