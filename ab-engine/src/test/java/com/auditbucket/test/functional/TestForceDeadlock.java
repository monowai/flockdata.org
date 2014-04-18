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

import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.endpoint.TagEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import com.auditbucket.track.bean.MetaInputBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestForceDeadlock {
    @Autowired
    FortressService fortressService;

    @Autowired
    TagService tagService;

    @Autowired
    private TrackEP trackEP;

    @Autowired
    private TagEP tagEP;


    @Autowired
    private Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TestForceDeadlock.class);
    private String mike = "test@ab.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    @Autowired
    TrackService trackService;

    @Autowired
    RegistrationService regService;

    @Before
    public void setSecurity() {
        SecurityContextHolder.getContext().setAuthentication(authMike);
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        if (!"rest".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Test
    public void tagsUnderLoad() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortress = fortressService.registerFortress("auditTest" + System.currentTimeMillis());

        CountDownLatch latch = new CountDownLatch(4);
        List<TagInputBean> tags = getTags(10);

        Map<Integer, TagRunner> runners = new HashMap<>();
        int threadMax = 3;
        boolean worked = true;
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
        assertEquals(true, worked);
    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Headers are not created
     *
     * @throws Exception
     */
    @Test
    public void metaHeaderUnderLoad() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortress = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";

        CountDownLatch latch = new CountDownLatch(4);
        ArrayList<TagInputBean> tags = getTags(10);

        Map<Integer, CallerRefRunner> runners = new HashMap<>();
        int threadMax = 15;
        for (int i = 0; i < threadMax; i++) {
            runners.put(i, addRunner(fortress, docType, "ABC" + i, 20, tags, latch));
        }

        latch.await();
        boolean working = false;
        Map<Integer, Future<Integer>> futures = new HashMap<>();
        String apiKey = fortress.getCompany().getApiKey();
        try {
            for (int i = 0; i < threadMax; i++) {
                futures.put(i, trackEP.trackHeadersAsync(runners.get(i).getInputBeans(), true, apiKey));
            }
            working = true;
        } catch (RuntimeException e) {
            logger.error("rte ", e);
        }
        for (int i = 0; i < threadMax; i++) {
            if (futures.get(i) != null) {
                while (!futures.get(i).isDone()) {
                    Thread.yield();
                }
                doFutureWorked(futures.get(i), runners.get(i).getMaxRun());
            }
        }
        assertEquals(true, working);
        assertNotNull(tagService.findTag(fortress.getCompany(), tags.get(0).getName(), tags.get(0).getIndex()));

        Map<String, Tag> createdTags = tagService.findTags(fortress.getCompany(), tags.get(0).getIndex());
        assertEquals(false, createdTags.isEmpty());
        assertEquals(10, createdTags.size());
    }

    private ArrayList<TagInputBean> getTags(int tagCount) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            TagInputBean tag = new TagInputBean("tag" + i, "tagRlx" + i);
            tag.setIndex("Deadlock");
            TagInputBean subTag = new TagInputBean("subtag" +i);
            subTag.setIndex("DeadlockSub");
            tag.setTargets("subtag",subTag);
            tags.add(tag);
        }
        return tags;
    }

    private void doFutureWorked(Future<Integer> future, int count) throws Exception {
        while (!future.isDone()) {
            Thread.yield();
        }
        assertEquals(count, future.get().intValue());

    }

    private CallerRefRunner addRunner(Fortress fortress, String docType, String callerRef, int docCount, ArrayList<TagInputBean> tags, CountDownLatch latch) {

        CallerRefRunner runner = new CallerRefRunner(callerRef, docType, fortress, tags, docCount, latch);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    private TagRunner addTagRunner(Fortress fortress, int docCount, List<TagInputBean> tags, CountDownLatch latch) {

        TagRunner runner = new TagRunner(fortress, tags, docCount, latch);
        Thread thread = new Thread(runner);
        thread.start();
        return runner;
    }

    class CallerRefRunner implements Runnable {
        String docType;
        String callerRef;
        Fortress fortress;
        CountDownLatch latch;
        int maxRun = 30;
        List<MetaInputBean> inputBeans;
        Collection<TagInputBean> tags;

        boolean worked = false;

        public CallerRefRunner(String callerRef, String docType, Fortress fortress, Collection<TagInputBean> tags, int maxRun, CountDownLatch latch) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.latch = latch;
            this.tags = tags;
            this.maxRun = maxRun;
            inputBeans = new ArrayList<>(maxRun);
        }

        public boolean isWorked() {
            return worked;
        }

        public int getMaxRun() {
            return maxRun;
        }

        public List<MetaInputBean> getInputBeans() {
            return inputBeans;
        }

        @Override
        public void run() {
            int count = 0;
            SecurityContextHolder.getContext().setAuthentication(authMike);
            logger.info("Hello from thread {}, Creating {} MetaHeaders", callerRef, maxRun);
            try {
                while (count < maxRun) {
                    MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef + count);
                    inputBean.setTags(tags);
                    //inputBean.setLog(new LogInputBean("john" + count, null, TestJson.getBigJsonText(count)));

                    inputBeans.add(inputBean);
                    count++;
                }
                worked = true;
            } catch (Exception e) {
                worked = false;
                logger.error("Help!!", e);
            } finally {
                latch.countDown();
            }


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
            SecurityContextHolder.getContext().setAuthentication(authMike);
            logger.info("Hello from TagRunner {}, Creating {} Tags", Thread.currentThread().getName(), maxRun);
            try {
                while (count < maxRun) {
                    tagEP.createTags(tags, null, null);
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

        public List<TagInputBean> getTags() {
            return tags;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }
}
