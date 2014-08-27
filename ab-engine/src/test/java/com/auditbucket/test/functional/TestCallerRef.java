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

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.MetaHeader;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

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
    private String mike = "mike";

    @Test
    public void nullCallerRefBehaviour() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));

        FortressInputBean fib = new FortressInputBean("auditTest" + System.currentTimeMillis());
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
        // Duplicate null caller ref keys
        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "harry", "TestTrack", new DateTime(), null);
        Assert.assertNotNull(mediationFacade.createHeader(inputBean, null).getMetaKey());
        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), null);
        String ahKey = mediationFacade.createHeader(fortress.getCompany(), fortress, inputBean).getMetaKey();

        assertNotNull(ahKey);
        MetaHeader metaHeader = trackService.getHeader(ahKey);
        assertNotNull(metaHeader);
        assertNull(metaHeader.getCallerRef());

        // By default this will be found via the header key as it was null when header created.
        assertNotNull(trackService.findByCallerRef(fortress, "TestTrack", ahKey));

    }

    @Test
    @Transactional
    public void findByCallerRefAcrossDocumentTypes() throws Exception {
        registrationEP.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");

        // Ok we now have a metakey, let's find it by callerRef ignoring the document and make sure we find the same thing
        String metaKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();
        Iterable<MetaHeader> results = trackEP.getByCallerRef(fortress.getName(), "ABC123", null, null);
        assertEquals(true, results.iterator().hasNext());
        assertEquals(metaKey, results.iterator().next().getMetaKey());

        // Same caller ref but different document - this scenario is the callers to resolve
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC123");
        trackEP.trackHeader(inputBean, null, null).getBody();

        results = trackEP.getByCallerRef(fortress.getName(), "ABC123", null, null);
        int count = 0;
        // Should be a total of 2, both for the same fortress but different document types
        for (MetaHeader result : results) {
            assertEquals("ABC123", result.getCallerRef());
            count ++;
        }
        assertEquals(2, count);

    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Headers are not created
     *
     * @throws Exception
     */
    @Test
    public void duplicateCallerRefKeysAndDocTypesNotCreated() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
        regService.registerSystemUser(new RegistrationBean(monowai, mike));

        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest" + System.currentTimeMillis()));

        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        int count =3;
        Collection<CallerRefRunner> runners = new ArrayList <>(count);
        CountDownLatch latch = new CountDownLatch(count);
        for( int i = 0; i< count ; i++){
            runners.add (addRunner(fortress, docType, callerRef, latch));
        }

        latch.await();
        Assert.assertNotNull(trackService.findByCallerRef(fortress, docType, callerRef));
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
            logger.info("Hello from thread {}", this.toString());
            try {
                while (count < maxRun) {
                    MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef);
                    TrackResultBean trackResult = mediationFacade.createHeader(fortress.getCompany(), fortress, inputBean);
                    assertNotNull(trackResult);
                    assertEquals(callerRef.toLowerCase(), trackResult.getCallerRef().toLowerCase());
                    MetaHeader byCallerRef = trackService.findByCallerRef(fortress, docType, callerRef);
                    assertNotNull(byCallerRef);
                    assertEquals(trackResult.getMetaHeader().getId(), byCallerRef.getId());
                    // disabled as SDN appears to update the metaKey if multiple threads create the same callerKeyRef
                    // https://groups.google.com/forum/#!topic/neo4j/l35zBVUA4eA
//                    assertEquals("Headers Don't match!", trackResult.getMetaKey(), byCallerRef.getMetaKey());
                    count++;
                }
                working = true;
                logger.info ("{} completed", this.toString());
            } catch (RuntimeException e) {
                logger.error("Help!!", e);
            } catch (DatagioException e) {
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.error("Unexpected", e);
            } finally {
                latch.countDown();
            }


        }
    }
}
