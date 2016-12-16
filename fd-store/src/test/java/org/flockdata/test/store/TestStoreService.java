/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.model.*;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.store.FdStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.store.service.StoreManager;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import redis.embedded.RedisServer;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.util.AssertionErrors.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FdStore.class})
@ActiveProfiles({"dev", "fd-auth-none", "riak", "redis"})
@WebAppConfiguration (value = "src/main/resources")
public class TestStoreService {


    private static RedisServer redisServer;
    @Autowired
    private IndexManager indexManager;
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext wac;
    private Logger logger = LoggerFactory.getLogger(TestStoreService.class);
    @Autowired
    private StoreManager storeManager;

    @BeforeClass
    public static void setup() throws Exception {

        if (redisServer == null) {
            // If you are on Windows
            if (System.getProperty("os.arch").equals("amd64") && System.getProperty("os.name").startsWith("Windows")) {
                URL url = TestStoreService.class.getResource("/redis/redis-server.exe");
                File redisServerExe = new File(url.getFile());
                redisServer = new RedisServer(redisServerExe, 6379); // or new RedisServer("/path/to/your/redis", 6379);
            } else {
                redisServer = new RedisServer(6379);
            }
            try {
                redisServer.start();
            } catch (Exception e ){
                tearDown();
                try {
                    redisServer.start(); // One off?? Had to manually kill redis so trying to be a bit richer in dealing with the scenario

                } catch (RuntimeException re){
                    System.out.println("Redis REALLY is not available so we cannot test it");
                }
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (redisServer != null && redisServer.isActive())
            redisServer.stop();
    }

    public static String getRandomJson() throws JsonProcessingException {
        return getRandomJson(null);
    }

    public static String getRandomJson(String s) throws JsonProcessingException {
        return getJsonFromObject(getData(s));
    }

    private static Map<String, Object> getData(String s) {
        Map<String, Object> o = getData();
        if (s == null)
            return o;

        o.put("random", s);
        return o;
    }

    public static String getJsonFromObject(Map<String, Object> what) throws JsonProcessingException {
        ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
        return mapper.writeValueAsString(what);
    }

    public static Map<String, Object> getData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lval", 123456789012345L);
        data.put("dval", 1234012345.990012d);
        // Duplicated to force compression
        data.put("sval", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        data.put("sval2", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        data.put("sval3", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        data.put("sval4", "Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party.Now is the time for all good men to come to the aid of the party");
        data.put("ival", 12345);
        data.put("bval", Boolean.TRUE);
        data.put("utf-8", "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}");
        return data;
    }

    static RequestPostProcessor noUser() {
        return user("noone");
    }

    @Before
    public void resetKvStore() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    public void riak_JsonTest() throws Exception {
        testStore(Store.RIAK);
    }

    @Test
    public void redis_JsonTest() throws Exception {
        testStore(Store.REDIS);
    }

    @Test
    public void memory_JsonTest() throws Exception {
        testStore(Store.MEMORY);
    }

    @Test
    public void redis_AttachmentTest() throws Exception {
        kvAttachmentTest(Store.REDIS);
    }

    private void testStore(Store storeToTest) throws Exception {
        if ( redisServer == null || !redisServer.isActive()){
            logger.info("!! REDIS is not installed so we cannot test it");
            return;
        }
        logger.debug("Registering system user!");

        String fortress = "Entity Test";
        String docType = "TestAuditX";
        String entityCode = "ABC123R";
        String company = "company";

        Map<String, Object> theData = getData();
        Fortress fort = new Fortress(
                new FortressInputBean("test", true),
                new Company("MyName"));
        // Represents identifiable entity information
        EntityInputBean entityInputBean = new EntityInputBean(fort, "wally", docType, new DateTime(), entityCode)
                .setContent(new ContentInputBean(theData));

        DocumentType documentType = new DocumentType(fort, docType);
        // The "Data" content

        // Emulate the creation of the entity
        Entity entity = EntityContentHelper.getEntity(company, fortress, "wally", documentType.getName());

        // Wrap the entity in a Track Result
        // TrackResultBean represents the general accumulated payload
        TrackResultBean trackResultBean = new TrackResultBean(fort, entity, documentType, entityInputBean);

        // Create a log with a random primary key
        Log graphLog = new Log(entity);

        // Sets some tracking properties in to the Log and wraps the ContentInputBean in a KV wrapping class
        // This occurs before the service persists the log
        graphLog = Store.prepareLog(storeToTest, trackResultBean, graphLog);
        // Graph tracks which KVService is storing this content
        EntityLog eLog = new EntityLog(entity, graphLog, new DateTime());

        // Wrap the log result in to the TrackResult
        trackResultBean.setCurrentLog(eLog);

        StorageBean storeBean = new StorageBean(trackResultBean);
        storeBean.setStore(storeToTest.name());
        assertEquals(storeToTest.name(), graphLog.getStorage());

        // Finally! the actual write occurs
        try {
            storeManager.doWrite(storeBean);

            String index = indexManager.toStoreIndex(storeToTest, entity);
            String type = indexManager.parseType(entity);
            String key = indexManager.resolveKey(new LogRequest(entity, trackResultBean.getCurrentLog().getLog()));
            StoredContent contentResult = storeManager.doRead(storeToTest,
                    index,
                    type,
                    key);

            validateContent("Validating result found via the service", contentResult);

            contentResult = getContent(noUser(), storeToTest, index, type, key, MockMvcResultMatchers.status().isOk());
            validateContent("Validating result found via Mock MVC", contentResult);

            // Testing that cancel works
            storeManager.delete(entity, trackResultBean.getCurrentLog().getLog());

        } catch (AmqpRejectAndDontRequeueException e) {
            // ToDo: Mock RIAK
            if (storeToTest.equals(Store.RIAK)) {
                logger.info("Silently passing as the {} store is not running", storeToTest);
            } else {
                logger.error("Store Error", e);
                fail("Unexpected KV error");
            }

        }
    }

    private void validateContent(String failureMessage, StoredContent contentResult) throws InterruptedException {
        assertNotNull(failureMessage, contentResult);
        assertNotNull(failureMessage + " - content was not found", contentResult.getContent());
        assertNotNull(failureMessage, contentResult.getContent().getKey());
        assertNotNull(failureMessage, contentResult.getContent().getCode());
        Map<String, Object> data = contentResult.getContent().getData();
        Thread.sleep(1500);
        assertEquals(failureMessage, data.get("sval"), contentResult.getData().get("sval"));
        assertEquals(failureMessage, data.get("lval"), contentResult.getData().get("lval"));
        assertEquals(failureMessage, data.get("dval"), contentResult.getData().get("dval"));
        assertEquals(failureMessage, data.get("ival"), contentResult.getData().get("ival"));
        assertEquals(failureMessage, data.get("bval"), contentResult.getData().get("bval"));
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        assertEquals(failureMessage, json, contentResult.getData().get("utf-8"));
    }

    private void kvAttachmentTest(Store storeToTest) throws Exception {
        logger.debug("Registering system user!");

        String docType = "KvTest";
        String entityCode = "ABC123R";
        Entity entity = EntityContentHelper.getEntity("myco", "myfort", "myuser", docType);
        DocumentType documentType = new DocumentType(null, entity.getType());

        EntityInputBean inputBean = EntityContentHelper.getEntityInputBean(docType, entity.getFortress(), "myuser", entityCode, DateTime.now());
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime());
        contentInputBean.setAttachment("test-attachment-data", "PDF", "testFile.txt");

        try {
            TrackResultBean trackResultBean = new TrackResultBean(null, entity, documentType, inputBean);
            StorageBean storeBean = new StorageBean(trackResultBean);
            storeManager.doWrite(storeBean);
            EntityLog entityLog = trackResultBean.getCurrentLog();
            StoredContent entityContent = storeManager.doRead(storeToTest,
                    indexManager.toStoreIndex(storeToTest, entity),
                    indexManager.parseType(entity),
                    trackResultBean.getCurrentLog().getLog().getId().toString());

            assertNotNull(entityContent);
            // Redis should always be available. RIAK is trickier to install

            assertEquals(contentInputBean.getFileName(), entityLog.getLog().getFileName());
            assertEquals("Value didn't convert to lowercase", "pdf", entityLog.getLog().getContentType());
            assertEquals(contentInputBean.getAttachment(), entityContent.getAttachment());
        } catch (Exception ies) {
            logger.info("KV Stores are configured in application.yml. This test is failing to find the {} server. Is it even installed?", storeToTest);
        }
    }

    private StoredContent getContent(RequestPostProcessor user, Store store, String index, String type, Object key, ResultMatcher status) throws Exception {
        MvcResult response =mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/data/{store}/{index}/{type}/{key}", store.name(), index, type, key)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(status).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toObject(json.getBytes(), StorageBean.class);
    }

}

