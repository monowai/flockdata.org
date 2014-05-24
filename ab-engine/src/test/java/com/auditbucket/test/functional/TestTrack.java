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

import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
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

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestTrack {
    @Autowired
    TrackService trackService;

    @Autowired
    TrackEP trackEP;

    @Autowired
    RegistrationEP regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    FortressEP fortressEP;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private MediationFacade mediationFacade;

    private Logger logger = LoggerFactory.getLogger(TestTrack.class);
    private String monowai = "Monowai";
    private String mike = "mike";
    private String mark = "mark";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "123");
    private Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "123");
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
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressService.registerFortress("auditTest");
        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        assertNotNull(trackEP.trackHeader(inputBean, null, null));

        LogInputBean aib = new LogInputBean("wally", new DateTime(), "{\"blah\":" + 1 + "}");
        aib.setCallerRef(fortress.getName(), "TestTrack", "ABC123");
        LogResultBean input = mediationFacade.processLog(aib);
        assertNotNull(input.getMetaKey());
        Assert.assertNotNull(trackService.findByCallerRef(fortress, aib.getDocumentType(), aib.getCallerRef()));
    }

    @Test
    public void nullMetaKey() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        assertNull (trackService.getHeader(null));

    }

    @Test
    public void metaHeaderDifferentLogsBulkEndpoint() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUserResultBean su = regService.registerSystemUser(new RegistrationBean(monowai, "mike")).getBody();
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest",true), su.getApiKey(), null).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        LogInputBean logInputBean = new LogInputBean("mike", new DateTime(), "{\"col\": 123}");
        inputBean.setLog(logInputBean);
        List<MetaInputBean>inputBeans = new ArrayList<>();
        inputBeans.add(inputBean);
        trackEP.trackHeadersAsync(inputBeans, false, su.getApiKey());
        Thread.yield();
        Thread.sleep(900);

        MetaHeader created = trackEP.getByCallerRef(fortress.getName(), "TestTrack", "ABC123", su.getApiKey(), su.getApiKey() ).getBody();
        Thread.sleep(600);
        assertNotNull (created);
        // Now we record a change
        logInputBean = new LogInputBean("mike", new DateTime(), "{\"col\": 321}");
        inputBean.setLog(logInputBean);
        inputBeans = new ArrayList<>();
        inputBeans.add(inputBean);
        trackEP.trackHeadersAsync(inputBeans, false, su.getApiKey());
        Thread.sleep (600);

        LogWhat what = trackEP.getLastChangeWhat(created.getMetaKey(), su.getApiKey(), su.getApiKey()).getBody();
        assertNotNull ( what);
        Object value = what.getWhat().get("col");
        Assert.assertNotNull(value);
        assertEquals("321", value.toString());
    }


    @Test
    public void locatingByCallerRefWillThrowAuthorizationException() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortressA = fortressService.registerFortress("auditTest");
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String key = mediationFacade.createHeader(inputBean, null).getMetaKey();
        // Check we can't create the same header twice for a given client ref
        inputBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String keyB = mediationFacade.createHeader(inputBean, null).getMetaKey();
        assertEquals(key, keyB);

        Authentication authB = new UsernamePasswordAuthenticationToken("sally", "123");
        SecurityContextHolder.getContext().setAuthentication(authB);
        regService.registerSystemUser(new RegistrationBean("TestTow", "sally"));
        Fortress fortressB = fortressService.registerFortress("auditTestB");
        mediationFacade.createHeader(new MetaInputBean(fortressB.getName(), "wally", "TestTrack", new DateTime(), "123ABC"), null);

        SecurityContextHolder.getContext().setAuthentication(authMike);

        assertNotNull(trackService.findByCallerRef(fortressA, "TestTrack", "ABC123"));
        assertNull( "Caller refs are case sensitive so this should not be found", trackService.findByCallerRef(fortressA, "TestTrack", "abc123"));
        assertNull("Security - shouldn't be able to see this header", trackService.findByCallerRef(fortressA, "TestTrack", "123ABC"));
        // Test non external user can't do this
        SecurityContextHolder.getContext().setAuthentication(authB);
        assertNull(trackService.findByCallerRef(fortressA.getCode(), "TestTrack", "ABC123"));

        try {
            assertNull(trackService.getHeader(key));
            fail("Security exception not thrown");

        } catch (SecurityException se) {

        }


    }

    @Test
    public void createHeaderTimeLogs() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String ahKey = mediationFacade.createHeader(inputBean, null).getMetaKey();

        assertNotNull(ahKey);

        assertNotNull(trackService.getHeader(ahKey));
        assertNotNull(trackService.findByCallerRef(fortress, "TestTrack", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
        watch.stop();
        logger.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);

        // Test that we get the expected number of log events
        assertEquals(max, (double) trackService.getLogCount(ahKey));
    }

    /**
     * Idempotent "what" data
     * Ensure duplicate logs are not created when content data has not changed
     */
    @Test
    public void noDuplicateLogsWithCompression() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressService.registerFortress("auditTest");

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "testDupe", new DateTime(), "ndlwcqw2");
        String metaKey = mediationFacade.createHeader(inputBean, null).getMetaKey();

        assertNotNull(metaKey);
        // Irrespective of the order of the fields, we see it as the same.
        String jsonA = "{\"name\": \"8888\", \"thing\": {\"m\": \"happy\"}}";
        String jsonB = "{\"thing\": {\"m\": \"happy\"},\"name\": \"8888\"}";


        assertNotNull(trackService.getHeader(metaKey));
        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));
        int i = 0;
        double max = 10d;
        String json;
        while (i < max) {
            // Same "what" text so should only be one auditLogCount record
            json = (i % 2 == 0 ? jsonA : jsonB);
            mediationFacade.processLog(new LogInputBean(metaKey, "wally", new DateTime(), json));
            i++;
        }
        assertEquals(1d, (double) trackService.getLogCount(metaKey));
        Set<TrackLog> logs = trackService.getLogs(fortress.getCompany(), metaKey);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
        logs.iterator().next().toString();
    }

    @Test
    public void correctLogCountsReturnedForAFortress() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 0}"));
        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\": 1}"));
        assertEquals(2, trackService.getLogCount(resultBean.getMetaKey()));
    }

    @Test
    public void testHeaderWithLogChange() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fo = fortressService.registerFortress("auditTest");

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "uouu87");
        LogInputBean logBean = new LogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setLog(logBean);
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getMetaKey());
        assertEquals(1, trackService.getLogCount(resultBean.getMetaKey()));
    }

    @Test
    public void testHeaderWithLogChangeTransactional() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fo = fortressService.registerFortress("auditTest");

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "232146");
        LogInputBean logBean = new LogInputBean(null, "wally", DateTime.now(), "{\"blah\":0}");
        inputBean.setLog(logBean);
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getMetaKey());
        assertEquals(1, trackService.getLogCount(resultBean.getMetaKey()));
    }

    @Test
    public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortressA = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = mediationFacade.createHeader(inputBean, null).getMetaKey();
        LogInputBean alb = new LogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressA.getName(), docType, callerRef);
        //assertNotNull (alb);
        LogResultBean arb = mediationFacade.processLog(alb);
        assertNotNull(arb);
        assertEquals(keyA, arb.getMetaKey());

        SecurityContextHolder.getContext().setAuthentication(authMark);
        regService.registerSystemUser(new RegistrationBean("TWEE", mark));
        Fortress fortressB = fortressService.registerFortress("auditTestB" + System.currentTimeMillis());
        inputBean = new MetaInputBean(fortressB.getName(), "wally", docType, new DateTime(), callerRef);
        String keyB = mediationFacade.createHeader(inputBean, null).getMetaKey();
        alb = new LogInputBean("logTest", new DateTime(), "{\"blah\":" + 0 + "}");
        alb.setCallerRef(fortressB.getName(), docType, callerRef);
        arb = mediationFacade.processLog(alb);
        assertNotNull(arb);
        assertEquals("This caller should not see KeyA", keyB, arb.getMetaKey());

    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        regService.registerSystemUser(new RegistrationBean("Company With Space", mike));
        Fortress fortressA = fortressService.registerFortress("track Test" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = mediationFacade.createHeader(inputBean, null).getMetaKey();
        assertNotNull(keyA);
    }

    @Test
    public void headersForDifferentCompaniesAreNotVisible() throws Exception {

        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        String hummingbird = "Hummingbird";
        regService.registerSystemUser(new RegistrationBean(hummingbird, mark));
        //Monowai/Mike
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "AHWP");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();
        assertNotNull(ahWP);
        assertNotNull(trackService.getHeader(ahWP));

        //Hummingbird/Gina
        SecurityContextHolder.getContext().setAuthentication(authMark);
        Fortress fortHS = fortressService.registerFortress(new FortressInputBean("honeysuckle", true));
        inputBean = new MetaInputBean(fortHS.getName(), "harry", "CompanyNode", new DateTime(), "AHHS");
        String ahHS = mediationFacade.createHeader(inputBean, null).getMetaKey();

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
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        // Create a second log record in order to workout who last change the MetaHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);

        Fortress fortWP = fortressService.registerFortress("wportfolio");
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();
        MetaHeader auditKey = trackService.getHeader(ahWP);
        mediationFacade.processLog(new LogInputBean(auditKey.getMetaKey(), "olivia@sunnybell.com", new DateTime(), what + "1\"}", "Update"));
        auditKey = trackService.getHeader(ahWP);
        FortressUser fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

        mediationFacade.processLog(new LogInputBean(auditKey.getMetaKey(), "harry@sunnybell.com", new DateTime(), what + "2\"}", "Update"));
        auditKey = trackService.getHeader(ahWP);

        fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("harry@sunnybell.com", fu.getCode());

        mediationFacade.processLog(new LogInputBean(auditKey.getMetaKey(), "olivia@sunnybell.com", new DateTime(), what + "3\"}", "Update"));
        auditKey = trackService.getHeader(ahWP);

        fu = fortressService.getUser(auditKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

    }

    @Test
    public void outOfSequenceLogsWorking() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        // Create a second log record in order to workout who last change the MetaHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);
        DateTime dt = new DateTime().toDateTime();
        DateTime earlyDate = dt.minusDays(2);

        Fortress fortWP = fortressService.registerFortress("wportfolio");
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();
        MetaHeader metaHeader = trackService.getHeader(ahWP);

        // Create the future one first.
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", new DateTime(), what + "1\"}", "Update"));
        metaHeader = trackService.getHeader(ahWP);
        FortressUser fu = fortressService.getUser(metaHeader.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());
        TrackLog compareLog = trackService.getLastLog(metaHeader);

        // Load a historic record. This should not become "last"
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "harry@sunnybell.com", earlyDate, what + "2\"}", "Update"));
        metaHeader = trackService.getHeader(ahWP);

        TrackLog lastLog = trackService.getLastLog(metaHeader);
        assertNotNull(lastLog);
        assertEquals(compareLog.getId(), lastLog.getId());

        fu = fortressService.getUser(metaHeader.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode()); // The first one we created is the "last one"


    }

    /**
     * test that we find the correct number of changes between a range of dates for a given header
     */
    @Test
    public void logDateRangesWorking() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        // Create a second log record in order to workout who last change the MetaHeaderNode
        SecurityContextHolder.getContext().setAuthentication(authMike);

        int max = 10;
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(max);
        DateTime workingDate = firstDate.toDateTime();

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "CompanyNode", firstDate, "123");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();
        MetaHeader metaHeader = trackService.getHeader(ahWP);
        int i = 0;
        while (i < max) {
            workingDate = workingDate.plusDays(1);
            assertEquals("Loop count " + i, LogInputBean.LogStatus.OK, mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", workingDate, what + i + "\"}")).getStatus());
            i++;
        }

        Set<TrackLog> aLogs = trackService.getLogs(fortress.getCompany(), metaHeader.getMetaKey());
        assertEquals(max, aLogs.size());

        TrackLog lastLog = trackService.getLastLog(metaHeader.getMetaKey());
        Log lastChange = lastLog.getChange();
        assertNotNull(lastChange);
        assertEquals(workingDate.toDate(), new Date(lastLog.getFortressWhen()));
        metaHeader = trackService.getHeader(ahWP);
        assertEquals(max, trackService.getLogCount(metaHeader.getMetaKey()));

        DateTime then = workingDate.minusDays(4);
        logger.info("Searching between " + then.toDate() + " and " + workingDate.toDate());
        Set<TrackLog> logs = trackService.getLogs(metaHeader.getMetaKey(), then.toDate(), workingDate.toDate());
        assertEquals(5, logs.size());
        Long logId = logs.iterator().next().getId();
        LogDetailBean change = trackService.getFullDetail(metaHeader.getMetaKey(), logId);
        assertNotNull(change);
        assertNotNull(change.getLog());
        assertNotNull(change.getWhat());
        assertEquals(logId, change.getLog().getId());


    }

    @Test
    public void cancelLastChangeBehaves() throws Exception {
        // For use in compensating transaction cases only
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();

        MetaHeader metaHeader = trackService.getHeader(ahWP);
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));
        Set<TrackLog> logs = trackService.getLogs(fortress.getCompany(), metaHeader.getMetaKey());
        assertEquals(2, logs.size());
        metaHeader = trackService.getHeader(ahWP);
        compareUser(metaHeader, "isabella@sunnybell.com");
        metaHeader = trackService.cancelLastLogSync(metaHeader.getMetaKey());

        assertNotNull(metaHeader);
        compareUser(metaHeader, "olivia@sunnybell.com");
        metaHeader = trackService.cancelLastLogSync(metaHeader.getMetaKey());
        assertNotNull(metaHeader);
    }

    @Test
    public void lastChangeDatesReconcileWithFortressInput() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC1");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();

        MetaHeader metaHeader = trackService.getHeader(ahWP);
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", new DateTime(), what + 1 + "\"}"));
        metaHeader = trackService.getHeader(ahWP); // Inflate the header on the server
        TrackLog lastLog = trackService.getLastLog(metaHeader.getMetaKey());
        assertNotNull(lastLog);
