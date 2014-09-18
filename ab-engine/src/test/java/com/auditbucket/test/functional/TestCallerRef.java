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

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
public class TestCallerRef extends TestEngineBase {

    private Logger logger = LoggerFactory.getLogger(TestCallerRef.class);
    private String monowai = "Monowai";

    @Test
    public void nullCallerRefBehaviour() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
        SystemUser su = registerSystemUser(monowai, mike_admin);

        FortressInputBean fib = new FortressInputBean("auditTest" + System.currentTimeMillis());
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
        // Duplicate null caller ref keys
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "harry", "TestTrack", new DateTime(), null);
        assertNotNull(mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey());
        inputBean = new EntityInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), null);
        String metaKey = mediationFacade.trackEntity(fortress, inputBean).getMetaKey();

        assertNotNull(metaKey);
        Entity entity = trackService.getEntity(su.getCompany(), metaKey);
        assertNotNull(entity);
        assertNull(entity.getCallerRef());

        assertNotNull("Not found via the metaKey as it was null when entity created.", trackService.findByCallerRef(fortress, "TestTrack", metaKey));

    }

    @Test
    @Transactional
    public void findByCallerRefAcrossDocumentTypes() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");

        // Ok we now have a metaKey, let's find it by callerRef ignoring the document and make sure we find the same entity
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        Iterable<Entity> results = trackService.findByCallerRef(su.getCompany(), fortress.getName(), "ABC123");
        assertEquals(true, results.iterator().hasNext());
        assertEquals(metaKey, results.iterator().next().getMetaKey());

        // Same caller ref but different document - this scenario is the callers to resolve
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        results = trackService.findByCallerRef(su.getCompany(), fortress.getName(), "ABC123");
        int count = 0;
        // Should be a total of 2, both for the same fortress but different document types
        for (Entity result : results) {
            assertEquals("ABC123", result.getCallerRef());
            count ++;
        }
        assertEquals(2, count);

    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Entities are not created
     *
     * @throws Exception
     */
    @Test
    public void duplicateCallerRefKeysAndDocTypesNotCreated() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
        SystemUser su = registerSystemUser(monowai, mike_admin);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis()));

        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        int count =3;
        Collection<CallerRefRunner> runners = new ArrayList <>(count);
        CountDownLatch latch = new CountDownLatch(count);
        for( int i = 0; i< count ; i++){
            runners.add (addRunner(fortress, docType, callerRef, latch));
        }

        latch.await();
        assertNotNull(trackService.findByCallerRef(fortress, docType, callerRef));
        for (CallerRefRunner runner : runners) {
            assertEquals(true, runner.isWorking());
        }


    }

    private CallerRefRunner addRunner(Fortress fortress, String docType, String callerRef, CountDownLatch latch) {

        CallerRefRunner runner = new CallerRefRunner(callerRef, docType, fortress, latch);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    class CallerRefRunner implements Runnable {
        String docType;
        String callerRef;
        Fortress fortress;
        CountDownLatch latch;
        int maxRun = 20;
        boolean working = false;

        public CallerRefRunner(String callerRef, String docType, Fortress fortress, CountDownLatch latch) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.latch = latch;
            this.working = false;
        }

        public boolean isWorking() {
            return working;
        }

        @Override
        public void run() {
            int count = 0;
            setSecurity();
            try {
                while (count < maxRun) {
                    EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef);
                    TrackResultBean trackResult = mediationFacade.trackEntity(fortress, inputBean);
                    assertNotNull(trackResult);
                    assertEquals(callerRef.toLowerCase(), trackResult.getCallerRef().toLowerCase());
                    Entity byCallerRef = trackService.findByCallerRef(fortress, docType, callerRef);
                    assertNotNull(byCallerRef);
                    assertEquals(trackResult.getEntity().getId(), byCallerRef.getId());
                    // disabled as SDN appears to update the metaKey if multiple threads create the same callerKeyRef
                    // https://groups.google.com/forum/#!topic/neo4j/l35zBVUA4eA
//                    assertEquals("Entities don't match!", trackResult.getMetaKey(), byCallerRef.getMetaKey());
                    count++;
                }
                working = true;
                logger.info ("{} completed", this.toString());
            } catch (RuntimeException | ExecutionException | InterruptedException | IOException | FlockException e) {
                logger.error("Help!!", e);
            } finally {
                latch.countDown();
            }


        }
    }
}
