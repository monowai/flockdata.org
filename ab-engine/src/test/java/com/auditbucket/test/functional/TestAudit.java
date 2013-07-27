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

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.IAuditWhen;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
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

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestAudit {
    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;

    private Logger log = LoggerFactory.getLogger(TestAudit.class);
    private String company = "Monowai";
    private String email = "mike@monowai.com";
    private String emailB = "mark@null.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");
    Authentication authB = new UsernamePasswordAuthenticationToken(emailB, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    @Test
    public void callerRefAuthzExcep() {
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fortressA = fortressService.registerFortress("auditTest");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String key = auditService.createHeader(inputBean).getAuditKey();
        // Check we can't create the same header twice for a given client ref
        inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String keyB = auditService.createHeader(inputBean).getAuditKey();
        assertEquals(key, keyB);

        Authentication authB = new UsernamePasswordAuthenticationToken("swagger", "user2");
        SecurityContextHolder.getContext().setAuthentication(authB);
        regService.registerSystemUser(new RegistrationBean("TestTow", "swagger", "bah"));
        IFortress fortressB = fortressService.registerFortress("auditTestB");
        auditService.createHeader(new AuditHeaderInputBean(fortressB.getName(), "wally", "TestAudit", new Date(), "123ABC"));

        SecurityContextHolder.getContext().setAuthentication(authA);

        assertNotNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "ABC123"));
        assertNotNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "abc123"));
        assertNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "123ABC"));
        // Test non external user can't do this
        SecurityContextHolder.getContext().setAuthentication(authB);
        try {
            assertNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "ABC123"));
            fail("Security exception not thrown");
        } catch (SecurityException se) {

        }

    }

    @Test
    public void createHeaderTimeLogs() throws Exception {

        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress(new FortressInputBean("auditTest"));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditService.findByCallerRef(fo.getId(), "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        log.info("Start-");
        watch.start();
        while (i < max) {
            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
        watch.stop();
        log.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);

        // Test that we get the expected number of log events
        assertEquals(max, (double) auditService.getAuditLogCount(ahKey));
    }

    /**
     * Idempotent "what" data
     * Ensure duplicate logs are not created when content data has not changed
     */
    @Test
    public void noDuplicateLogsWithCompression() throws Exception {

        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "9999");
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);
        // Irrespective of the order of the fields, we see it as the same.
        String jsonA = "{\"name\": \"8888\", \"thing\": {\"m\": \"happy\"}}";
        String jsonB = "{\"thing\": {\"m\": \"happy\"},\"name\": \"8888\"}";


        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));
        int i = 0;
        double max = 10d;
        String json;
        while (i < max) {
            // Same "what" text so should only be one auditLogCount record
            json = (i % 2 == 0 ? jsonA : jsonB);
            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), json));
            i++;
        }
        assertEquals(1d, (double) auditService.getAuditLogCount(ahKey));
        Set<IAuditLog> logs = auditService.getAuditLogs(ahKey);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
    }

    /**
     * Ensures that the eventtype gets set to the correct default for create and update.
     */
    @Test
    public void testEventType() throws Exception {

        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "YYY");
        AuditResultBean resultBean = auditService.createHeader(inputBean);
        String ahKey = resultBean.getAuditKey();
        assertNotNull(ahKey);

        IAuditHeader header = auditService.getHeader(resultBean.getAuditKey());
        assertNotNull(header.getDocumentType());

        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        IAuditWhen when = auditService.getLastChange(ahKey);
        assertNotNull(when);
        assertEquals(IAuditLog.CREATE, when.getAuditLog().getEvent()); // log event default

        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        IAuditWhen whenB = auditService.getLastChange(ahKey);
        assertNotNull(whenB);

        assertFalse(whenB.equals(when));
        assertEquals(IAuditLog.UPDATE, whenB.getAuditLog().getEvent());  // log event default
    }

    @Test
    public void testFortressLogCount() throws Exception {

        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "YYY");
        AuditResultBean resultBean = auditService.createHeader(inputBean);
        String ahKey = resultBean.getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);

        assertNotNull(auditService.getHeader(ahKey));

        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        auditService.getLastChange(ahKey);
        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        // ToDo: How to count the ElasticSearch audit hits. Currently this code is just for exercising the code.
    }

    @Test
    public void testHeaderWithLogChange() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "9999");
        AuditLogInputBean logBean = new AuditLogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setAuditLog(logBean);
        AuditResultBean resultBean = auditService.createHeader(inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getAuditKey());
        assertEquals(1, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void testHeaderWithLogChangeTransactional() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "9999");
        AuditLogInputBean logBean = new AuditLogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setAuditLog(logBean);
        AuditResultBean resultBean = auditService.createHeader(inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getAuditKey());
        assertEquals(1, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fortressA = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        log.info(fortressA.toString());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new Date(), callerRef);
        String keyA = auditService.createHeader(inputBean).getAuditKey();
        AuditLogInputBean alb = new AuditLogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressA.getName(), docType, callerRef);
        alb = auditService.createLog(alb);
        assertNotNull(alb);
        assertEquals(keyA, alb.getAuditKey());

        SecurityContextHolder.getContext().setAuthentication(authB);
        regService.registerSystemUser(new RegistrationBean("TWEE", emailB, "bah"));
        IFortress fortressB = fortressService.registerFortress("auditTestB" + System.currentTimeMillis());
        inputBean = new AuditHeaderInputBean(fortressB.getName(), "wally", docType, new Date(), callerRef);
        String keyB = auditService.createHeader(inputBean).getAuditKey();
        alb = new AuditLogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressB.getName(), docType, callerRef);
        alb = auditService.createLog(alb);
        assertNotNull(alb);
        assertEquals("This caller should not see KeyA", keyB, alb.getAuditKey());

    }

}
