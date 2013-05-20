package com.auditbucket.test.unit;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditInputBean;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.time.StopWatch;
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


    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(auth);
        Neo4jHelper.cleanDb(template);
    }

    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "user1");

    @Test
    public void testEscapedJson() {
        String test = JSONObject.escape("{\"name\": \"value\"}");
        System.out.println(test);
    }

    @Test
    public void testHeader() {

        ISystemUser su = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        String ahKey = auditService.createHeader(new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date()));

        assertNotNull(ahKey);
        System.out.println(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        System.out.println("Start-");
        watch.start();
        while (i < max) {
            auditService.createLog(new AuditInputBean(ahKey, "wally", new DateTime().toString(), "{blah:" + i + "}"));
            i++;
        }
        watch.stop();
        System.out.println("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);

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

        String ahKey = auditService.createHeader(new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date()));

        assertNotNull(ahKey);
        System.out.println(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));
        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();


        watch.start();
        while (i < max) {
            // Same "what" text so should only be one auditLogCount record
            auditService.createLog(new AuditInputBean(ahKey, "wally", new DateTime().toString(), "{blah: 0}"));
            i++;
        }
        watch.stop();
        assertEquals(1d, (double) auditService.getAuditLogCount(ahKey));
    }

    /**
     * Ensures that the eventtype gets set to the correct default for create and update.
     */
    @Test
    public void testEventType() {

        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress("auditTest");

        String ahKey = auditService.createHeader(new AuditHeaderInputBean(fo.getName(), "wally", "testDupe", new Date()));

        assertNotNull(ahKey);
        System.out.println(ahKey);

        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        auditService.createLog(new AuditInputBean(ahKey, "wally", new DateTime().toString(), "{blah: 0}"));
        IAuditLog log = auditService.getLastChange(ahKey);
        assertNotNull(log);
        assertEquals(IAuditLog.CREATE, log.getEvent()); // log event default

        auditService.createLog(new AuditInputBean(ahKey, "wally", new DateTime().toString(), "{blah: 1}"));
        IAuditLog change = auditService.getLastChange(ahKey);
        assertNotNull(change);
        assertFalse(change.equals(log));
        assertEquals(IAuditLog.UPDATE, change.getEvent());  // log event default
    }
}
