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

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.TrackLog;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StopWatch;

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
public class TestTrack extends TestEngineBase {

    private Logger logger = LoggerFactory.getLogger(TestTrack.class);

    @org.junit.Before
    public void setup(){
        engineConfig.setDuplicateRegistration(true);
    }

    @Test
    public void duplicateCallerRefMultipleLastChange() throws Exception {
        logger.debug("### duplicateCallerRefMultipleLastChange");
        String callerRef = "dcABC1";
        SystemUser su = registerSystemUser(monowai, mike_admin);

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);


        inputBean.setLog(new LogInputBean("poppy", DateTime.now(), TestHelper.getSimpleMap("name", "a")));
        List<EntityInputBean> entityInputBeans = new ArrayList<>();
        entityInputBeans.add(inputBean);

        inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);
        inputBean.setLog(new LogInputBean("poppy", DateTime.now(), TestHelper.getSimpleMap("name", "a")));
        entityInputBeans.add(inputBean);

        inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);
        inputBean.setLog(new LogInputBean("poppy", DateTime.now(), TestHelper.getSimpleMap("name", "a")));
        entityInputBeans.add(inputBean);
        logger.info("Tracking...");

        trackEP.trackHeaders(entityInputBeans, su.getApiKey(), su.getApiKey());
        logger.info("Tracked...");
        Entity header = trackEP.getByCallerRef(fortWP.getName(), "CompanyNode", callerRef, su.getApiKey(), su.getApiKey());
        junit.framework.Assert.assertNotNull(header);
        waitForFirstLog(su.getCompany(), header);

        Set<TrackLog> logs = trackEP.getLogs(header.getMetaKey(), su.getApiKey(), su.getApiKey());
        org.junit.Assert.assertNotNull(logs);
        assertEquals("3 Identical changes should result in a single log", 1, logs.size());
    }

    @Test
    public void logChangeWithNullAuditKeyButCallerRefExists() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest",true));
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        assertNotNull(mediationFacade.trackEntity(su.getCompany(), inputBean));

        LogInputBean aib = new LogInputBean("wally", new DateTime(), TestHelper.getSimpleMap("blah", 1));
        aib.setCallerRef(fortress.getName(), "TestTrack", "ABC123");
        LogResultBean input = mediationFacade.processLog(su.getCompany(), aib).getLogResult();
        assertNotNull(input.getMetaKey());
        Assert.assertNotNull(trackService.findByCallerRef(fortress, aib.getDocumentType(), aib.getCallerRef()));
    }

    @Test
    public void trackByCallerRef_FortressUserInHeaderButNotLog() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        //Fortress fortress = fortressService.registerFortress("auditTest");
        FortressInputBean fortress = new FortressInputBean("trackByCallerRef", true);

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        LogInputBean aib = new LogInputBean("wally", new DateTime(), TestHelper.getSimpleMap("blah", 1));
        aib.setFortressUser(null); // We want AB to extract this from the entity
        aib.setCallerRef(fortress.getName(), "TestTrack", "ABC123");
        inputBean.setLog(aib);
        // This call expects the service layer to create the missing fortress from the entityInput
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Assert.assertNotNull(result);
        assertNotNull(result.getMetaKey());
        Assert.assertNotNull(trackService.findByCallerRef(fortress.getName(), aib.getDocumentType(), aib.getCallerRef()));
    }


    @Test
    public void nullMetaKey() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        assertNull (trackService.getEntity(su.getCompany(), null));
    }

    @Test
    public void locatingByCallerRefWillThrowAuthorizationException() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser(monowai, mike_admin);

        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        // Check we can't create the same header twice for a given client ref
        inputBean = new EntityInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String keyB = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        assertEquals(key, keyB);

        setSecurity(sally_admin); // Sally can register users
        SystemUser suB= registerSystemUser("TestTow", harry);
        setSecurity(harry); // Harry can access them
        Fortress fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("auditTestB", true));
        mediationFacade.trackEntity(suB.getCompany(), new EntityInputBean(fortressB.getName(), "wally", "TestTrack", new DateTime(), "123ABC"));

        setSecurity(mike_admin);

        assertNotNull(trackService.findByCallerRef(fortressA, "TestTrack", "ABC123"));
        assertNull( "Caller refs are case sensitive so this should not be found", trackService.findByCallerRef(fortressA, "TestTrack", "abc123"));
        assertNull("Security - shouldn't be able to see this header", trackService.findByCallerRef(fortressA, "TestTrack", "123ABC"));

        setSecurity(harry);
        assertNull("User does not belong to this company.Fortress so should not be able to see it",
                trackService.findByCallerRef(fortressA.getCode(), "TestTrack", "ABC123"));

        try {
            assertNull(trackService.getEntity(suB.getCompany(), key));
            fail("Security exception not thrown");

        } catch (SecurityException se) {
            logger.debug("Good stuff!");
        }
    }

    @Test
    public void createHeaderTimeLogs() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("createHeaderTimeLogs", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        Entity header = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        String ahKey = header.getMetaKey();

        assertNotNull(ahKey);

        assertNotNull(trackService.getEntity(su.getCompany(), ahKey));
        assertNotNull(trackService.findByCallerRef(fortress, "TestTrack", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            TrackResultBean subsequent = mediationFacade.processLog(su.getCompany(), new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getSimpleMap("blah", i)));

            if ( i == 0){
                waitForFirstLog(su.getCompany(), header);
            } else
                waitForLogCount(su.getCompany(), subsequent.getEntity(),i+1);
            i++;
        }
        watch.stop();
        logger.info(watch.prettyPrint() + " avg = " + (watch.getLastTaskTimeMillis() / 1000d) / max);

        // Test that we get the expected number of log events
//        waitAWhile();
        assertEquals(max, (double) trackService.getLogCount(su.getCompany(), ahKey));
    }

    /**
     * Idempotent "what" data
     * Ensure duplicate logs are not created when content data has not changed
     */
    @Test
    public void noDuplicateLogsWithCompression() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "testDupe", new DateTime(), "ndlwcqw2");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        assertNotNull(metaKey);
        // Irrespective of the order of the fields, we see it as the same.
        //String jsonA = "{\"name\": \"8888\", \"thing\": {\"m\": \"happy\"}}";
        //String jsonB = "{\"thing\": {\"m\": \"happy\"},\"name\": \"8888\"}";

        Map<String, Object> jsonA = TestHelper.getSimpleMap("name", "8888");
        jsonA.put("thing", TestHelper.getSimpleMap("m", "happy"));

        Map<String, Object> jsonB = TestHelper.getSimpleMap("thing", TestHelper.getSimpleMap("m", "happy"));
        jsonB.put("name", "8888");
        jsonA.put("thing", TestHelper.getSimpleMap("m", "happy"));



        assertNotNull(trackService.getEntity(su.getCompany(), metaKey));
        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));
        int i = 0;
        double max = 10d;
        Map<String,Object> json;
        while (i < max) {
            // Same "what" text so should only be one auditLogCount record
            json = (i % 2 == 0 ? jsonA : jsonB);
            mediationFacade.processLog(su.getCompany(), new LogInputBean("wally", metaKey, new DateTime(), json));
            i++;
        }
        assertEquals(1d, (double) trackService.getLogCount(su.getCompany(), metaKey));
        Set<TrackLog> logs = trackService.getLogs(fortress.getCompany(), metaKey);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
        for (TrackLog log : logs) {
            LogWhat what = trackEP.getLogWhat(metaKey, log.getId(), su.getApiKey(), su.getApiKey()).getBody();
            assertNotNull (what);
            assertNotNull(what.getWhatString());
        }
    }

    @Test
    public void correctLogCountsReturnedForAFortress() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getEntity(su.getCompany(), ahKey));

        mediationFacade.processLog(su.getCompany(), new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getSimpleMap("blah", 0)));
        mediationFacade.processLog(su.getCompany(), new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getSimpleMap("blah", 1)));
        assertEquals(2, trackService.getLogCount(su.getCompany(), resultBean.getMetaKey()));
    }

    @Test
    public void testHeaderWithLogChange() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "uouu87");
        inputBean.setName("MikesNameTest");
        LogInputBean logBean = new LogInputBean("wally", null, DateTime.now(), TestHelper.getSimpleMap("blah", 0));
        inputBean.setLog(logBean);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getMetaKey());
        assertEquals("MikesNameTest", resultBean.getEntity().getName());
        assertTrue(resultBean.getEntity().toString().contains(resultBean.getMetaKey()));
        assertEquals(1, trackService.getLogCount(su.getCompany(), resultBean.getMetaKey()));
    }

    @Test
    public void testHeaderWithLogChangeTransactional() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest",true));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "232146");
        LogInputBean logBean = new LogInputBean("wally", null, DateTime.now(), TestHelper.getSimpleMap("blah", 0));
        inputBean.setLog(logBean);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getMetaKey());
        assertEquals(1, trackService.getLogCount(su.getCompany(), resultBean.getMetaKey()));
    }

    @Test
    public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
        setSecurity(mike_admin);
        // Registering the internal admin as a data access user
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(),true));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        LogInputBean alb = new LogInputBean("logTest", new DateTime(), TestHelper.getSimpleMap("blah", 0));
        alb.setCallerRef(fortressA.getName(), docType, callerRef);
        //assertNotNull (alb);
        LogResultBean arb = mediationFacade.processLog(su.getCompany(), alb).getLogResult();
        assertNotNull(arb);
        assertEquals(keyA, arb.getMetaKey());

        // Scenario - create a new data access user
        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser("TWEE", harry);
        // Switch to the data access user
        setSecurity(harry);
        Fortress fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestB" + System.currentTimeMillis(),true));
        inputBean = new EntityInputBean(fortressB.getName(), "wally", docType, new DateTime(), callerRef);
        String keyB = mediationFacade.trackEntity(suB.getCompany(), inputBean).getMetaKey();
        alb = new LogInputBean("logTest", new DateTime(), TestHelper.getSimpleMap("blah", 0));
        alb.setCallerRef(fortressB.getName(), docType, callerRef);
        arb = mediationFacade.processLog(su.getCompany(), alb).getLogResult();
        assertNotNull(arb);
        assertEquals("This caller should not see KeyA", keyB, arb.getMetaKey());

    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        logger.info ( "## companyAndFortressWithSpaces");
        SystemUser su = registerSystemUser("companyAndFortressWithSpaces", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("track Test" + System.currentTimeMillis(), true));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        assertNotNull(keyA);
    }

    @Test
    public void headersForDifferentCompaniesAreNotVisible() throws Exception {
        logger.info("## headersForDifferentCompaniesAreNotVisible");
        SystemUser su = registerSystemUser(monowai, mike_admin);
        String hummingbird = "Hummingbird";

        //Monowai/Mike
        Authentication authMike = setSecurity(mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "AHWP");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        assertNotNull(ahWP);
        assertNotNull(trackService.getEntity(su.getCompany(), ahWP));

        //Hummingbird/Gina
        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser(hummingbird, harry);
        Authentication authHarry = setSecurity(harry); // Harry can create data
        Fortress fortHS = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("honeysuckle", true));
        inputBean = new EntityInputBean(fortHS.getName(), "harry", "CompanyNode", new DateTime(), "AHHS");
        String ahHS = mediationFacade.trackEntity(suB.getCompany(), inputBean).getMetaKey();

        assertNotNull(fortressService.getFortressUser(fortWP, "wally", true));
        assertNotNull(fortressService.getFortressUser(fortHS, "harry", true));
        assertNull(fortressService.getFortressUser(fortWP, "wallyz", false));

        double max = 2000d;
        StopWatch watch = new StopWatch();
        watch.start();

        createLogRecords(authMike, su, ahWP, "house", 20);
        createLogRecords(authHarry, suB, ahHS, "house", 40);
        watch.stop();
        logger.info(watch.prettyPrint()+ " avg = " + (watch.getLastTaskTimeMillis() / 1000d) / max);


    }

    @Test
    public void lastChangedWorks() throws Exception {
        logger.info ("## lastChangedWorks");
        SystemUser su = registerSystemUser(monowai, mike_admin);
        // Create a second log record in order to workout who last change the EntityNode

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio",true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity trackKey = trackService.getEntity(su.getCompany(), ahWP);
        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", trackKey.getMetaKey(), new DateTime(), TestHelper.getSimpleMap("house", "house1"), "Update"));
        trackKey = trackService.getEntity(su.getCompany(), ahWP);
        FortressUser fu = fortressService.getUser(trackKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

        mediationFacade.processLog(su.getCompany(), new LogInputBean("harry@sunnybell.com", trackKey.getMetaKey(), new DateTime(), TestHelper.getSimpleMap("house", "house2"), "Update"));
        trackKey = trackService.getEntity(su.getCompany(), ahWP);

        fu = fortressService.getUser(trackKey.getLastUser().getId());
        assertEquals("harry@sunnybell.com", fu.getCode());

        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", trackKey.getMetaKey(), new DateTime(), TestHelper.getSimpleMap("house", "house3"), "Update"));
        trackKey = trackService.getEntity(su.getCompany(), ahWP);

        fu = fortressService.getUser(trackKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

    }

    @Test
    public void outOfSequenceLogsWorking() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        DateTime dt = new DateTime().toDateTime();
        DateTime earlyDate = dt.minusDays(2);

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio",true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity entity = trackService.getEntity(su.getCompany(), ahWP);

        // Create the future one first.
        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), TestHelper.getSimpleMap("house", "house1"), "Update"));
        entity = trackService.getEntity(su.getCompany(), ahWP);
        FortressUser fu = fortressService.getUser(entity.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());
        TrackLog compareLog = logService.getLastLog(entity);

        // Load a historic record. This should not become "last"
        mediationFacade.processLog(su.getCompany(), new LogInputBean("harry@sunnybell.com", entity.getMetaKey(), earlyDate, TestHelper.getSimpleMap("house", "house2"), "Update"));
        entity = trackService.getEntity(su.getCompany(), ahWP);

        TrackLog lastLog = logService.getLastLog(entity);
        assertNotNull(lastLog);
        assertEquals(compareLog.getId(), lastLog.getId());

        fu = fortressService.getUser(entity.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode()); // The first one we created is the "last one"


    }

    /**
     * test that we find the correct number of changes between a range of dates for a given header
     */
    @Test
    public void logDateRangesWorking() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);

        int max = 10;
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(max);
        DateTime workingDate = firstDate.toDateTime();

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "CompanyNode", firstDate, "123");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity entity = trackService.getEntity(su.getCompany(), ahWP);
        int i = 0;
        while (i < max) {
            workingDate = workingDate.plusDays(1);
            assertEquals("Loop count " + i,
                    LogInputBean.LogStatus.OK, mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), workingDate, TestHelper.getSimpleMap("house", "house" + i))).
                    getLogResult().getStatus());
            i++;
        }

        Set<TrackLog> aLogs = trackService.getLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(max, aLogs.size());

        TrackLog lastLog = trackService.getLastLog(entity.getMetaKey());
        Log lastChange = lastLog.getLog();
        assertNotNull(lastChange);
        assertEquals(workingDate, new DateTime(lastLog.getFortressWhen()));
        entity = trackService.getEntity(su.getCompany(), ahWP);
        assertEquals(max, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));

        DateTime then = workingDate.minusDays(4);
        logger.info("Searching between " + then.toDate() + " and " + workingDate.toDate());
        Set<TrackLog> logs = trackService.getLogs(entity.getMetaKey(), then.toDate(), workingDate.toDate());
        assertEquals(5, logs.size());
        Long logId = logs.iterator().next().getId();
        LogDetailBean change = trackService.getFullDetail(entity.getMetaKey(), logId);
        assertNotNull(change);
        assertNotNull(change.getLog());
        assertNotNull(change.getWhat());
        assertEquals(logId, change.getLog().getId());


    }

    @Test
    public void cancelLastChangeBehaves() throws Exception {
        // For use in compensating transaction cases only
        // DAT-53
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), ahWP);
        LogResultBean firstLog  = mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), firstDate, TestHelper.getSimpleMap("house", "house1"))).getLogResult();
        LogResultBean secondLog = mediationFacade.processLog(su.getCompany(), new LogInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), TestHelper.getSimpleMap("house", "house2"))).getLogResult();
        assertNotSame(0l, firstLog.getWhatLog().getTrackLog().getFortressWhen());
        assertNotSame(0l, secondLog.getWhatLog().getTrackLog().getFortressWhen());
        Set<TrackLog> logs = trackService.getLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(2, logs.size());
        entity = trackService.getEntity(su.getCompany(), ahWP);
        compareUser(entity, secondLog.getFortressUser());
        assertEquals(secondLog.getWhatLog().getTrackLog().getFortressWhen(), entity.getFortressLastWhen());

        // Test block
        trackService.cancelLastLog(fortress.getCompany(), entity);
        logs = trackService.getLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(1, logs.size());
        entity = trackService.getEntity(su.getCompany(), ahWP, true); // Refresh the entity
        compareUser(entity, firstLog.getFortressUser());
        assertEquals(firstLog.getWhatLog().getTrackLog().getFortressWhen(), entity.getFortressLastWhen());

        // Last change cancelled
        trackService.cancelLastLog(fortress.getCompany(), entity);
        logs = trackService.getLogs(fortress.getCompany(), entity.getMetaKey());
        assertTrue(logs.isEmpty());
    }

    @Test
    public void lastChangeDatesReconcileWithFortressInput() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC1");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), ahWP);
        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), TestHelper.getSimpleMap("house", "house1")));
        entity = trackService.getEntity(su.getCompany(), ahWP); // Inflate the header on the server
        TrackLog lastLog = trackService.getLastLog(entity.getMetaKey());
        assertNotNull(lastLog);
