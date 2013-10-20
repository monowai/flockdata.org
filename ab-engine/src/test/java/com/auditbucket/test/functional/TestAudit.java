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

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.bean.*;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateMidnight;
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

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

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

    @Autowired
    private AuditManagerService auditManagerService;

    private Logger logger = LoggerFactory.getLogger(TestAudit.class);
    private String monowai = "Monowai";
    private String mike = "test@ab.com";
    private String mark = "mark@null.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    private Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "user1");
    private String what = "{\"house\": \"house";

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
    public void logChangeWithNullAuditKeyButCallerRefExists() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        assertNotNull(auditManagerService.createHeader(inputBean));

        AuditLogInputBean aib = new AuditLogInputBean("wally", new DateTime(), "{\"blah\":" + 1 + "}");
        aib.setCallerRef(fortressA.getName(), "TestAudit", "ABC123");
        AuditLogResultBean input = auditManagerService.createLog(aib);
        assertNotNull(input.getAuditKey());


    }

    @Test
    public void locatingByCallerRefWillThrowAuthorizationException() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String key = auditManagerService.createHeader(inputBean).getAuditKey();
        // Check we can't create the same header twice for a given client ref
        inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String keyB = auditManagerService.createHeader(inputBean).getAuditKey();
        assertEquals(key, keyB);

        Authentication authB = new UsernamePasswordAuthenticationToken("swagger", "user2");
        SecurityContextHolder.getContext().setAuthentication(authB);
        regService.registerSystemUser(new RegistrationBean("TestTow", "swagger", "bah"));
        Fortress fortressB = fortressService.registerFortress("auditTestB");
        auditManagerService.createHeader(new AuditHeaderInputBean(fortressB.getName(), "wally", "TestAudit", new DateTime(), "123ABC"));

        SecurityContextHolder.getContext().setAuthentication(authMike);

        assertNotNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "ABC123"));
        assertNotNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "abc123"));
        assertNull("Security - shouldn't be able to see this header", auditService.findByCallerRef(fortressA.getId(), "TestAudit", "123ABC"));
        // Test non external user can't do this
        SecurityContextHolder.getContext().setAuthentication(authB);
        try {
            assertNull(auditService.findByCallerRef(fortressA.getId(), "TestAudit", "ABC123"));
            fail("Security exception not thrown");

        } catch (SecurityException se) {

        }
        try {
            assertNull(auditService.getHeader(key));
            fail("Security exception not thrown");

        } catch (SecurityException se) {

        }

        try {
            assertNull("Should have returned an error message", auditService.getHeader("Illegal Key"));
            fail("Illegal Argument Excepiton not thrown");
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void createHeaderTimeLogs() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String ahKey = auditManagerService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditService.findByCallerRef(fo.getId(), "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            auditManagerService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
        watch.stop();
        logger.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);

        // Test that we get the expected number of log events
        assertEquals(max, (double) auditService.getAuditLogCount(ahKey));
    }

    /**
     * Idempotent "what" data
     * Ensure duplicate logs are not created when content data has not changed
     */
    @Test
    public void noDuplicateLogsWithCompression() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "9999");
        String ahKey = auditManagerService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
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
            auditManagerService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), json));
            i++;
        }
        assertEquals(1d, (double) auditService.getAuditLogCount(ahKey));
        Set<AuditLog> logs = auditService.getAuditLogs(ahKey);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
        logs.iterator().next().toString();
    }

    /**
     * Ensures that the event type gets set to the correct default for create and update.
     */
    @Test
    public void defaultEventTypesAreHandled() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        AuditResultBean resultBean = auditManagerService.createHeader(inputBean);
        String ahKey = resultBean.getAuditKey();
        assertNotNull(ahKey);

        AuditHeader header = auditService.getHeader(ahKey);
        assertNotNull(header.getDocumentType());

        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        auditManagerService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        AuditLog when = auditService.getLastAuditLog(ahKey);
        assertNotNull(when);
        assertEquals(AuditChange.CREATE, when.getAuditChange().getEvent().getName()); // log event default
        assertEquals(AuditChange.CREATE.toLowerCase(), when.getAuditChange().getEvent().getName().toLowerCase()); // log event default

        auditManagerService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        AuditLog whenB = auditService.getLastAuditLog(ahKey);
        assertNotNull(whenB);

        assertFalse(whenB.equals(when));
        assertNotNull(whenB.getAuditChange().getEvent());
        assertEquals(AuditChange.UPDATE, whenB.getAuditChange().getEvent().getName());  // log event default
    }

    @Test
    public void correctLogCountsReturnedForAFortress() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        AuditResultBean resultBean = auditManagerService.createHeader(inputBean);
        String ahKey = resultBean.getAuditKey();

        assertNotNull(ahKey);
        assertNotNull(auditService.getHeader(ahKey));

        auditManagerService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        auditManagerService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        assertEquals(2, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void testHeaderWithLogChange() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "9999");
        AuditLogInputBean logBean = new AuditLogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setAuditLog(logBean);
        AuditResultBean resultBean = auditManagerService.createHeader(inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getAuditKey());
        assertEquals(1, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void testHeaderWithLogChangeTransactional() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "9999");
        AuditLogInputBean logBean = new AuditLogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setAuditLog(logBean);
        AuditResultBean resultBean = auditManagerService.createHeader(inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getAuditKey());
        assertEquals(1, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = auditManagerService.createHeader(inputBean).getAuditKey();
        AuditLogInputBean alb = new AuditLogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressA.getName(), docType, callerRef);
        AuditLogResultBean arb = auditManagerService.createLog(alb);
        assertNotNull(arb);
        assertEquals(keyA, arb.getAuditKey());

        SecurityContextHolder.getContext().setAuthentication(authMark);
        regService.registerSystemUser(new RegistrationBean("TWEE", mark, "bah"));
        Fortress fortressB = fortressService.registerFortress("auditTestB" + System.currentTimeMillis());
        inputBean = new AuditHeaderInputBean(fortressB.getName(), "wally", docType, new DateTime(), callerRef);
        String keyB = auditManagerService.createHeader(inputBean).getAuditKey();
        alb = new AuditLogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressB.getName(), docType, callerRef);
        arb = auditManagerService.createLog(alb);
        assertNotNull(arb);
        assertEquals("This caller should not see KeyA", keyB, arb.getAuditKey());

    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        regService.registerSystemUser(new RegistrationBean("Company With Space", mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("audit Test" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = auditManagerService.createHeader(inputBean).getAuditKey();
        assertNotNull(keyA);
    }

    @Test
    public void headersForDifferentCompaniesAreNotVisible() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        String hummingbird = "Hummingbird";
        regService.registerSystemUser(new RegistrationBean(hummingbird, mark, "bah"));
        //Monowai/Mike
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "AHWP");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();
        assertNotNull(ahWP);
        assertNotNull(auditService.getHeader(ahWP));

        //Hummingbird/Gina
        SecurityContextHolder.getContext().setAuthentication(authMark);
        Fortress fortHS = fortressService.registerFortress(new FortressInputBean("honeysuckle", true));
        inputBean = new AuditHeaderInputBean(fortHS.getName(), "harry", "CompanyNode", new DateTime(), "AHHS");
        String ahHS = auditManagerService.createHeader(inputBean).getAuditKey();

        assertNotNull(fortressService.getFortressUser(fortWP, "wally", true));
        assertNotNull(fortressService.getFortressUser(fortHS, "harry", true));
        assertNull(fortressService.getFortressUser(fortWP, "wallyz", false));

        double max = 2000d;
        StopWatch watch = new StopWatch();
        watch.start();

        createLogRecords(authMike, ahWP, what, 20);
        createLogRecords(authMark, ahHS, what, 40);
        watch.stop();
        logger.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);


    }

    @Test
    public void lastChangedWorks() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);

        Fortress fortWP = fortressService.registerFortress("wportfolio");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();
        AuditHeader auditKey = auditService.getHeader(ahWP);
        auditManagerService.createLog(new AuditLogInputBean(auditKey.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "1\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);
        FortressUser fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

        auditManagerService.createLog(new AuditLogInputBean(auditKey.getAuditKey(), "harry@sunnybell.com", new DateTime(), what + "2\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);

        fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("harry@sunnybell.com", fu.getCode());

        auditManagerService.createLog(new AuditLogInputBean(auditKey.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "3\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);

        fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

    }

    @Test
    public void outOfSequenceLogsWorking() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);
        DateTime dt = new DateTime().toDateTime();
        DateTime earlyDate = dt.minusDays(2);

        Fortress fortWP = fortressService.registerFortress("wportfolio");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();
        AuditHeader auditHeader = auditService.getHeader(ahWP);

        // Create the future one first.
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "1\"}", "Update"));
        auditHeader = auditService.getHeader(ahWP);
        FortressUser fu = fortressService.getUser(auditHeader.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());
        AuditLog compareLog = auditService.getLastAuditLog(auditHeader);

        // Load a historic record. This should not become "last"
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "harry@sunnybell.com", earlyDate, what + "2\"}", "Update"));
        auditHeader = auditService.getHeader(ahWP);

        AuditLog lastLog = auditService.getLastAuditLog(auditHeader);
        assertNotNull(lastLog);
        assertEquals(compareLog.getId(), lastLog.getId());

        fu = fortressService.getUser(auditHeader.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode()); // The first one we created is the "last one"


    }

    /**
     * test that we find the correct number of changes between a range of dates for a given header
     */
    @Test
    public void logDateRangesWorking() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);

        int max = 10;
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(max);
        DateTime workingDate = firstDate.toDateTime();

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", firstDate, "123");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();
        AuditHeader auditHeader = auditService.getHeader(ahWP);
        int i = 0;
        while (i < max) {
            workingDate = workingDate.plusDays(1);
            assertEquals("Loop count " + i, AuditLogInputBean.LogStatus.OK, auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", workingDate, what + i + "\"}")).getStatus());
            i++;
        }

        Set<AuditLog> aLogs = auditService.getAuditLogs(auditHeader.getAuditKey());
        assertEquals(max, aLogs.size());

        AuditLog lastLog = auditService.getLastAuditLog(auditHeader.getAuditKey());
        AuditChange lastChange = lastLog.getAuditChange();
        assertNotNull(lastChange);
        assertEquals(workingDate.toDate(), new Date(lastLog.getFortressWhen()));
        auditHeader = auditService.getHeader(ahWP);
        assertEquals(max, auditService.getAuditLogCount(auditHeader.getAuditKey()));

        DateTime then = workingDate.minusDays(4);
        logger.info("Searching between " + then.toDate() + " and " + workingDate.toDate());
        Set<AuditLog> logs = auditService.getAuditLogs(auditHeader.getAuditKey(), then.toDate(), workingDate.toDate());
        assertEquals(5, logs.size());
        Long logId = logs.iterator().next().getId();
        AuditLogDetailBean change = auditService.getFullDetail(auditHeader.getAuditKey(), logId);
        assertNotNull(change);
        assertNotNull(change.getLog());
        assertNotNull(change.getWhat());
        assertEquals(logId, change.getLog().getId());


    }

    @Test
    public void cancelLastChangeBehaves() throws Exception {
        // For use in compensating transaction cases only
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();

        AuditHeader auditHeader = auditService.getHeader(ahWP);
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));
        Set<AuditLog> logs = auditService.getAuditLogs(auditHeader.getAuditKey());
        assertEquals(2, logs.size());
        auditHeader = auditService.getHeader(ahWP);
        compareUser(auditHeader, "isabella@sunnybell.com");
        auditHeader = auditService.cancelLastLogSync(auditHeader.getAuditKey());

        assertNotNull(auditHeader);
        compareUser(auditHeader, "olivia@sunnybell.com");
        auditHeader = auditService.cancelLastLogSync(auditHeader.getAuditKey());
        assertNotNull(auditHeader);
    }

    @Test
    public void lastChangeDatesReconcileWithFortressInput() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();

        AuditHeader auditHeader = auditService.getHeader(ahWP);
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + 1 + "\"}"));
        auditHeader = auditService.getHeader(ahWP); // Inflate the header on the server
        AuditLog lastLog = auditService.getLastLog(auditHeader.getAuditKey());
        assertNotNull(lastLog);
        assertNotNull(lastLog.getAuditChange().getWhat());
        AuditWhat whatResult = auditService.getWhat(lastLog.getAuditChange());
        assertTrue(whatResult.getWhatMap().containsKey("house"));
    }

    @Test
    public void dateCreatedAndLastUpdated() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime fortressDateCreated = DateTime.now();
        Thread.sleep(500);
        DateTime logTime = DateTime.now();
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", fortressDateCreated, "ABC1");
        AuditLogInputBean auditLogInputBean = new AuditLogInputBean(mike, logTime, "{\"abx\": 1 }");
        inputBean.setAuditLog(auditLogInputBean);
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();
        AuditLog log = auditService.getLastAuditLog(ahWP);
        assertEquals("Fortress modification date&time do not match", log.getFortressWhen().longValue(), logTime.getMillis());
        AuditHeader header = auditService.getHeader(ahWP);
        assertEquals(fortressDateCreated, header.getFortressDateCreated());
        assertEquals("Fortress log time doesn't match", logTime.getMillis(), log.getFortressWhen().longValue());

    }

    @Test
    public void missingLogDateGeneratesSystemDate() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        //dt = dt.minusDays(2);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", dt, "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();
        AuditHeader auditHeader = auditService.getHeader(ahWP);

        // Check that TimeZone information is used to correctly establish Now when not passed in a log
        // No Date, so default to NOW in the Fortress Timezone
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", null, what + 1 + "\"}"));
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", null, what + 2 + "\"}"));
        Set<AuditLog> logs = auditService.getAuditLogs(auditHeader.getId());
        assertEquals("Logs with missing dates not correctly recorded", 2, logs.size());

        // Same date should still log
        DateMidnight dateMidnight = new DateTime().toDateMidnight();
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", dateMidnight.toDateTime(), what + 3 + "\"}"));
        AuditLog thirdLog = auditService.getLastLog(ahWP);
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", dateMidnight.toDateTime(), what + 4 + "\"}"));
        logs = auditService.getAuditLogs(auditHeader.getId());
        assertEquals(4, logs.size());
        AuditLog lastLog = auditService.getLastLog(ahWP);
        assertNotSame("Last log in should be the last", lastLog.getAuditChange().getId(), thirdLog.getAuditChange().getId());
    }

    @Test
    public void auditSummaryWorking() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();

        AuditHeader auditHeader = auditService.getHeader(ahWP);
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        auditManagerService.createLog(new AuditLogInputBean(auditHeader.getAuditKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));

        AuditSummaryBean auditSummary = auditService.getAuditSummary(ahWP);
        assertNotNull(auditSummary);
        assertEquals(ahWP, auditSummary.getHeader().getAuditKey());
        assertNotNull(auditSummary.getHeader().getLastUser());
        assertNotNull(auditSummary.getHeader().getCreatedBy());
        assertNotNull(auditSummary.getHeader().getFortress());
        assertEquals(2, auditSummary.getChanges().size());
        for (AuditLog log : auditSummary.getChanges()) {
            AuditChange change = log.getAuditChange();
            assertNotNull(change.getEvent());
            assertNotNull(change.getWho().getCode());
            AuditWhat whatResult = auditService.getWhat(change);
            assertTrue(whatResult.getWhatMap().containsKey("house"));
        }
    }

    @Test
    public void fullHeaderDetailsByCallerRef() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean).getAuditKey();

        AuditHeader header = auditService.findByCallerRefFull(fortWP.getId(), "CompanyNode", "ABC1");
        assertNotNull(header);
        assertNotNull(header.getDocumentType());
        assertEquals(ahWP, header.getAuditKey());
    }

    @Test
    public void testFortressTimeBoundaries() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);

        FortressInputBean usFortress = new FortressInputBean("usFortress", true);
        usFortress.setTimeZone(TimeZone.getTimeZone("GMT").getID());
        Fortress fortressGMT = fortressService.registerFortress(usFortress);
        assertEquals(TimeZone.getTimeZone("GMT").getID(), fortressGMT.getTimeZone());

        FortressInputBean astFortress = new FortressInputBean("astFortress", true);
        astFortress.setTimeZone(TimeZone.getTimeZone("AST").getID());
        Fortress fortressAST = fortressService.registerFortress(astFortress);
        assertEquals(TimeZone.getTimeZone("AST").getID(), fortressAST.getTimeZone());

        DateTime dt = new DateTime();
        AuditHeaderInputBean astAuditBean = new AuditHeaderInputBean(fortressGMT.getName(), "olivia@ast.com", "CompanyNode", null, "ABC1");
        AuditHeaderInputBean gmtAuditBean = new AuditHeaderInputBean(fortressAST.getName(), "olivia@gmt.com", "CompanyNode", null, "ABC1");
        String result = auditManagerService.createHeader(astAuditBean).getAuditKey();
        AuditHeader header = auditService.getHeader(result);
        DateTime astTime = new DateTime(header.getFortressDateCreated());

        result = auditManagerService.createHeader(gmtAuditBean).getAuditKey();
        header = auditService.getHeader(result);
        DateTime gmtTime = new DateTime(header.getFortressDateCreated());

        assertNotSame(astTime.getHourOfDay(), gmtTime.getHourOfDay());

    }

    private void compareUser(AuditHeader header, String userName) {
        FortressUser fu = fortressService.getUser(header.getLastUser().getId());
        assertEquals(userName, fu.getCode());

    }

    private void createLogRecords(Authentication auth, String auditHeader, String textToUse, double recordsToCreate) throws Exception {
        int i = 0;
        SecurityContextHolder.getContext().setAuthentication(auth);
        while (i < recordsToCreate) {
            auditManagerService.createLog(new AuditLogInputBean(auditHeader, "wally", new DateTime(), textToUse + i + "\"}", (String) null));
            i++;
        }
        assertEquals(recordsToCreate, (double) auditService.getAuditLogCount(auditHeader));
    }


}
