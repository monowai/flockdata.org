/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.model.*;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.store.FdStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoreContent;
import org.flockdata.store.bean.StoreBean;
import org.flockdata.store.service.FdStoreConfig;
import org.flockdata.store.service.StoreService;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.embedded.RedisServer;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(FdStore.class)
@ActiveProfiles({"dev", "fd-auth-test"})
public class StoreServiceTest {


    @Autowired
    private FdStoreConfig storeConfig;

    private Logger logger = LoggerFactory.getLogger(StoreServiceTest.class);

    private static RedisServer redisServer;

    @Autowired
    private StoreService storeService;

    @Before
    public void resetKvStore() {
        storeConfig.setStoreEnabled("true");
        storeConfig.setStore(Store.MEMORY);
    }

    @BeforeClass
    public static void setup() throws Exception {

        if (redisServer == null) {
            // If you are on Windows
            if (System.getProperty("os.arch").equals("amd64") && System.getProperty("os.name").startsWith("Windows")) {
                URL url = StoreServiceTest.class.getResource("/redis/redis-server.exe");
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
        if (redisServer != null && redisServer.isActive())
            redisServer.stop();
    }

    @Test
    public void defaults_StoreEnabled() throws Exception {
        assertEquals(Store.MEMORY, storeConfig.store());

    }

    @Test
    public void riak_JsonTest() throws Exception {
        storeConfig.setStore(Store.RIAK);
        kvMapTest();
        storeConfig.setStore(Store.MEMORY);
    }

    @Test
    public void redis_JsonTest() throws Exception {
        storeConfig.setStore(Store.REDIS);
        kvMapTest();
        storeConfig.setStore(Store.MEMORY);
    }

    @Test
    public void memory_JsonTest() throws Exception {
        storeConfig.setStore(Store.MEMORY);
        kvMapTest();
    }


    @Test
    public void redis_AttachmentTest() throws Exception {
        storeConfig.setStore(Store.REDIS);
        kvAttachmentTest();
        storeConfig.setStore(Store.MEMORY);
    }


    private void kvMapTest() throws Exception {
        logger.debug("Registering system user!");

        String fortress = "Entity Test";
        String docType = "TestAuditX";
        String callerRef = "ABC123R";
        String company = "company";

        Map<String, Object> what = getWhatMap();
        Fortress fort = new Fortress(
                new FortressInputBean("test", true),
                new Company("MyName"));
        // Represents identifiable entity information
        EntityInputBean entityInputBean = new EntityInputBean(fort, "wally", docType, new DateTime(), callerRef)
                .setContent(new ContentInputBean(what));

        DocumentType documentType = new DocumentType(fort, docType);
        // The "What" content

        // Emulate the creation of the entity
        Entity entity = EntityContentHelper.getEntity(company, fortress, "wally", documentType.getName());

        // Wrap the entity in a Track Result
        // TrackResultBean represents the general accumulated payload
        TrackResultBean trackResultBean = new TrackResultBean(fort, entity, documentType, entityInputBean);

        // Create a log with a random primary key
        Log graphLog = new Log(entity);

        // Sets some tracking properties in to the Log and wraps the ContentInputBean in a KV wrapping class
        // This occurs before the service persists the log
        graphLog = Store.prepareLog(Store.MEMORY, trackResultBean, graphLog);
        // Graph tracks which KVService is storing this content
        EntityLog eLog = new EntityLog(entity, graphLog, new DateTime());

        // Wrap the log result in to the TrackResult
        trackResultBean.setCurrentLog(eLog);

        StoreBean storeBean = new StoreBean(trackResultBean);
        storeBean.setStore(graphLog.getStorage());
        // RIAK requires a bucket. Other KV stores do not.
        assertNotNull(storeBean.getBucket());

        // Finally! the actual write occurs
        try {
            storeService.doWrite(storeBean);

            // Retrieve the content we just created
            StoreContent storeContent = storeService.getContent(new LogRequest(entity, trackResultBean.getCurrentLog().getLog()));
            assertNotNull(storeContent);
            assertNotNull(storeContent.getContent().getMetaKey());
            assertNotNull(storeContent.getContent().getCode());

            validateWhat(what, storeContent);
            // Testing that cancel works
            storeService.delete(entity, trackResultBean.getCurrentLog().getLog());

        } catch (AmqpRejectAndDontRequeueException e) {
            // ToDo: Mock RIAK
            if (storeConfig.store().equals(Store.RIAK)) {
                logger.error("Silently passing. No what data to process for {}. KV store is not running", storeConfig.store());
            } else {
                logger.error("KV Error", e);
                fail("Unexpected KV error");
            }

        }
    }

    private void validateWhat(Map<String, Object> what, StoreContent storeContent) throws InterruptedException {
        Thread.sleep(1500);
        assertEquals(what.get("sval"), storeContent.getData().get("sval"));
        assertEquals(what.get("lval"), storeContent.getData().get("lval"));
        assertEquals(what.get("dval"), storeContent.getData().get("dval"));
        assertEquals(what.get("ival"), storeContent.getData().get("ival"));
        assertEquals(what.get("bval"), storeContent.getData().get("bval"));
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        assertEquals(json, storeContent.getData().get("utf-8"));
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
        ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
        return mapper.writeValueAsString(what);
    }

    public static Map<String, Object> getWhatMap() {
        Map<String, Object> what = new HashMap<>();
        what.put("lval", 123456789012345L);
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
        logger.debug("Registering system user!");

        String docType = "KvTest";
        String callerRef = "ABC123R";
        Entity entity = EntityContentHelper.getEntity("myco", "myfort", "myuser", docType);
        DocumentType documentType = new DocumentType(null, entity.getType());

        EntityInputBean inputBean = EntityContentHelper.getEntityInputBean(docType, entity.getFortress(), "myuser", callerRef, DateTime.now());
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime());
        contentInputBean.setAttachment("test-attachment-data", "PDF", "testFile.txt");
        //inputBean.setContent(contentInputBean);

        try {
            TrackResultBean tr = new TrackResultBean(null, entity, documentType, inputBean);
            StoreBean storeBean = new StoreBean(tr);
            storeService.doWrite(storeBean);
            EntityLog entityLog = tr.getCurrentLog();
            StoreContent entityContent = storeService.getContent(new LogRequest(entity, entityLog.getLog()));

            assertNotNull(entityContent);
            // Redis should always be available. RIAK is trickier to install

            assertEquals(contentInputBean.getFileName(), entityLog.getLog().getFileName());
            assertEquals("Value didn't convert to lowercase", "pdf", entityLog.getLog().getContentType());
            assertEquals(contentInputBean.getAttachment(), entityContent.getAttachment());
        } catch (Exception ies) {
            logger.error("KV Stores are configured in application.yml. This test is failing to find the {} server. Is it even installed?", storeConfig.store());
        }
    }


}

