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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.flockdata.data.Fortress;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.junit.Before;
import org.junit.Test;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.annotation.Repeat;

/**
 * @author mholdsworth
 * @since 1/12/2013
 */
@EnableRetry
public class TestTagDeadlock extends EngineBase {


    @Override
    public void cleanUpGraph() {
        // DAT-348
        logger.debug("Cleaning up graph");
        super.cleanUpGraph();
    }


    @Before
    public void setSingleTenanted() {
        cleanUpGraph();
        engineConfig.setMultiTenanted(false);
        engineConfig.setTestMode(true);
    }

    @Test
    public void testDisabled() {
        // DAT-422 -
    }

    //
    @Repeat(value = 1)
    public void tagsUnderLoad() throws Exception {

        try {

            String companyName = "tagsUnderLoad";
            SystemUser su = registerSystemUser(companyName, "tagsUnderLoad");
            setSecurity();
            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tagsUnderLoad", true));
            int tagCount = 1;
            int runCount = 1;
            int threadMax = 4;

            List<TagInputBean> tags = getTags(tagCount, false);

            Map<Integer, TagRunner> runners = new HashMap<>();
            CountDownLatch startSignal = new CountDownLatch(1);

            CountDownLatch latch = new CountDownLatch(threadMax);
            for (int i = 0; i < threadMax; i++) {
                runners.put(i, addTagRunner(i + 1, fortress, runCount, tags, latch, startSignal));
            }

            startSignal.countDown();
            latch.await();
            Thread.yield();
            for (int i = 0; i < threadMax; i++) {
                assertEquals("Thread " + i + 1, true, runners.get(i).isWorked());
            }
            for (Integer integer : runners.keySet()) {
                assertEquals(true, runners.get(integer).isWorked());
            }
        } finally {
            cleanUpGraph(); // No transaction so need to clear down the graph
        }
    }

    private ArrayList<TagInputBean> getTags(int tagCount, boolean addSubTag) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            TagInputBean tag = new TagInputBean("tag" + i, "DeadlockTag", "tagRlx" + i);
            if (addSubTag) {
                TagInputBean subTag = new TagInputBean("subtag" + i);
                subTag.setLabel("DeadlockSubTag");
                tag.setTargets("subtag", subTag);
            }
            tags.add(tag);
        }
        return tags;
    }

    private TagRunner addTagRunner(int tCount, Fortress fortress, int maxRun, List<TagInputBean> tags, CountDownLatch latch, CountDownLatch startSignal) {
        TagRunner runner = new TagRunner(tCount, fortress, tags, maxRun, latch, startSignal);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    class TagRunner implements Runnable {
        Fortress fortress;
        CountDownLatch latch;
        CountDownLatch startSignal;
        int maxRun = 30;
        List<TagInputBean> tags;
        boolean worked = false;
        private int myThread;

        public TagRunner(int myThread, Fortress fortress, List<TagInputBean> tags, int maxRun, CountDownLatch latch, CountDownLatch startSignal) {
            this.fortress = fortress;
            this.latch = latch;
            this.tags = tags;
            this.maxRun = maxRun;
            this.startSignal = startSignal;
            this.myThread = myThread;
        }

        public boolean isWorked() {
            return worked;
        }

        @Override
        public void run() {
            int count = 0;
            try {
                setSecurity();
                worked = false;
                logger.debug("Thread " + myThread);
                startSignal.await();
                while (count < maxRun) {
                    mediationFacade.createTags(this.fortress.getCompany(), tags);
                    count++;
                }
                worked = true;

            } catch (Exception e) {
                logger.info("Tag Thread Run error", e);

            } finally {
                logger.debug("*** Finally " + myThread + " worked = " + worked);
                latch.countDown();
            }

        }


    }
}
