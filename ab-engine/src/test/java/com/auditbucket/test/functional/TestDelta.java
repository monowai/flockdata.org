/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.audit.bean.AuditDeltaBean;
import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.bean.AuditResultBean;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestDelta {
    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    WhatService whatService;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private AuditManagerService auditManagerService;

    private Logger logger = LoggerFactory.getLogger(TestAudit.class);
    private String monowai = "Monowai";
    private String mike = "mike";
    private String mark = "mark@null.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    private Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "user1");

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


    @Test
    public void jsonDeltasAreFound() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
         setSecurity();
        Fortress fortress = fortressService.registerFortress("DELTAForce");
        assertNotNull(fortress);

        String typeA = "TypeA";

        String jsonA = "{\"house\": \"red\", \"bedrooms\": 2, \"garage\": \"Y\"}";
        String jsonB = "{\"house\": \"green\", \"bedrooms\": 2, \"list\": [1,2,3]}";


        AuditHeaderInputBean header = new AuditHeaderInputBean("DELTAForce", "auditTest", typeA, new DateTime(), "abdelta");
        AuditLogInputBean log = new AuditLogInputBean("Mike", new DateTime(), jsonA);
        header.setAuditLog(log);
        AuditResultBean result = auditManagerService.createHeader(header);
        AuditLog first = auditService.getLastAuditLog(result.getAuditHeader());
        Assert.assertNotNull(first);
        log = new AuditLogInputBean(result.getAuditKey(), "Mike", new DateTime(), jsonB);
        auditManagerService.createLog(log);
        AuditLog second = auditService.getLastAuditLog(result.getAuditHeader());
        Assert.assertNotNull(second);


        AuditDeltaBean deltaBean = whatService.getDelta(result.getAuditHeader(), first.getAuditChange(), second.getAuditChange());
        Map added = deltaBean.getAdded();
        Assert.assertNotNull(added);
        assertTrue (added.containsKey("list"));

        Map removed = deltaBean.getRemoved();
        Assert.assertNotNull(removed);
        assertTrue (removed.containsKey("garage"));

        Map changed = deltaBean.getChanged();
        Assert.assertNotNull(changed);
        assertTrue(changed.containsKey("house"));

        assertNotNull ( deltaBean);


    }



}
