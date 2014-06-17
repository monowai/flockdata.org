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

import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.MetaInputBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

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


    private Logger logger = LoggerFactory.getLogger(TestForceDeadlock.class);
    private String mike = "mike";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "123");

    @Test
    public void matrixQuery() throws Exception {
        String monowai = "Monowai";
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest" + System.currentTimeMillis(), true));

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
        MatrixResults results = queryEP.getMatrixResult(input, su.getApiKey(), su.getApiKey());
        assertFalse(results.getResults().isEmpty());
        int cCount = 5;
        // ToDo: How to assert it worked!

//        assertEquals(concepts * (concepts-1), results.getResults().size());

        input.setDocuments(docs);
        concepts.clear();   // Return everything
        input.setConcepts(concepts);
        results = queryEP.getMatrixResult(input, su.getApiKey(), su.getApiKey());
        cCount = 7;
        assertFalse(results.getResults().isEmpty());
  //      assertEquals(concepts * (concepts-1), results.getResults().size());

        concepts.clear();
        concepts.add(VEGETABLE);
        input.setConcepts(concepts);
        results = queryEP.getMatrixResult(input, su.getApiKey(), su.getApiKey());
        // Though peas is recorded against both A matrix ignores occurrence with the same "concept". If both had Peas, then a Peas-Potatoes would be returned
        assertEquals("Vegetable should has no co-occurrence", 0, results.getResults().size());
    }

    @Before
    public void setSecurity() {
        SecurityContextHolder.getContext().setAuthentication(authMike);
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        if (!"rest".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }
}
