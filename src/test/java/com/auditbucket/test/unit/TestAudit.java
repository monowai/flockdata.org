package com.auditbucket.test.unit;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import java.util.Date;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * User: mike
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestAudit {
    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;

    private Log log = LogFactory.getLog(TestAudit.class);

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");

    @Test
    public void testEscapedJson() {
        String test = JSONObject.escape("{\"name\": \"value\"}");
        log.info(test);
    }

    @Test
    public void testClientRef() {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fortressA = fortressService.registerFortress("auditTest");
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String key = auditService.createHeader(inputBean).getAuditKey();
        // Check we can't create the same header twice for a given client ref
        inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String keyB = auditService.createHeader(inputBean).getAuditKey();
        assertEquals(key, keyB);

        Authentication authB = new UsernamePasswordAuthenticationToken("swagger", "user2");
        SecurityContextHolder.getContext().setAuthentication(authB);
        regService.registerSystemUser(new RegistrationBean("TestTow", "swagger", "bah"));
        IFortress fortressB = fortressService.registerFortress("auditTestB");
        auditService.createHeader(new AuditHeaderInputBean(fortressB.getName(), "wally", "TestAudit", new Date(), "123ABC"));

        SecurityContextHolder.getContext().setAuthentication(authA);

        assertNotNull(auditService.findByName(fortressA.getId(), "TestAudit", "ABC123"));
        assertNotNull(auditService.findByName(fortressA.getId(), "TestAudit", "abc123"));
        assertNull(auditService.findByName(fortressA.getId(), "TestAudit", "123ABC"));
        // Test non external user can't do this
        SecurityContextHolder.getContext().setAuthentication(authB);
        try {
            assertNull(auditService.findByName(fortressA.getId(), "TestAudit", "ABC123"));
            fail("Security exception not thrown");
        } catch (SecurityException se) {

        }

    }

    @Test
    public void testHeader() {

        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditService.findByName(fo.getId(), "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        log.info("Start-");
        watch.start();
        while (i < max) {
            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{blah:" + i + "}"));
            i++;
        }
        watch.stop();
        log.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);

        // Test that we get the expected number of log events
        assertEquals(max, (double) auditService.getAuditLogCount(ahKey));
    }

    /**
     * Idempotent "what" data
     * Ensure duplicate logs are not created when content data has not changed
     */
    @Test
    public void testDupeLog() {

        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "9999");
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));
        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();


        watch.start();
        while (i < max) {
            // Same "what" text so should only be one auditLogCount record
            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{blah: 0}"));
            i++;
        }
        watch.stop();
        assertEquals(1d, (double) auditService.getAuditLogCount(ahKey));
        Set<IAuditLog> logs = auditService.getAuditLogs(ahKey);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.size());
    }

    /**
     * Ensures that the eventtype gets set to the correct default for create and update.
     */
    @Test
    public void testEventType() {

        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "YYY");
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{blah: 0}"));
        IAuditLog log = auditService.getLastChange(ahKey);
        assertNotNull(log);
        assertEquals(IAuditLog.CREATE, log.getEvent()); // log event default

        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{blah: 1}"));
        IAuditLog change = auditService.getLastChange(ahKey);
        assertNotNull(change);
        assertFalse(change.equals(log));
        assertEquals(IAuditLog.UPDATE, change.getEvent());  // log event default
    }

    @Test
    public void testFortressLogCount() {

        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date(), "YYY");
        auditService.createHeader(inputBean);
        String ahKey = inputBean.getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);

        assertNotNull(auditService.getHeader(ahKey));

        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{blah: 0}"));
        auditService.getLastChange(ahKey);
        auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{blah: 1}"));
        // ToDo: How to count the ElasticSearch audit hits. Currently this code is just for exercising the code.
    }


}
