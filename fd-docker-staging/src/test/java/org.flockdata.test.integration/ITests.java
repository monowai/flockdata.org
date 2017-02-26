/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.integration;

import junit.framework.TestCase;
import net.jcip.annotations.NotThreadSafe;
import org.flockdata.client.FdClientIo;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FdTemplate;
import org.flockdata.integration.FileProcessor;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.EsSearchResult;
import org.flockdata.search.QueryParams;
import org.flockdata.track.bean.*;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static junit.framework.TestCase.*;
import static org.flockdata.test.integration.IntegrationHelper.ADMIN_REGRESSION_PASS;
import static org.flockdata.test.integration.IntegrationHelper.ADMIN_REGRESSION_USER;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Establishes the integration test environment. Descendant classes use @Test functions against
 * this established stack
 *
 * @author mholdsworth
 * @tag Test, Docker
 * @since 3/04/2016
 */
@ContextConfiguration(classes = {
        ClientConfiguration.class,
        FileProcessor.class,
        FdTemplate.class,
        FdClientIo.class,
        FdRabbitClient.class,
        AmqpRabbitConfig.class,
        RestTemplate.class,
        IntegrationHelper.class

})
@RunWith(SpringRunner.class)
@Configuration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"fd-client", "fd-auth-test"})
@NotThreadSafe
public class ITests {


    @ClassRule
    public static FdDocker stack = new FdDocker();

    private static Logger logger = LoggerFactory.getLogger(ITests.class);
    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private IntegrationHelper integrationHelper;
    @Autowired
    private RestTemplate restTemplate; // ensure resource is wired
    @Autowired
    private FdTemplate fdTemplate;

    private SearchHelper searchHelper = new SearchHelper();
    /**
     * Uses the standard FdClientIo services, which use a RestTemplate and RabbitClient, to talk to FlockData.
     * By default the service URL points to fd-engine but can be reconfigured via clientConfiguration.setServiceUrl(...)
     * to talk to fd-search or fd-store (for ping requests only!).
     * <p>
     * By design, only fd-engine is secured
     */
    @Autowired
    private FdClientIo fdClientIo;

    @Before
    public void setupServices() {
        assertNotNull(restTemplate);
        assertNotNull(clientConfiguration);
        clientConfiguration.setBatchSize(1);
        integrationHelper.waitForServices();
    }

    @Test
    public void aaPingFdStore() {
        logger.info("Ping FD Store");
        clientConfiguration.setServiceUrl(integrationHelper.getStore());
        String result = fdClientIo.ping();
        assertEquals("Couldn't ping fd-store", "pong", result);
    }

    @Test
    public void aaPingFdEngine() {
        logger.info("Ping FD Engine");
        String result = fdClientIo.ping();
        assertEquals("Couldn't ping fd-engine", "pong", result);
    }

    @Test
    public void aaPingFdSearch() {
        logger.info("Ping FD Search");
        clientConfiguration.setServiceUrl(integrationHelper.getSearch());

        Ping ping = new Ping(fdClientIo);
        ping.exec();
        assertTrue(ping.error(), ping.worked());

        assertEquals("Couldn't ping fd-search", "pong", ping.result());
    }

    @Test
    public void simpleLogin() {
        logger.info("Testing simpleLogin");
        SystemUserResultBean suResult = fdTemplate.getFdIoInterface().login(ADMIN_REGRESSION_USER, "123");
        assertNotNull(suResult);
        assertTrue("User Roles missing", suResult.getUserRoles().length != 0);
    }

    @Test
    public void registration() throws Exception {
        // An authorised user can create DataAccess users for a given company
        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        assertNotNull(login);
        assertNotNull(login.getApiKey());

    }

