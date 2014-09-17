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

import com.auditbucket.engine.repo.EntityContentData;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.functional.TestEngineBase;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityContent;
import com.auditbucket.track.model.EntityLog;
import com.auditbucket.track.model.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.embedded.RedisServer;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
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

    @BeforeClass
    public static void setup() throws Exception {
        if (redisServer == null) {
            // If you are on Windows
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
    public void riak_JsonTest() throws Exception {
        engineConfig.setKvStore("RIAK");
        kvMapTest();
        engineConfig.setKvStore("REDIS");
    }

    @Test
    public void redis_JsonTest() throws Exception {
        engineConfig.setKvStore("REDIS");
        kvMapTest();
        engineConfig.setKvStore("REDIS");
    }


    @Test
    public void redis_AttachmentTest() throws Exception {
        engineConfig.setKvStore("REDIS");
        kvAttachmentTest();
        engineConfig.setKvStore("REDIS");
    }


    private void kvMapTest() throws Exception {
        setSecurity();
        logger.debug("Registering system user!");
        SystemUser su = registerSystemUser("Company", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        Map<String, Object> what = getWhatMap();
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), what);
        inputBean.setLog(contentInputBean);

        Entity entity;


        //String whatString = getJsonFromObject(what);
        try {
            entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        } catch (Exception e) {
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?", engineConfig.getKvStore());
            return;
        }
        EntityLog entityLog = trackService.getLastEntityLog(entity.getId());
        assertNotNull(entityLog);

        //When
        try {
            EntityContent entityContent = kvService.getContent(entity, entityLog.getLog());

            assertNotNull(entityContent);
            // Redis should always be available. RIAK is trickier to install
            if (engineConfig.getKvStore().equals(com.auditbucket.kv.service.KvService.KV_STORE.REDIS) || entityContent.getWhat().keySet().size() > 1) {
                validateWhat(what, entityContent);
                //new EntityContentData(compareTo.getEntityContent(), compareTo)
                //assertEquals(true, kvService.isSame(entity, entityLog.getLog(), contentInputBean));
                // Testing that cancel works
                trackService.cancelLastLog(fortressA.getCompany(), entity);
                assertNull(logService.getLastLog(entity));
                assertNull("This log should have been deleted and nothing returned", kvService.getContent(entity, entityLog.getLog()));
                assertTrue(kvService.sameJson(entityContent, contentInputBean));
            } else {
                // ToDo: Mock RIAK
                logger.error("Silently passing. No what data to process for {}. Possibly KV store is not running", engineConfig.getKvStore());
            }
        } catch (Exception ies) {
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?", engineConfig.getKvStore());
        }
    }

    private void validateWhat(Map<String, Object> what, EntityContent entityContent) throws InterruptedException {
        Thread.sleep(1500);
        assertEquals(what.get("sval"), entityContent.getWhat().get("sval"));
        assertEquals(what.get("lval"), entityContent.getWhat().get("lval"));
        assertEquals(what.get("dval"), entityContent.getWhat().get("dval"));
        assertEquals(what.get("ival"), entityContent.getWhat().get("ival"));
        assertEquals(what.get("bval"), entityContent.getWhat().get("bval"));
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        assertEquals(json, entityContent.getWhat().get("utf-8"));
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

    private void kvAttachmentTest() throws Exception {
        setSecurity();
        logger.debug("Registering system user!");
        SystemUser su = registerSystemUser("Company", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("Audit Test", true));
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime());
        contentInputBean.setAttachment("test-attachment-data", Log.ContentType.PDF, "testFile.txt");
        inputBean.setLog(contentInputBean);

        Entity entity;
        try {
            entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        } catch (Exception e) {
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?", engineConfig.getKvStore());
            return;
        }
        EntityLog entityLog = trackService.getLastEntityLog(entity.getId());
        assertNotNull(entityLog);

        try {
            EntityContent entityContent = kvService.getContent(entity, entityLog.getLog());

            assertNotNull(entityContent);
            // Redis should always be available. RIAK is trickier to install


            assertEquals(contentInputBean.getFileName(), entityLog.getLog().getFileName());
            assertEquals(contentInputBean.getContentType(), entityLog.getLog().getContentType());
            assertEquals(contentInputBean.getAttachment(), entityContent.getAttachment());
        } catch (Exception ies) {
            logger.error("KV Stores are configured in config.properties. This test is failing to find the {} server. Is it even installed?", engineConfig.getKvStore());
        }
    }


}
