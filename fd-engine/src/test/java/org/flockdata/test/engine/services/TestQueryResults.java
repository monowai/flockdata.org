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

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.data.Entity;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.MatrixInputBean;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 12/06/2014
 */
public class TestQueryResults extends EngineBase {
    public static final String VEGETABLE = "Vegetable";
    public static final String FRUIT = "Fruit";

    @Test
    public void matrixQuery() throws Exception {
        SystemUser su = registerSystemUser("matrixQuery", mike_admin);
        FortressNode fortress = createFortress(su);

        EntityInputBean inputBean = new EntityInputBean(fortress, "mike", "Study", new DateTime(), "StudyA");
        inputBean.addTag(new TagInputBean("Apples", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Pears", TestQueryResults.FRUIT, "likes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Oranges", TestQueryResults.FRUIT, "dislikes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Grapes", TestQueryResults.FRUIT, "allergic").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Potatoes", TestQueryResults.VEGETABLE, "likes").setLabel(VEGETABLE)); // No co-occurrence
        Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        assertEquals(5, entityTagService.findEntityTags(entity).size());

        inputBean = new EntityInputBean(fortress, "mike", "Study", new DateTime(), "StudyB");
        inputBean.addTag(new TagInputBean("Apples", TestQueryResults.FRUIT, "dislikes"));
        inputBean.addTag(new TagInputBean("Pears", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Oranges", TestQueryResults.FRUIT, "allergic"));
        inputBean.addTag(new TagInputBean("Grapes", TestQueryResults.FRUIT, "dislikes"));
        inputBean.addTag(new TagInputBean("Kiwi", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Peas", TestQueryResults.VEGETABLE, "dislikes"));
        entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        assertEquals(6, entityTagService.findEntityTags(entity).size());

        MatrixInputBean input = new MatrixInputBean();
        input.setSampleSize(-1); // Disable fd-search
        ArrayList<String> docs = new ArrayList<>();
        docs.add("Study");
        ArrayList<String> concepts = new ArrayList<>();

        concepts.add(FRUIT);
        input.setConcepts(concepts);
        int fruitCount = 5, things = 2;

        MatrixResults results = matrixService.getMatrix(su.getCompany(), input);
        //MatrixResults results = queryEP.getMatrixResult(input, su.getApiKey(), su.getApiKey());
        assertFalse(results.getEdges().isEmpty());
        assertEquals(4 + (4 * 4), results.getEdges().size());
        int cCount = 5;
        // ToDo: How to assert it worked!

//        assertEquals(concepts * (concepts-1), results.getEdges().size());

        input.setDocuments(docs);
        concepts.clear();   // Return everything
        input.setConcepts(concepts);
        results = matrixService.getMatrix(su.getCompany(), input);
        cCount = 7;
        assertFalse(results.getEdges().isEmpty());
        //      assertEquals(concepts * (concepts-1), results.getEdges().size());

        concepts.clear();
        concepts.add(VEGETABLE);
        input.setConcepts(concepts);
        results = matrixService.getMatrix(su.getCompany(), input);

        // Though peas is recorded against both A matrix ignores occurrence with the same "concept". If both had Peas, then a Peas-Potatoes would be returned
        assertEquals("Vegetable should have no co-occurrence", 0, results.getEdges().size());

        concepts.clear();
        concepts.add(FRUIT);
        ArrayList<String> filter = new ArrayList<>();
        filter.add("allergic");
        filter.add("dislikes");

        input.setFromRlxs(filter);
        input.setToRlxs(filter);
        results = matrixService.getMatrix(su.getCompany(), input);
        assertFalse(results.getEdges().isEmpty());
        ArrayList<String> fortresses = new ArrayList<>();
        fortresses.add(fortress.getName());

        Collection<DocumentResultBean> documentTypes = conceptService.getDocumentsInUse(su.getCompany(), fortresses);
        assertFalse(documentTypes.isEmpty());

        ArrayList<String> filterFrom = new ArrayList<>();
        filterFrom.add("allergic");

        // Bipartite
        ArrayList<String> filterTo = new ArrayList<>();
        filterTo.add("dislikes");
        input.setFromRlxs(filterFrom);
        input.setToRlxs(filterTo);
        results = matrixService.getMatrix(su.getCompany(), input);
        assertFalse(results.getEdges().isEmpty());

        input.setFromRlxs(filterTo);
        input.setToRlxs(filterFrom);
        results = matrixService.getMatrix(su.getCompany(), input);
        assertFalse(results.getEdges().isEmpty());
    }


}