    /**
     * FlockData ships with some basic static data like Countries and Cities.
     * This test checks that they are tracked in to the service. Validates a number of things:
     * Country Content Profile
     * Tag being tracked over an AMQP endpoint
     * Countries can be found via the Tag endpoint by label
     */
    @Test
    public void loadCountries() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        clientConfiguration.setBatchSize(5);

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/countries.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/countries.json", contentModel);
//       ToDo: figure out getting ImportProfiles into the fileProcessor
        int countryInputs = fileProcessor.processFile(extractProfile, "/fd-cow.txt");

        assertEquals("Countries not processed", 250, countryInputs); //+1 "Undefined"
        TagsGet countries = new TagsGet(fdClientIo, "Country");
        // Tags are processed over a messageQ so will take a wee bit of time to be processed
        integrationHelper.longSleep();
        countries.exec();
        assertTrue("Error loading countries" + countries.error(), countries.worked());

        TagResultBean[] countryResults = countries.result();
        // By this stage we may or may not have processed all the tags depending on how resource constrained the host machine
        // running the integration stack is. So we'll just check at least two batches of 10 have been processed assuming the
        // rest will pass
        assertTrue("No countries found!", countryResults.length > 10);

        TagGet countryByIsoShort = new TagGet(fdClientIo, "Country", "AU");
        countryByIsoShort.exec();
        assertTrue(countryByIsoShort.error(), countryByIsoShort.worked());
        assertNotNull("Couldn't find Australia", countryByIsoShort.result());

        TagGet countryByIsoLong = new TagGet(fdClientIo, "Country", "AUS");
        countryByIsoLong.exec();
        assertTrue(countryByIsoLong.error(), countryByIsoLong.worked());
        assertNotNull("Couldn't find Australia", countryByIsoLong.result());


        TagGet countryByName = new TagGet(fdClientIo, "Country", "Australia");
        countryByName.exec();
        integrationHelper.assertWorked("Country by name - ", countryByName);
        assertNotNull("Couldn't find Australia", countryByName.result());

        assertEquals("By Code and By Name they are the same country so should equal", countryByIsoShort.result(), countryByName.result());
        assertEquals("By short code and long code they are the same country so should equal", countryByIsoLong.result(), countryByIsoShort.result());
        integrationHelper.shortSleep();
        QueryParams qp = searchHelper.getTagQuery("country", "australia");
        SearchEsPost searchEsPost = new SearchEsPost(fdClientIo, qp);
        integrationHelper.assertWorked("Located Country by name", searchEsPost);
        searchHelper.assertHitCount("Should have found just 1 hit for Australia", 1, searchEsPost.result());

