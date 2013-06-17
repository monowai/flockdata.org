package com.auditbucket.test.unit;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
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
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 10:41 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestTags {
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
    public void testTxTags() {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        ITagRef txTag = auditService.beginTransaction();
        assertNotNull(txTag);
        // commitTransaction (AuditHeader)
        // getChanges (txTag)

    }

    @Test
    public void testTags() {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fortressA = fortressService.registerFortress("auditTest");
        String tagRef = "MyTXTag";
        AuditHeaderInputBean aBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new Date(), "ABC123", tagRef);

        String key = auditService.createHeader(aBean).getAuditKey();
        assertNotNull(key);
        IAuditHeader header = auditService.getHeader(key, true);
        assertNotNull(header);
        assertEquals(1, header.getTxTags().size());
        AuditLogInputBean alb = new AuditLogInputBean(key, "charlie", DateTime.now(), "some change");
        alb.setTxRef(aBean.getTxRef());
        auditService.createLog(alb);
        alb = new AuditLogInputBean(key, "harry", DateTime.now(), "some other");
        alb.setTxRef(aBean.getTxRef());
        auditService.createLog(alb);
        Map<String, Object> result = auditService.findByTXRef(alb.getTxRef());
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        Object o = result.get("logs");
        assertNotNull(o);
        Set<IAuditChange> changes = (Set<IAuditChange>) o;
        assertEquals(2, changes.size());


        //auditService.getAuditLogs(key, aBean.getTxRef());


    }
}
