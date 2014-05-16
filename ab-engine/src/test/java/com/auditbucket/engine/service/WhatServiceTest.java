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

package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackDao;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.test.utils.AbstractRedisSupport;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    TrackService trackService;
    @Autowired
    RegistrationService regService;
    @Autowired
    FortressService fortressService;
    @Autowired
    MediationFacade auditManager;
    @Autowired
    TrackDao trackDAO;
    @Autowired
    private WhatService whatService;

    private Logger logger = LoggerFactory.getLogger(WhatServiceTest.class);

    @Autowired
    private EngineConfig engineConfig;

    private String email = "mike";
    private Authentication authA = new UsernamePasswordAuthenticationToken("mike", "123");

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
        logger.debug("Registering system user");
        regService.registerSystemUser(new RegistrationBean("Company", email).setIsUnique(false));
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = auditManager.createHeader(inputBean, null).getMetaKey();
        assertNotNull(ahKey);
        MetaHeader header = trackService.getHeader(ahKey);
        Map<String, Object> what = getWhatMap();
        String whatString = getJsonFromObject(what);
        try{
            auditManager.processLog(new LogInputBean(ahKey, "wally", new DateTime(), whatString));
        } catch (Exception e ){
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?",engineConfig.getKvStore());
            return;
        }
        TrackLog trackLog = trackDAO.getLastLog(header.getId());
        assertNotNull(trackLog);

        //When
        LogWhat logWhat = whatService.getWhat(header, trackLog.getChange());

        Assert.assertNotNull(logWhat);
        validateWhat(what, logWhat);

        Assert.assertTrue(whatService.isSame(header, trackLog.getChange(), whatString));
        // Testing that cancel works
        trackService.cancelLastLogSync(ahKey);
        Assert.assertNull(trackService.getLastLog(header));
        Assert.assertNull(whatService.getWhat(header, trackLog.getChange()).getWhatString());
    }

    private void validateWhat(Map<String, Object> what, LogWhat logWhat) {
        assertEquals(what.get("lval"), logWhat.getWhat().get("lval"));
        assertEquals(what.get("dval"), logWhat.getWhat().get("dval"));
        assertEquals(what.get("sval"), logWhat.getWhat().get("sval"));
        assertEquals(what.get("ival"), logWhat.getWhat().get("ival"));
        assertEquals(what.get("bval"), logWhat.getWhat().get("bval"));
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
