/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import junit.framework.TestCase;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 1/12/2013
 */
public class TestCallerCode extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(TestCallerCode.class);
    private String monowai = "Monowai";

    @Override
    public void cleanUpGraph() {

        // DAT-348 Overriding the @BeforeTransaction annotation
        super.cleanUpGraph();
    }

    @Test
    public void nullCodeBehaviour() throws Exception {
        try {
//            cleanUpGraph();
            SystemUser su = registerSystemUser(monowai, "nullCallerRefBehaviour");
            setSecurity(su.getLogin());
            FortressInputBean fib = new FortressInputBean("trackTest" + System.currentTimeMillis(), true);
            FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
            // Duplicate null caller ref keys
            EntityInputBean inputBean = new EntityInputBean(fortress, "harry", "TestTrack", new DateTime(), null);
            assertNotNull(mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey());
            inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), null);
            String key = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean).getEntity().getKey();

            assertNotNull(key);
            EntityNode entity = entityService.getEntity(su.getCompany(), key);
            assertNotNull(entity);
            assertNull(entity.getCode());

            assertNotNull("Not found via the key as it was null when entity created.", entityService.findByCode(fortress, "TestTrack", key));
        } finally {
            cleanUpGraph(); // No transaction so need to clear down the graph
        }

    }

    @Test
    public void findByCallerCodeAcrossDocumentTypes() throws Exception {
        try {
            cleanUpGraph();
            SystemUser su = registerSystemUser(monowai, mike_admin);
            FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

            EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");

            // Ok we now have a key, let's find it by code ignoring the document and make sure we find the same entity
            String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
            Iterable<Entity> results = entityService.findByCode(su.getCompany(), fortress.getName(), "ABC123");
            assertEquals(true, results.iterator().hasNext());
            assertEquals(key, results.iterator().next().getKey());

            // Same caller ref but different document - this scenario is the callers to resolve
            inputBean = new EntityInputBean(fortress, "wally", "DocTypeZ", new DateTime(), "ABC123");
            mediationFacade.trackEntity(su.getCompany(), inputBean);

            results = entityService.findByCode(su.getCompany(), fortress.getName(), "ABC123");
            int count = 0;
            // Should be a total of 2, both for the same fortress but different document types
            for (Entity result : results) {
                assertEquals("ABC123", result.getCode());
                count++;
            }
            assertEquals(2, count);
        } finally {
            cleanUpGraph();

        }

    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Entities are not created
     *
     * @throws Exception test failure
     */
//    @Test
    //@Repeat(5)
    public void duplicateCallerRefKeysAndDocTypesNotCreated() throws Exception {
        try {
            cleanUpGraph();
            SystemUser su = registerSystemUser(monowai, "dupex");
            setSecurity();

            FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("zCallerRef" + System.currentTimeMillis(), true));

            String docType = "StressDupez";
            String code = "ABC123X";
            int runnersToRun = 5;
            Collection<CodeRefRunner> runners = new ArrayList<>();

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch latch = new CountDownLatch(runnersToRun);

            for (int i = 0; i < runnersToRun; i++) {
                runners.add(addRunner(fortress, docType, code, latch, startLatch));
            }

            TestCase.assertEquals(runnersToRun, runners.size());
            startLatch.countDown();

            latch.await();

            assertNotNull(entityService.findByCode(fortress, docType, code));
            logger.info("Runner Count {}", runners.size());
            int i = 1;

            for (CodeRefRunner runner : runners) {
                assertEquals("failed to get a good result when checking if the runner worked " + i, true, runner.getWorked());
                i++;
            }
        } finally {
            cleanUpGraph();
        }


    }

    private CodeRefRunner addRunner(Fortress fortress, String docType, String callerRef, CountDownLatch latch, CountDownLatch startLatch) {

        CodeRefRunner runner = new CodeRefRunner(callerRef, docType, fortress, latch, startLatch);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    class CodeRefRunner implements Runnable {
        String docType;
        String code;
        Fortress fortress;
        CountDownLatch latch;
        CountDownLatch startLatch;
        int maxRun = 10;
        boolean worked = false;

        public CodeRefRunner(String callerRef, String docType, Fortress fortress, CountDownLatch latch, CountDownLatch startLatch) {
            this.code = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.latch = latch;
            this.startLatch = startLatch;
            this.worked = false;
        }

        public boolean getWorked() {
            return worked;
        }

        @Override
        public void run() {
            int count = 0;

            try {
                startLatch.await();
                logger.debug("Running... ");
                while (count < maxRun) {
                    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", docType, new DateTime(), code);
                    assert (docType != null);
                    TrackResultBean trackResult = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);
                    assertNotNull(trackResult);
                    assertEquals(code.toLowerCase(), trackResult.getEntity().getCode().toLowerCase());
                    Entity byCode = entityService.findByCode(fortress, docType, code);
                    assertNotNull(byCode);
                    Assert.assertEquals(trackResult.getEntity().getId(), byCode.getId());
                    count++;
                }
                worked = true;
                logger.debug("{} completed", this.toString());

            } catch (ExecutionException | IOException | FlockException e) {
                logger.error("Help!! [" + count + "]", e);
            } catch (InterruptedException e) {
                logger.error("Interrupted [" + count + "]", e);
            } finally {
                latch.countDown();
            }


        }
    }
}
