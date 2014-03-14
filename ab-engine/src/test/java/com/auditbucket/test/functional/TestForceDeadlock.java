package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.engine.endpoint.AuditEP;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;

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
    private AuditEP auditEP;

    @Autowired
    private Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TestForceDeadlock.class);
    private String mike = "test@ab.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    @Autowired
    AuditService auditService;

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

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Headers are not created
     *
     * @throws Exception
     */
    @Test
    public void forceDeadlockUnderLoadIsHandled() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph

        String monowai = "Monowai";
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
//        SecurityContextHolder.getContext().setAuthentication(authMike);
        Fortress fortress = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";

        CountDownLatch latch = new CountDownLatch(4);
        ArrayList<TagInputBean>tags = getTags(10, 20);

        Map<Integer,CallerRefRunner> runners = new HashMap<>();
        int threadMax = 15;
        for (int i = 0; i <threadMax ; i++) {
            runners.put(i, addRunner(fortress, docType, "ABC" + i, 20, tags, latch));
        }

        latch.await();
        boolean working = false;
        Map<Integer,Future<Integer>>futures = new HashMap<>();

        try {
            for (int i = 0; i <threadMax ; i++) {
                futures.put(i, auditEP.createHeadersF(runners.get(i).getInputBeans(), false));
                //futures.put(i, auditEP.createHeadersF(runners.get(i).getInputBeans(), false));
            }
            working = true;
        } catch (RuntimeException e) {
            logger.error("rte ", e);
        }
        for (int i = 0; i <threadMax ; i++) {
            while ( futures.get(i).get() == null ){
                Thread.yield();
            }
            doFutureWorked(futures.get(i), runners.get(i).getMaxRun());
        }
        assertEquals(true, working);


    }

    private ArrayList<TagInputBean> getTags(int auditTag, int regTag) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (int i = 0; i < auditTag; i++) {
            tags.add(new TagInputBean("audittag"+i, "tagRlx"+i));
        }

        for (int i = 0; i < regTag; i++) {
            TagInputBean tag = new TagInputBean("tag"+i);
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

    class CallerRefRunner implements Runnable {
        String docType;
        String callerRef;
        Fortress fortress;
        CountDownLatch latch;
        int maxRun = 30;
        AuditHeaderInputBean inputBeans[] ;
        Collection<TagInputBean> tags;

        boolean worked = false;

        public CallerRefRunner(String callerRef, String docType, Fortress fortress, Collection<TagInputBean> tags, int maxRun, CountDownLatch latch) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.latch = latch;
            this.tags = tags;
            this.maxRun= maxRun;
            inputBeans = new AuditHeaderInputBean[maxRun];
        }

        public int getMaxRun() {
            return maxRun;
        }

        public AuditHeaderInputBean[] getInputBeans() {
            return inputBeans;
        }

        public boolean isWorked() {
            return worked;
        }

        @Override
        public void run() {
            int count = 0;
            Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
            logger.info("Hello from thread {}, Creating {} AuditHeaders", callerRef, maxRun);
            try {
                while (count < maxRun) {
                    AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef + count);
                    inputBean.setTags(tags);
                    inputBeans[count] = inputBean;
                    count++;
                }
                worked = true;
            } catch (Exception e) {

                logger.error("Help!!", e);
            } finally {
                latch.countDown();
            }


        }
    }
}
