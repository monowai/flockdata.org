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

import com.auditbucket.audit.model.AuditEvent;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditLogResultBean;
import com.auditbucket.engine.service.AuditEventService;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
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

import java.util.Date;
import java.util.Set;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

/**
 * User: Mike Holdsworth
 * Since: 6/09/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestAuditEvent {
    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    AuditEventService auditEventService;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private AuditManagerService auditManagerService;

    private Logger logger = LoggerFactory.getLogger(TestAuditEvent.class);
    private String monowai = "Monowai";
    private String mike = "test@ab.com";
    private String mark = "mark@null.com";
    Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "user1");
    String what = "{\"house\": \"house";

    @Before
    public void setSecurity() {
        SecurityContextHolder.getContext().setAuthentication(authMike);
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        if (!"http".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Test
    public void noDuplicateEventsForACompany() {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        Company company = fortressA.getCompany();
        assertNotNull(company);
        String eventCode = "DuplicateNotAllowed";
        AuditEvent event = auditEventService.processEvent(eventCode);

        assertNotNull(event);
        Long existingId = event.getId();
        assertEquals(eventCode, event.getCode());
        assertEquals(company.getId(), event.getCompany().getId());
        Set<AuditEvent> events = auditEventService.getCompanyEvents(company.getId());
        assertEquals(1, events.size());
        event = auditEventService.processEvent(eventCode);
        assertEquals(existingId, event.getId());
        assertEquals(1, events.size());

    }
}