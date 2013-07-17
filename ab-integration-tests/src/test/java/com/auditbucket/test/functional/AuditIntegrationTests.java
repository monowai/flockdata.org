package com.auditbucket.test.functional;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.junit.Assert;
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

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by IntelliJ IDEA.
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class AuditIntegrationTests {

    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;


    private Logger log = LoggerFactory.getLogger(AuditIntegrationTests.class);
    private String company = "Monowai";
    private String email = "mike@monowai.com";
    private String emailB = "mark@null.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");
    Authentication authB = new UsernamePasswordAuthenticationToken(emailB, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    @Test
    public void testCreateLogHeaderWithSearchActivated(){
        Assert.assertTrue(true);
    }

    @Test
    public void createHeaderTimeLogsWithSearchActivated() throws Exception {

        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        IFortress fo = fortressService.registerFortress(new FortressInputBean("auditTest",true));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        log.info(ahKey);
        Thread.sleep(5000);
//        byte[] docs = alRepo.findOne(auditService.getHeader(ahKey));
//        assertNotNull(docs);
        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditService.findByCallerRef(fo.getId(), "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;
        double max = 10d;
        StopWatch watch = new StopWatch();
        log.info("Start-");
        watch.start();
        while (i < max) {
            AuditLogInputBean auditLogInputBean = auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
        watch.stop();
        log.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);

        // Test that we get the expected number of log events
        assertEquals(max, (double) auditService.getAuditLogCount(ahKey));


    }
}
