package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
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
import java.util.Date;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 10:41 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestTxReference {
    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    private Neo4jTemplate template;

    String escJsonA = "{\"blah\":1}";
    String escJsonB = "{\"blah\":2}";

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    private String uid = "mike@monowai.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");


    @Test
    public void testAuthorisedToViewTransaction() throws Exception {
        ISystemUser suABC = regService.registerSystemUser(new RegistrationBean("ABC", "mike@monowai.com", "bah"));
        ISystemUser suCBA = regService.registerSystemUser(new RegistrationBean("CBA", "null@monowai.com", "bah"));

        Authentication authABC = new UsernamePasswordAuthenticationToken(suABC.getName(), "user1");
        Authentication authCBA = new UsernamePasswordAuthenticationToken(suCBA.getName(), "user1");

// ABC Data
        IFortress fortressABC = fortressService.registerFortress("abcTest");
        AuditHeaderInputBean abcHeader = new AuditHeaderInputBean(fortressABC.getName(), "wally", "TestAudit", new Date(), "ABC123");
        abcHeader.setAuditLog(new AuditLogInputBean(null, "charlie", DateTime.now(), escJsonA, true));

        abcHeader = auditService.createHeader(abcHeader);
        AuditLogInputBean abcLog = abcHeader.getAuditLog();
        assertNotNull(abcLog);

        assertEquals("ABC Logger Not Created", AuditService.LogStatus.OK, abcLog.getAbStatus());
        String abcTxRef = abcLog.getTxRef();
        assertNotNull(abcTxRef);

// CBA data
        SecurityContextHolder.getContext().setAuthentication(authCBA);
        IFortress fortressCBA = fortressService.registerFortress("cbaTest");
        AuditHeaderInputBean cbaHeader = new AuditHeaderInputBean(fortressCBA.getName(), "wally", "TestAudit", new Date(), "ABC123");
        String cbaKey = auditService.createHeader(cbaHeader).getAuditKey();

        AuditLogInputBean cbaLog = new AuditLogInputBean(cbaKey, "charlie", DateTime.now(), escJsonA, true);
        assertEquals("CBA Logger Not Created", AuditService.LogStatus.OK, auditService.createLog(cbaLog).getAbStatus());
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
        abcHeader = new AuditHeaderInputBean(fortressABC.getName(), "wally", "TestAudit", new Date(), "ZZZAAA");
        abcHeader.setAuditLog(new AuditLogInputBean(null, "wally", DateTime.now(), escJsonA, null, cbaTxRef));
        AuditHeaderInputBean result = auditService.createHeader(abcHeader);
        assertNotNull(result);
        // It works because TX References have only to be unique for a company
        //      ab generated references are GUIDs, but the caller is allowed to define their own transaction
        assertNotNull(auditService.findTx(cbaTxRef));


    }

    @Test
    public void testTxCommits() throws Exception {
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        IFortress fortressA = fortressService.registerFortress("auditTest");
        String tagRef = "MyTXTag";
        AuditHeaderInputBean aBean = new AuditHeaderInputBean(fortressA.getName(), "wally", "TestAudit", new Date(), "ABC123");

        String key = auditService.createHeader(aBean).getAuditKey();
        assertNotNull(key);
        IAuditHeader header = auditService.getHeader(key, true);
        assertNotNull(header);
        //assertEquals(1, header.getTxTags().size());
        AuditLogInputBean alb = new AuditLogInputBean(key, "charlie", DateTime.now(), escJsonA, null, tagRef);
        assertTrue(alb.isTransactional());
        String albTxRef = auditService.createLog(alb).getTxRef();

        alb = new AuditLogInputBean(key, "harry", DateTime.now(), escJsonB);


        alb.setTxRef(albTxRef);
        String txStart = albTxRef;

        auditService.createLog(alb);
        Map<String, Object> result = auditService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        Collection<IAuditLog> logs = (Collection<IAuditLog>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        // Create a new Logger for a different transaction
        alb = new AuditLogInputBean(key, "mikey", DateTime.now(), escJsonA);
        alb.setTransactional(true);
        assertNull(alb.getTxRef());
        alb.setTxRef("");
        assertNull("Should be Null if it is blank", alb.getTxRef());
        assertTrue(alb.isTransactional());
        alb = auditService.createLog(alb);
        String txEnd = alb.getTxRef();
        assertNotNull(txEnd);
        assertNotSame(txEnd, txStart);

        result = auditService.findByTXRef(txStart);
        assertNotNull(result);
        assertEquals(tagRef, result.get("txRef"));
        logs = (Collection<IAuditLog>) result.get("logs");
        assertNotNull(logs);
        assertEquals(2, logs.size());

        result = auditService.findByTXRef(txEnd);
        assertNotNull(result);
        assertEquals(txEnd, result.get("txRef"));
        logs = (Collection<IAuditLog>) result.get("logs");
        assertNotNull(logs);
        assertEquals(1, logs.size());


    }
}
