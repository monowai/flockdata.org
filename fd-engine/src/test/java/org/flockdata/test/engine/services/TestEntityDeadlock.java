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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.SystemUser;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author mholdsworth
 * @since 22/03/2015
 */

@EnableRetry
public class TestEntityDeadlock extends EngineBase {
    @Override
    @Before
    public void cleanUpGraph() {
        // DAT-348
        logger.debug("Cleaning Graph DB");
        super.cleanUpGraph();
    }

    @Test
    public void doNothing() {
        // Commenting out while Bamboo is failing to create artifacts  despite it being quarantineed
    }


    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Entities are not created
     *
     * @throws Exception
     */
//    @Test
    //@Repeat(value = 1)
    public void entitiesUnderLoad() throws Exception {
        // This test suffered under DAT-348 and was quarantined.
        String companyName = "entitiesUnderLoad";
        setSecurity();
        SystemUser su = registerSystemUser(companyName, "entitiesUnderLoad");

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entitiesUnderLoad", true));
        String docType = "entitiesUnderLoad";

        int tagCount = 1; // unique tags per entity - tags are shared across the entities
        int docCount = 1; // how many entities to create per thread
        // Tried reducing threadMax
        int threadMax = 3; // Each thread will create a unique document type
        ArrayList<TagInputBean> tags = getTags(tagCount, false);

        Collection<Tag> createdTags = tagService.findTags(su.getCompany(), tags.get(0).getLabel());
        assertEquals("Database is not in a cleared down state", 0, createdTags.size());

        Map<Integer, EntityRunner> runners = new HashMap<>();

        CountDownLatch latch = new CountDownLatch(threadMax);
        CountDownLatch startSignal = new CountDownLatch(1);
        for (int thread = 0; thread < threadMax; thread++) {
            EntityRunner runner = addEntityRunner(thread + 1, su, fortress, docType, "ABC" + thread, docCount, tags, latch, startSignal);
            runners.put(thread, runner);
        }
        startSignal.countDown();
        latch.await();
        Tag found = null;

        for (int thread = 0; thread < threadMax; thread++) {
            assertEquals("Thread " + (thread + 1), true, runners.get(thread).isWorked());
            for (int count = 0; count < docCount; count++) {
                //Thread.sleep(2000);
                Entity entity = entityService.findByCode(su.getCompany(), fortress.getName(), docType, "ABC" + thread + "" + count);
                assertNotNull(entity);
                Collection<EntityTag> entityTags = entityTagService.findEntityTags(entity);
                if (entityTags.size() == 0) {
                    logger.debug("Why is this 0?");
                }
                assertEquals(tagCount, entityTags.size());
                // Make sure every thread's tags point to the same tag
                if (found == null) {
                    found = entityTags.iterator().next().getTag();
                } else {
                    assertEquals(found.toString() + " / " + entityTags.iterator().next().getTag().toString(),
                        found.getId(), entityTags.iterator().next().getTag().getId());
                }
            }
        }
        assertNotNull(tagService.findTag(fortress.getCompany(), "Deadlock", null, tags.get(0).getCode()));
        assertEquals(su.getCompany().getName(), fortress.getCompany().getName());
        createdTags = tagService.findTags(su.getCompany(), "Deadlock");
        assertEquals(false, createdTags.isEmpty());
        if (createdTags.size() != tagCount) {

            for (Tag createdTag : createdTags) {
                //logger.info(createdTag.toString());
                logger.info("Finding... {}", createdTag.toString());
                Tag xtra = tagService.findTag(su.getCompany(), createdTag.getLabel(), null, createdTag.getCode());

                logger.info(xtra.toString());
            }

        }
        assertEquals(tagCount, createdTags.size());

    }

    private ArrayList<TagInputBean> getTags(int tagCount, boolean addSubTag) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            TagInputBean tag = new TagInputBean("tag" + i, "Deadlock", "tagRlx" + i);
            if (addSubTag) {
                TagInputBean subTag = new TagInputBean("subtag" + i);
                subTag.setLabel("DeadlockSub");
                tag.setTargets("subtag", subTag);
            }
            tags.add(tag);
        }
        return tags;
    }

    private EntityRunner addEntityRunner(int myThread, SystemUser su, Fortress fortress, String docType, String callerRef, int docCount, ArrayList<TagInputBean> tags, CountDownLatch latch, CountDownLatch startSignal) {
        EntityRunner runner = new EntityRunner(myThread, su, callerRef, docType, fortress, tags, docCount, latch, startSignal);
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
        Collection<EntityInputBean> inputBeans;
        Collection<TagInputBean> tags;
        String apiKey;
        CountDownLatch latch;
        CountDownLatch startSignal;
        int count = 0;
        int myThread;
        String entityKey = null;
        Company company = null;

        boolean worked = false;
        private boolean done;

        public EntityRunner(int myThread, SystemUser su, String callerRef, String docType, Fortress fortress, Collection<TagInputBean> tags, int maxRun, CountDownLatch latch, CountDownLatch startSignal) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.tags = tags;
            this.latch = latch;
            this.startSignal = startSignal;
            this.maxRun = maxRun;
            this.myThread = myThread;
            this.apiKey = su.getApiKey();
            this.company = su.getCompany();
            inputBeans = new ArrayList<>();
            int count = 0;
            while (count < maxRun) {
                EntityInputBean inputBean = new EntityInputBean(fortress, "wally", docType, new DateTime(), callerRef + count);
                inputBean.setTags(tags);
                inputBeans.add(inputBean);
                count++;
            }
        }

        public boolean isWorked() {
            return entityKey != null;
        }

        @Override
        public void run() {
            logger.debug("Running " + myThread);
            try {
                startSignal.await();
                Collection<TrackRequestResult> results = mediationFacade.trackEntities(inputBeans, apiKey);
                assertEquals("Error creating entity", 1, results.size());
                if (entityKey == null) {
                    entityKey = results.iterator().next().getKey();
                }
                assertNotNull(entityService.getEntity(company, entityKey));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                done = true;
                logger.debug("*** Done " + myThread + " worked " + worked);
                latch.countDown();
            }
        }

        public int getCount() {
            return count;
        }

    }
}
