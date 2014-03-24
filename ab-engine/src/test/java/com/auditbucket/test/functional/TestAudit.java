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

import com.auditbucket.audit.bean.*;
import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.engine.endpoint.AuditEP;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.FortressService;
import junit.framework.Assert;
import org.apache.commons.lang.time.StopWatch;
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
import java.util.TimeZone;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
    AuditEP auditEP;

    @Autowired
    RegistrationEP regService;
    //RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    FortressEP fortressEP;

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
    public void testApiKeysWorkInPrecedence() throws Exception {
        // No Security Access necessary.
        SecurityContextHolder.getContext().setAuthentication(null);
        SystemUser sysUser = regService.register(new RegistrationBean(monowai, mike, "bah")).getBody();
        assertNotNull(sysUser);
        String apiKey = sysUser.getCompany().getApiKey();

        Assert.assertNotNull(apiKey);
        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("testApiKeysWorkInPrecedence"), sysUser.getCompany().getApiKey()).getBody();
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");

        // Fails due to NoAuth or key
        AuditResultBean result ;
        try {
            result= auditEP.createHeader(inputBean, null, null).getBody();
            assertNull(result);
            fail("Security Exception did not occur");
        } catch (SecurityException e) {
            // Good
        }

        // Should work now
        SecurityContextHolder.getContext().setAuthentication(authMike);//

        result = auditEP.createHeader(inputBean, null, null).getBody(); // Works due to basic authz
        assertNotNull(result);  // works coz basic authz

        final AuditHeader header = auditEP.getAudit(result.getAuditKey(), apiKey, apiKey).getBody();
        assertNotNull (header);
        SecurityContextHolder.getContext().setAuthentication(authMark);// Wrong user, but valid API key
        assertNotNull(auditEP.createHeader(inputBean, apiKey, null));// works
        assertNotNull(auditEP.createHeader(inputBean, null, apiKey));// works
        assertNotNull(auditEP.createHeader(inputBean, "invalidApiKey", apiKey));// Header overrides request
        try {
            assertNull(auditEP.createHeader(inputBean, apiKey, "123")); // Illegal result
            Assert.fail("this should not have worked due to invalid api key");
        } catch (AuditException e) {
            // this should happen due to invalid api key
        }
        SecurityContextHolder.getContext().setAuthentication(null);// No user context, but valid API key
        assertNotNull(auditEP.createHeader(inputBean, apiKey, null));// works
        assertNotNull(auditEP.createHeader(inputBean, null, apiKey));// works
        assertNotNull(auditEP.createHeader(inputBean, "invalidApiKey", apiKey));// Header overrides request
        try {
            assertNull(auditEP.createHeader(inputBean, apiKey, "123")); // Illegal result
            Assert.fail("this should not have worked due to invalid api key");
        } catch (AuditException e) {
            // this should happen due to invalid api key
        }

    }

    @Test
    public void logChangeWithNullAuditKeyButCallerRefExists() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortress = fortressService.registerFortress("auditTest");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortress.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        assertNotNull(auditEP.createHeader(inputBean, null, null));

        AuditLogInputBean aib = new AuditLogInputBean("wally", new DateTime(), "{\"blah\":" + 1 + "}");
        aib.setCallerRef(fortress.getName(), "TestAudit", "ABC123");
        AuditLogResultBean input = auditManagerService.processLog(aib);
        assertNotNull(input.getAuditKey());
        Assert.assertNotNull(auditService.findByCallerRef(fortress, aib.getDocumentType(), aib.getCallerRef()));



    }

    @Test
    public void locatingByCallerRefWillThrowAuthorizationException() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String key = auditManagerService.createHeader(inputBean, null).getAuditKey();
        // Check we can't create the same header twice for a given client ref
        inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String keyB = auditManagerService.createHeader(inputBean, null).getAuditKey();
        assertEquals(key, keyB);

        Authentication authB = new UsernamePasswordAuthenticationToken("swagger", "user2");
        SecurityContextHolder.getContext().setAuthentication(authB);
        regService.register(new RegistrationBean("TestTow", "swagger", "bah"));
        Fortress fortressB = fortressService.registerFortress("auditTestB");
        auditManagerService.createHeader(new AuditHeaderInputBean(fortressB.getName(), "wally", "TestAudit", new DateTime(), "123ABC"), null);

        SecurityContextHolder.getContext().setAuthentication(authMike);

        assertNotNull(auditService.findByCallerRef(fortressA, "TestAudit", "ABC123"));
        assertNotNull(auditService.findByCallerRef(fortressA, "TestAudit", "abc123"));
        assertNull("Security - shouldn't be able to see this header", auditService.findByCallerRef(fortressA, "TestAudit", "123ABC"));
        // Test non external user can't do this
        SecurityContextHolder.getContext().setAuthentication(authB);
        assertNull(auditService.findByCallerRef(fortressA.getCode(), "TestAudit", "ABC123"));

        try {
            assertNull(auditService.getHeader(key));
            fail("Security exception not thrown");

        } catch (SecurityException se) {

        }


    }

    @Test
    public void createHeaderTimeLogs() throws Exception {

        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortress.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String ahKey = auditManagerService.createHeader(inputBean, null).getAuditKey();

        assertNotNull(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditService.findByCallerRef(fortress, "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            auditManagerService.processLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
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

        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "ndlwcqw2");
        String ahKey = auditManagerService.createHeader(inputBean, null).getAuditKey();

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
            auditManagerService.processLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), json));
            i++;
        }
        assertEquals(1d, (double) auditService.getAuditLogCount(ahKey));
        Set<AuditLog> logs = auditService.getAuditLogs(ahKey);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
        logs.iterator().next().toString();
    }

    @Test
    public void correctLogCountsReturnedForAFortress() throws Exception {

        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        AuditResultBean resultBean = auditManagerService.createHeader(inputBean, null);
        String ahKey = resultBean.getAuditKey();

        assertNotNull(ahKey);
        assertNotNull(auditService.getHeader(ahKey));

        auditManagerService.processLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        auditManagerService.processLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        assertEquals(2, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void testHeaderWithLogChange() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "uouu87");
        AuditLogInputBean logBean = new AuditLogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setAuditLog(logBean);
        AuditResultBean resultBean = auditManagerService.createHeader(inputBean, null);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getAuditKey());
        assertEquals(1, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void testHeaderWithLogChangeTransactional() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "232146");
        AuditLogInputBean logBean = new AuditLogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setAuditLog(logBean);
        AuditResultBean resultBean = auditManagerService.createHeader(inputBean, null);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getAuditKey());
        assertEquals(1, auditService.getAuditLogCount(resultBean.getAuditKey()));
    }

    @Test
    public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = auditManagerService.createHeader(inputBean, null).getAuditKey();
        AuditLogInputBean alb = new AuditLogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressA.getName(), docType, callerRef);
        AuditLogResultBean arb = auditManagerService.processLog(alb);
        assertNotNull(arb);
        assertEquals(keyA, arb.getAuditKey());

        SecurityContextHolder.getContext().setAuthentication(authMark);
        regService.register(new RegistrationBean("TWEE", mark, "bah"));
        Fortress fortressB = fortressService.registerFortress("auditTestB" + System.currentTimeMillis());
        inputBean = new AuditHeaderInputBean(fortressB.getName(), "wally", docType, new DateTime(), callerRef);
        String keyB = auditManagerService.createHeader(inputBean, null).getAuditKey();
        alb = new AuditLogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressB.getName(), docType, callerRef);
        arb = auditManagerService.processLog(alb);
        assertNotNull(arb);
        assertEquals("This caller should not see KeyA", keyB, arb.getAuditKey());

    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        regService.register(new RegistrationBean("Company With Space", mike, "bah"));
        Fortress fortressA = fortressService.registerFortress("audit Test" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = auditManagerService.createHeader(inputBean, null).getAuditKey();
        assertNotNull(keyA);
    }

    @Test
    public void headersForDifferentCompaniesAreNotVisible() throws Exception {

        regService.register(new RegistrationBean(monowai, mike, "bah"));
        String hummingbird = "Hummingbird";
        regService.register(new RegistrationBean(hummingbird, mark, "bah"));
        //Monowai/Mike
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "AHWP");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();
        assertNotNull(ahWP);
        assertNotNull(auditService.getHeader(ahWP));

        //Hummingbird/Gina
        SecurityContextHolder.getContext().setAuthentication(authMark);
        Fortress fortHS = fortressService.registerFortress(new FortressInputBean("honeysuckle", true));
        inputBean = new AuditHeaderInputBean(fortHS.getName(), "harry", "CompanyNode", new DateTime(), "AHHS");
        String ahHS = auditManagerService.createHeader(inputBean, null).getAuditKey();

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
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);

        Fortress fortWP = fortressService.registerFortress("wportfolio");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();
        AuditHeader auditKey = auditService.getHeader(ahWP);
        auditManagerService.processLog(new AuditLogInputBean(auditKey.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "1\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);
        FortressUser fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

        auditManagerService.processLog(new AuditLogInputBean(auditKey.getAuditKey(), "harry@sunnybell.com", new DateTime(), what + "2\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);

        fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("harry@sunnybell.com", fu.getCode());

        auditManagerService.processLog(new AuditLogInputBean(auditKey.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "3\"}", "Update"));
        auditKey = auditService.getHeader(ahWP);

        fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

    }

    @Test
    public void outOfSequenceLogsWorking() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);
        DateTime dt = new DateTime().toDateTime();
        DateTime earlyDate = dt.minusDays(2);

        Fortress fortWP = fortressService.registerFortress("wportfolio");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();
        com.auditbucket.audit.model.AuditHeader auditHeader = auditService.getHeader(ahWP);

        // Create the future one first.
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + "1\"}", "Update"));
        auditHeader = auditService.getHeader(ahWP);
        FortressUser fu = fortressService.getUser(auditHeader.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());
        AuditLog compareLog = auditService.getLastAuditLog(auditHeader);

        // Load a historic record. This should not become "last"
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "harry@sunnybell.com", earlyDate, what + "2\"}", "Update"));
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
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        // Create a second log record in order to workout who last change the AuditHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);

        int max = 10;
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(max);
        DateTime workingDate = firstDate.toDateTime();

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "wally", "CompanyNode", firstDate, "123");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();
        com.auditbucket.audit.model.AuditHeader auditHeader = auditService.getHeader(ahWP);
        int i = 0;
        while (i < max) {
            workingDate = workingDate.plusDays(1);
            assertEquals("Loop count " + i, AuditLogInputBean.LogStatus.OK, auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", workingDate, what + i + "\"}")).getStatus());
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
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();

        com.auditbucket.audit.model.AuditHeader auditHeader = auditService.getHeader(ahWP);
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));
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
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();

        com.auditbucket.audit.model.AuditHeader auditHeader = auditService.getHeader(ahWP);
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", new DateTime(), what + 1 + "\"}"));
        auditHeader = auditService.getHeader(ahWP); // Inflate the header on the server
        AuditLog lastLog = auditService.getLastLog(auditHeader.getAuditKey());
        assertNotNull(lastLog);
