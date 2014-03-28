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

import com.auditbucket.audit.bean.LogInputBean;
import com.auditbucket.audit.bean.LogResultBean;
import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.bean.TrackResultBean;
import com.auditbucket.audit.model.ChangeLog;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TrackService;
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
    TrackService trackService;

    @Autowired
    MediationFacade auditManager;

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
        MetaInputBean abcHeader = new MetaInputBean(fortressABC.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        abcHeader.setLog(new LogInputBean(null, "charlie", DateTime.now(), escJsonA, true));

        TrackResultBean resultBean = auditManager.createHeader(abcHeader, null);
        LogResultBean logResultBean = resultBean.getLogResult();
        assertNotNull(logResultBean);
        String abcTxRef = logResultBean.getTxReference();
        assertNotNull(abcTxRef);

// CBA data
        SecurityContextHolder.getContext().setAuthentication(authCBA);
        Fortress fortressCBA = fortressService.registerFortress("cbaTest");
        MetaInputBean cbaHeader = new MetaInputBean(fortressCBA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        String cbaKey = auditManager.createHeader(cbaHeader, null).getMetaKey();

        LogInputBean cbaLog = new LogInputBean(cbaKey, "charlie", DateTime.now(), escJsonA, true);
        assertEquals("CBA Logger Not Created", LogInputBean.LogStatus.OK, auditManager.processLog(cbaLog).getStatus());
        String cbaTxRef = cbaLog.getTxRef();
        assertNotNull(cbaTxRef);

        // CBA Caller can not see the ABC transaction
        assertNotNull(trackService.findTx(cbaTxRef));
        assertNull(trackService.findTx(abcTxRef));

        // ABC Caller cannot see the CBA transaction
        SecurityContextHolder.getContext().setAuthentication(authABC);
        assertNotNull(trackService.findTx(abcTxRef));
        assertNull(trackService.findTx(cbaTxRef));

        // WHat happens if ABC tries to use CBA's TX Ref.
        abcHeader = new MetaInputBean(fortressABC.getName(), "wally", "TestAudit", new DateTime(), "ZZZAAA");
        abcHeader.setLog(new LogInputBean(null, "wally", DateTime.now(), escJsonA, null, cbaTxRef));
        TrackResultBean result = auditManager.createHeader(abcHeader, null);
        assertNotNull(result);
        // It works because TX References have only to be unique for a company
        //      ab generated references are GUIDs, but the caller is allowed to define their own transaction
        assertNotNull(trackService.findTx(cbaTxRef));


    }

    @Test
    public void testTxCommits() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        String tagRef = "MyTXTag";
        MetaInputBean aBean = new MetaInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");

        String key = auditManager.createHeader(aBean, null).getMetaKey();
        assertNotNull(key);
        MetaHeader header = trackService.getHeader(key);
        assertNotNull(header);
        //assertEquals(1, header.getTxTags().size());
        LogInputBean alb = new LogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = auditManager.processLog(alb).getTxReference();

        alb = new LogInputBean(key, "harry", DateTime.now(), escJsonB);


        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        auditManager.processLog(alb);
        Map<String, Object> result = trackService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        Collection<ChangeLog> logs = (Collection<ChangeLog>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        // Create a new Logger for a different transaction
        alb = new LogInputBean(key, "mikey", DateTime.now(), escJsonA);
        alb.setTransactional(true);
        assertNull(alb.getTxRef());
        alb.setTxRef("");
        assertNull("Should be Null if it is blank", alb.getTxRef());
        assertTrue(alb.isTransactional());
        LogResultBean arb = auditManager.processLog(alb);
        String txEnd = arb.getTxReference();
        assertNotNull(txEnd);
        assertNotSame(txEnd, txStart);

        result = trackService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        logs = (Collection<ChangeLog>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        result = trackService.findByTXRef(txEnd);
        assertNotNull(result);
        assertEquals(txEnd, result.get("txRef"));
        logs = (Collection<ChangeLog>) result.get("logs");
        assertNotNull(logs);
        assertEquals(1, logs.size());


    }

    @Test
    public void txHeadersTracked() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        String tagRef = "MyTXTag";
        MetaInputBean aBean = new MetaInputBean(fortressA.getName(), "wally", "TestAudit", new DateTime(), "ABC123");

        String key = auditManager.createHeader(aBean, null).getMetaKey();
        assertNotNull(key);
        MetaHeader header = trackService.getHeader(key);
        assertNotNull(header);
        LogInputBean alb = new LogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = auditManager.processLog(alb).getTxReference();

        alb = new LogInputBean(key, "harry", DateTime.now(), escJsonB);

        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        auditManager.processLog(alb);
        // All headers touched by this transaction. ToDo: All changes affected
        Set<MetaHeader> result = trackService.findTxHeaders(txStart);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        for (MetaHeader metaHeader : result) {
            assertNotNull(metaHeader.getMetaKey());
        }


    }
}
