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

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.service.TrackService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
public class TestForceDeadlock extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(TestForceDeadlock.class);
    @Autowired
    TrackService trackService;

    @Autowired
    RegistrationService regService;

    @Before
    public void setSingleTenanted() {
        cleanUpGraph();
        engineConfig.setMultiTenanted(false);
    }

    @Test
    public void tagsUnderLoad() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        SystemUser su = registerSystemUser(monowai, "tagsUnderLoad");
        setSecurity();
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(), true));
        int tagCount = 1;
        int runCount = 1;
        int threadMax = 10;

        List<TagInputBean> tags = getTags(tagCount, false);

        Map<Integer, TagRunner> runners = new HashMap<>();
        CountDownLatch startSignal = new CountDownLatch(1);

        CountDownLatch latch = new CountDownLatch(threadMax);
        for (int i = 0; i < threadMax; i++) {
            runners.put(i, addTagRunner(fortress, runCount, tags, latch, startSignal));
        }

        startSignal.countDown();
        latch.await();
        for (int i = 0; i < threadMax; i++) {
            while (runners.get(i) == null || !runners.get(i).isDone()) {
                Thread.yield();
            }
            assertEquals("Error occurred creating tags under load", true, runners.get(i).isWorked());
        }
        for (Integer integer : runners.keySet()) {
            assertEquals(true, runners.get(integer).isWorked());
        }
    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Entities are not created
     *
     * @throws Exception
     */
    @Test
    @Repeat(value = 1)
    public void entitiesUnderLoad() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        SystemUser su = registerSystemUser(monowai, mike_admin);
        setSecurity();
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(), true));
        String docType = "entitiesUnderLoad";
        Thread.sleep(500);


        int tagCount = 1; // unique tags per entity - tags are shared across the entities
        int docCount = 1; // how many entities to create per thread
        int threadMax = 35; // Each thread will create a unique document type
        ArrayList<TagInputBean> tags = getTags(tagCount,false);


        Map<Integer, EntityRunner> runners = new HashMap<>();

        CountDownLatch latch = new CountDownLatch(threadMax);
        CountDownLatch startSignal = new CountDownLatch(1);
        for (int thread = 0; thread < threadMax; thread++) {
            EntityRunner runner = addEntityRunner(su, fortress, docType, "ABC" + thread, docCount, tags, latch, startSignal);
            runners.put(thread, runner);
        }
        startSignal.countDown();
        latch.await();
        for (int i = 0; i < threadMax; i++) {
            while (runners.get(i) == null || !runners.get(i).isDone()) {
                Thread.yield();
            }
            assertEquals("Error occurred creating entities under load", true, runners.get(i).isWorked());
        }
        for (Integer integer : runners.keySet()) {
            assertEquals(true, runners.get(integer).isWorked());
        }
        assertNotNull(tagService.findTag(fortress.getCompany(), tags.get(0).getLabel(), tags.get(0).getName()));

        Collection<Tag> createdTags = tagService.findTags(fortress.getCompany(), tags.get(0).getLabel());
        assertEquals(false, createdTags.isEmpty());
        assertEquals(tagCount, createdTags.size());

        for (int thread = 0; thread < threadMax; thread++) {
            for ( int count =0; count < docCount; count ++ ) {
                Entity entity = trackService.findByCallerRef(su.getCompany(), fortress.getName(), docType, "ABC" + thread + "" + count);
                assertNotNull(entity);
                assertEquals(tagCount, entityTagService.findEntityTags(su.getCompany(), entity).size());
            }
        }
    }

    private ArrayList<TagInputBean> getTags(int tagCount, boolean addSubTag) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            TagInputBean tag = new TagInputBean("tag" + i, "tagRlx" + i);
            tag.setLabel("Deadlock");
            if ( addSubTag) {
                TagInputBean subTag = new TagInputBean("subtag" + i);
                subTag.setLabel("DeadlockSub");
                tag.setTargets("subtag", subTag);
            }
            tags.add(tag);
        }
        return tags;
    }

    private TagRunner addTagRunner(Fortress fortress, int maxRun, List<TagInputBean> tags, CountDownLatch latch, CountDownLatch startSignal) {
        TagRunner runner = new TagRunner(fortress, tags, maxRun, latch, startSignal);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    private EntityRunner addEntityRunner(SystemUser su, Fortress fortress, String docType, String callerRef, int docCount, ArrayList<TagInputBean> tags, CountDownLatch latch, CountDownLatch startSignal) {
        EntityRunner runner = new EntityRunner(su, callerRef, docType, fortress, tags, docCount, latch, startSignal);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    class EntityRunner implements Runnable {
        String docType;
        String callerRef;
        Fortress fortress;
        int maxRun = 30;
        List<EntityInputBean> inputBeans;
        Collection<TagInputBean> tags;
        SystemUser su;
        CountDownLatch latch;
        CountDownLatch startSignal;
        int count = 0;

        boolean worked = false;
        private boolean done;

        public EntityRunner(SystemUser su, String callerRef, String docType, Fortress fortress, Collection<TagInputBean> tags, int maxRun, CountDownLatch latch, CountDownLatch startSignal) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.tags = tags;
            this.latch = latch;
            this.startSignal = startSignal;
            this.maxRun = maxRun;
            this.su = su;
            inputBeans = new ArrayList<>();
            int count = 0;
            while (count < maxRun) {
                EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef + count);
                inputBean.setTags(tags);
                inputBeans.add(inputBean);
                count ++;
            }
        }

        public int getMaxRun() {
            return maxRun;
        }

        public boolean isWorked() {
            return worked;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                worked = false;
                for (EntityInputBean inputBean : inputBeans) {
                    mediationFacade.trackEntity(inputBean, su.getApiKey());
                }

                worked = true;
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            } finally {
                done = true;
                latch.countDown();
            }


        }

        public int getCount() {
            return count;
        }

        public boolean isDone() {
            return done;
        }

    }

    class TagRunner implements Runnable {
        Fortress fortress;
        CountDownLatch latch;
        CountDownLatch startSignal;
        int maxRun = 30;
        List<TagInputBean> tags;

        boolean worked = false;
        private boolean done;

        public TagRunner(Fortress fortress, List<TagInputBean> tags, int maxRun, CountDownLatch latch, CountDownLatch startSignal) {
            this.fortress = fortress;
            this.latch = latch;
            this.tags = tags;
            this.maxRun = maxRun;
            this.startSignal = startSignal;
        }

        public boolean isWorked() {
            return worked;
        }

        @Override
        public void run() {
            int count = 0;
            setSecurity();

            try {
                startSignal.await();
                while (count < maxRun) {
                    mediationFacade.createTags(this.fortress.getCompany(), tags);
                    count++;
                }
                worked = true;
            } catch (Exception e) {
                worked = false;

            } finally {
                latch.countDown();
                done = true;
            }
        }

        public boolean isDone() {
            return done;
        }

    }
}
