package com.auditbucket.test.functional;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditResultBean;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestCallerRef {
    @Autowired
    FortressService fortressService;

    @Autowired
    TagService tagService;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private AuditManagerService auditManagerService;

    private Logger logger = LoggerFactory.getLogger(TestCallerRef.class);
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

    @Test
    public void nullCallerRefBehaviour() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));

        Fortress fortress = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        // Duplicate null caller ref keys
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortress.getName(), "harry", "TestAudit", new DateTime(), null);
        auditManagerService.createHeader(inputBean).getAuditKey();
        inputBean = new AuditHeaderInputBean(fortress.getName(), "wally", "TestAudit", new DateTime(), null);
        String ahKey = auditManagerService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        AuditHeader auditHeader = auditService.getHeader(ahKey);
        assertNotNull(auditHeader);
        assertNull(auditHeader.getCallerRef());

        // By default this will be found via the header key as it was null when header created.
        assertNotNull(auditService.findByCallerRef(fortress, "TestAudit", ahKey));

    }

    /**
     * Multi threaded test that tests to make sure duplicate Doc Types and Headers are not created
     *
     * @throws Exception
     */
    @Test
    public void duplicateCallerRefKeysAndDocTypesNotCreated() throws Exception {
        cleanUpGraph(); // No transaction so need to clear down the graph
        regService.registerSystemUser(new RegistrationBean(monowai, mike, "bah"));

        Fortress fortress = fortressService.registerFortress("auditTest" + System.currentTimeMillis());
        String docType = "TestAuditX";
        String callerRef = "ABC123X";

        CountDownLatch latch = new CountDownLatch(3);

        CallerRefRunner ta = addRunner(fortress, docType, callerRef, latch);
        CallerRefRunner tb = addRunner(fortress, docType, callerRef, latch);
        CallerRefRunner tc = addRunner(fortress, docType, callerRef, latch);
        latch.await();
        Assert.assertNotNull(auditService.findByCallerRef(fortress, docType, callerRef));
        assertEquals(true, ta.isWorking());
        assertEquals(true, tb.isWorking());
        assertEquals(true, tc.isWorking());


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
        int maxRun = 20;
        boolean working = false;

        public CallerRefRunner(String callerRef, String docType, Fortress fortress, CountDownLatch latch) {
            this.callerRef = callerRef;
            this.docType = docType;
            this.fortress = fortress;
            this.latch = latch;
            this.working = false;
        }

        public boolean isWorking() {
            return working;
        }

        @Override
        public void run() {
            int count = 0;
            setSecurity();
            logger.info("Hello from thread {}", this.toString());
            try {
                while (count < maxRun) {
                    AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef);
                    AuditResultBean arb;
                    arb = auditManagerService.createHeader(inputBean, fortress.getCompany(), fortress, false);
                    assertNotNull(arb);
                    assertEquals(callerRef.toLowerCase(), arb.getCallerRef().toLowerCase());
                    AuditHeader byCallerRef = auditService.findByCallerRef(fortress, docType, callerRef);
                    assertNotNull(byCallerRef);
                    assertEquals(arb.getAuditKey(), byCallerRef.getAuditKey());
                    count++;
                }
                working = true;
            } catch (RuntimeException e) {

                logger.error("Help!!", e);
            } catch (AuditException e) {

                logger.error("AE Help!!", e);
            } finally {
                latch.countDown();
            }


        }
    }
}
