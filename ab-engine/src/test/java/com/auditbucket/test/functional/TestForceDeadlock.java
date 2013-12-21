package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.engine.endpoint.AuditEP;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import junit.framework.Assert;
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
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private AuditManagerService auditManagerService;

    private Logger logger = LoggerFactory.getLogger(TestForceDeadlock.class);
    private String monowai = "Monowai";
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
    public void forceDeadlockUnderLoadIsHandled() throws InterruptedException {
        cleanUpGraph(); // No transaction so need to clear down the graph

        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortress = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";

        CountDownLatch latch = new CountDownLatch(3);

        CallerRefRunner ta = addRunner(fortress, docType, "123", latch);
        CallerRefRunner tb = addRunner(fortress, docType, "124", latch);
        CallerRefRunner tc = addRunner(fortress, docType, "125", latch);
        CallerRefRunner td = addRunner(fortress, docType, "126", latch);
        latch.await();
        boolean working = false;
        try {

            auditEP.createHeadersF(ta.getInputBeans(), false);
            auditEP.createHeadersF(tb.getInputBeans(), false);
            auditEP.createHeadersF(tc.getInputBeans(), true);
            // Waiting for the async result to finish...
            auditEP.createHeadersF(td.getInputBeans(), true);
//            Thread.sleep(5000);
            working = true;
        } catch (RuntimeException e) {
            logger.error("rte ", e);
        } catch (AuditException e) {

            logger.error("ae ", e);
        }

        assertEquals(true, working);


    }

    private CallerRefRunner addRunner(Fortress fortress, String docType, String callerRef, CountDownLatch latch) {

        CallerRefRunner runA = new CallerRefRunner(callerRef, docType, fortress, latch);
        Thread tA = new Thread(runA);
        tA.start();
        return runA;
    }

    class CallerRefRunner implements Runnable {
        String docType;
        String callerRef;
        Fortress fortress;
        CountDownLatch latch;
        int maxRun = 30;
        AuditHeaderInputBean inputBeans[] = new AuditHeaderInputBean[maxRun];
        Map<String, Object> tagMap = new HashMap<>();

        boolean working = false;

        public CallerRefRunner(String callerRef, String docType, Fortress fortress, CountDownLatch latch) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.latch = latch;
            this.working = false;
            tagMap.put("tagA", null);
            tagMap.put("tagB", null);
//            tagMap.put ("tagC", null);
//            tagMap.put ("tagD", null);
//            tagMap.put ("tagE", null);
//            tagMap.put ("tagF", null);

        }

        public int getMaxRun() {
            return maxRun;
        }

        public AuditHeaderInputBean[] getInputBeans() {
            return inputBeans;
        }

        public boolean isWorking() {
            return working;
        }

        @Override
        public void run() {
            int count = 0;

            logger.info("Hello from thread {}", this.toString());
            try {
                while (count < maxRun) {
                    AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef + count);
                    inputBean.setTagValues(tagMap);
                    inputBeans[count] = inputBean;
                    count++;
                }
                working = true;
            } catch (RuntimeException e) {

                logger.error("Help!!", e);
            } finally {
                latch.countDown();
            }


        }
    }
}