//        assertNotNull(lastLog.getAuditChange().getWhat());
        AuditWhat whatResult = auditService.getWhat(auditHeader, lastLog.getAuditChange());
        assertNotNull(whatResult);
        assertTrue(whatResult.getWhatMap().containsKey("house"));
    }

    @Test
    public void dateCreatedAndLastUpdated() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime fortressDateCreated = DateTime.now();
        Thread.sleep(500);
        DateTime logTime;
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", fortressDateCreated, "dcalbu");
        AuditLogInputBean auditLogInputBean = new AuditLogInputBean(mike, fortressDateCreated, "{\"abx\": 1 }");
        // Time will come from the Log
        inputBean.setAuditLog(auditLogInputBean);
        AuditResultBean auditResultBean = auditManagerService.createHeader(inputBean, null);
        String ahWP = auditResultBean.getAuditKey();

        assertEquals(fortressDateCreated.getMillis(), auditResultBean.getAuditHeader().getFortressDateCreated().getMillis());

        // Creating the 2nd log will advance the last modified time
        logTime = DateTime.now();
        auditLogInputBean = new AuditLogInputBean(ahWP, mike, logTime, "{\"abx\": 2 }");
        auditManagerService.processLog(auditLogInputBean);

        AuditLog log = auditService.getLastAuditLog(ahWP);
        assertEquals("Fortress modification date&time do not match", log.getFortressWhen().longValue(), logTime.getMillis());
        AuditHeader header = auditService.getHeader(ahWP);
        assertEquals(fortressDateCreated.getMillis(), header.getFortressDateCreated().getMillis());
        assertEquals("Fortress log time doesn't match", logTime.getMillis(), log.getFortressWhen().longValue());

    }

    @Test
    public void missingLogDateGeneratesSystemDate() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", dt, "mldgsd99");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();
        com.auditbucket.audit.model.AuditHeader auditHeader = auditService.getHeader(ahWP);

        // Check that TimeZone information is used to correctly establish Now when not passed in a log
        // No Date, so default to NOW in the Fortress Timezone
        AuditLogResultBean log = auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", null, what + 1 + "\"}"));
        logger.info("1 " + new Date(log.getSysWhen()).toString());

        log = auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", null, what + 2 + "\"}"));
        logger.info("2 " + new Date(log.getSysWhen()).toString());

        Set<AuditLog> logs = auditService.getAuditLogs(auditHeader.getId());
        assertEquals("Logs with missing dates not correctly recorded", 2, logs.size());

        // Same date should still log
        DateTime dateMidnight = new DateTime();
        log = auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", dateMidnight.toDateTime(), what + 3 + "\"}"));
        logger.info("3 " + new Date(log.getSysWhen()).toString());
        AuditLog thirdLog = auditService.getLastLog(ahWP);
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", dateMidnight.toDateTime(), what + 4 + "\"}"));
        logger.info("4 " + new Date(log.getSysWhen()).toString());
        logs = auditService.getAuditLogs(auditHeader.getId());
        assertEquals(4, logs.size());
        for (AuditLog next : logs) {
            logger.info(next.getId() + " - " + new Date(next.getSysWhen()).toString());
        }
        AuditLog lastLog = auditService.getLastLog(ahWP);
        logger.info("L " + new Date(lastLog.getSysWhen()).toString());
        assertNotSame("Last log in should be the last", lastLog.getAuditChange().getId(), thirdLog.getAuditChange().getId());
    }

    @Test
    public void auditSummaryWorking() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();

        com.auditbucket.audit.model.AuditHeader auditHeader = auditService.getHeader(ahWP);
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        auditManagerService.processLog(new AuditLogInputBean(auditHeader.getAuditKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));

        AuditSummaryBean auditSummary = auditService.getAuditSummary(ahWP, null);
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
            AuditWhat whatResult = auditService.getWhat(auditHeader, change);
            assertTrue(whatResult.getWhatMap().containsKey("house"));
        }
    }

    @Test
    public void fullHeaderDetailsByCallerRef() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "ABC1");
        String ahWP = auditManagerService.createHeader(inputBean, null).getAuditKey();

        com.auditbucket.audit.model.AuditHeader header = auditService.findByCallerRefFull(fortWP, "CompanyNode", "ABC1");
        assertNotNull(header);
        assertNotNull(header.getDocumentType());
        assertEquals(ahWP, header.getAuditKey());
    }

    @Test
    public void testFortressTimeBoundaries() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);

        FortressInputBean usFortress = new FortressInputBean("usFortress", true);
        usFortress.setTimeZone(TimeZone.getTimeZone("GMT").getID());
        Fortress fortressGMT = fortressService.registerFortress(usFortress);
        assertEquals(TimeZone.getTimeZone("GMT").getID(), fortressGMT.getTimeZone());

        FortressInputBean astFortress = new FortressInputBean("astFortress", true);
        astFortress.setTimeZone(TimeZone.getTimeZone("AST").getID());
        Fortress fortressAST = fortressService.registerFortress(astFortress);
        assertEquals(TimeZone.getTimeZone("AST").getID(), fortressAST.getTimeZone());

        AuditHeaderInputBean astAuditBean = new AuditHeaderInputBean(fortressGMT.getName(), "olivia@ast.com", "CompanyNode", null, "ABC1");
        AuditHeaderInputBean gmtAuditBean = new AuditHeaderInputBean(fortressAST.getName(), "olivia@gmt.com", "CompanyNode", null, "ABC1");
        String result = auditManagerService.createHeader(astAuditBean, null).getAuditKey();
        com.auditbucket.audit.model.AuditHeader header = auditService.getHeader(result);
        DateTime astTime = new DateTime(header.getFortressDateCreated());

        result = auditManagerService.createHeader(gmtAuditBean, null).getAuditKey();
        header = auditService.getHeader(result);
        DateTime gmtTime = new DateTime(header.getFortressDateCreated());

        assertNotSame(astTime.getHourOfDay(), gmtTime.getHourOfDay());

    }

    @Test
    public void headersByFortressAndDocType() throws Exception {
        regService.register(new RegistrationBean(monowai, mike, "bah"));

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        String typeA = "TypeA";
        String typeB = "Type B";

        auditManagerService.createHeader(new AuditHeaderInputBean("ABC", "auditTest", typeA, new DateTime(), "abc"), null);
        auditManagerService.createHeader(new AuditHeaderInputBean("ABC", "auditTest", typeA, new DateTime(), "abd"), null);
        auditManagerService.createHeader(new AuditHeaderInputBean("ABC", "auditTest", typeB, new DateTime(), "abc"), null);

        assertEquals(3, auditService.getAuditHeaders(fortress, 0l).size());
        assertEquals(2, auditService.getAuditHeaders(fortress, typeA, 0l).size());
        assertEquals("Case sensitivity failed", 2, auditService.getAuditHeaders(fortress, "typea", 0l).size());
        assertEquals(1, auditService.getAuditHeaders(fortress, typeB, 0l).size());
        assertEquals("Case sensitivity failed", 1, auditService.getAuditHeaders(fortress, "type b", 0l).size());


    }

    private void compareUser(com.auditbucket.audit.model.AuditHeader header, String userName) {
        FortressUser fu = fortressService.getUser(header.getLastUser().getId());
        assertEquals(userName, fu.getCode());

    }

    private void createLogRecords(Authentication auth, String auditHeader, String textToUse, double recordsToCreate) throws Exception {
        int i = 0;
        SecurityContextHolder.getContext().setAuthentication(auth);
        while (i < recordsToCreate) {
            auditManagerService.processLog(new AuditLogInputBean(auditHeader, "wally", new DateTime(), textToUse + i + "\"}", (String) null));
            i++;
        }
        assertEquals(recordsToCreate, (double) auditService.getAuditLogCount(auditHeader));
    }


}
