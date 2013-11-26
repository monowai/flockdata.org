package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
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
import org.junit.*;
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
    private String email = "test@ab.com";
    private Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");

    @Test
    public void testLogWhat() throws Exception {
        //Given
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
        byte[] whatInfos = redisRepo.getValue(auditLog.getAuditChange().getId());
        String whatDecompressed = CompressionHelper.decompress(whatInfos, false);
        Assert.assertNotNull(whatInfos);
        String whatExpected = "{\"blah\":" + 1 + "}";
        Assert.assertEquals(whatDecompressed, whatExpected);
    }

    @Test
    public void testGetWhat() throws Exception {
        //Given
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("Company", email, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = auditManager.createHeader(inputBean).getAuditKey();
        assertNotNull(ahKey);
        AuditHeader header = auditService.getHeader(ahKey);
        auditManager.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + 1 + "}"));
        AuditLog auditLog = auditDAO.getLastAuditLog(header.getId());
        assertNotNull(auditLog);

        //When
        AuditWhat auditWhat = whatService.getWhat(auditLog.getAuditChange());


        //Then
        Assert.assertNotNull(auditWhat);
        String whatExpected = "{\"blah\":" + 1 + "}";
        Assert.assertEquals(auditWhat.getWhat(), whatExpected);
    }

    @Test
    public void testIsSame() throws Exception {

    }

    @Test
    public void testGetDelta() throws Exception {

    }
}
