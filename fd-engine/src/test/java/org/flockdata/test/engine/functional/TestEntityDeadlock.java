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
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.joda.time.DateTime;
import org.junit.Before;
import org.springframework.retry.annotation.EnableRetry;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mike on 22/03/15.
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

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Entities are not created
     *
     * @throws Exception
     */
    //@Test
    //@Repeat(value = 1)
    public void entitiesUnderLoad() throws Exception {
        // This test suffered under DAT-348 and was quarantined.
        String companyName = "entitiesUnderLoad";
        setSecurity();
        SystemUser su = registerSystemUser(companyName, "entitiesUnderLoad");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entitiesUnderLoad", true));
        String docType = "entitiesUnderLoad";

        int tagCount = 1; // unique tags per entity - tags are shared across the entities
        int docCount = 1; // how many entities to create per thread
        // Tried reducing threadMax
        int threadMax = 3; // Each thread will create a unique document type
        ArrayList<TagInputBean> tags = getTags(tagCount, false);

        Collection<Tag> createdTags = tagService.findTags(fortress.getCompany(), tags.get(0).getLabel());
        assertEquals("Database is not in a cleared down state", 0, createdTags.size());

        Map<Integer, EntityRunner> runners = new HashMap<>();

        CountDownLatch latch = new CountDownLatch(threadMax);
        CountDownLatch startSignal = new CountDownLatch(1);
        for (int thread = 0; thread < threadMax; thread++) {
            EntityRunner runner = addEntityRunner(thread+1, su, fortress, docType, "ABC" + thread, docCount, tags, latch, startSignal);
            runners.put(thread, runner);
        }
        startSignal.countDown();
        latch.await();
        Long id = null;

        for (int thread = 0; thread < threadMax; thread++) {
            assertEquals("Thread "+ (thread +1), true, runners.get(thread).isWorked());
            for (int count = 0; count < docCount; count++) {
                Entity entity = entityService.findByCallerRef(su.getCompany(), fortress.getName(), docType, "ABC" + thread + "" + count);
                assertNotNull(entity);
                Collection<EntityTag> entityTags = entityTagService.findEntityTags(su.getCompany(), entity);
                assertEquals(tagCount, entityTags.size());
                // Make sure every thread's tags point to the same tag
                if (id == null )
                    id = entityTags.iterator().next().getTag().getId();
                else
                    assertEquals(id, entityTags.iterator().next().getTag().getId());
            }
        }
        assertNotNull(tagService.findTag(fortress.getCompany(), "Deadlock", tags.get(0).getName()));

        createdTags = tagService.findTags(fortress.getCompany(), "Deadlock");
        assertEquals(false, createdTags.isEmpty());
        if (createdTags.size() != tagCount){

            for (Tag createdTag : createdTags) {
                //logger.info(createdTag.toString());
                logger.info("Finding... {}", createdTag.toString() );
                Tag xtra= tagService.findTag(su.getCompany(), createdTag.getLabel(), createdTag.getCode());

                logger.info(xtra.toString());
            }

        }
        assertEquals(tagCount, createdTags.size());

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
        List<EntityInputBean> inputBeans;
        Collection<TagInputBean> tags;
        String apiKey;
        CountDownLatch latch;
        CountDownLatch startSignal;
        int count = 0;
        int myThread;
        Entity entity =null ;

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
            return entity!=null;
        }

        @Override
        public void run() {
            worked = true;
            logger.debug("Running "+myThread);
            try {
                startSignal.await();
                Collection<TrackResultBean> results = mediationFacade.trackEntities(inputBeans, apiKey);
                assertEquals("Error creating entity", 1, results.size());
                entity = results.iterator().next().getEntity();



            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                done = true;
                logger.debug ("*** Done "+myThread +" worked "+worked);
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
