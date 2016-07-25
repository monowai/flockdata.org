/*
 *  Copyright 2012-2016 the original author or authors.
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

import org.flockdata.client.FdTemplate;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.helper.JsonUtils;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileDeserializer;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.*;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.flockdata.test.integration.IntegrationHelper.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Establishes the integration test environment. Descendant classes use @Test functions against
 * this established stack
 * <p>
 * Created by mike on 3/04/16.
 */
@SpringApplicationConfiguration({
        ClientConfiguration.class,
        FdBatchWriter.class,
        FdTemplate.class,
        FdRabbitClient.class,
        AmqpRabbitConfig.class,
        Exchanges.class,
        FileProcessor.class,
        IntegrationHelper.class

})
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ActiveProfiles({"fd-server", "fd-batch"})
public class ITests {


    @ClassRule
    public static FdDocker stack = new FdDocker();

//    private static DockerComposeContainer stack = FdDocker.stack;

//    @ClassRule
//    public static ExternalResource resource= new ExternalResource() {
//        @Override
//        protected void before() throws Throwable {
//            if ( stack!=null)
//                stack.start();
//        }
//
//        @Override
//        protected void after() {
//            if (stack!=null)
//                stack.stop();
//        }
//    };

    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    IntegrationHelper integrationHelper;

    private SearchHelper searchHelper = new SearchHelper();

    /**
     * Contains a RestTemplate configured to talk to FlockData. By default this is fd-engine
     * but by calling clientConfiguration.setServiceUrl(...) it can be used to talk to
     * fd-search or fd-store. Only fd-engine is secured by default
     */
    @Autowired
    private FdTemplate fdTemplate;

    private static Logger logger = LoggerFactory.getLogger(ITests.class);

    @Before
    public void setupServices() {
        clientConfiguration.setServiceUrl(getEngine());
        integrationHelper.waitForServices();
    }

    @Test
    public void aaPingFdStore() {
        logger.info("Ping FD Store");
        clientConfiguration.setServiceUrl(integrationHelper.getStore());
        String result = fdTemplate.ping();
        assertEquals("Couldn't ping fd-store", "pong", result);
    }

    @Test
    public void aaPingFdEngine() {
        logger.info("Ping FD Engine");
        String result = fdTemplate.ping();
        assertEquals("Couldn't ping fd-engine", "pong", result);
    }

    @Test
    public void aaPingFdSearch() {
        logger.info("Ping FD Search");
        clientConfiguration.setServiceUrl(integrationHelper.getSearch());

        Ping ping = new Ping(fdTemplate);
        ping.exec();
        assertTrue(ping.error(), ping.worked());

        assertEquals("Couldn't ping fd-search", "pong", ping.result());
    }

    @Test
    public void aaStatusChecks() {
        logger.info("HealthChecks");
        // If the services can't see each other, its not worth proceeding
        integrationHelper.login(fdTemplate, ADMIN_REGRESSION_USER, "123").exec();
        Login login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        integrationHelper.assertWorked("Login error ", login.exec());
        assertTrue("Unexpected login error " + login.error(), login.worked());
        Health health = new Health(fdTemplate);
        integrationHelper.assertWorked("Health Check", health.exec());

        Map<String, Object> healthResult = health.result();
        assertTrue("Should be more than 1 entry in the health results", healthResult.size() > 1);
        assertNotNull("Could not find an entry for fd-search", healthResult.get("fd-search"));
        assertTrue("Failure for fd-engine to connect to fd-search in the container " + healthResult.get("fd-search"), healthResult.get("fd-search").toString().toLowerCase().startsWith("ok"));
        assertNotNull("Could not find an entry for fd-store", healthResult.get("fd-store"));
        assertTrue("Failure for fd-engine to connect to fd-store in the container", healthResult.get("fd-store").toString().toLowerCase().startsWith("ok"));


    }

