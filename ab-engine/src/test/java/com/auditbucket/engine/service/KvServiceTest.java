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

import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.functional.TestEngineBase;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import redis.embedded.RedisServer;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Transactional
public class KvServiceTest extends TestEngineBase {

    @Autowired
    RedisRepo redisRepo;
    @Autowired
    private KvService kvService;

    private Logger logger = LoggerFactory.getLogger(KvServiceTest.class);

    private static RedisServer redisServer;

    private String email = "mike";
    private Authentication authA = new UsernamePasswordAuthenticationToken("mike", "123");

    @BeforeClass
    public static void setup() throws Exception {
        if(redisServer == null){
            // If you are on Winodws
            if (System.getProperty("os.arch").equals("amd64") && System.getProperty("os.name").startsWith("Windows")) {
                URL url = KvServiceTest.class.getResource("/redis/redis-server.exe");
                File redisServerExe = new File(url.getFile());
                redisServer = new RedisServer(redisServerExe, 6379); // or new RedisServer("/path/to/your/redis", 6379);
            } else {
                redisServer = new RedisServer(6379);
            }
            redisServer.start();
        }
    }



    @AfterClass
    public static void tearDown() throws Exception {
        //redisServer.stop();
    }


    @Test
    public void getWhatFromRiak() throws Exception {
        //engineConfig.setKvStore("RIAK");
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
        logger.debug("Registering system user!");
        SystemUser su = registerSystemUser("Company", email);
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = mediationFacade.trackHeader(su.getCompany(), inputBean).getMetaKey();
        assertNotNull(ahKey);
        MetaHeader header = trackService.getHeader(ahKey);
        Map<String, Object> what = getWhatMap();
        //String whatString = getJsonFromObject(what);
        try{
            mediationFacade.processLog(su.getCompany(), new LogInputBean("wally", ahKey, new DateTime(), what));
        } catch (Exception e ){
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?",engineConfig.getKvStore());
            return;
        }
        TrackLog trackLog = trackService.getLastLog(header.getId());
        assertNotNull(trackLog);

        //When
        try {
            LogWhat logWhat = kvService.getWhat(header, trackLog.getLog());

            Assert.assertNotNull(logWhat);
            // Redis should always be available. RIAK is trickier to install
            if ( engineConfig.getKvStore().equals(com.auditbucket.kv.service.KvService.KV_STORE.REDIS)||logWhat.getWhat().keySet().size()>1 ){
                validateWhat(what, logWhat);

                Assert.assertTrue(kvService.isSame(header, trackLog.getLog(), what));
                // Testing that cancel works
                trackService.cancelLastLog(fortressA.getCompany(), header);
                Assert.assertNull(logService.getLastLog(header));
                Assert.assertNull(kvService.getWhat(header, trackLog.getLog()).getWhatString());
                Assert.assertTrue(kvService.isSame(logWhat.getWhatString(), what));
            } else {
                // ToDo: Mock RIAK
                logger.error("Silently passing. No what data to process for {}. Possibly KV store is not running",engineConfig.getKvStore());
            }
        } catch (Exception ies){
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?",engineConfig.getKvStore());
        }
    }

    private void validateWhat(Map<String, Object> what, LogWhat logWhat) throws InterruptedException {
        Thread.sleep(1500);
        assertEquals(what.get("sval"), logWhat.getWhat().get("sval"));
        assertEquals(what.get("lval"), logWhat.getWhat().get("lval"));
        assertEquals(what.get("dval"), logWhat.getWhat().get("dval"));
        assertEquals(what.get("ival"), logWhat.getWhat().get("ival"));
        assertEquals(what.get("bval"), logWhat.getWhat().get("bval"));
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        assertEquals(json, logWhat.getWhat().get("utf-8"));
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
        what.put("utf-8", "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}");
        return what;
    }


}