//        assertNotNull(lastLog.getAuditChange().getWhat());
        LogWhat whatResult = trackService.getWhat(metaHeader, lastLog.getChange());
        assertNotNull(whatResult);
        assertTrue(whatResult.getWhat().containsKey("house"));
    }

    @Test
    public void dateCreatedAndLastUpdated() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime fortressDateCreated = DateTime.now();
        Thread.sleep(500);
        DateTime logTime;
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", fortressDateCreated, "dcalbu");
        LogInputBean logInputBean = new LogInputBean(mike, fortressDateCreated, "{\"abx\": 1 }");
        // Time will come from the Log
        inputBean.setLog(logInputBean);
        TrackResultBean trackResultBean = mediationFacade.createHeader(inputBean, null);
        String ahWP = trackResultBean.getMetaKey();

        assertEquals(fortressDateCreated.getMillis(), trackResultBean.getMetaHeader().getFortressDateCreated().getMillis());

        // Creating the 2nd log will advance the last modified time
        logTime = DateTime.now();
        logInputBean = new LogInputBean(ahWP, mike, logTime, "{\"abx\": 2 }");
        mediationFacade.processLog(logInputBean);

        TrackLog log = trackService.getLastLog(ahWP);
        assertEquals("Fortress modification date&time do not match", log.getFortressWhen().longValue(), logTime.getMillis());
        MetaHeader header = trackService.getHeader(ahWP);
        assertEquals(fortressDateCreated.getMillis(), header.getFortressDateCreated().getMillis());
        assertEquals("Fortress log time doesn't match", logTime.getMillis(), log.getFortressWhen().longValue());

    }

    @Test
    public void missingLogDateGeneratesSystemDate() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", dt, "mldgsd99");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();
        MetaHeader metaHeader = trackService.getHeader(ahWP);

        // Check that TimeZone information is used to correctly establish Now when not passed in a log
        // No Date, so default to NOW in the Fortress Timezone
        LogResultBean log = mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", null, what + 1 + "\"}"));
        logger.info("1 " + new Date(log.getSysWhen()).toString());

        log = mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", null, what + 2 + "\"}"));
        logger.info("2 " + new Date(log.getSysWhen()).toString());

        Set<TrackLog> logs = trackService.getLogs(metaHeader.getId());
        assertEquals("Logs with missing dates not correctly recorded", 2, logs.size());

        // Same date should still log
        DateTime dateMidnight = new DateTime();
        log = mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", dateMidnight.toDateTime(), what + 3 + "\"}"));
        logger.info("3 " + new Date(log.getSysWhen()).toString());
        TrackLog thirdLog = trackService.getLastLog(ahWP);
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", dateMidnight.toDateTime(), what + 4 + "\"}"));
        logger.info("4 " + new Date(log.getSysWhen()).toString());
        logs = trackService.getLogs(metaHeader.getId());
        assertEquals(4, logs.size());
        for (TrackLog next : logs) {
            logger.info(next.getId() + " - " + new Date(next.getSysWhen()).toString());
        }
        TrackLog lastLog = trackService.getLastLog(ahWP);
        logger.info("L " + new Date(lastLog.getSysWhen()).toString());
        assertNotSame("Last log in should be the last", lastLog.getChange().getId(), thirdLog.getChange().getId());
    }

    @Test
    public void auditSummaryWorking() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();

        MetaHeader metaHeader = trackService.getHeader(ahWP);
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "olivia@sunnybell.com", firstDate, what + 1 + "\"}"));
        mediationFacade.processLog(new LogInputBean(metaHeader.getMetaKey(), "isabella@sunnybell.com", firstDate.plusDays(1), what + 2 + "\"}"));

        TrackedSummaryBean auditSummary = trackService.getMetaSummary(null, ahWP);
        assertNotNull(auditSummary);
        assertEquals(ahWP, auditSummary.getHeader().getMetaKey());
        assertNotNull(auditSummary.getHeader().getLastUser());
        assertNotNull(auditSummary.getHeader().getCreatedBy());
        assertNotNull(auditSummary.getHeader().getFortress());
        assertEquals(2, auditSummary.getChanges().size());
        for (TrackLog log : auditSummary.getChanges()) {
            Log change = log.getChange();
            assertNotNull(change.getEvent());
            assertNotNull(change.getWho().getCode());
            LogWhat whatResult = trackService.getWhat(metaHeader, change);
            assertTrue(whatResult.getWhat().containsKey("house"));
        }
    }

    @Test
    public void fullHeaderDetailsByCallerRef() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortWP = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        MetaInputBean inputBean = new MetaInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "ABC1");
        String ahWP = mediationFacade.createHeader(inputBean, null).getMetaKey();

        MetaHeader header = trackService.findByCallerRefFull(fortWP, "CompanyNode", "ABC1");
        assertNotNull(header);
        assertNotNull(header.getDocumentType());
        assertEquals(ahWP, header.getMetaKey());
    }

    @Test
    public void testFortressTimeBoundaries() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        SecurityContextHolder.getContext().setAuthentication(authMike);

        FortressInputBean usFortress = new FortressInputBean("usFortress", true);
        usFortress.setTimeZone(TimeZone.getTimeZone("GMT").getID());
        Fortress fortressGMT = fortressService.registerFortress(usFortress);
        assertEquals(TimeZone.getTimeZone("GMT").getID(), fortressGMT.getTimeZone());

        FortressInputBean astFortress = new FortressInputBean("astFortress", true);
        astFortress.setTimeZone(TimeZone.getTimeZone("AST").getID());
        Fortress fortressAST = fortressService.registerFortress(astFortress);
        assertEquals(TimeZone.getTimeZone("AST").getID(), fortressAST.getTimeZone());

        MetaInputBean astAuditBean = new MetaInputBean(fortressGMT.getName(), "olivia@ast.com", "CompanyNode", null, "ABC1");
        MetaInputBean gmtAuditBean = new MetaInputBean(fortressAST.getName(), "olivia@gmt.com", "CompanyNode", null, "ABC1");
        String result = mediationFacade.createHeader(astAuditBean, null).getMetaKey();
        MetaHeader header = trackService.getHeader(result);
        DateTime astTime = new DateTime(header.getFortressDateCreated());

        result = mediationFacade.createHeader(gmtAuditBean, null).getMetaKey();
        header = trackService.getHeader(result);
        DateTime gmtTime = new DateTime(header.getFortressDateCreated());

        assertNotSame(astTime.getHourOfDay(), gmtTime.getHourOfDay());

    }

    @Test
    public void headersByFortressAndDocType() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        String typeA = "TypeA";
        String typeB = "Type B";

        mediationFacade.createHeader(new MetaInputBean("ABC", "auditTest", typeA, new DateTime(), "abc"), null);
        mediationFacade.createHeader(new MetaInputBean("ABC", "auditTest", typeA, new DateTime(), "abd"), null);
        mediationFacade.createHeader(new MetaInputBean("ABC", "auditTest", typeB, new DateTime(), "abc"), null);

        assertEquals(3, trackService.getHeaders(fortress, 0l).size());
        assertEquals(2, trackService.getHeaders(fortress, typeA, 0l).size());
        assertEquals("Case sensitivity failed", 2, trackService.getHeaders(fortress, "typea", 0l).size());
        assertEquals(1, trackService.getHeaders(fortress, typeB, 0l).size());
        assertEquals("Case sensitivity failed", 1, trackService.getHeaders(fortress, "type b", 0l).size());
    }

    @Test
    public void findMetaHeadersForCollectionOfMetaKeys() throws Exception{
        String suB = regService.registerSystemUser(new RegistrationBean("othercompany", mark)).getBody().getApiKey();
        String suA = regService.registerSystemUser(new RegistrationBean(monowai, mike)).getBody().getApiKey();

        Fortress fortressA = fortressService.registerFortress("ABC");
        assertNotNull(fortressA);
        Fortress fortressB = fortressService.registerFortress("XYZ");

        String typeA = "TypeA";
        String typeB = "Type B";

        TrackResultBean ra = mediationFacade.createHeader(new MetaInputBean(fortressA.getName(), "auditTest", typeA, new DateTime(), "aba"), suA);
        TrackResultBean rb = mediationFacade.createHeader(new MetaInputBean(fortressA.getName(), "auditTest", typeA, new DateTime(), "abb"), suA);
        TrackResultBean rc = mediationFacade.createHeader(new MetaInputBean(fortressA.getName(), "auditTest", typeB, new DateTime(), "abc"), suA);
        TrackResultBean validButNotForCallerA = mediationFacade.createHeader(new MetaInputBean(fortressB.getName(), "auditTest", typeB, new DateTime(), "abc"), suB);
        Collection<String>toFind = new ArrayList<>();
        toFind.add(ra.getMetaKey());
        toFind.add(rb.getMetaKey());
        toFind.add(rc.getMetaKey());
        toFind.add(validButNotForCallerA.getMetaKey());

        Collection<MetaHeader>foundHeaders = trackEP.getMetaHeaders(toFind, suA, suA);
        assertEquals("Caller was authorised to find 3 headers", 3, foundHeaders.size());

        // This is the other user, and despite there being valid keys, they will only get theirs back
        foundHeaders = trackEP.getMetaHeaders(toFind, suA, suB);
        assertEquals("Caller was only authorised to find 1 header", 1, foundHeaders.size());

    }




    private void compareUser(MetaHeader header, String userName) {
        FortressUser fu = fortressService.getUser(header.getLastUser().getId());
        assertEquals(userName, fu.getCode());

    }

    private void createLogRecords(Authentication auth, String auditHeader, String textToUse, double recordsToCreate) throws Exception {
        int i = 0;
        SecurityContextHolder.getContext().setAuthentication(auth);
        while (i < recordsToCreate) {
            mediationFacade.processLog(new LogInputBean(auditHeader, "wally", new DateTime(), textToUse + i + "\"}", (String) null));
            i++;
        }
        assertEquals(recordsToCreate, (double) trackService.getLogCount(auditHeader));
    }


}