        searchEsPost = new SearchEsPost(fdClientIo, searchHelper
                .getTagMatchQuery("country", "aka.bgn_longname", "commonwealth of australia"));
        searchHelper.assertHitCount("Didn't find Australia by alias", 1, searchEsPost.exec().result());

    }

    @Test
    public void trackEntityOverHttp() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntity", false))
                .setDocumentType(new DocumentTypeInputBean("someThing"))
                .setContent(new ContentInputBean(Helper.getRandomMap()))
                .addTag(new TagInputBean("someCode", "SomeLabel"));
        TrackEntityPost trackEntity = new TrackEntityPost((FdClientIo) fdTemplate.getFdIoInterface(), entityInputBean);

        integrationHelper.assertWorked("Track Entity - ", trackEntity.exec());

        assertNotNull(trackEntity.result());
        assertNotNull(trackEntity.result().getKey());
        assertEquals("Should be a new Entity", trackEntity.result().isNewEntity(), true);
        assertEquals("Problem creating the Content", trackEntity.result().getLogStatus(), ContentInputBean.LogStatus.OK);

        EntityGet foundEntity = new EntityGet(fdClientIo, trackEntity.result().getKey());
        integrationHelper.waitForEntityKey(logger, "trackEntityOverHttp", foundEntity.exec());
        integrationHelper.assertWorked("Find Entity - ", foundEntity);
        assertNotNull(foundEntity.result().getKey());

    }

    @Test
    public void trackEntityOverAmqpThenFindInSearch() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntityAmqp")
                        .setSearchEnabled(true))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")))
                .addTag(new TagInputBean("someCode", "SomeLabel"));

        fdTemplate.writeEntity(entityInputBean, true);

        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean)
                .exec();

        integrationHelper.waitForEntityKey(logger, "trackEntityOverAmqpThenFindInSearch", entityGet);

        EntityResultBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());
        integrationHelper.waitForSearch(logger, "trackEntityOverAmqpThenFindInSearch", entityGet, 1);
        entityResult = entityGet.result();
        assertFalse("Search was incorrectly suppressed", entityResult.isSearchSuppressed());
        assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, entityResult.getSearch());
        assertEquals("Search Key was not set to the code of the entityInput", entityInputBean.getCode(), entityResult.getSearchKey());

        integrationHelper.shortSleep();
        QueryParams qp = new QueryParams(entityResult.getCode())
                .setFortress(entityInputBean.getFortress().getName());

        SearchFdPost search = new SearchFdPost(fdClientIo, qp);
        integrationHelper.assertWorked("Searching - ", search);
        EsSearchResult searchResults = search.result();
        assertEquals("Didn't get a search hit on the Entity", 1, searchResults.getResults().size());
        assertEquals("Keys do not match", entityResult.getKey(), searchResults.getResults().iterator().next().getKey());

        // Test we can find via UTF-8
        qp.setSearchText(entityInputBean.getContent().getData().get("key").toString());
        search.exec();
        assertEquals("Couldn't find via UTF-8 Text search", entityResult.getKey(), searchResults.getResults().iterator().next().getKey());

    }

    @Test
    public void validateEntityLogs() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("validateEntityLogs", false))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("validateEntityLogs"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "value")))
                .addTag(new TagInputBean("someCode", "SomeLabel"));

        fdTemplate.writeEntity(entityInputBean, true);
        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean).exec();
        integrationHelper.waitForEntityKey(logger, "validateEntityLogs", entityGet);

        EntityResultBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());

        EntityLogsGet entityLogs = new EntityLogsGet(fdClientIo, entityResult.getKey());
        integrationHelper.waitForEntityLog(logger, "validateEntityLogs", entityLogs, 1);
        assertNotNull(entityLogs.result());
        assertEquals("Didn't find a log", 1, entityLogs.result().length);
        assertNotNull("No data was returned", entityLogs.result()[0].getData());
        assertEquals("Content property not found", "value", entityLogs.result()[0].getData().get("key"));

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("validateEntityLogs", false))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("validateEntityLogs"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "updated")));


        // Updating an existing entity
        fdTemplate.writeEntity(entityInputBean, true);
        integrationHelper.waitForEntityLog(logger, "validateEntityLogs", entityLogs, 2);
        assertEquals("Didn't find the second log", 2, entityLogs.result().length);
        assertEquals("Didn't find the updated field as the first result", "updated", entityLogs.result()[0].getData().get("key"));
        assertEquals("Didn't find the original field as the second result", "value", entityLogs.result()[1].getData().get("key"));
    }

    @Test
    public void findByESPassThroughWithUTF8() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("findByESPassThroughWithUTF8")
                        .setSearchEnabled(true))
                .setCode("Katerina Neumannová")
                .setDescription("Katerina Neumannová")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")));

        fdTemplate.writeEntity(entityInputBean, true);
        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean);
        integrationHelper.waitForEntityKey(logger, "findByESPassThroughWithUTF8", entityGet);

        EntityResultBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());
        integrationHelper.waitForSearch(logger, "findByESPassThroughWithUTF8", entityGet, 1);
        assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, entityGet.result().getSearch());

        integrationHelper.shortSleep();
        QueryParams qp = new QueryParams()
                .setFortress(entityInputBean.getFortress().getName())
                .setQuery(JsonUtils.toMap("{\n" +
                        "    \"filtered\": {\n" +
                        "      \"query\": {\n" +
                        "        \"query_string\": {\n" +
                        "          \"query\": \"*\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "}"));

        SearchEsPost search = new SearchEsPost(fdClientIo, qp);
        integrationHelper.assertWorked("Search Reply ", search);

        assertFalse("errors were found " + search.result().get("errors"), search.result().containsKey("errors"));

        searchHelper.assertHitCount("Expected 1 hit", 1, search.result());
        assertTrue("UTF-8 failure. Couldn't find " + entityInputBean.getCode(), searchHelper.getHits(search.result()).contains(entityInputBean.getCode()));
    }

    @Test
    public void simpleTags() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        Collection<TagInputBean> tags = new ArrayList<>();
        String tagLabel = "simpleTag";
        TagInputBean tagInputBean = new TagInputBean("ACode", tagLabel).setDescription("Description of the Tag");
        tags.add(tagInputBean);
        tagInputBean = new TagInputBean("BCode", tagLabel);
        tags.add(tagInputBean);

        fdClientIo.writeTags(tags);
        integrationHelper.longSleep();  // Async delivery, so lets wait a bit....
        TagsGet getTags = new TagsGet(fdClientIo, tagLabel);

        integrationHelper.waitForTagCount(logger, "simpleTags", getTags, 2);

        TagResultBean[] foundTags = getTags.exec().result();
        assertNotNull(foundTags);
        assertEquals("Missing tags", 2, foundTags.length);

        QueryParams qp = searchHelper.getTagQuery(tagInputBean.getLabel(), "*");

        SearchEsPost search = new SearchEsPost(fdClientIo, qp);
        integrationHelper.shortSleep(); // Extra time for ES to commit
        integrationHelper.assertWorked("Search Reply ", search);

        Map<String, Object> esResult = search.result();
        assertFalse("errors were found " + esResult.get("errors"), esResult.containsKey("errors"));

        searchHelper.assertHitCount("Expected 2 hits for two tags when searching for *", 2, search.result());

        // Assert that updates work
        tags.clear();
        tagInputBean = new TagInputBean("ACode", tagLabel)
                .setName("acode wonder")
                .setMerge(true);

        tagInputBean.setProperty("aprop", 123);

        tags.add(tagInputBean);
        fdClientIo.writeTags(tags);
        integrationHelper.longSleep();  // Async delivery, so lets wait a bit....

        qp = searchHelper.getTagQuery(tagInputBean.getLabel(), "wonder");

        search = new SearchEsPost(fdClientIo, qp);
        integrationHelper.shortSleep();

        integrationHelper.assertWorked("Search Reply ", search);
        searchHelper.assertHitCount("Expected single hit for an updated tag", 1, search.result());

        String json = searchHelper.getHits(search.result());
        assertNotNull(json);
        assertTrue("ACode tag should have been in the result", json.contains("ACode"));
        assertTrue("Didn't find correct search text", json.contains("acode wonder"));

    }

    @Test
    public void bulkTagsDontBlock() throws Exception {
        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        Collection<TagInputBean> setA = getRandomTags("Set", "codea");
        Collection<TagInputBean> setB = getRandomTags("Set", "codea");
        Collection<TagInputBean> setC = getRandomTags("Set", "codea");
        Collection<TagInputBean> setD = getRandomTags("Set", "codea");
        fdTemplate.writeTags(setA);
        fdTemplate.writeTags(setB);
        fdTemplate.writeTags(setC);
        fdTemplate.writeTags(setD);
        integrationHelper.longSleep();
        QueryParams qp = searchHelper.getTagQuery("Set*", "code*");
        SearchEsPost search = new SearchEsPost((FdClientIo) fdTemplate.getFdIoInterface(), qp);
        search.exec();
        integrationHelper.assertWorked("Not finding any tags", search);


    }

    @Test
    public void purgeFortressRemovesEsIndex() throws Exception {
        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeFortressRemovesEsIndex")
                        .setSearchEnabled(true))
                .setCode("SearchDoc")
                .setDocumentType(new DocumentTypeInputBean("DeleteSearchDoc"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        fdTemplate.writeEntity(entityInputBean, true);

        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeFortressRemovesEsIndex", entityGet);
        integrationHelper.waitForSearch(logger, "purgeFortressRemovesEsIndex", entityGet, 1);
        integrationHelper.longSleep(); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress("purgeFortressRemovesEsIndex");
        qp.setTypes("DeleteSearchDoc");
        SearchFdPost search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit", 1, search.result().getResults().size());

        AdminPurgeFortress purge = new AdminPurgeFortress(fdClientIo, "purgeFortressRemovesEsIndex");
        purge.exec();
        integrationHelper.longSleep(); // Give ES time to commit
        assertNull("The entity should not exist because the fortress was purged", entityGet.exec().result());

        assertEquals("The entity search doc was not removed", 0, search.exec().result().getResults().size());


    }

    @Test
    public void purgeSegmentRemovesOnlyTheSpecifiedOne() throws Exception {
        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        DocumentTypeInputBean docType = new DocumentTypeInputBean("DeleteSearchDoc");

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, 1);

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchB")
                .setSegment("2016")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
        entityGet = new EntityGet(fdClientIo, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, 1);

        integrationHelper.shortSleep(); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress("purgeSegment");
        qp.setTypes(docType.getCode());

        SearchFdPost search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("Searching across both segments returns 2", 2, search.result().getResults().size());

        qp.setSegment("2015");
        search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());

        qp.setSegment("2016");
        search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        // Now to purge 2015 so we are left only with 2016
        AdminPurgeFortressSegment delete = new AdminPurgeFortressSegment(fdClientIo, "purgeSegment", docType.getName(), "2015");
        delete.exec();
        integrationHelper.longSleep();

        assertNotNull(search.exec().result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        qp.setSegment("2015");
        assertNotNull("Command failed to execute - " + search.error(), search.exec().error());
//        assertNotNull("Expected an index not found type error", search.result().getFdSearchError());
//        assertTrue("2015 index should be reported as missing", search.result().getFdSearchError().contains("no such index"));

        // Check we can track back into previously purged fortress
        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Find Me")));

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
        integrationHelper.longSleep();
        qp.setSegment("2015");
        search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertNull("Didn't expect an error - " + search.error(), search.error());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());


    }

    /**
     * Much the same as purgeSegmentRemovesOnlyTheSpecifiedOne but ensures the functions
     * work for entities that have no logs
     *
     * @throws Exception anything
     */
    @Test
    public void purgeSegmentEntitiesWithNoLogs() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);
        final String FORTRESS = "purgeSegmentEntitiesWithNoLogs";
        DocumentTypeInputBean docType = new DocumentTypeInputBean(FORTRESS);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setEntityOnly(true);

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentEntitiesWithNoLogs", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, 1);

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchB")
                .setSegment("2016")
                .setDocumentType(docType)
                .setEntityOnly(true);

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
        entityGet = new EntityGet(fdClientIo, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentEntitiesWithNoLogs", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, 1);

        integrationHelper.shortSleep(); // Give ES some extra time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress(FORTRESS);
        qp.setTypes(docType.getCode());

        SearchFdPost search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertNull(search.error());
        assertEquals("Searching across both segments returns 2", 2, search.result().getResults().size());

        qp.setSegment("2015");
        search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());

        qp.setSegment("2016");
        search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        // Now to purge 2015 so we are left only with 2016
        AdminPurgeFortressSegment delete = new AdminPurgeFortressSegment(fdClientIo, FORTRESS, docType.getName(), "2015");
        delete.exec();
        integrationHelper.longSleep();

        assertNotNull(search.exec().result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        qp.setSegment("2015");
        assertNotNull("Command should generate an error", search.exec().error());
//        assertNotNull("Expected an index not found type error", search.result().getFdSearchError());
//        assertTrue("2015 index should be reported as missing", search.result().getFdSearchError().contains("no such index"));

        // Check we can track back into previously purged fortress
        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setEntityOnly(true);

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
        integrationHelper.longSleep();
        qp.setSegment("2015");
        search = new SearchFdPost(fdClientIo, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("Didn't expect an error", null, search.error());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());


    }

    @Test
    public void getEntityFieldStructure() throws Exception {
        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("getEntityFieldStructure")
                        .setSearchEnabled(true))
                .setCode("Katerina Neumannová")
                .setDescription("Katerina Neumannová")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .addTag(new TagInputBean("anyCode", "anyLabel").addEntityTagLink("anyrlx"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")));

        fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean);
        integrationHelper.waitForEntityKey(logger, "getEntityFieldStructure", entityGet);

        EntityResultBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());
        integrationHelper.waitForSearch(logger, "getEntityFieldStructure", entityGet, 1);
        assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, entityGet.result().getSearch());

        // Searching with no parameters
        ContentStructure structure = fdClientIo.entityFields(entityInputBean.getFortress().getName(), entityInputBean.getDocumentType().getName());

        assertNotNull(structure);

        assertTrue("All data columns were un-faceted strings so nothing should be returned", structure.getData().isEmpty());
        assertFalse(structure.getLinks().isEmpty());
        assertFalse(structure.getSystem().isEmpty());
    }

    @Test
    public void persistEntityRelationshipModel() throws Exception {
        integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/test-entity-relationships.json");
        assertNotNull(contentModel);
        Collection<ContentModel> models = new ArrayList<>();
        models.add(contentModel);

        ModelPost sendModels = new ModelPost(fdClientIo, models);
        assertNull(sendModels.exec().error());
        assertEquals("Expected a response equal to the number of inputs", 1, sendModels.result().size());
        ContentModel found = fdClientIo.getContentModel(contentModel.getFortress().getName(), contentModel.getDocumentType().getCode());
        assertNotNull(found);
        assertFalse(found.getContent().isEmpty());
    }

    @Test
    public void versionableEntity() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("value", "alpha");
        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntity")
                        .setSearchEnabled(true)
                        .setStoreEnabled(true))
                .setDocumentType(new DocumentTypeInputBean("someThing"))
                .setContent(new ContentInputBean(dataMap));

        TrackEntityPost trackEntity = new TrackEntityPost(fdClientIo, entityInputBean);

        integrationHelper.assertWorked("Track Entity - ", trackEntity.exec());

        assertNotNull(trackEntity.result());
        assertNotNull(trackEntity.result().getKey());
        assertEquals("Should be a new Entity", trackEntity.result().isNewEntity(), true);
        assertEquals("Problem creating the Content", trackEntity.result().getLogStatus(), ContentInputBean.LogStatus.OK);

        EntityGet foundEntity = new EntityGet(fdClientIo, trackEntity.result().getKey());
        integrationHelper.waitForEntityKey(logger, "versionableEntity", foundEntity.exec());
        integrationHelper.assertWorked("Find Entity - ", foundEntity);
        assertNotNull(foundEntity.result().getKey());

        EntityLogsGet getLogs = new EntityLogsGet(fdClientIo, foundEntity.result().getKey());
        getLogs.exec();
        assertEquals("Expected one log", 1, getLogs.result().length);
        EntityLogResult foundLog = getLogs.result()[0];
        assertEquals("Data value mismatch", dataMap.get("value").toString(), foundLog.getData().get("value").toString());

        // Now test that the value updates
        dataMap.put("value", "beta");
        entityInputBean.setContent(new ContentInputBean(dataMap));
        entityInputBean.setKey(foundEntity.result().getKey());

        trackEntity = new TrackEntityPost(fdClientIo, entityInputBean);
        integrationHelper.assertWorked("Track Entity - ", trackEntity.exec());
        integrationHelper.shortSleep();
        getLogs.exec();
        assertEquals("Expected two logs", 2, getLogs.result().length);

    }

    @Test
    public void suppressVersionsOnByDocBasis() throws Exception {

        SystemUserResultBean login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        assertNotNull(login);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("value", "alpha");
        EntityInputBean entityInputBean = new EntityInputBean()
                .setCode(new Date().toString())
                .setFortress(new FortressInputBean("suppressVersionsOnByDocBasis")
                        .setSearchEnabled(true)
                        .setStoreEnabled(true)) // Enable the store
                .setDocumentType(new DocumentTypeInputBean("someThing")
                        .setVersionStrategy(Document.VERSION.DISABLE)) // But suppress version history for this class of Entity
                .setContent(new ContentInputBean(dataMap));
        EntityGet entityGet = new EntityGet(fdClientIo, entityInputBean).exec();
        assertTrue("Expected entity not to exist", entityGet.error()!=null);
        TrackEntityPost trackEntityPost = new TrackEntityPost(fdClientIo, entityInputBean);

        integrationHelper.assertWorked("Track Entity - ", trackEntityPost.exec());
        TrackRequestResult trackResult = trackEntityPost.result();
        String key = trackResult.getKey();

        assertEquals("Should be a new Entity", true, trackResult.isNewEntity());
        assertEquals("Problem creating the Content", trackResult.getLogStatus(), ContentInputBean.LogStatus.OK);

        EntityLogsGet getLogs = new EntityLogsGet(fdClientIo, key);
        getLogs.exec();
        assertEquals("Expecting one Mock log", 1, getLogs.result().length);
        EntityLogResult mockedLog = getLogs.result()[0];
        assertTrue ( "Log was not flagged as mocked",mockedLog.isMocked());
        
        EntityData entityDataByKey = new EntityData(fdClientIo, key);
        integrationHelper.assertWorked("Get Data by key = ", entityDataByKey.exec());

        assertNotNull(entityDataByKey.result());
        assertFalse(entityDataByKey.result().isEmpty());
        TestCase.assertEquals(dataMap.get("value"), entityDataByKey.result().get("value"));

        EntityData entityDataByCode = new EntityData(fdClientIo, entityInputBean);
        integrationHelper.assertWorked("Get Data by code= ", entityDataByCode.exec());

        assertNotNull(entityDataByCode.result());
        assertFalse(entityDataByCode.result().isEmpty());
        TestCase.assertEquals(dataMap.get("value"), entityDataByCode.result().get("value"));

        // Now test that the value updates
        dataMap.put("value", "beta");
        entityInputBean.setContent(new ContentInputBean(dataMap));

        EntityGet entityByKey = new EntityGet(fdClientIo, key);
        integrationHelper.waitForEntityKey(logger, "suppressVersionsOnByDocBasis", entityByKey.exec());
        integrationHelper.assertWorked("Find Entity by key - ", entityByKey);

        EntityGet entityByCode = new EntityGet(fdClientIo, entityInputBean);
        integrationHelper.waitForEntityKey(logger, "suppressVersionsOnByDocBasis", entityByCode.exec());
        integrationHelper.assertWorked("Find Entity by code - ", entityByCode);

        entityInputBean.setKey(entityByKey.result().getKey());

        trackEntityPost = new TrackEntityPost(fdClientIo, entityInputBean);
        integrationHelper.assertWorked("Track Entity - ", trackEntityPost.exec());
        integrationHelper.shortSleep();
        entityDataByKey = new EntityData(fdClientIo, entityByKey.result().getKey());
        integrationHelper.assertWorked("Get Data", entityDataByKey.exec());

        assertNotNull(entityDataByKey.result());
        assertFalse(entityDataByKey.result().isEmpty());
        TestCase.assertEquals(dataMap.get("value"), entityDataByKey.result().get("value"));
    }

    private Collection<TagInputBean> getRandomTags(String label, String code) {
        int i = 0;
        int max = 20;

        Collection<TagInputBean> tags = new ArrayList<>();
        while (i < max) {
            TagInputBean tagInputBean = new TagInputBean(code + i, label + i);
            tags.add(tagInputBean);

            i++;
        }
        return tags;
    }

}
