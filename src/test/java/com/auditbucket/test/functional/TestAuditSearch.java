package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.service.AuditSearchService;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:35 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestAuditSearch {
    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    AuditSearchService searchService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;

    ObjectMapper om = new ObjectMapper();

    private Logger log = LoggerFactory.getLogger(TestAuditSearch.class);

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

    @org.junit.Test
    public void testSearchKeysForNonAccumulatingFortresses() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fo = fortressService.registerFortress(new FortressInputBean("testSearchCancel", false));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
        inputBean.setAuditLog(new AuditLogInputBean("wally", new DateTime(), "{\"blah\":" + 0 + "}"));
        String ahKey = auditService.createHeader(inputBean).getAuditKey();

        assertNotNull(ahKey);
        IAuditHeader auditHeader = auditService.getHeader(ahKey);
        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditHeader.getSearchKey());

        int i = 1;
        int max = 10;
        while (i < max) {
            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
        Set<IAuditLog> logs = auditService.getAuditLogs(ahKey);
        Iterator<IAuditLog> it = logs.iterator();
        assertNotNull(logs);
        assertEquals(max, logs.size());
        while (it.hasNext()) {
            IAuditLog next = it.next();
            assertNull(next.getSearchKey());
        }
        byte[] parent = searchService.findOne(auditHeader, auditHeader.getSearchKey());

        assertNotNull(parent);
        Map<String, Object> ac = om.readValue(parent, Map.class);
        assertNotNull(ac);
        assertEquals(auditHeader.getAuditKey(), ac.get("auditKey"));
        assertEquals("wally", ac.get("who"));
        assertEquals(max - 1, ac.get("blah"));

        // Test that we synchronise correctly when cancelling
        i = max - 1;
        while (i > 0) {
            auditService.cancelLastLog(ahKey);
            parent = searchService.findOne(auditHeader);
            ac = om.readValue(parent, Map.class);
            assertEquals(i - 1, ac.get("blah"));
            i--;
        }

    }
}
