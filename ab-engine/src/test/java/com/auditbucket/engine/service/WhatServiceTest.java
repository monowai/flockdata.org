package com.auditbucket.engine.service;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.AuditWhat;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.test.utils.AbstractRedisSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
    public void getWhatFromRiak() throws Exception {
        engineConfig.setKvStore("RIAK");
        testKVStore();
        engineConfig.setKvStore("REDIS");
    }

    @Test
    public void getWhatFromRedis() throws Exception {
        engineConfig.setKvStore("REDIS");
        testKVStore();
        engineConfig.setKvStore("REDIS");
    }

    private void testKVStore() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("Company", email, "bah"));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = auditManager.createHeader(inputBean, null).getAuditKey();
        assertNotNull(ahKey);
        AuditHeader header = auditService.getHeader(ahKey);
        Map<String, Object> what = getWhatMap();
        String whatString = getJsonFromObject(what);
        auditManager.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), whatString));
        AuditLog auditLog = auditDAO.getLastAuditLog(header.getId());
        assertNotNull(auditLog);

        //When
        AuditWhat auditWhat = whatService.getWhat(header, auditLog.getAuditChange());

        Assert.assertNotNull(auditWhat);
        validateWhat(what, auditWhat);

        Assert.assertTrue(whatService.isSame(header, auditLog.getAuditChange(), whatString));
        // Testing that cancel works
        auditService.cancelLastLogSync(ahKey);
        Assert.assertNull(auditService.getLastAuditLog(header));
        Assert.assertNull(whatService.getWhat(header, auditLog.getAuditChange()).getWhat());
    }

    private void validateWhat(Map<String, Object> what, AuditWhat auditWhat) {
        assertEquals(what.get("lval"), auditWhat.getWhatMap().get("lval"));
        assertEquals(what.get("dval"), auditWhat.getWhatMap().get("dval"));
        assertEquals(what.get("sval"), auditWhat.getWhatMap().get("sval"));
        assertEquals(what.get("ival"), auditWhat.getWhatMap().get("ival"));
        assertEquals(what.get("bval"), auditWhat.getWhatMap().get("bval"));
    }


    public static String getRandomJson() throws JsonProcessingException {
        return getRandomJson(null);
    }

    public static String getRandomJson(String s) throws JsonProcessingException {
        return getJsonFromObject(getWhatMap(s));
    }

    private static Map<String, Object> getWhatMap(String s) {
        Map<String, Object> o = getWhatMap();
        if (s == null)
            return o;

        o.put("random", s);
        return o;
    }

    public static String getJsonFromObject(Map<String, Object> what) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(what);
    }

    public static Map<String, Object> getWhatMap() {
        Map<String, Object> what = new HashMap<>();
        what.put("lval", 123456789012345l);
        what.put("dval", 1234012345.990012d);
        // Duplicated to force compression
        what.put("sval", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        what.put("sval2", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        what.put("sval3", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        what.put("sval4", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        what.put("ival", 12345);
        what.put("bval", Boolean.TRUE);
        return what;
    }


}
