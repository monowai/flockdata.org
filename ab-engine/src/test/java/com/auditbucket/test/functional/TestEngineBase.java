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

import com.auditbucket.company.endpoint.CompanyEP;
import com.auditbucket.engine.endpoint.AdminEP;
import com.auditbucket.engine.endpoint.QueryEP;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.*;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.endpoint.TagEP;
import com.auditbucket.registration.service.*;
import org.junit.Before;
import org.junit.Ignore;
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

/**
 * User: mike
 * Date: 16/06/14
 * Time: 7:54 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Ignore
public class TestEngineBase {

    @Autowired
    FortressEP fortressEP;

    @Autowired
    QueryEP queryEP;

    @Autowired
    RegistrationService regService;

    @Autowired
    RegistrationEP registrationEP;

    @Autowired
    SchemaService schemaService;

    @Autowired
    FortressService fortressService;

    @Autowired
    TrackService trackService;

    @Autowired
    TrackEP trackEP;

    @Autowired
    RegistrationEP regEP;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    SystemUserService systemUserService;

    @Autowired
    TagEP tagEP;

    @Autowired
    TagService tagService;

    @Autowired
    AdminEP adminEP;

    @Autowired
    EngineConfig engineAdmin;

    @Autowired
    WhatService whatService;

    @Autowired
    CompanyService companyService;

    @Autowired
    CompanyEP companyEP;

    @Autowired
    Neo4jTemplate template;

    @Autowired
    EngineConfig engineConfig;

    // These have to be in spring-security.xml that is authorised to create registrations
    String sally = "sally";
    String mike = "mike";
    String harry = "harry";

    String monowai = "Monowai"; // just a test constant

    Authentication authDefault = new UsernamePasswordAuthenticationToken(mike, "123");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        setSecurity();
        engineConfig.setMultiTenanted(false);
        if (!"http".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Before
    public void setSecurity() {
        SecurityContextHolder.getContext().setAuthentication(authDefault);
    }

    public static void setSecurity(Authentication auth){
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static Authentication setSecurity (String userName){
        Authentication auth = new UsernamePasswordAuthenticationToken(userName, "123");
        setSecurity(auth);
        return auth;
    }


}
