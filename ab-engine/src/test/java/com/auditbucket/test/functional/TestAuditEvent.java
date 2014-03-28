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

import com.auditbucket.audit.bean.LogInputBean;
import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.bean.TrackResultBean;
import com.auditbucket.audit.model.ChangeEvent;
import com.auditbucket.audit.model.ChangeLog;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.audit.model.TrackLog;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TrackEventService;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
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

import java.util.Set;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 6/09/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestAuditEvent {
    @Autowired
    TrackService trackService;

    @Autowired
    RegistrationEP regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private MediationFacade mediationFacade;

    private Logger logger = LoggerFactory.getLogger(TestAuditEvent.class);
    private String monowai = "Monowai";
    private String mike = "test@ab.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");

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
    public void noDuplicateEventsForACompany() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        Company company = fortressA.getCompany();
        assertNotNull(company);
        String eventName = "DuplicateNotAllowed";
        ChangeEvent event = trackEventService.processEvent(eventName);

        assertNotNull(event);
        Long existingId = event.getId();
        assertEquals(eventName, event.getName());
        assertEquals(eventName.toLowerCase(), event.getCode());
        //assertEquals(company.getId(), event.getCompany().getId());
        Set<ChangeEvent> events = trackEventService.getCompanyEvents(company.getId());
        assertEquals(1, events.size());
        event = trackEventService.processEvent(eventName);
        assertEquals(existingId, event.getId());
        assertEquals(1, events.size());

    }

    /**
     * Ensures that the event type gets set to the correct default for create and update.
     */
    @Test
    public void defaultEventTypesAreHandled() throws Exception {

        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();
        assertNotNull(ahKey);

        MetaHeader header = trackService.getHeader(ahKey);
        assertNotNull(header.getDocumentType());

        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));

        TrackLog when = trackService.getLastLog(ahKey);
        assertNotNull(when);
        assertEquals(ChangeLog.CREATE, when.getChange().getEvent().getName()); // log event default
        assertEquals(ChangeLog.CREATE.toLowerCase(), when.getChange().getEvent().getName().toLowerCase()); // log event default

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        TrackLog whenB = trackService.getLastLog(ahKey);
        assertNotNull(whenB);

        assertFalse(whenB.equals(when));
        assertNotNull(whenB.getChange().getEvent());
        assertEquals(ChangeLog.UPDATE, whenB.getChange().getEvent().getName());  // log event default
    }
}