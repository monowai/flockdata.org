/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.engine.functional;

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.test.annotation.Repeat;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mike on 22/03/15.
 */
public class TestEntityDeadlock extends EngineBase{
    @Override
    public void cleanUpGraph() {
        // DAT-348
        super.cleanUpGraph();
    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Entities are not created
     *
     * @throws Exception
     */
    @Test
    @Repeat(value = 1)
    public void entitiesUnderLoad() throws Exception {
        try {
            // ToDo: This test suffers from DAT-348 and has been quarantined.
            // To me a favour and figure out what's going wrong!

            String companyName = "entitiesUnderLoad";
            setSecurity();
            SystemUser su = registerSystemUser(companyName, "entitiesUnderLoad");

            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entitiesUnderLoad", true));
            String docType = "entitiesUnderLoad";

            int tagCount = 1; // unique tags per entity - tags are shared across the entities
            int docCount = 1; // how many entities to create per thread
            // Tried reducing threadMax
            int threadMax = 20; // Each thread will create a unique document type
            ArrayList<TagInputBean> tags = getTags(tagCount, false);

            Collection<Tag> createdTags = tagService.findTags(fortress.getCompany(), tags.get(0).getLabel());
            assertEquals("Database is not in a cleared down state", 0, createdTags.size());

            Map<Integer, EntityRunner> runners = new HashMap<>();

            CountDownLatch latch = new CountDownLatch(threadMax);
            CountDownLatch startSignal = new CountDownLatch(1);
            for (int thread = 0; thread < threadMax; thread++) {
                EntityRunner runner = addEntityRunner(su, fortress, docType, "ABC" + thread, docCount, tags, latch, startSignal);
                runners.put(thread, runner);
            }
            startSignal.countDown();
            latch.await();

            assertNotNull(tagService.findTag(fortress.getCompany(), tags.get(0).getLabel(), tags.get(0).getName()));

            createdTags = tagService.findTags(fortress.getCompany(), tags.get(0).getLabel());
            assertEquals(false, createdTags.isEmpty());
            assertEquals(tagCount, createdTags.size());

            for (int thread = 0; thread < threadMax; thread++) {
                assertEquals(true, runners.get(thread).isWorked());
                for (int count = 0; count < docCount; count++) {
                    Entity entity = entityService.findByCallerRef(su.getCompany(), fortress.getName(), docType, "ABC" + thread + "" + count);
                    assertNotNull(entity);
                    assertNotNull(su.getCompany());
                    assertEquals(tagCount, entityTagService.findEntityTags(su.getCompany(), entity).size());
                }
            }
        } finally {
            cleanUpGraph(); // No transaction so need to clear down the graph
        }
    }

    private ArrayList<TagInputBean> getTags(int tagCount, boolean addSubTag) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            TagInputBean tag = new TagInputBean("tag" + i, "tagRlx" + i);
            tag.setLabel("Deadlock");
            if (addSubTag) {
                TagInputBean subTag = new TagInputBean("subtag" + i);
                subTag.setLabel("DeadlockSub");
                tag.setTargets("subtag", subTag);
            }
            tags.add(tag);
        }
        return tags;
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
        // Reduce the runs
        int maxRun = 10;
        List<EntityInputBean> inputBeans;
        Collection<TagInputBean> tags;
        String apiKey;
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
            this.apiKey = su.getApiKey();
            inputBeans = new ArrayList<>();
            int count = 0;
            while (count < maxRun) {
                EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef + count);
                inputBean.setTags(tags);
                inputBeans.add(inputBean);
                count++;
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
            worked = false;
            try {
                startSignal.await();
                Thread.yield();
                for (EntityInputBean inputBean : inputBeans) {
                    mediationFacade.trackEntity(inputBean, apiKey);
                }

                worked = true;
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            } finally {
                Thread.yield();
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
}
