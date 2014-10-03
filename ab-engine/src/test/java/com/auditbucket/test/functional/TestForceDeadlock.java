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
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
public class TestForceDeadlock extends TestEngineBase {

    private Logger logger = LoggerFactory.getLogger(TestForceDeadlock.class);
    @Autowired
    com.auditbucket.track.service.TrackService trackService;

    @Autowired
    RegistrationService regService;

    @Before
    public void setSingleTenanted() {
        engineConfig.setMultiTenanted(false);
    }

    @Test
    public void tagsUnderLoad() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        SystemUser su = registerSystemUser(monowai, "tagsUnderLoad");
        setSecurity();
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(), true));

        CountDownLatch latch = new CountDownLatch(4);
        List<TagInputBean> tags = getTags(10);

        Map<Integer, TagRunner> runners = new HashMap<>();
        int threadMax = 3;
        for (int i = 0; i < threadMax; i++) {
            runners.put(i, addTagRunner(fortress, 5, tags, latch));
        }

        //latch.await();
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
    public void entitiesUnderLoad() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        SystemUser su = registerSystemUser(monowai, mike_admin);
        setSecurity();
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(),true));
        String docType = "TestAuditX";
        Thread.sleep(500);

        //CountDownLatch latch = new CountDownLatch(4);
        ArrayList<TagInputBean> tags = getTags(10);

        Map<Integer, CallerRefRunner> runners = new HashMap<>();
        int threadMax = 15;
        Map<Integer, Future<Collection<TrackResultBean>>> futures = new HashMap<>();
        for (int i = 0; i < threadMax; i++) {
            CallerRefRunner runner = addRunner(fortress, docType, "ABC" + i, 20, tags);
            runners.put(i, runner);
            List<EntityInputBean> inputBeans = runners.get(i).getInputBeans();
            Future<Collection<TrackResultBean>> runResult = mediationFacade.trackEntitiesAsync(su.getCompany(), inputBeans);
            futures.put(i,runResult );
        }

        for (int i = 0; i < threadMax; i++) {
            Future<Collection<TrackResultBean>> future = futures.get(i);
            if (future != null) {
                while (!future.isDone()) {
                    Thread.yield();
                }
                doFutureWorked(future, runners.get(i).getMaxRun());
            }
        }
        assertNotNull(tagService.findTag(fortress.getCompany(), tags.get(0).getName(), tags.get(0).getLabel()));

        Collection<Tag> createdTags = tagService.findTags(fortress.getCompany(), tags.get(0).getLabel());
        assertEquals(false, createdTags.isEmpty());
        assertEquals(10, createdTags.size());
    }

    private ArrayList<TagInputBean> getTags(int tagCount) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            TagInputBean tag = new TagInputBean("tag" + i, "tagRlx" + i);
            tag.setLabel("Deadlock");
            TagInputBean subTag = new TagInputBean("subtag" +i);
            subTag.setLabel("DeadlockSub");
            tag.setTargets("subtag",subTag);
            tags.add(tag);
        }
        return tags;
    }

    private void doFutureWorked(Future<Collection<TrackResultBean>> future, int count) throws Exception {
        while (!future.isDone()) {
            Thread.yield();
        }
        assertEquals(count, future.get().size());

    }

    private CallerRefRunner addRunner(Fortress fortress, String docType, String callerRef, int docCount, ArrayList<TagInputBean> tags) {

        return new CallerRefRunner(callerRef, docType, fortress, tags, docCount);
    }

    private TagRunner addTagRunner(Fortress fortress, int docCount, List<TagInputBean> tags, CountDownLatch latch) {

        TagRunner runner = new TagRunner(fortress, tags, docCount, latch);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    class CallerRefRunner  {
        String docType;
        String callerRef;
        Fortress fortress;
        int maxRun = 30;
        List<EntityInputBean> inputBeans;
        Collection<TagInputBean> tags;

        boolean worked = false;

        public CallerRefRunner(String callerRef, String docType, Fortress fortress, Collection<TagInputBean> tags, int maxRun) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.tags = tags;
            this.maxRun = maxRun;
            inputBeans = new ArrayList<>(maxRun);
        }

        public int getMaxRun() {
            return maxRun;
        }

        public List<EntityInputBean> getInputBeans() {
            int count = 0;
            setSecurity();
            try {
                while (count < maxRun) {
                    EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef + count);
                    inputBean.setTags(tags);
                    inputBeans.add(inputBean);
                    count++;
                }
                worked = true;
            } catch (Exception e) {
                worked = false;
                logger.error("Help!!", e);
            }
            return inputBeans;
        }


    }

    class TagRunner implements Runnable {
        Fortress fortress;
        CountDownLatch latch;
        int maxRun = 30;
        List<TagInputBean> tags;

        boolean worked = false;
        private boolean done;

        public TagRunner(Fortress fortress, List<TagInputBean> tags, int maxRun, CountDownLatch latch) {
            this.fortress = fortress;
            this.latch = latch;
            this.tags = tags;
            this.maxRun = maxRun;
        }

        public boolean isWorked() {
            return worked;
        }

        @Override
        public void run() {
            int count = 0;
            setSecurity();

            try {
                while (count < maxRun) {
                    mediationFacade.createTags(this.fortress.getCompany(), tags);
                    count++;
                }
                worked = true;
            } catch (Exception e) {
                worked = false;
                logger.error("Help!!", e);

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
