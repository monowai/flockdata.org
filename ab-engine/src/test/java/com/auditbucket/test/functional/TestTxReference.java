/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditLogResultBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 10:41 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestTxReference {
    @Autowired
    AuditService auditService;

    @Autowired
    AuditManagerService auditManager;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;

    private String escJsonA = "{\"blah\":1}";
    private String escJsonB = "{\"blah\":2}";

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    private String uid = "mike@monowai.com";
    private Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");


    @Test
    public void testAuthorisedToViewTransaction() throws Exception {
        SystemUser suABC = regService.registerSystemUser(new RegistrationBean("ABC", "mike@monowai.com", "bah"));
        SystemUser suCBA = regService.registerSystemUser(new RegistrationBean("CBA", "null@monowai.com", "bah"));

        Authentication authABC = new UsernamePasswordAuthenticationToken(suABC.getName(), "user1");
        Authentication authCBA = new UsernamePasswordAuthenticationToken(suCBA.getName(), "user1");

// ABC Data
        Fortress fortressABC = fortressService.registerFortress("abcTest");
        AuditHeaderInputBean abcHeader = new AuditHeaderInputBean(fortressABC.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        abcHeader.setAuditLog(new AuditLogInputBean(null, "charlie", DateTime.now(), escJsonA, true));

        AuditResultBean resultBean = auditManager.createHeader(abcHeader);
        AuditLogResultBean logResultBean = resultBean.getLogResult();
        assertNotNull(logResultBean);
        String abcTxRef = logResultBean.getTxReference();
        assertNotNull(abcTxRef);

// CBA data
        SecurityContextHolder.getContext().setAuthentication(authCBA);
        Fortress fortressCBA = fortressService.registerFortress("cbaTest");
        AuditHeaderInputBean cbaHeader = new AuditHeaderInputBean(fortressCBA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String cbaKey = auditManager.createHeader(cbaHeader).getAuditKey();

        AuditLogInputBean cbaLog = new AuditLogInputBean(cbaKey, "charlie", DateTime.now(), escJsonA, true);
        assertEquals("CBA Logger Not Created", AuditLogInputBean.LogStatus.OK, auditManager.createLog(cbaLog).getStatus());
        String cbaTxRef = cbaLog.getTxRef();
        assertNotNull(cbaTxRef);

        // CBA Caller can not see the ABC transaction
        assertNotNull(auditService.findTx(cbaTxRef));
        assertNull(auditService.findTx(abcTxRef));

        // ABC Caller cannot see the CBA transaction
        SecurityContextHolder.getContext().setAuthentication(authABC);
        assertNotNull(auditService.findTx(abcTxRef));
        assertNull(auditService.findTx(cbaTxRef));

        // WHat happens if ABC tries to use CBA's TX Ref.
        abcHeader = new AuditHeaderInputBean(fortressABC.getName(), "wally", "TestAudit", new DateTime(), "ZZZAAA");
        abcHeader.setAuditLog(new AuditLogInputBean(null, "wally", DateTime.now(), escJsonA, null, cbaTxRef));
        AuditResultBean result = auditManager.createHeader(abcHeader);
        assertNotNull(result);
        // It works because TX References have only to be unique for a company
        //      ab generated references are GUIDs, but the caller is allowed to define their own transaction
        assertNotNull(auditService.findTx(cbaTxRef));


    }

    @Test
    public void testTxCommits() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        String tagRef = "MyTXTag";
        AuditHeaderInputBean aBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");

        String key = auditManager.createHeader(aBean).getAuditKey();
        assertNotNull(key);
        AuditHeader header = auditService.getHeader(key);
        assertNotNull(header);
        //assertEquals(1, header.getTxTags().size());
        AuditLogInputBean alb = new AuditLogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = auditManager.createLog(alb).getTxReference();

        alb = new AuditLogInputBean(key, "harry", DateTime.now(), escJsonB);


        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        auditManager.createLog(alb);
        Map<String, Object> result = auditService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        Collection<AuditChange> logs = (Collection<AuditChange>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        // Create a new Logger for a different transaction
        alb = new AuditLogInputBean(key, "mikey", DateTime.now(), escJsonA);
        alb.setTransactional(true);
        assertNull(alb.getTxRef());
        alb.setTxRef("");
        assertNull("Should be Null if it is blank", alb.getTxRef());
        assertTrue(alb.isTransactional());
        AuditLogResultBean arb = auditManager.createLog(alb);
        String txEnd = arb.getTxReference();
        assertNotNull(txEnd);
        assertNotSame(txEnd, txStart);

        result = auditService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        logs = (Collection<AuditChange>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        result = auditService.findByTXRef(txEnd);
        assertNotNull(result);
        assertEquals(txEnd, result.get("txRef"));
        logs = (Collection<AuditChange>) result.get("logs");
        assertNotNull(logs);
        assertEquals(1, logs.size());


    }

    @Test
    public void txHeadersTracked() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        String tagRef = "MyTXTag";
        AuditHeaderInputBean aBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");

        String key = auditManager.createHeader(aBean).getAuditKey();
        assertNotNull(key);
        AuditHeader header = auditService.getHeader(key);
        assertNotNull(header);
        AuditLogInputBean alb = new AuditLogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = auditManager.createLog(alb).getTxReference();

        alb = new AuditLogInputBean(key, "harry", DateTime.now(), escJsonB);

        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        auditManager.createLog(alb);
        Set<AuditHeader> result = auditService.findTxHeaders(txStart);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        for (AuditHeader auditHeader : result) {
            assertNotNull(auditHeader.getAuditKey());
        }


    }
}
