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

import com.auditbucket.engine.endpoint.QueryEP;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.DocumentType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 14/06/14
 * Time: 10:40 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestQuery {
    @Autowired
    TrackEP trackEP;

    @Autowired
    QueryEP queryEP;

    @Autowired
    FortressEP fortressEP;

    @Autowired
    RegistrationEP registrationEP;

    @Autowired
    private Neo4jTemplate template;

    // This has to be a user in spring-security.xml that is authorised to create registrations
    private Authentication authMike = new UsernamePasswordAuthenticationToken("mike", "123");
    private String what = "{\"house\": \"house";

    @Test
    public void queryInputsReturned () throws Exception{
        //      Each fortress one MetaHeader (diff docs)
        //          One MH with same tags over both companies
        //          One MH with company unique tags
        SecurityContextHolder.getContext().setAuthentication(authMike);

        // Two companies
        //  Each with two fortresses

        SystemUserResultBean suA = registrationEP.registerSystemUser(new RegistrationBean("CompanyA", "userA")).getBody();
        SystemUserResultBean suB = registrationEP.registerSystemUser(new RegistrationBean("CompanyB", "userB")).getBody();

        Fortress coAfA = fortressEP.registerFortress(new FortressInputBean("coAfA", true), suA.getApiKey(), suA.getApiKey()).getBody();
        Fortress coAfB = fortressEP.registerFortress(new FortressInputBean("coAfB", true), suA.getApiKey(), suA.getApiKey()).getBody();

        Fortress coBfA = fortressEP.registerFortress(new FortressInputBean("coBfA", true), suB.getApiKey(), suB.getApiKey()).getBody();
        Fortress coBfB = fortressEP.registerFortress(new FortressInputBean("coBfB", true), suB.getApiKey(), suB.getApiKey()).getBody();

        SecurityContextHolder.getContext().setAuthentication(authMike);
        //
        //
        MetaInputBean inputBean = new MetaInputBean(coAfA.getName(), "poppy", "SalesDocket", DateTime.now(), "ABC1"); // Sales fortress
        inputBean.addTag(new TagInputBean("c123", "purchased").setIndex("Customer")); // This tag tracks over two fortresses
        trackEP.trackHeader(inputBean, suA.getApiKey(), null);
        inputBean = new MetaInputBean(coAfB.getName(), "poppy", "SupportSystem", DateTime.now(), "ABC2"); // Support system fortress
        inputBean.addTag(new TagInputBean("c123","called").setIndex("Customer")); // Customer number - this will be the same tag as for the sales fortress
        inputBean.addTag(new TagInputBean("p111","about").setIndex("Product"));   // Product code - unique to this fortress
        trackEP.trackHeader(inputBean, suA.getApiKey(), null);


        inputBean = new MetaInputBean(coBfA.getName(), "petal", "SalesDocket", DateTime.now(), "ABC1"); // Sales fortress
        inputBean.addTag(new TagInputBean("c123","purchased").setIndex("Customer")); // This tag tracks over two fortresses
        inputBean.addTag(new TagInputBean("ricky", "from").setIndex("SalesRep")); // This tag is unique to this company
        trackEP.trackHeader(inputBean, suB.getApiKey(), null);
        inputBean = new MetaInputBean(coBfB.getName(), "petal", "SupportSystem", DateTime.now(), "ABC2"); // Support system fortress
        inputBean.addTag(new TagInputBean("c123","called").setIndex("Customer")); // Customer number - this will be the same tag as for the sales fortress
        inputBean.addTag(new TagInputBean("p111", "about").setIndex("Product"));   // Product code - unique to this fortress
        trackEP.trackHeader(inputBean, suB.getApiKey(), null);

        Collection<String> fortresses = new ArrayList<>();
        fortresses.add(coAfA.getName());
        Collection<DocumentType> foundDocs = queryEP.getDocumentsInUse (fortresses, suA.getApiKey(), suA.getApiKey());
        assertEquals(1, foundDocs.size());

        fortresses.add(coAfB.getName());
        foundDocs = queryEP.getDocumentsInUse (fortresses, suA.getApiKey(), suA.getApiKey());
        assertEquals(2, foundDocs.size());

        // Company B
        fortresses.clear();
        fortresses.add(coBfA.getName());
        assertEquals(1, queryEP.getDocumentsInUse (fortresses, suB.getApiKey(), suB.getApiKey()).size());
        fortresses.add(coBfB.getName());
        assertEquals(2, queryEP.getDocumentsInUse (fortresses, suB.getApiKey(), suB.getApiKey()).size());



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
