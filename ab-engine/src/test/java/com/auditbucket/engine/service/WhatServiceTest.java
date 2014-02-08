package com.auditbucket.engine.service;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.test.utils.AbstractRedisSupport;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class WhatServiceTest extends AbstractRedisSupport {

    @Autowired
    RedisRepo redisRepo;
    @Autowired
    Neo4jTemplate template;
    @Autowired
    AuditService auditService;
    @Autowired
    RegistrationService regService;
    @Autowired
    FortressService fortressService;
    @Autowired
    AuditManagerService auditManager;
    @Autowired
    AuditDao auditDAO;
    @Autowired
    private WhatService whatService;

    @Autowired
    private EngineConfig engineConfig;

    private String email = "test@ab.com";
    private Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");

    @Test
    public void whatLogFromRedis() throws Exception {
        //Given
        engineConfig.setKvStore("REDIS");
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("Company", email, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = auditManager.createHeader(inputBean).getAuditKey();
        assertNotNull(ahKey);
        AuditHeader header = auditService.getHeader(ahKey);

        //When
        auditManager.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + 1 + "}"));

        //Then
        AuditLog auditLog = auditDAO.getLastAuditLog(header.getId());

        assertNotNull(auditLog);
        byte[] whatInfos = redisRepo.getValue(header,auditLog.getAuditChange().getId());
        String whatDecompressed = CompressionHelper.decompress(whatInfos, false);
        Assert.assertNotNull(whatInfos);
        String whatExpected = "{\"blah\":" + 1 + "}";
        Assert.assertEquals(whatDecompressed, whatExpected);
        auditService.cancelLastLogSync(ahKey);
        Assert.assertNull(auditService.getLastAuditLog(header));
        Assert.assertNull(whatService.getWhat(header, auditLog.getAuditChange()).getWhat());

    }

    @Test
    public void getWhatFromRedis() throws Exception {
        engineConfig.setKvStore("REDIS");
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("Company", email, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123Z";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = auditManager.createHeader(inputBean).getAuditKey();
        assertNotNull(ahKey);
        AuditHeader header = auditService.getHeader(ahKey);
        auditManager.createLog(header, new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + 1 + "}"));
        AuditLog auditLog = auditDAO.getLastAuditLog(header.getId());
        assertNotNull(auditLog);

        //When
        AuditWhat auditWhat = whatService.getWhat(header, auditLog.getAuditChange());

        //Then
        Assert.assertNotNull(auditWhat);
        String whatExpected = "{\"blah\":" + 1 + "}";
        Assert.assertNotNull(auditWhat.getWhat());
        Assert.assertEquals(auditWhat.getWhat(), whatExpected);
        Assert.assertTrue(whatService.isSame(header, auditLog.getAuditChange(), whatExpected));
        auditService.cancelLastLogSync(ahKey);
        Assert.assertNull(auditService.getLastAuditLog(header));
        Assert.assertNull(whatService.getWhat(header, auditLog.getAuditChange()).getWhat());
    }

    @Test
    public void getWhatFromRiak() throws Exception {
        engineConfig.setKvStore("RIAK");
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("Company", email, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = auditManager.createHeader(inputBean).getAuditKey();
        assertNotNull(ahKey);
        AuditHeader header = auditService.getHeader(ahKey);
        auditManager.createLog(header, new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + 1 + "}"));
        AuditLog auditLog = auditDAO.getLastAuditLog(header.getId());
        assertNotNull(auditLog);

        //When
        AuditWhat auditWhat = whatService.getWhat(header, auditLog.getAuditChange());

        //Then
        Assert.assertNotNull(auditWhat);
        String whatExpected = "{\"blah\":" + 1 + "}";
        Assert.assertEquals(auditWhat.getWhat(), whatExpected);
        Assert.assertTrue(whatService.isSame(header, auditLog.getAuditChange(), whatExpected));
        // Testing that cancel works
        auditService.cancelLastLogSync(ahKey);
        Assert.assertNull(auditService.getLastAuditLog(header));
        Assert.assertNull(whatService.getWhat(header, auditLog.getAuditChange()).getWhat());

    }
    @Test
    public void testIsSame() throws Exception {

    }

    @Test
    public void testGetDelta() throws Exception {

    }
}
