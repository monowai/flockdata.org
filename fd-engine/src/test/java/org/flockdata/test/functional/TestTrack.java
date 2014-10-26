/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.functional;

import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.track.bean.*;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityContent;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.Log;
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
public class TestTrack extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(TestTrack.class);

    @org.junit.Before
    public void setup(){
        engineConfig.setDuplicateRegistration(true);
    }

    @Test
    public void duplicateCallerRefMultipleLastChange() throws Exception {
        logger.debug("### duplicateCallerRefMultipleLastChange");
        String callerRef = "dcABC1";
        SystemUser su = registerSystemUser("duplicateCallerRefMultipleLastChange");

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);


        inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), Helper.getSimpleMap("name", "a")));
        List<EntityInputBean> entityInputBeans = new ArrayList<>();
        entityInputBeans.add(inputBean);

        inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);
        inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), Helper.getSimpleMap("name", "a")));
        entityInputBeans.add(inputBean);

        inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);
        inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), Helper.getSimpleMap("name", "a")));
        entityInputBeans.add(inputBean);
        logger.info("Tracking...");

        mediationFacade.trackEntities(su.getCompany(), entityInputBeans);
        logger.info("Tracked...");
        Entity entity = trackService.findByCallerRef(fortWP, "CompanyNode", callerRef);
        assertNotNull(entity);
        waitForFirstLog(su.getCompany(), entity);

        Set<EntityLog> logs = trackService.getEntityLogs(su.getCompany(), entity.getMetaKey());
        org.junit.Assert.assertNotNull(logs);
        assertEquals("3 Identical changes should result in a single log", 1, logs.size());
    }

    @Test
    public void logChangeWithNullAuditKeyButCallerRefExists() throws Exception {
        SystemUser su = registerSystemUser("logChangeWithNullAuditKeyButCallerRefExists");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest",true));
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        assertNotNull(mediationFacade.trackEntity(su.getCompany(), inputBean));

        ContentInputBean aib = new ContentInputBean("wally", new DateTime(), Helper.getSimpleMap("blah", 1));
        aib.setCallerRef(fortress.getName(), "TestTrack", "ABC123");
        LogResultBean input = mediationFacade.trackLog(su.getCompany(), aib).getLogResult();
        assertNotNull(input.getMetaKey());
        assertNotNull(trackService.findByCallerRef(fortress, aib.getDocumentType(), aib.getCallerRef()));
    }

    @Test
    public void trackByCallerRef_FortressUserInEntityButNotLog() throws Exception {
        SystemUser su = registerSystemUser("trackByCallerRef_FortressUserInEntityButNotLog");
        //Fortress fortress = fortressService.registerFortress("auditTest");
        FortressInputBean fortress = new FortressInputBean("FortressUserInEntityButNotLog", true);
        fortressService.registerFortress(su.getCompany(), fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        ContentInputBean aib = new ContentInputBean("wally", new DateTime(), Helper.getSimpleMap("blah", 1));
        aib.setFortressUser(null); // We want AB to extract this from the entity
        aib.setCallerRef(fortress.getName(), "TestTrack", "ABC123");
        inputBean.setContent(aib);
        // This call expects the service layer to create the missing fortress from the entityInput
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        assertNotNull(result.getMetaKey());
        assertNotNull(trackService.findByCallerRef(su.getCompany(), fortress.getName(), aib.getDocumentType(), aib.getCallerRef()));
    }


    @Test
    public void nullMetaKey() throws Exception {
        SystemUser su = registerSystemUser("nullMetaKey");
        assertNull (trackService.getEntity(su.getCompany(), null));
    }

    @Test
    public void locatingByCallerRefWillThrowAuthorizationException() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("locatingByCallerRefWillThrowAuthorizationException");

        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        // Check we can't create the same entity twice for a given client ref
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
        assertNull("Security - shouldn't be able to see this entity", trackService.findByCallerRef(fortressA, "TestTrack", "123ABC"));

        setSecurity(harry);
        assertNull("User does not belong to this company.Fortress so should not be able to see it",
                trackService.findByCallerRef(suB.getCompany(), fortressA.getCode(), "TestTrack", "ABC123"));

        try {
            assertNull(trackService.getEntity(suB.getCompany(), key));
            fail("Security exception not thrown");

        } catch (SecurityException se) {
            logger.debug("Good stuff!");
        }
    }

    @Test
    public void createEntityTimeLogs() throws Exception {
        SystemUser su = registerSystemUser("createEntityTimeLogs");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("createEntityTimeLogs", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        String ahKey = entity.getMetaKey();

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
            TrackResultBean subsequent = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", ahKey, new DateTime(), Helper.getSimpleMap("blah", i)));

            if ( i == 0){
                waitForFirstLog(su.getCompany(), entity);
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
        SystemUser su = registerSystemUser("noDuplicateLogsWithCompression");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "testDupe", new DateTime(), "ndlwcqw2");
        Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();


        assertNotNull(entity);
        // Irrespective of the order of the fields, we see it as the same.
        //String jsonA = "{\"name\": \"8888\", \"thing\": {\"m\": \"happy\"}}";
        //String jsonB = "{\"thing\": {\"m\": \"happy\"},\"name\": \"8888\"}";

        Map<String, Object> jsonA = Helper.getSimpleMap("name", "8888");
        jsonA.put("thing", Helper.getSimpleMap("m", "happy"));

        Map<String, Object> jsonB = Helper.getSimpleMap("thing", Helper.getSimpleMap("m", "happy"));
        jsonB.put("name", "8888");
        jsonA.put("thing", Helper.getSimpleMap("m", "happy"));



        assertNotNull(trackService.getEntity(su.getCompany(), entity.getMetaKey()));
        assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
        assertNull(fortressService.getFortressUser(fortress, "wallyz", false));
        int i = 0;
        double max = 10d;
        Map<String,Object> json;
        while (i < max) {
            // Same "what" text so should only be one auditLogCount record
            json = (i % 2 == 0 ? jsonA : jsonB);
            mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", entity.getMetaKey(), new DateTime(), json));
            i++;
        }
        assertEquals(1d, (double) trackService.getLogCount(su.getCompany(), entity.getMetaKey()));
        Set<EntityLog> logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
        for (EntityLog entityLog : logs) {
            EntityContent content = kvService.getContent(entity, entityLog.getLog());
            assertNotNull(content);
            assertNotNull(content.getWhat());
            assertFalse(content.getWhat().isEmpty());
        }
    }

    @Test
    public void correctLogCountsReturnedForAFortress() throws Exception {
        SystemUser su = registerSystemUser("correctLogCountsReturnedForAFortress");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getEntity(su.getCompany(), ahKey));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", ahKey, new DateTime(), Helper.getSimpleMap("blah", 0)));
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", ahKey, new DateTime(), Helper.getSimpleMap("blah", 1)));
        assertEquals(2, trackService.getLogCount(su.getCompany(), resultBean.getMetaKey()));
    }

    @Test
    public void testEntityWithLogChange() throws Exception {
        SystemUser su = registerSystemUser("testEntityWithLogChange");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "uouu87");
        inputBean.setName("MikesNameTest");
        ContentInputBean logBean = new ContentInputBean("wally", null, DateTime.now(), Helper.getSimpleMap("blah", 0));
        inputBean.setContent(logBean);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getMetaKey());
        assertEquals("MikesNameTest", resultBean.getEntity().getName());
        assertTrue(resultBean.getEntity().toString().contains(resultBean.getMetaKey()));
        assertEquals(1, trackService.getLogCount(su.getCompany(), resultBean.getMetaKey()));
    }

    @Test
    public void testEntityWithLogChangeTransactional() throws Exception {
        SystemUser su = registerSystemUser("testEntityWithLogChangeTransactional");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest",true));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "232146");
        ContentInputBean logBean = new ContentInputBean("wally", null, DateTime.now(), Helper.getSimpleMap("blah", 0));
        inputBean.setContent(logBean);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        assertNotNull(resultBean.getMetaKey());
        assertEquals(1, trackService.getLogCount(su.getCompany(), resultBean.getMetaKey()));
    }

    @Test
    public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
        setSecurity(mike_admin);
        // Registering the internal admin as a data access user
        SystemUser su = registerSystemUser("updateByCallerRefNoAuditKeyMultipleClients");
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(),true));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        String keyA = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        ContentInputBean alb = new ContentInputBean("logTest", new DateTime(), Helper.getSimpleMap("blah", 0));
        alb.setCallerRef(fortressA.getName(), docType, callerRef);
        //assertNotNull (alb);
        LogResultBean arb = mediationFacade.trackLog(su.getCompany(), alb).getLogResult();
        assertNotNull(arb);
        assertEquals(keyA, arb.getMetaKey());

        // Scenario - create a new data access user
        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser("TWEE", harry);
        // Switch to the data access user
        setSecurity(harry);
        Fortress fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("auditTestB" + System.currentTimeMillis(),true));
        inputBean = new EntityInputBean(fortressB.getName(), "wally", docType, new DateTime(), callerRef);
        String keyB = mediationFacade.trackEntity(suB.getCompany(), inputBean).getMetaKey();

        alb = new ContentInputBean("logTest", new DateTime(), Helper.getSimpleMap("blah", 0));
        alb.setCallerRef(fortressB.getName(), docType, callerRef);
        arb = mediationFacade.trackLog(suB.getCompany(), alb).getLogResult();
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
    public void entitysForDifferentCompaniesAreNotVisible() throws Exception {
        logger.info("## entitysForDifferentCompaniesAreNotVisible");
        SystemUser su = registerSystemUser("entitysForDifferentCompaniesAreNotVisible");
        String hummingbird = "Hummingbird";

        Authentication authMike = setSecurity(mike_admin);
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean entityInputBean = new EntityInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "AHWP");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), entityInputBean).getMetaKey();
        assertNotNull(metaKey);
        assertNotNull(trackService.getEntity(su.getCompany(), metaKey));

        //Hummingbird/Gina
        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser(hummingbird, harry);
        Authentication authHarry = setSecurity(harry); // Harry can create data
        Fortress fortHS = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("honeysuckle", true));
        entityInputBean = new EntityInputBean(fortHS.getName(), "harry", "CompanyNode", new DateTime(), "AHHS");
        String ahHS = mediationFacade.trackEntity(suB.getCompany(), entityInputBean).getMetaKey();

        assertNotNull(fortressService.getFortressUser(fortWP, "wally", true));
        assertNotNull(fortressService.getFortressUser(fortHS, "harry", true));
        assertNull(fortressService.getFortressUser(fortWP, "wallyz", false));

        double max = 2000d;
        StopWatch watch = new StopWatch();
        watch.start();

        createLogRecords(authMike, su, metaKey, "house", 20);
        createLogRecords(authHarry, suB, ahHS, "house", 40);
        watch.stop();
        logger.info(watch.prettyPrint()+ " avg = " + (watch.getLastTaskTimeMillis() / 1000d) / max);


    }

    @Test
    public void lastChangedWorks() throws Exception {
        logger.info ("## lastChangedWorks");
        SystemUser su = registerSystemUser("lastChangedWorks");
        // Create a second log record in order to workout who last change the EntityNode

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fwportfolio",true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity trackKey = trackService.getEntity(su.getCompany(), metaKey);
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", trackKey.getMetaKey(), new DateTime(), Helper.getSimpleMap("house", "house1"), "Update"));
        trackKey = trackService.getEntity(su.getCompany(), metaKey);
        FortressUser fu = fortressService.getUser(trackKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("harry@sunnybell.com", trackKey.getMetaKey(), new DateTime(), Helper.getSimpleMap("house", "house2"), "Update"));
        trackKey = trackService.getEntity(su.getCompany(), metaKey);

        fu = fortressService.getUser(trackKey.getLastUser().getId());
        assertEquals("harry@sunnybell.com", fu.getCode());

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", trackKey.getMetaKey(), new DateTime(), Helper.getSimpleMap("house", "house3"), "Update"));
        trackKey = trackService.getEntity(su.getCompany(), metaKey);

        fu = fortressService.getUser(trackKey.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());

    }

    @Test
    public void outOfSequenceLogsWorking() throws Exception {
        SystemUser su = registerSystemUser("outOfSequenceLogsWorking");
        DateTime dt = new DateTime().toDateTime();
        DateTime earlyDate = dt.minusDays(2);

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio",true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "wally", "CompanyNode", new DateTime(), "ZZZZ");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity entity = trackService.getEntity(su.getCompany(), metaKey);

        // Create the future one first.
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), Helper.getSimpleMap("house", "house1"), "Update"));
        entity = trackService.getEntity(su.getCompany(), metaKey);
        FortressUser fu = fortressService.getUser(entity.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode());
        EntityLog compareLog = logService.getLastLog(entity);

        // Load a historic record. This should not become "last"
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("harry@sunnybell.com", entity.getMetaKey(), earlyDate, Helper.getSimpleMap("house", "house2"), "Update"));
        entity = trackService.getEntity(su.getCompany(), metaKey);

        EntityLog lastLog = logService.getLastLog(entity);
        assertNotNull(lastLog);
        assertEquals(compareLog.getId(), lastLog.getId());

        fu = fortressService.getUser(entity.getLastUser().getId());
        assertEquals("olivia@sunnybell.com", fu.getCode()); // The first one we created is the "last one"


    }

    /**
     * test that we find the correct number of changes between a range of dates for a given entity
     */
    @Test
    public void logDateRangesWorking() throws Exception {
        SystemUser su = registerSystemUser("logDateRangesWorking");

        int max = 10;
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(max);
        DateTime workingDate = firstDate.toDateTime();

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "CompanyNode", firstDate, "123");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        int i = 0;
        while (i < max) {
            workingDate = workingDate.plusDays(1);
            assertEquals("Loop count " + i,
                    ContentInputBean.LogStatus.OK, mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), workingDate, Helper.getSimpleMap("house", "house" + i))).
                    getLogResult().getStatus());
            i++;
        }

        Set<EntityLog> aLogs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(max, aLogs.size());

        EntityLog lastLog = trackService.getLastEntityLog(su.getCompany(), entity.getMetaKey());
        Log lastChange = lastLog.getLog();
        assertNotNull(lastChange);
        assertEquals(workingDate, new DateTime(lastLog.getFortressWhen()));
        entity = trackService.getEntity(su.getCompany(), metaKey);
        assertEquals(max, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));

        DateTime then = workingDate.minusDays(4);
        logger.info("Searching between " + then.toDate() + " and " + workingDate.toDate());
        Set<EntityLog> logs = trackService.getEntityLogs(su.getCompany(), entity.getMetaKey(), then.toDate(), workingDate.toDate());
        assertEquals(5, logs.size());
        Long logId = logs.iterator().next().getId();
        LogDetailBean change = trackService.getFullDetail(su.getCompany(), entity.getMetaKey(), logId);
        assertNotNull(change);
        assertNotNull(change.getLog());
        assertNotNull(change.getWhat());
        assertEquals(logId, change.getLog().getId());


    }

    @Test
    public void cancelLastChangeBehaves() throws Exception {
        // For use in compensating transaction cases only
        // DAT-53
        SystemUser su = registerSystemUser("cancelLastChangeBehaves");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        LogResultBean firstLog  = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), firstDate, Helper.getSimpleMap("house", "house1"))).getLogResult();
        LogResultBean secondLog = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), Helper.getSimpleMap("house", "house2"))).getLogResult();
        assertNotSame(0l, firstLog.getWhatLog().getEntityLog().getFortressWhen());
        assertNotSame(0l, secondLog.getWhatLog().getEntityLog().getFortressWhen());
        Set<EntityLog> logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(2, logs.size());
        entity = trackService.getEntity(su.getCompany(), metaKey);
        compareUser(entity, secondLog.getFortressUser());
        assertEquals(secondLog.getWhatLog().getEntityLog().getFortressWhen(), entity.getFortressDateUpdated());

        // Test block
        trackService.cancelLastLog(fortress.getCompany(), entity);
        logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(1, logs.size());
        entity = trackService.getEntity(su.getCompany(), metaKey, true); // Refresh the entity
        compareUser(entity, firstLog.getFortressUser());
        assertEquals(firstLog.getWhatLog().getEntityLog().getFortressWhen(), entity.getFortressDateUpdated());

        // Last change cancelled
        trackService.cancelLastLog(fortress.getCompany(), entity);
        logs = trackService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertTrue(logs.isEmpty());
    }

    @Test
    public void lastChangeDatesReconcileWithFortressInput() throws Exception {
        SystemUser su = registerSystemUser("lastChangeDatesReconcileWithFortressInput");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC1");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), Helper.getSimpleMap("house", "house1")));
        entity = trackService.getEntity(su.getCompany(), metaKey); // Inflate the entity on the server
        EntityLog lastLog = trackService.getLastEntityLog(su.getCompany(), entity.getMetaKey());
        assertNotNull(lastLog);
//        assertNotNull(lastLog.getAuditChange().getLogInputBean());
        EntityContent content = trackService.getWhat(entity, lastLog.getLog());
        assertNotNull(content);
        assertTrue(content.getWhat().containsKey("house"));
    }

    @Test
    public void dateCreatedAndLastUpdated() throws Exception {
        SystemUser su = registerSystemUser("dateCreatedAndLastUpdated");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime fortressDateCreated = DateTime.now();
        Thread.sleep(500);
        DateTime logTime;
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", fortressDateCreated, "dcalbu");
        ContentInputBean contentInputBean = new ContentInputBean(mike_admin, fortressDateCreated, Helper.getSimpleMap("abx", "1"));
        // Time will come from the Log
        inputBean.setContent(contentInputBean);
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String metaKey = trackResultBean.getMetaKey();

        assertEquals(fortressDateCreated.getMillis(), trackResultBean.getEntity().getFortressDateCreated().getMillis());

        // Creating the 2nd log will advance the last modified time
        logTime = DateTime.now();
        contentInputBean = new ContentInputBean(mike_admin, metaKey, logTime, Helper.getSimpleMap("abx", "2"));
        mediationFacade.trackLog(su.getCompany(), contentInputBean);

        EntityLog log = trackService.getLastEntityLog(su.getCompany(), metaKey);
        assertEquals("Fortress modification date&time do not match", log.getFortressWhen().longValue(), logTime.getMillis());
        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        assertEquals(fortressDateCreated.getMillis(), entity.getFortressDateCreated().getMillis());
        assertEquals("Fortress log time doesn't match", logTime.getMillis(), log.getFortressWhen().longValue());

    }

    @Test
    public void missingLogDateGeneratesSystemDate() throws Exception {
        SystemUser su = registerSystemUser("missingLogDateGeneratesSystemDate");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        DateTime dt = new DateTime().toDateTime();
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", dt, "mldgsd99");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Entity entity = trackService.getEntity(su.getCompany(), metaKey);

        // Check that TimeZone information is used to correctly establish Now when not passed in a log
        // No Date, so default to NOW in the Fortress Timezone
        LogResultBean log = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), null, Helper.getSimpleMap("house", "house1"))).getLogResult();
        logger.info("1 " + new Date(log.getSysWhen()).toString());

        log = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), null, Helper.getSimpleMap("house", "house2"))).getLogResult();
        logger.info("2 " + new Date(log.getSysWhen()).toString());

        Set<EntityLog> logs = trackService.getEntityLogs(entity.getId());
        assertEquals("Logs with missing dates not correctly recorded", 2, logs.size());

        // Same date should still log
        DateTime dateMidnight = new DateTime();
        log = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), dateMidnight.toDateTime(), Helper.getSimpleMap("house", "house3"))).getLogResult();
        logger.info("3 " + new Date(log.getSysWhen()).toString());
        EntityLog thirdLog = trackService.getLastEntityLog(su.getCompany(), metaKey);
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), dateMidnight.toDateTime(), Helper.getSimpleMap("house", "house4")));
        logger.info("4 " + new Date(log.getSysWhen()).toString());
        logs = trackService.getEntityLogs(entity.getId());
        assertEquals(4, logs.size());
        for (EntityLog next : logs) {
            logger.info(next.getId() + " - " + new Date(next.getSysWhen()).toString());
        }
        EntityLog lastLog = trackService.getLastEntityLog(su.getCompany(), metaKey);
        logger.info("L " + new Date(lastLog.getSysWhen()).toString());
        assertNotSame("Last log in should be the last", lastLog.getLog().getId(), thirdLog.getLog().getId());
    }

    @Test
    public void fullEntityDetailsByCallerRef() throws Exception {
        SystemUser su = registerSystemUser("fullEntityDetailsByCallerRef");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "ABC1");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.findByCallerRefFull(fortWP, "CompanyNode", "ABC1");
        assertNotNull(entity);
        assertNotNull(entity.getDocumentType());
        assertEquals(metaKey, entity.getMetaKey());
    }

    @Test
    public void testFortressTimeBoundaries() throws Exception {
        SystemUser su = registerSystemUser("testFortressTimeBoundaries");

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
        Entity entity = trackService.getEntity(su.getCompany(), result);
        DateTime astTime = new DateTime(entity.getFortressDateCreated());

        result = mediationFacade.trackEntity(su.getCompany(), gmtAuditBean).getMetaKey();
        entity = trackService.getEntity(su.getCompany(), result);
        DateTime gmtTime = new DateTime(entity.getFortressDateCreated());

        assertNotSame(astTime.getHourOfDay(), gmtTime.getHourOfDay());

    }

    @Test
    public void entitiesByFortressAndDocType() throws Exception {
        SystemUser su = registerSystemUser("entitiesByFortressAndDocType");

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
        SystemUser suA = registerSystemUser("findEntitiesForCollectionOfMetaKeys", "findEntitiesForCollectionOfMetaKeys");

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

        Collection<Entity>foundEntitys = trackService.getEntities(suA.getCompany(), toFind).values();
        assertEquals("Caller was authorised to find 3 entitys", 3, foundEntitys.size());

        foundEntitys = trackService.getEntities(suB.getCompany(), toFind).values();
        assertEquals("Caller was only authorised to find 1 entity", 1, foundEntitys.size());

    }

    @Test
    public void utf8Strings() throws Exception{
        Map<String, Object> json = Helper.getSimpleMap("Athlete", "Katerina Neumannová") ;
        //getSimpleMap("house", "house1");
        SystemUser su = registerSystemUser("utf8Strings", "utf8Strings");

        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), "ABC1");
        inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), json));
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        waitForFirstLog(su.getCompany(), trackResultBean.getEntity());
        EntityLog lastLog = logService.getLastLog(trackResultBean.getEntity());

        EntityContent content = kvService.getContent(trackResultBean.getEntity(), lastLog.getLog());
        assertEquals(json.get("Athlete"), content.getWhat().get("Athlete"));

        // Second call should say that nothing has changed
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        assertEquals(ContentInputBean.LogStatus.IGNORE, result.getLogResult().getStatus());
    }

    @Test
    public void metaSummaryReturnsLogs() throws Exception {
        SystemUser su = registerSystemUser("metaSummaryReturnsLogs", "metaSummaryReturnsLogs");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entitySummary", true));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), firstDate, Helper.getSimpleMap("house", "house1")));
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), Helper.getSimpleMap("house", "house2")));
        waitForLogCount(su.getCompany(), entity,2);
        EntitySummaryBean entitySummary = trackService.getEntitySummary(su.getCompany(), metaKey);
        assertNotNull(entitySummary);
        assertEquals(metaKey, entitySummary.getEntity().getMetaKey());
        assertNotNull(entitySummary.getEntity().getLastUser());
        assertNotNull(entitySummary.getEntity().getCreatedBy());
        assertNotNull(entitySummary.getEntity().getFortress());
        assertEquals(2, entitySummary.getChanges().size());
        for (EntityLog log : entitySummary.getChanges()) {
            Log change = log.getLog();
            assertNotNull(change.getEvent());
            assertNotNull(change.getWho().getCode());
            EntityContent whatResult = trackService.getWhat(entity, change);
            assertTrue(whatResult.getWhat().containsKey("house"));
        }
    }

    @Test
    public void lastLog_CorrectlySequencesInSeparateCallsViaBatchLoad() throws Exception {
        SystemUser su = registerSystemUser("lastLog_CorrectlySequencesInSeparateCallsViaBatchLoad");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityEntityDiff", true));
        assertFalse(fortress.isSearchActive());
        String callerRef = UUID.randomUUID().toString();
        List<EntityInputBean> inputBeans = new ArrayList<>();

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), callerRef);
        ContentInputBean contentInputBean = new ContentInputBean("mike", new DateTime(), Helper.getSimpleMap("col", 123));
        inputBean.setContent(contentInputBean);
        inputBeans.add(inputBean);
        logger.debug("** First Track Event");
        Collection<TrackResultBean> results = mediationFacade.trackEntities(fortress, inputBeans, 10);
        Entity entity = results.iterator().next().getEntity();
        waitForFirstLog(su.getCompany(), entity);

        inputBeans.clear();

        entity = trackService.findByCallerRef(fortress, "TestTrack", callerRef );
        assertNotNull(entity);

        // Now we record a change
        contentInputBean = new ContentInputBean("mike", new DateTime(), Helper.getSimpleMap("col", 321));
        inputBean.setContent(contentInputBean);
        inputBeans = new ArrayList<>();
        inputBeans.add(inputBean);
        logger.info ("creating {} entitys. Current count = {}", inputBeans.size(), trackService.getLogCount(su.getCompany(), entity.getMetaKey()));

        logger.debug("** Second Track Event");
        mediationFacade.trackEntities(fortress, inputBeans, 1);
        logger.info ("Current count now at {}", trackService.getLogCount(su.getCompany(), entity.getMetaKey()));

        waitForLogCount(su.getCompany(), entity, 2);
        entity = trackService.findByCallerRef(fortress, "TestTrack", callerRef );
        EntityLog lastLog = trackService.getLastEntityLog(su.getCompany(), entity.getMetaKey());
        assertNotNull(lastLog);
        EntityContent what = kvService.getContent(entity, lastLog.getLog());

        assertNotNull(what);
        Object value = what.getWhat().get("col");
        assertNotNull(value);
        assertEquals("321", value.toString());
    }

    @Test
    public void datesInEntitysAndLogs() throws Exception{
        SystemUser su = registerSystemUser("datesInEntitysAndLogs", "datesInEntitysAndLogs");
        FortressInputBean f = new FortressInputBean("dateFun", true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), f);

        DateTime past = new DateTime(2010, 10, 1, 11,35);

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "poppy", "CompanyNode", past, "ABC1");
        inputBean.setContent(new ContentInputBean("poppy", past, Helper.getSimpleMap("name", "value")));
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        waitForFirstLog(su.getCompany(), trackResultBean.getEntity());
        EntityLog lastLog = logService.getLastLog(trackResultBean.getEntity());
        assertEquals(past.getMillis(), lastLog.getFortressWhen().longValue());
        assertEquals(past.getMillis(), trackResultBean.getEntity().getFortressDateCreated().getMillis());
        assertEquals("Modified " + new Date(trackResultBean.getEntity().getLastUpdate()),
                past.getMillis(), trackResultBean.getEntity().getFortressDateUpdated().longValue());

        logger.info(lastLog.toString());
    }
    @Test
    public void abFortressDateFields() throws Exception {
        // DAT-196
        logger.info("## utcDateFields");
        SystemUser su = registerSystemUser("abFortressDateFields", "userDatesFields");
        FortressInputBean fib = new FortressInputBean("utcDateFields", true);
        String timeZone = "Europe/Copenhagen"; // Arbitrary TZ
        fib.setTimeZone(timeZone);
        Fortress fo = fortressService.registerFortress(su.getCompany(), fib);
        DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone));

        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4,30,tz);
        DateTime lastUpdated = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone)));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", fortressDateCreated, "ABC123");
        assertEquals("MetaInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
        inputBean.setContent(new ContentInputBean("wally", lastUpdated, Helper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        Entity entity = result.getEntity();
        assertEquals(EntitySearchSchema.PREFIX+su.getCompany().getCode() + "."+fo.getCode(), entity.getIndexName());
        assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(entity.getFortressDateCreated()));

        EntityLog log = trackService.getLastEntityLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
    }
    @Test
    public void clientInDifferentTZ() throws Exception {
        // DAT-196
        logger.info("## clientInDifferentTZ");
        SystemUser su = registerSystemUser("clientInDifferentTZ", "clienttz");
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
        inputBean.setContent(new ContentInputBean("wally", lastUpdated, Helper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());
        logger.debug("***  problem {}", entity.toString());
        logger.debug("**** Fortress {}, Company {}, Entity Fortress {}", entity.getFortress(), entity.getFortress().getCompany(), result.getEntity().getFortress());
        assertEquals("Why is this failing", EntitySearchSchema.PREFIX+su.getCompany().getCode()+"." + fo.getCode(), entity.getIndexName());
        assertEquals("DateCreated not in Fortress TZ", 0, expectedCreateDate.compareTo(entity.getFortressDateCreated()));

        EntityLog log = trackService.getLastEntityLog(su.getCompany(), result.getMetaKey());
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
        inputBean.setContent(new ContentInputBean("wally", lastUpdated, Helper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(fortress, inputBean); // Mock result as we're not tracking

        fortress = fortressService.findByName(su.getCompany(), fortressBean.getName());
        assertNotNull (fortress);
        assertEquals(fortressTz, fortress.getTimeZone());

        Entity entity = trackService.getEntity(su.getCompany(), result.getMetaKey());
        assertEquals("DateCreated not in Fortress TZ", 0, expectedCreateDate.compareTo(entity.getFortressDateCreated()));

        EntityLog log = trackService.getLastEntityLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
    }
    private void compareUser(Entity entity, String userName) {
        FortressUser fu = fortressService.getUser(entity.getLastUser().getId());
        assertEquals(userName, fu.getCode());

    }

    private void createLogRecords(Authentication auth, SystemUser su, String auditEntity, String key, double recordsToCreate) throws Exception {
        int i = 0;
        SecurityContextHolder.getContext().setAuthentication(auth);
        while (i < recordsToCreate) {
            mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", auditEntity, new DateTime(), Helper.getSimpleMap(key, "house" + i), (String) null));
            i++;
        }
        assertEquals(recordsToCreate, (double) trackService.getLogCount(su.getCompany(), auditEntity));
    }


}
