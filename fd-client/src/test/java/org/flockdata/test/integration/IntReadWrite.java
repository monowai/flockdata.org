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

import org.flockdata.client.amqp.AmqpServices;
import org.flockdata.client.commands.*;
import org.flockdata.client.rest.FdRestWriter;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.flockdata.test.integration.IntegrationHelper.getEngine;
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
        FdBatcher.class,
        FdRestWriter.class,
        Exchanges.class,
        FileProcessor.class,
        IntegrationHelper.class,
        AmqpRabbitConfig.class,
        AmqpServices.class

})
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ActiveProfiles("fd-server")
public class IntReadWrite {


    // Uncomment this ClassRue to run the stack only for this class
    //  otherwise leave it commented and run the Suite
//    @ClassRule
//    public static FdDocker stack = new FdDocker();

    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    IntegrationHelper integrationHelper;

    @Autowired
    private AmqpServices amqpServices;

    private SearchHelper searchHelper = new SearchHelper();

    @Before
    public void setupServices() {
        integrationHelper.waitForServices();
    }

    /**
     * Contains a RestTemplate configured to talk to FlockData. By default this is fd-engine
     * but by calling clientConfiguration.setServiceUrl(...) it can be used to talk to
     * fd-search or fd-store. Only fd-engine is secured by default
     */
    @Autowired
    private FdRestWriter fdRestWriter;

    private static Logger logger = LoggerFactory.getLogger(IntReadWrite.class);

    @Test
    public void simpleLogin() {
        logger.info("Testing simpleLogin");
        clientConfiguration.setHttpUser(IntegrationHelper.ADMIN_REGRESSION_USER);
        clientConfiguration.setHttpPass("123");
        clientConfiguration.setServiceUrl(getEngine());
        SystemUserResultBean suResult = fdRestWriter.login(clientConfiguration);
        assertNotNull(suResult);
        assertTrue("User Roles missing", suResult.getUserRoles().length != 0);
    }

    @Before
    public void resetClientConfiguration() {
        clientConfiguration.setServiceUrl(getEngine());
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
     * @throws Exception
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

        assertEquals("Countries not processed", countryInputs, 249);
        TagsGet countries = new TagsGet(clientConfiguration, fdRestWriter, "Country");
        // Tags are processed over a messageQ so will take a wee bit of time to be processed
        integrationHelper.longSleep();
        countries.exec();
        assertTrue("Error loading countries" + countries.error(), countries.worked());

        TagResultBean[] countryResults = countries.result();
        // By this stage we may or may not have processed all the tags depending on how resource constrained the host machine
        // running the integration stack is. So we'll just check at least two batches of 10 have been processed assuming the
        // rest will pass
        assertTrue("No countries found!", countryResults.length > 10);

        TagGet countryByIsoShort = new TagGet(clientConfiguration, fdRestWriter, "Country", "AU");
        countryByIsoShort.exec();
        assertTrue(countryByIsoShort.error(), countryByIsoShort.worked());
        assertNotNull("Couldn't find Australia", countryByIsoShort.result());

        TagGet countryByIsoLong = new TagGet(clientConfiguration, fdRestWriter, "Country", "AUS");
        countryByIsoLong.exec();
        assertTrue(countryByIsoLong.error(), countryByIsoLong.worked());
        assertNotNull("Couldn't find Australia", countryByIsoLong.result());


        TagGet countryByName = new TagGet(clientConfiguration, fdRestWriter, "Country", "Australia");
        countryByName.exec();
        integrationHelper.assertWorked("Country by name - ", countryByName);
        assertNotNull("Couldn't find Australia", countryByName.result());

        assertEquals("By Code and By Name they are the same country so should equal", countryByIsoShort.result(), countryByName.result());
        assertEquals("By short code and long code they are the same country so should equal", countryByIsoLong.result(), countryByIsoShort.result());
        integrationHelper.shortSleep();
        QueryParams qp = searchHelper.getTagQuery("country", "australia");
        SearchEsPost searchEsPost = new SearchEsPost(clientConfiguration, fdRestWriter, qp);
        integrationHelper.assertWorked("Located Country by name", searchEsPost);
        searchHelper.assertHitCount("Should have found just 1 hit for Australia", 1, searchEsPost.result());

        searchEsPost = new SearchEsPost(clientConfiguration, fdRestWriter, searchHelper
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
        TrackEntityPost trackEntity = new TrackEntityPost(clientConfiguration, fdRestWriter, entityInputBean);

        integrationHelper.assertWorked("Track Entity - ", trackEntity.exec());

        assertNotNull(trackEntity.result());
        assertNotNull(trackEntity.result().getKey());
        assertEquals("Should be a new Entity", trackEntity.result().isNewEntity(), true);
        assertEquals("Problem creating the Content", trackEntity.result().getLogStatus(), ContentInputBean.LogStatus.OK);

        EntityGet foundEntity = new EntityGet(clientConfiguration, fdRestWriter, trackEntity.result().getKey());
        integrationHelper.waitForEntityKey(logger, foundEntity.exec());
        integrationHelper.assertWorked("Find Entity - ", foundEntity);
        assertNotNull(foundEntity.result().getKey());

    }

    @Test
    public void trackEntityOverAmqpThenFindInSearch() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntityAmqp")
                        .setSearchEnabled(true))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")))
                .addTag(new TagInputBean("someCode", "SomeLabel"));