//        assertNotNull(lastLog.getAuditChange().getWhat());
        LogWhat whatResult = trackService.getWhat(entity, lastLog.getLog());
        assertNotNull(whatResult);
        assertTrue(whatResult.getWhat().containsKey("house"));
    }

    @Test
    public void dateCreatedAndLastUpdated() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime fortressDateCreated = DateTime.now();
        Thread.sleep(500);
        DateTime logTime;
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", fortressDateCreated, "dcalbu");
        LogInputBean logInputBean = new LogInputBean(mike_admin, fortressDateCreated, TestHelper.getSimpleMap("abx", "1"));
        // Time will come from the Log
        inputBean.setLog(logInputBean);
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String ahWP = trackResultBean.getMetaKey();

        assertEquals(fortressDateCreated.getMillis(), trackResultBean.getEntity().getFortressDateCreated().getMillis());

        // Creating the 2nd log will advance the last modified time
        logTime = DateTime.now();
        logInputBean = new LogInputBean(mike_admin, ahWP, logTime, TestHelper.getSimpleMap("abx", "2"));
        mediationFacade.processLog(su.getCompany(), logInputBean);

        TrackLog log = trackService.getLastLog(ahWP);
        assertEquals("Fortress modification date&time do not match", log.getFortressWhen().longValue(), logTime.getMillis());
        Entity header = trackService.getEntity(su.getCompany(), ahWP);
        assertEquals(fortressDateCreated.getMillis(), header.getFortressDateCreated().getMillis());
        assertEquals("Fortress log time doesn't match", logTime.getMillis(), log.getFortressWhen().longValue());

    }

    @Test
    public void missingLogDateGeneratesSystemDate() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", dt, "mldgsd99");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity entity = trackService.getEntity(su.getCompany(), ahWP);

        // Check that TimeZone information is used to correctly establish Now when not passed in a log
        // No Date, so default to NOW in the Fortress Timezone
        LogResultBean log = mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), null, TestHelper.getSimpleMap("house", "house1"))).getLogResult();
        logger.info("1 " + new Date(log.getSysWhen()).toString());

        log = mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), null, TestHelper.getSimpleMap("house", "house2"))).getLogResult();
        logger.info("2 " + new Date(log.getSysWhen()).toString());

        Set<TrackLog> logs = trackService.getLogs(entity.getId());
        assertEquals("Logs with missing dates not correctly recorded", 2, logs.size());

        // Same date should still log
        DateTime dateMidnight = new DateTime();
        log = mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), dateMidnight.toDateTime(), TestHelper.getSimpleMap("house", "house3"))).getLogResult();
        logger.info("3 " + new Date(log.getSysWhen()).toString());
        TrackLog thirdLog = trackService.getLastLog(ahWP);
        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), dateMidnight.toDateTime(), TestHelper.getSimpleMap("house", "house4")));
        logger.info("4 " + new Date(log.getSysWhen()).toString());
        logs = trackService.getLogs(entity.getId());
        assertEquals(4, logs.size());
        for (TrackLog next : logs) {
            logger.info(next.getId() + " - " + new Date(next.getSysWhen()).toString());
        }
        TrackLog lastLog = trackService.getLastLog(ahWP);
        logger.info("L " + new Date(lastLog.getSysWhen()).toString());
        assertNotSame("Last log in should be the last", lastLog.getLog().getId(), thirdLog.getLog().getId());
    }

    @Test
    public void fullHeaderDetailsByCallerRef() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "ABC1");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity header = trackService.findByCallerRefFull(fortWP, "CompanyNode", "ABC1");
        assertNotNull(header);
        assertNotNull(header.getDocumentType());
        assertEquals(ahWP, header.getMetaKey());
    }

    @Test
    public void testFortressTimeBoundaries() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);

        FortressInputBean usFortress = new FortressInputBean("usFortress", true);
        usFortress.setTimeZone(TimeZone.getTimeZone("GMT").getID());
        Fortress fortressGMT = fortressService.registerFortress(su.getCompany(), usFortress);
        assertEquals(TimeZone.getTimeZone("GMT").getID(), fortressGMT.getTimeZone());

        FortressInputBean astFortress = new FortressInputBean("astFortress", true);
        astFortress.setTimeZone(TimeZone.getTimeZone("AST").getID());
        Fortress fortressAST = fortressService.registerFortress(su.getCompany(), astFortress);
        assertEquals(TimeZone.getTimeZone("AST").getID(), fortressAST.getTimeZone());

        EntityInputBean astAuditBean = new EntityInputBean(fortressGMT.getName(), "olivia@ast.com", "CompanyNode", null, "ABC1");
        EntityInputBean gmtAuditBean = new EntityInputBean(fortressAST.getName(), "olivia@gmt.com", "CompanyNode", null, "ABC1");
        String result = mediationFacade.trackEntity(su.getCompany(), astAuditBean).getMetaKey();
        Entity header = trackService.getEntity(su.getCompany(), result);
        DateTime astTime = new DateTime(header.getFortressDateCreated());

        result = mediationFacade.trackEntity(su.getCompany(), gmtAuditBean).getMetaKey();
        header = trackService.getEntity(su.getCompany(), result);
        DateTime gmtTime = new DateTime(header.getFortressDateCreated());

        assertNotSame(astTime.getHourOfDay(), gmtTime.getHourOfDay());

    }

    @Test
    public void headersByFortressAndDocType() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        String typeA = "TypeA";
        String typeB = "Type B";

        mediationFacade.trackEntity(su.getCompany(), new EntityInputBean("ABC", "auditTest", typeA, new DateTime(), "abc"));
        mediationFacade.trackEntity(su.getCompany(), new EntityInputBean("ABC", "auditTest", typeA, new DateTime(), "abd"));
        mediationFacade.trackEntity(su.getCompany(), new EntityInputBean("ABC", "auditTest", typeB, new DateTime(), "abc"));

        assertEquals(3, trackService.getEntities(fortress, 0l).size());
        assertEquals(2, trackService.getEntities(fortress, typeA, 0l).size());
        assertEquals("Case sensitivity failed", 2, trackService.getEntities(fortress, "typea", 0l).size());
        assertEquals(1, trackService.getEntities(fortress, typeB, 0l).size());
        assertEquals("Case sensitivity failed", 1, trackService.getEntities(fortress, "type b", 0l).size());
    }

    @Test
    public void findEntitiesForCollectionOfMetaKeys() throws Exception{
        SystemUser suA = registerSystemUser(monowai, mike_admin);

        Fortress fortressA = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortressA);

        String typeA = "TypeA";
        String typeB = "Type B";

        TrackResultBean ra = mediationFacade.trackEntity(suA.getCompany(), new EntityInputBean(fortressA.getName(), "auditTest", typeA, new DateTime(), "aba"));
        TrackResultBean rb = mediationFacade.trackEntity(suA.getCompany(), new EntityInputBean(fortressA.getName(), "auditTest", typeA, new DateTime(), "abb"));
        TrackResultBean rc = mediationFacade.trackEntity(suA.getCompany(), new EntityInputBean(fortressA.getName(), "auditTest", typeB, new DateTime(), "abc"));

        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser("other company", harry);
        setSecurity(harry); // Harry can create data
        Fortress fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("XYZ"),true);
        TrackResultBean validButNotForCallerA = mediationFacade.trackEntity(suB.getCompany(), new EntityInputBean(fortressB.getName(), "auditTest", typeB, new DateTime(), "abc"));
        Collection<String>toFind = new ArrayList<>();
        setSecurity(mike_admin);
        toFind.add(ra.getMetaKey());
        toFind.add(rb.getMetaKey());
        toFind.add(rc.getMetaKey());
        toFind.add(validButNotForCallerA.getMetaKey());

        Collection<Entity>foundHeaders = trackEP.getEntities(toFind, suA.getApiKey(), suA.getApiKey());
        assertEquals("Caller was authorised to find 3 headers", 3, foundHeaders.size());

        // This is the other user, and despite there being valid keys, they will only get theirs back
        foundHeaders = trackEP.getEntities(toFind, suA.getApiKey(), suB.getApiKey());
        assertEquals("Caller was only authorised to find 1 header", 1, foundHeaders.size());

    }

    @Test
    public void utf8Strings() throws Exception{
        Map<String, Object> json = TestHelper.getSimpleMap("Athlete", "Katerina Neumannová") ;
        //getSimpleMap("house", "house1");
        SystemUser su = registerSystemUser(monowai, mike_admin);

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), "ABC1");
        inputBean.setLog(new LogInputBean("poppy", DateTime.now(), json));
        TrackResultBean trackResultBean = trackEP.trackEntity(inputBean, su.getApiKey(), su.getApiKey()).getBody();
        waitForFirstLog(su.getCompany(), trackResultBean.getEntity());
        TrackLog lastLog = logService.getLastLog(trackResultBean.getEntity());

        LogWhat what = kvService.getWhat(trackResultBean.getEntity(),  lastLog.getLog());
        assertEquals(json.get("Athlete"), what.getWhat().get("Athlete"));

        // Second call should say that nothing has changed
        TrackResultBean result = trackEP.trackEntity(inputBean, su.getApiKey(), su.getApiKey()).getBody();
        Assert.assertNotNull(result);
        assertEquals(LogInputBean.LogStatus.IGNORE, result.getLogResult().getStatus());
    }

    @Test
    public void metaSummaryReturnsLogs() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entitySummary", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
        String ahWP = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), ahWP);
        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", entity.getMetaKey(), firstDate, TestHelper.getSimpleMap("house", "house1")));
        mediationFacade.processLog(su.getCompany(), new LogInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), TestHelper.getSimpleMap("house", "house2")));
        waitForLogCount(su.getCompany(), entity,2);
        EntitySummaryBean auditSummary = trackService.getEntitySummary(null, ahWP);
        assertNotNull(auditSummary);
        assertEquals(ahWP, auditSummary.getHeader().getMetaKey());
        assertNotNull(auditSummary.getHeader().getLastUser());
        assertNotNull(auditSummary.getHeader().getCreatedBy());
        assertNotNull(auditSummary.getHeader().getFortress());
        assertEquals(2, auditSummary.getChanges().size());
        for (TrackLog log : auditSummary.getChanges()) {
            Log change = log.getLog();
            assertNotNull(change.getEvent());
            assertNotNull(change.getWho().getCode());
            LogWhat whatResult = trackService.getWhat(entity, change);
            assertTrue(whatResult.getWhat().containsKey("house"));
        }
    }

    @Test
    public void lastLog_CorrectlySequencesInSeparateCallsViaBatchLoad() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityHeaderDiff"),true);
        String callerRef = UUID.randomUUID().toString();
        List<EntityInputBean> inputBeans = new ArrayList<>();

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), callerRef);
        LogInputBean logInputBean = new LogInputBean("mike", new DateTime(), TestHelper.getSimpleMap("col", 123));
        inputBean.setLog(logInputBean);
        inputBeans.add(inputBean);
        logger.debug("** First Track Event");
        Collection<TrackResultBean> results = mediationFacade.trackHeaders(fortress, inputBeans, 10);
        Entity header = results.iterator().next().getEntity();
        waitForFirstLog(su.getCompany(), header);

        inputBeans.clear();

        header = trackService.findByCallerRef(fortress, "TestTrack", callerRef );
        assertNotNull(header);

        // Now we record a change
        logInputBean = new LogInputBean("mike", new DateTime(), TestHelper.getSimpleMap("col", 321));
        inputBean.setLog(logInputBean);
        inputBeans = new ArrayList<>();
        inputBeans.add(inputBean);
        logger.info ("creating {} headers. Current count = {}", inputBeans.size(), trackService.getLogCount(su.getCompany(), header.getMetaKey()));

        logger.debug("** Second Track Event");
        mediationFacade.trackHeaders(fortress, inputBeans, 1);
        logger.info ("Current count now at {}", trackService.getLogCount(su.getCompany(), header.getMetaKey()));

        waitForLogCount(su.getCompany(), header, 2);
        header = trackService.findByCallerRef(fortress, "TestTrack", callerRef );
        TrackLog lastLog = trackService.getLastLog(su.getCompany(), header.getMetaKey());
        assertNotNull(lastLog);
        LogWhat what = kvService.getWhat(header, lastLog.getLog());

        assertNotNull(what);
        Object value = what.getWhat().get("col");
        junit.framework.Assert.assertNotNull(value);
        assertEquals("321", value.toString());
    }

    @Test
    public void datesInHeadersAndLogs() throws Exception{
        SystemUser su = registerSystemUser(monowai, mike_admin);
        FortressInputBean f = new FortressInputBean("dateFun", true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), f);

        DateTime past = new DateTime(2010, 10, 1, 11,35);

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "poppy", "CompanyNode", past, "ABC1");
        inputBean.setLog(new LogInputBean("poppy", past, TestHelper.getSimpleMap("name", "value")));
        TrackResultBean trackResultBean = trackEP.trackEntity(inputBean, su.getApiKey(), su.getApiKey()).getBody();
        waitForFirstLog(su.getCompany(), trackResultBean.getEntity());
        TrackLog lastLog = logService.getLastLog(trackResultBean.getEntity());
        assertEquals(past.getMillis(), lastLog.getFortressWhen().longValue());
        assertEquals(past.getMillis(), trackResultBean.getEntity().getFortressDateCreated().getMillis());
        assertEquals("Modified " + new Date(trackResultBean.getEntity().getLastUpdate()),
                past.getMillis(), trackResultBean.getEntity().getFortressLastWhen().longValue());

        logger.info(lastLog.toString());
    }
    @Test
    public void abFortressDateFields() throws Exception {
        // DAT-196
        logger.info("## utcDateFields");
        SystemUser su = registerSystemUser(monowai, mike_admin);
        FortressInputBean fib = new FortressInputBean("utcDateFields", true);
        String timeZone = "Europe/Copenhagen"; // Arbitrary TZ
        fib.setTimeZone(timeZone);
        Fortress fo = fortressService.registerFortress(su.getCompany(), fib);
        DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone));

        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4,30,tz);
        DateTime lastUpdated = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone)));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", fortressDateCreated, "ABC123");
        assertEquals("MetaInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
        inputBean.setLog(new LogInputBean("wally", lastUpdated, TestHelper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());
        assertEquals("ab.monowai." + fo.getCode(), entity.getIndexName());
        assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(entity.getFortressDateCreated()));

        TrackLog log = trackService.getLastLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
    }
    @Test
    public void clientInDifferentTZ() throws Exception {
        // DAT-196
        logger.info("## clientInDifferentTZ");
        SystemUser su = registerSystemUser();
        FortressInputBean fib = new FortressInputBean("clientInDifferentTZ", true);
        String fortressTz = "Europe/Copenhagen"; // Arbitrary TZ
        fib.setTimeZone(fortressTz);
        Fortress fo = fortressService.registerFortress(su.getCompany(), fib);
        logger.debug("Fortress obtained - SU Company {}, Fortress Company {}", su.getCompany(), fo.getCompany());

        DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortressTz));

        // No timezone is specifically created and the client is in a different country
        //  and sending data to the server.
        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4,30);// This should get converted to the fortress TZ, not left in the clients
        DateTime expectedCreateDate = new DateTime(fortressDateCreated, tz); // The date we are expecting
        DateTime lastUpdated = new DateTime();// In the clients TZ. Needs to be treated as if in the fortress TZ

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TZTest", fortressDateCreated, "ABC123");
        assertEquals("EntityInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
        inputBean.setLog(new LogInputBean("wally", lastUpdated, TestHelper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());
        logger.debug("***  problem {}", entity.toString());
        logger.debug("**** Fortress {}, Company {}, Entity Fortress {}", entity.getFortress(), entity.getFortress().getCompany(), result.getEntity().getFortress());
        assertEquals("Why is this failing", "ab.monowai." + fo.getCode(), entity.getIndexName());
        assertEquals("DateCreated not in Fortress TZ", 0, expectedCreateDate.compareTo(entity.getFortressDateCreated()));

        TrackLog log = trackService.getLastLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
    }

    @Test
    public void defaultFortressTZWhenNoneExists() throws Exception {
        // DAT-196
        logger.info("## defaultFortressTZWhenNoneExists");
        SystemUser su = registerSystemUser("defaultFortressTZWhenNoneExists", "defaultFortressTZWhenNoneExists");

        String fortressTz = "Europe/Copenhagen"; // Arbitrary TZ

        DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortressTz));

        // No timezone is specifically created and the client is in a different country
        //  and sending data to the server.
        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4,30);// This should get converted to the fortress TZ, not left in the clients
        DateTime expectedCreateDate = new DateTime(fortressDateCreated, tz); // The date we are expecting
        DateTime lastUpdated = new DateTime();// In the clients TZ. Needs to be treated as if in the fortress TZ

        FortressInputBean fortressBean = new FortressInputBean("clientInDifferentTZ", true);
        fortressBean.setTimeZone(fortressTz);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fortressBean);

        EntityInputBean inputBean = new EntityInputBean("A Description", "wally", "TestTrack", fortressDateCreated);

        assertEquals("MetaInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
        inputBean.setLog(new LogInputBean("wally", lastUpdated, TestHelper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackHeader(fortress, inputBean); // Mock result as we're not tracking

        fortress = fortressService.findByName(su.getCompany(), fortressBean.getName());
        assertNotNull (fortress);
        assertEquals(fortressTz, fortress.getTimeZone());

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());
        assertEquals("DateCreated not in Fortress TZ", 0, expectedCreateDate.compareTo(entity.getFortressDateCreated()));

        TrackLog log = trackService.getLastLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
    }
    private void compareUser(Entity header, String userName) {
        FortressUser fu = fortressService.getUser(header.getLastUser().getId());
        assertEquals(userName, fu.getCode());

    }

    private void createLogRecords(Authentication auth, SystemUser su, String auditHeader, String key, double recordsToCreate) throws Exception {
        int i = 0;
        SecurityContextHolder.getContext().setAuthentication(auth);
        while (i < recordsToCreate) {
            mediationFacade.processLog(su.getCompany(), new LogInputBean("wally", auditHeader, new DateTime(), TestHelper.getSimpleMap(key, "house" + i), (String) null));
            i++;
        }
        assertEquals(recordsToCreate, (double) trackService.getLogCount(su.getCompany(), auditHeader));
    }


}
