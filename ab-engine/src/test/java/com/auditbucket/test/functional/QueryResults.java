/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.test.functional;

import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.bean.MetaInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * User: mike
 * Date: 12/06/14
 * Time: 1:16 PM
 */
@Transactional
public class QueryResults  extends TestEngineBase {
    public static final String VEGETABLE = "Vegetable";
    public static final String FRUIT = "Fruit";

    @Test
    public void matrixQuery() throws Exception {
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Fortress fortress = createFortress(su);

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "mike", "Study", new DateTime(), "StudyA");
        inputBean.addTag(new TagInputBean("Apples", "likes").setIndex(QueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Pears", "likes").setIndex(QueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Oranges", "dislikes").setIndex(QueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Grapes", "allergic").setIndex(QueryResults.FRUIT));
//        inputBean.addTag(new TagInputBean("Peas", "dislikes").setIndex(VEGETABLE));
        inputBean.addTag(new TagInputBean("Potatoes", "likes").setIndex(VEGETABLE)); // No co-occurrence
        trackEP.trackHeader(inputBean, su.getApiKey(), su.getApiKey()) ;

        inputBean = new MetaInputBean(fortress.getName(), "mike", "Study", new DateTime(), "StudyB");
        inputBean.addTag(new TagInputBean("Apples", "dislikes").setIndex(FRUIT));
        inputBean.addTag(new TagInputBean("Pears", "likes").setIndex(FRUIT));
        inputBean.addTag(new TagInputBean("Oranges", "allergic").setIndex(FRUIT));
        inputBean.addTag(new TagInputBean("Grapes", "dislikes").setIndex(FRUIT));
        inputBean.addTag(new TagInputBean("Kiwi", "likes").setIndex(FRUIT));
        inputBean.addTag(new TagInputBean("Peas", "dislikes").setIndex(VEGETABLE));
        trackEP.trackHeader(inputBean, su.getApiKey(), su.getApiKey()) ;

        MatrixInputBean input = new MatrixInputBean();
        ArrayList<String>docs = new ArrayList<>();
        docs.add("Study");
        ArrayList<String>concepts = new ArrayList<>();

        concepts.add(FRUIT);
        input.setConcepts(concepts);
        int fruitCount = 5, things = 2;
        MatrixResults results= getMatrixResult(su, input);
        //MatrixResults results = queryEP.getMatrixResult(input, su.getApiKey(), su.getApiKey());
        assertFalse(results.getResults().isEmpty());
        assertEquals(4+(4*4), results.getResults().size());
        int cCount = 5;
        // ToDo: How to assert it worked!

//        assertEquals(concepts * (concepts-1), results.getResults().size());

        input.setDocuments(docs);
        concepts.clear();   // Return everything
        input.setConcepts(concepts);
        results = getMatrixResult(su, input);
        cCount = 7;
        assertFalse(results.getResults().isEmpty());
  //      assertEquals(concepts * (concepts-1), results.getResults().size());

        concepts.clear();
        concepts.add(VEGETABLE);
        input.setConcepts(concepts);
        results = getMatrixResult(su, input);

        // Though peas is recorded against both A matrix ignores occurrence with the same "concept". If both had Peas, then a Peas-Potatoes would be returned
        assertEquals("Vegetable should has no co-occurrence", 0, results.getResults().size());

        concepts.clear();
        concepts.add(FRUIT);
        ArrayList<String>filter = new ArrayList<>();
        filter.add("allergic");
        filter.add("dislikes");

        input.setFromRlxs(filter);
        input.setToRlxs(filter);
        results = getMatrixResult(su, input);
        assertFalse(results.getResults().isEmpty());
        ArrayList<String>fortresses = new ArrayList<>();
        fortresses.add(fortress.getName());
        Collection<DocumentResultBean>documentTypes = TestQuery.getDocuments(su, fortresses);
        assertFalse(documentTypes.isEmpty());

    }

    private MatrixResults getMatrixResult(SystemUser su, MatrixInputBean input) throws Exception {
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/query/matrix/")
                        .header("Api-Key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), MatrixResults.class);
    }




}