        amqpServices.publish(integrationHelper.toCollection(entityInputBean));

        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean)
                .exec();

        integrationHelper.waitForEntityKey(logger, entityGet);

        EntityBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());
        integrationHelper.waitForSearch(logger, entityGet, 1);
        entityResult = entityGet.result();
        assertFalse("Search was incorrectly suppressed", entityResult.isSearchSuppressed());
        assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, entityResult.getSearch());
        assertEquals("Search Key was not set to the code of the entityInput", entityInputBean.getCode(), entityResult.getSearchKey());

        integrationHelper.shortSleep();
        QueryParams qp = new QueryParams(entityResult.getCode())
                .setFortress(entityInputBean.getFortress().getName());

        SearchFdPost search = new SearchFdPost(clientConfiguration, fdRestWriter, qp);
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

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean).exec();
        integrationHelper.waitForEntityKey(logger, entityGet);

        EntityBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());

        EntityLogsGet entityLogs = new EntityLogsGet(clientConfiguration, fdRestWriter, entityResult.getKey());
        integrationHelper.waitForEntityLog(logger, entityLogs, 1);
        assertNotNull(entityLogs.result());
        assertEquals("Didn't find a log", 1, entityLogs.result().length);
        assertNotNull("No data was returned", entityLogs.result()[0].getData());
        assertEquals("Content property not found", "value", entityLogs.result()[0].getData().get("key"));

        entityInputBean.setKey(entityResult.getKey())
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "updated")));

        // Updating an existing entity
        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        integrationHelper.waitForEntityLog(logger, entityLogs, 2);
        assertEquals("Didn't find the second log", 2, entityLogs.result().length);
        assertEquals("Didn't find the updated field as the first result", "updated", entityLogs.result()[0].getData().get("key"));
        assertEquals("Didn't find the original field as the second result", "value", entityLogs.result()[1].getData().get("key"));
    }

    @Test
    public void findByESPassThroughWithUTF8() throws Exception {

        integrationHelper.assertWorked("Login failed ", integrationHelper.login(IntegrationHelper.ADMIN_REGRESSION_USER, "123").exec());
        clientConfiguration.setApiKey(integrationHelper.makeDataAccessUser().getApiKey());

        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("findByESPassThroughWithUTF8")
                        .setSearchEnabled(true))
                .setCode("Katerina Neumannová")
                .setDescription("Katerina Neumannová")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")));

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);
        integrationHelper.waitForEntityKey(logger, entityGet);

        EntityBean entityResult = entityGet.result();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());
        integrationHelper.waitForSearch(logger, entityGet, 1);
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

        SearchEsPost search = new SearchEsPost(clientConfiguration, fdRestWriter, qp);
        integrationHelper.assertWorked("Search Reply ", search);

        assertFalse("errors were found " + search.result().get("errors"), search.result().containsKey("errors"));

        searchHelper.assertHitCount("Expected 1 hit", 1, search.result());
        assertTrue("UTF-8 failure. Couldn't find " + entityInputBean.getCode(), searchHelper.getHits(search.result()).contains(entityInputBean.getCode()));
    }
}