    @Test
    public void simpleLogin() {
        logger.info("Testing simpleLogin");
        clientConfiguration.setHttpUser(IntegrationHelper.ADMIN_REGRESSION_USER);
        clientConfiguration.setHttpPass("123");
        clientConfiguration.setServiceUrl(getEngine());
        SystemUserResultBean suResult = fdTemplate.login();
        assertNotNull(suResult);
        assertTrue("User Roles missing", suResult.getUserRoles().length != 0);
    }

    @Test
    public void registration() {
        // An authorised user can create DataAccess users for a given company
        integrationHelper.assertWorked("Registration failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, IntegrationHelper.ADMIN_REGRESSION_PASS).exec());
        SystemUserResultBean suResult = integrationHelper.makeDataAccessUser();
        assertNotNull(suResult);
        assertNotNull(suResult.getApiKey());

    }

    /**
     * FlockData ships with some basic static data like Countries and Cities.
     * This test checks that they are tracked in to the service. Validates a number of things:
     * Country Content Profile
     * Tag being tracked over an AMQP endpoint
     * Countries can be found via the Tag endpoint by label
     *
     */
    @Test
    public void loadCountries() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());

        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());
        clientConfiguration.setBatchSize(5);

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/countries.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/countries.json", contentModel);
//       ToDo: figure out getting ImportProfiles into the fileProcessor
        int countryInputs = fileProcessor.processFile(extractProfile, "/fd-cow.txt");

        assertEquals("Countries not processed", 250, countryInputs); //+1 "Undefined"
        TagsGet countries = new TagsGet(fdTemplate, "Country");
        // Tags are processed over a messageQ so will take a wee bit of time to be processed
        integrationHelper.longSleep();
        countries.exec();
        assertTrue("Error loading countries" + countries.error(), countries.worked());

        TagResultBean[] countryResults = countries.result();
        // By this stage we may or may not have processed all the tags depending on how resource constrained the host machine
        // running the integration stack is. So we'll just check at least two batches of 10 have been processed assuming the
        // rest will pass
        assertTrue("No countries found!", countryResults.length > 10);

        TagGet countryByIsoShort = new TagGet(fdTemplate, "Country", "AU");
        countryByIsoShort.exec();
        assertTrue(countryByIsoShort.error(), countryByIsoShort.worked());
        assertNotNull("Couldn't find Australia", countryByIsoShort.result());

        TagGet countryByIsoLong = new TagGet(fdTemplate, "Country", "AUS");
        countryByIsoLong.exec();
        assertTrue(countryByIsoLong.error(), countryByIsoLong.worked());
        assertNotNull("Couldn't find Australia", countryByIsoLong.result());


        TagGet countryByName = new TagGet(fdTemplate, "Country", "Australia");
        countryByName.exec();
        integrationHelper.assertWorked("Country by name - ", countryByName);
        assertNotNull("Couldn't find Australia", countryByName.result());

        assertEquals("By Code and By Name they are the same country so should equal", countryByIsoShort.result(), countryByName.result());
        assertEquals("By short code and long code they are the same country so should equal", countryByIsoLong.result(), countryByIsoShort.result());
        integrationHelper.shortSleep();
        QueryParams qp = searchHelper.getTagQuery("country", "australia");
        SearchEsPost searchEsPost = new SearchEsPost(fdTemplate, qp);
        integrationHelper.assertWorked("Located Country by name", searchEsPost);
        searchHelper.assertHitCount("Should have found just 1 hit for Australia", 1, searchEsPost.result());

        searchEsPost = new SearchEsPost(fdTemplate, searchHelper
                .getTagMatchQuery("country", "aka.bgn_longname", "commonwealth of australia"));
        searchHelper.assertHitCount("Didn't find Australia by alias", 1, searchEsPost.exec().result());

    }

    @Test
    public void trackEntityOverHttp() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntity", false))
                .setDocumentType(new DocumentTypeInputBean("someThing"))
                .setContent(new ContentInputBean(Helper.getRandomMap()))
                .addTag(new TagInputBean("someCode", "SomeLabel"));
        TrackEntityPost trackEntity = new TrackEntityPost(fdTemplate, entityInputBean);

        integrationHelper.assertWorked("Track Entity - ", trackEntity.exec());

        assertNotNull(trackEntity.result());
        assertNotNull(trackEntity.result().getKey());
        assertEquals("Should be a new Entity", trackEntity.result().isNewEntity(), true);
        assertEquals("Problem creating the Content", trackEntity.result().getLogStatus(), ContentInputBean.LogStatus.OK);

        EntityGet foundEntity = new EntityGet(clientConfiguration, fdTemplate, trackEntity.result().getKey());
        integrationHelper.waitForEntityKey(logger, "trackEntityOverHttp", foundEntity.exec());
        integrationHelper.assertWorked("Find Entity - ", foundEntity);
        assertNotNull(foundEntity.result().getKey());

    }

    @Test
    public void trackEntityOverAmqpThenFindInSearch() throws Exception {

        Login login = integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec();
        integrationHelper.assertWorked("Login failed ", login);
        SystemUserResultBean su= integrationHelper.makeDataAccessUser();
        assertNotNull(su);
        clientConfiguration.setApiKey(su.getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntityAmqp")
                        .setSearchEnabled(true))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")))
                .addTag(new TagInputBean("someCode", "SomeLabel"));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean)
                .exec();

        integrationHelper.waitForEntityKey(logger, "trackEntityOverAmqpThenFindInSearch", entityGet);

        EntityBean entityResult = entityGet.result();
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

        SearchFdPost search = new SearchFdPost(fdTemplate, qp);
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

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("validateEntityLogs", false))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("validateEntityLogs"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "value")))
                .addTag(new TagInputBean("someCode", "SomeLabel"));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean).exec();
        integrationHelper.waitForEntityKey(logger, "validateEntityLogs", entityGet);

        EntityBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());

        EntityLogsGet entityLogs = new EntityLogsGet(fdTemplate, entityResult.getKey());
        integrationHelper.waitForEntityLog(logger, "validateEntityLogs", entityLogs, 1);
        assertNotNull(entityLogs.result());
        assertEquals("Didn't find a log", 1, entityLogs.result().length);
        assertNotNull("No data was returned", entityLogs.result()[0].getData());
        assertEquals("Content property not found", "value", entityLogs.result()[0].getData().get("key"));

        entityInputBean.setKey(entityResult.getKey())
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "updated")));

        // Updating an existing entity
        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        integrationHelper.waitForEntityLog(logger, "validateEntityLogs", entityLogs, 2);
        assertEquals("Didn't find the second log", 2, entityLogs.result().length);
        assertEquals("Didn't find the updated field as the first result", "updated", entityLogs.result()[0].getData().get("key"));
        assertEquals("Didn't find the original field as the second result", "value", entityLogs.result()[1].getData().get("key"));
    }

    @Test
    public void findByESPassThroughWithUTF8() throws Exception {

        Login login = integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123");
        integrationHelper.assertWorked("Login failed ", login.exec());
        SystemUserResultBean su = integrationHelper.makeDataAccessUser();
        assertNotNull("message = "+login.error(), su);
        clientConfiguration.setApiKey(su.getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("findByESPassThroughWithUTF8")
                        .setSearchEnabled(true))
                .setCode("Katerina Neumannová")
                .setDescription("Katerina Neumannová")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean);
        integrationHelper.waitForEntityKey(logger, "findByESPassThroughWithUTF8", entityGet);

        EntityBean entityResult = entityGet.result();
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

        SearchEsPost search = new SearchEsPost(fdTemplate, qp);
        integrationHelper.assertWorked("Search Reply ", search);

        assertFalse("errors were found " + search.result().get("errors"), search.result().containsKey("errors"));

        searchHelper.assertHitCount("Expected 1 hit", 1, search.result());
        assertTrue("UTF-8 failure. Couldn't find " + entityInputBean.getCode(), searchHelper.getHits(search.result()).contains(entityInputBean.getCode()));
    }

    @Test
    public void simpleTags() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        Collection<TagInputBean> tags = new ArrayList<>();
        String tagLabel = "simpleTag";
        TagInputBean tagInputBean = new TagInputBean("ACode", tagLabel).setDescription("Description of the Tag");
        tags.add(tagInputBean);
        tagInputBean = new TagInputBean("BCode", tagLabel);
        tags.add(tagInputBean);

        fdTemplate.writeTags(tags);
        integrationHelper.longSleep();  // Async delivery, so lets wait a bit....
        TagsGet getTags = new TagsGet(fdTemplate, tagLabel);

        integrationHelper.waitForTagCount(logger, "simpleTags", getTags, 2);

        TagResultBean[] foundTags = getTags.exec().result();
        assertNotNull(foundTags);
        assertEquals("Missing tags", 2, foundTags.length);

        QueryParams qp = searchHelper.getTagQuery(tagInputBean.getLabel(), "*");

        SearchEsPost search = new SearchEsPost(fdTemplate, qp);
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
        fdTemplate.writeTags(tags);
        integrationHelper.longSleep();  // Async delivery, so lets wait a bit....

        qp = searchHelper.getTagQuery(tagInputBean.getLabel(), "wonder");

        search = new SearchEsPost(fdTemplate, qp);
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
        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());
        Collection<TagInputBean> setA = getRandomTags("codea", "Set");
        Collection<TagInputBean> setB = getRandomTags("codea", "Set");
        Collection<TagInputBean> setC = getRandomTags("codea", "Set");
        Collection<TagInputBean> setD = getRandomTags("codea", "Set");
        fdTemplate.writeTags(setA);
        fdTemplate.writeTags(setB);
        fdTemplate.writeTags(setC);
        fdTemplate.writeTags(setD);
        integrationHelper.longSleep();
        QueryParams qp = searchHelper.getTagQuery("Set*", "code*");
        SearchEsPost search = new SearchEsPost(fdTemplate, qp);
        search.exec();
        integrationHelper.assertWorked("Not finding any tags", search);


    }

    @Test
    public void purgeFortressRemovesEsIndex() throws Exception {
        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeFortressRemovesEsIndex")
                        .setSearchEnabled(true))
                .setCode("SearchDoc")
                .setDocumentType(new DocumentTypeInputBean("DeleteSearchDoc"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeFortressRemovesEsIndex", entityGet);
        integrationHelper.waitForSearch(logger, "purgeFortressRemovesEsIndex", entityGet, 1);
        integrationHelper.longSleep(); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress("purgeFortressRemovesEsIndex");
        qp.setTypes("DeleteSearchDoc");
        SearchFdPost search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit", 1, search.result().getResults().size());

        AdminPurgeFortress purge = new AdminPurgeFortress(fdTemplate, "purgeFortressRemovesEsIndex");
        purge.exec();
        integrationHelper.longSleep(); // Give ES time to commit
        assertNull("The entity should not exist because the fortress was purged", entityGet.exec().result());

        assertEquals("The entity search doc was not removed", 0, search.exec().result().getResults().size());


    }

    @Test
    public void purgeSegmentRemovesOnlyTheSpecifiedOne() throws Exception {
        Login loginResult = integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec();
        integrationHelper.assertWorked("Login failed ", loginResult);
        SystemUserResultBean su = integrationHelper.makeDataAccessUser();
        assertNotNull("Data Access User should not be null", su);
        clientConfiguration.setApiKey(su.getApiKey());

        DocumentTypeInputBean docType = new DocumentTypeInputBean("DeleteSearchDoc");

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, 1);

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchB")
                .setSegment("2016")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, 1);

        integrationHelper.shortSleep(); // Give ES time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress("purgeSegment");
        qp.setTypes(docType.getCode());

        SearchFdPost search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("Searching across both segments returns 2", 2, search.result().getResults().size());

        qp.setSegment("2015");
        search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());

        qp.setSegment("2016");
        search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        // Now to purge 2015 so we are left only with 2016
        AdminPurgeFortressSegment delete = new AdminPurgeFortressSegment(fdTemplate, "purgeSegment", docType.getName(), "2015");
        delete.exec();
        integrationHelper.longSleep();

        assertNotNull(search.exec().result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        qp.setSegment("2015");
        assertNotNull("Command failed to execute", search.exec().result());
        assertNotNull("Expected an index not found type error", search.result().getFdSearchError());
        assertTrue("2015 index should be reported as missing", search.result().getFdSearchError().contains("2015"));

        // Check we can track back into previously purged fortress
        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("purgeSegment")
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Find Me")));

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        integrationHelper.longSleep();
        qp.setSegment("2015");
        search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertNull("Didn't expect an error - " + search.error(), search.error());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());


    }

    /**
     * Much the same as purgeSegmentRemovesOnlyTheSpecifiedOne but ensures the functions
     * work for entities that have no logs
     *
     * @throws Exception   anything
     */
    @Test
    public void purgeSegmentEntitiesWithNoLogs() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());
        final String FORTRESS = "purgeSegmentEntitiesWithNoLogs";
        DocumentTypeInputBean docType = new DocumentTypeInputBean(FORTRESS);

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setEntityOnly(true);

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentEntitiesWithNoLogs", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, 1);

        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchB")
                .setSegment("2016")
                .setDocumentType(docType)
                .setEntityOnly(true);

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        entityGet = new EntityGet(clientConfiguration, fdTemplate, entityInputBean);

        integrationHelper.waitForEntityKey(logger, "purgeSegmentEntitiesWithNoLogs", entityGet);
        integrationHelper.waitForSearch(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, 1);

        integrationHelper.shortSleep(); // Give ES some extra time to commit

        QueryParams qp = new QueryParams("*");
        qp.setFortress(FORTRESS);
        qp.setTypes(docType.getCode());

        SearchFdPost search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertNull(search.error());
        assertEquals("Searching across both segments returns 2", 2, search.result().getResults().size());

        qp.setSegment("2015");
        search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());

        qp.setSegment("2016");
        search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        // Now to purge 2015 so we are left only with 2016
        AdminPurgeFortressSegment delete = new AdminPurgeFortressSegment(fdTemplate, FORTRESS, docType.getName(), "2015");
        delete.exec();
        integrationHelper.longSleep();

        assertNotNull(search.exec().result());
        assertEquals("expected 1 hit on segment 2016", 1, search.result().getResults().size());

        qp.setSegment("2015");
        assertNotNull("Command failed to execute", search.exec().result());
        assertNotNull("Expected an index not found type error", search.result().getFdSearchError());
        assertTrue("2015 index should be reported as missing", search.result().getFdSearchError().contains("2015"));

        // Check we can track back into previously purged fortress
        entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean(FORTRESS)
                        .setSearchEnabled(true))
                .setCode("MySearchA")
                .setSegment("2015")
                .setDocumentType(docType)
                .setEntityOnly(true);

        fdTemplate.writeEntities(integrationHelper.toCollection(entityInputBean));
        integrationHelper.longSleep();
        qp.setSegment("2015");
        search = new SearchFdPost(fdTemplate, qp)
                .exec();

        assertNotNull(search.result());
        assertNull("Didn't expect an error", search.error());
        assertEquals("expected 1 hit on segment 2015", 1, search.result().getResults().size());


    }

    private Collection<TagInputBean> getRandomTags(String code, String label) {
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
