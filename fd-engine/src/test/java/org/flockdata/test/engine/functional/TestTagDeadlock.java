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

package org.flockdata.test.engine.functional;

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.track.service.EntityService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
public class TestTagDeadlock extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(TestTagDeadlock.class);
    @Autowired
    EntityService entityService;

    @Autowired
    RegistrationService regService;

    @Before
    public void setSingleTenanted() {
        cleanUpGraph();
        engineConfig.setMultiTenanted(false);
        engineConfig.setTestMode(true);
    }

    @Test
    public void tagsUnderLoad() throws Exception {

        try {
            String companyName = "tagsUnderLoad";
            SystemUser su = registerSystemUser(companyName, "tagsUnderLoad");
            setSecurity();
            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tagsUnderLoad", true));
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
            Thread.yield();
            for (int i = 0; i < threadMax; i++) {
                assertEquals("Error occurred creating tags under load", true, runners.get(i).isWorked());
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

    private TagRunner addTagRunner(Fortress fortress, int maxRun, List<TagInputBean> tags, CountDownLatch latch, CountDownLatch startSignal) {
        TagRunner runner = new TagRunner(fortress, tags, maxRun, latch, startSignal);
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
            worked = false;
            try {
                startSignal.await();
                while (count < maxRun) {
                    mediationFacade.createTags(this.fortress.getCompany(), tags);
                    count++;
                }
                worked = true;
                latch.countDown();

            } catch (Exception e) {
                e.getStackTrace();
                latch.countDown();

            }
        }


    }
}
