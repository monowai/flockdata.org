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
import org.flockdata.profile.ContentProfileDeserializer;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.registration.*;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.*;
import org.flockdata.test.Helper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import static junit.framework.TestCase.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

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
public class ITDockerStack {
    private static final int DEBUG_ENGINE = 61000;
    private static final int DEBUG_SEARCH = 61001;
    private static final int DEBUG_STORE = 61002;

    private static final int SERVICE_ENGINE = 8090;
    private static final int SERVICE_SEARCH = 8091;
    private static final int SERVICE_STORE = 8092;

    @Value("${org.fd.test.pause:150}")
    private
    int waitSeconds;

    // start is called before the tests are run and stop is called after the class
    // is finished. Given that is how Junit wants to play, all integration tests that
    // want to use this stack should be in this class, otherwise a new Stack will be created
    // increasing the time it takes to start

    // http://testcontainers.viewdocs.io/testcontainers-java/usage/docker_compose/
    @ClassRule
//    public static DockerComposeContainer stack = null;
    public static DockerComposeContainer stack =// null;
                new DockerComposeContainer(new File("src/test/resources/int/docker-compose.yml"))
                        .withExposedService("rabbit_1", 5672)
                        .withExposedService("rabbit_1", 15672)
                        .withExposedService("fdengine_1", SERVICE_ENGINE)
                        .withExposedService("fdengine_1", DEBUG_ENGINE)
                        .withExposedService("fdsearch_1", SERVICE_SEARCH)
                        .withExposedService("fdsearch_1", DEBUG_SEARCH)
                        .withExposedService("fdstore_1", SERVICE_STORE)
                        .withExposedService("fdstore_1", DEBUG_STORE);

    private static Logger logger = LoggerFactory.getLogger(ITDockerStack.class);

    /**
     * Contains properties used by rabbitConfig and fdRestWriter
     */
    @Autowired
    private ClientConfiguration clientConfiguration;

    @Autowired
    private FileProcessor fileProcessor; // Legacy CSV file processor

    @Autowired
    IntegrationHelper integrationHelper;

    /**
     * Rabbit connectivity
     */
    @Autowired
    private AmqpRabbitConfig rabbitConfig;

    @Autowired
    private AmqpServices amqpServices;

    /**
     * Contains a RestTemplate configured to talk to FlockData. By default this is fd-engine
     * but by calling clientConfiguration.setServiceUrl(...) it can be used to talk to
     * fd-search or fd-store. Only fd-engine is secured by default
     */
    @Autowired
    private FdRestWriter fdRestWriter;

    private static boolean setupComplete = false;

    // If any one of FD's services fail to come up we can't perform integration testing
    private static boolean stackFailed = false;

    @Before
    public void waitForServices() throws InterruptedException {
        clientConfiguration.setServiceUrl(getEngine());
        if (stackFailed)
            fail("Stack has failed to startup cleanly - test will fail");
        if (setupComplete)
            return; // This method is called before every @Test - it's expensive :o)

        logger.debug("Running with debug logging");
        clientConfiguration.setServiceUrl(getEngine());
        Ping enginePing = new Ping(clientConfiguration, fdRestWriter);
        clientConfiguration.setServiceUrl(getStore());
        Ping storePing = new Ping(clientConfiguration, fdRestWriter);
        clientConfiguration.setServiceUrl(getSearch());
        Ping searchPing = new Ping(clientConfiguration, fdRestWriter);
        rabbitConfig.setServicePoint(getIpAddress(), getRabbitPort());

        logger.info("FDEngine - {} - reachable @ {}", SERVICE_ENGINE, getEngine());
        logger.info("FDSearch - {} - reachable @ {}", SERVICE_SEARCH, getSearch());
        logger.info("FDStore - {} - reachable @ {}", SERVICE_STORE, getStore());
        logger.info("FDEngine-Debug - {} - reachable @ {}", DEBUG_ENGINE, getIpAddress() + ":" + getEngineDebug());
        logger.info("FDSearch-Debug - {} - reachable @ {}", DEBUG_SEARCH, getIpAddress() + ":" + getSearchDebug());
        logger.info("FDStore-Debug  - {} - reachable @ {}", DEBUG_STORE, getIpAddress() + ":" + getStoreDebug());
        logger.info("Rabbit Admin on http://{}:{}", getIpAddress(), getRabbitAdmin());
        // ToDo: Bind in yourkit profiler and expose port

        logger.info("Initial wait for docker containers to startup. --org.fd.test.pause={} seconds ..... ", waitSeconds);

        if (stack != null)
            integrationHelper.pauseUntil(enginePing, "pong", waitSeconds);

        waitForService("fd-engine", enginePing, getEngine(), 30);
        waitForService("fd-search", searchPing, getSearch(), 30);
        waitForService("fd-store", storePing, getStore(), 30);

        if (!stackFailed)
            logger.info("Stack is running");
        else
            logger.error("Failed to start the stack");

        setupComplete = true;
    }

    private void waitForService(String service, Ping pingCommand, String url, int countDown) throws InterruptedException {
        String result;
        if (stackFailed)
            return;
        logger.info("looking for {}", service);
        do {
            countDown--;

            result = pingCommand.exec();
            if (!result.equals("pong")) {
                int waitSecs = (stack == null ? 2 : 60);
                logger.info("Waiting {} seconds for {} to come on-line", waitSecs, service);
                integrationHelper.pauseUntil(pingCommand, "pong", waitSecs);
            }

        } while (!Objects.equals(result, "pong") && countDown > 0);

        if (!result.equals("pong")) {
            setupComplete = true;
            stackFailed = true;
            fail("Failed to ping " + service + " before timeout");
        }
        logger.info("{} is running. [{}]", service, url);
    }

    /**
     * A login is associated with a single company. Create different fortresses to partion
     * data access users.
     * <p>
     * The user name you want to create has to exist in the security context otherwise login will fail
     *
     * @return details about the DataAcessUser
     */
    SystemUserResultBean getDefaultUser() {
        return fdRestWriter.register("mike", "TestCompany");
    }

    private static String getIpAddress() {
        //return stack.getHostIpAddress();
        return "192.168.99.100";
        //return DockerClientFactory.instance().dockerHostIpAddress();
    }

    private static String getUrl() {

        return "http://" + getIpAddress();
    }

    private Integer getRabbitAdmin() {
        return (stack != null ? stack.getServicePort("rabbit_1", 15672) : 15672);
    }

    private static String getEngine() {
        return getUrl() + ":" + (stack != null ? stack.getServicePort("fdengine_1", SERVICE_ENGINE) : SERVICE_ENGINE);
    }

    private Integer getRabbitPort() {
        return (stack != null ? stack.getServicePort("rabbit_1", 5672) : 5672);
    }

    private static String getSearch() {
        return getUrl() + ":" + (stack != null ? stack.getServicePort("fdsearch_1", SERVICE_SEARCH) : SERVICE_SEARCH);
    }

    private static String getStore() {
        return getUrl() + ":" + (stack != null ? stack.getServicePort("fdstore_1", SERVICE_STORE) : SERVICE_STORE);
    }

    private Integer getEngineDebug() {
        return (stack != null ? stack.getServicePort("fdengine_1", DEBUG_ENGINE) : DEBUG_ENGINE);
    }

    private Integer getSearchDebug() {
        return (stack != null ? stack.getServicePort("fdsearch_1", DEBUG_SEARCH) : DEBUG_SEARCH);
    }

    private Integer getStoreDebug() {
        return (stack != null ? stack.getServicePort("fdstore_1", DEBUG_STORE) : DEBUG_STORE);
    }


    private Login getLogin(String user, String pass) {
        clientConfiguration.setServiceUrl(getEngine())
                .setHttpUser(user)
                .setHttpPass(pass);

        return new Login(clientConfiguration, fdRestWriter);
    }

    @Test
    public void pingFdEngine() {
        String result = fdRestWriter.ping();
        assertEquals("Couldn't ping fd-engine", "pong", result);
    }

    @Test
    public void pingFdStore() {
        clientConfiguration.setServiceUrl(ITDockerStack.getStore());
        String result = fdRestWriter.ping();
        assertEquals("Couldn't ping fd-store", "pong", result);
    }

    @Test
    public void pingFdSearch() {
        clientConfiguration.setServiceUrl(ITDockerStack.getSearch());

        Ping ping = new Ping(clientConfiguration, fdRestWriter);
        assertNotNull(ping.exec());

        assertEquals("Couldn't ping fd-search", "pong", ping.getResult());
    }

    @Test
    public void simpleLogin() {
        clientConfiguration.setHttpUser("mike");
        clientConfiguration.setHttpPass("123");
        UserProfile profile = fdRestWriter.login(clientConfiguration);
        assertNotNull(profile);
        assertTrue("User Roles missing", profile.getUserRoles().length != 0);
    }

    /**
     * Engine connects to both search and store over HTTP so here we verify that
     * connectivity is working
     */
    @Test
    public void engineHealth() {
        Login login = getLogin("mike", "123");
        assertEquals("Unexpected login error", null, login.exec());
        Health health = new Health(clientConfiguration, fdRestWriter);
        assertNull("Unexpected error running executing Health", health.exec());
        Map<String, Object> healthResult = health.getResult();
        assertTrue("Should be more than 1 entry in the health results", healthResult.size() > 1);
        assertNotNull("Could not find an entry for fd-search", healthResult.get("fd-search"));
        assertTrue("Failure for fd-engine to connect to fd-search in the container", healthResult.get("fd-search").toString().toLowerCase().startsWith("ok"));
        assertNotNull("Could not find an entry for fd-store", healthResult.get("fd-store"));
        assertTrue("Failure for fd-engine to connect to fd-store in the container", healthResult.get("fd-store").toString().toLowerCase().startsWith("ok"));

    }

    @Test
    public void registration() {
        // An authorised user can create DataAccess users for a given company
        assertNull(getLogin("mike", "123").exec());
        SystemUserResultBean suResult = getDefaultUser();
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
        assertNull(getLogin("mike", "123").exec());

        SystemUserResultBean suResult = getDefaultUser();
        clientConfiguration.setApiKey(suResult.getApiKey());
        clientConfiguration.setBatchSize(5);
        ContentProfile contentProfile = ContentProfileDeserializer.getContentProfile("/countries.json");
        int countryInputs = fileProcessor.processFile(contentProfile, "/fd-cow.txt");
        assertEquals("Countries not processed", countryInputs, 249);
        TagsGet countries = new TagsGet(clientConfiguration, fdRestWriter, "Country");
        // Tags are processed over a messageQ so will take a wee bit of time to be processed
        assertNull(countries.exec());
        Thread.sleep(4000);
        assertNull(countries.exec());

        TagResultBean[] countryResults = countries.getResults();
        // By this stage we may or may not have processed all the tags depending on how resource constrained the host machine
        // running the integration stack is. So we'll just check at least two batches of 10 have been processed assuming the
        // rest will pass
        assertTrue("No countries found!", countryResults.length > 10);

        TagGet countryByIsoShort = new TagGet(clientConfiguration, fdRestWriter, "Country", "AU");
        assertNull(countryByIsoShort.exec());
        assertNotNull("Couldn't find Australia", countryByIsoShort.getResult());

        TagGet countryByIsoLong = new TagGet(clientConfiguration, fdRestWriter, "Country", "AUS");
        assertNull(countryByIsoLong.exec());
        assertNotNull("Couldn't find Australia", countryByIsoLong.getResult());


        TagGet countryByName = new TagGet(clientConfiguration, fdRestWriter, "Country", "Australia");
        assertNull(countryByName.exec());
        assertNotNull("Couldn't find Australia", countryByName.getResult());

        assertEquals("By Code and By Name they are the same country so should equal", countryByIsoShort.getResult(), countryByName.getResult());
        assertEquals("By short code and long code they are the same country so should equal", countryByIsoLong.getResult(), countryByIsoShort.getResult());
    }

    @Test
    public void trackEntityOverHttp() throws Exception {
        assertNull(getLogin("mike", "123").exec());

        SystemUserResultBean suResult = getDefaultUser();
        clientConfiguration.setApiKey(suResult.getApiKey());
        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntity", false))
                .setDocumentType(new DocumentTypeInputBean("entity"))
                .setContent(new ContentInputBean(Helper.getRandomMap()))
                .addTag(new TagInputBean("someCode", "SomeLabel"));
        TrackEntityPost trackEntity = new TrackEntityPost(clientConfiguration, fdRestWriter, entityInputBean);
        assertNull(trackEntity.exec());
        assertNotNull(trackEntity.getResult());
        assertNotNull(trackEntity.getResult().getKey());
        assertEquals("Should be a new Entity", trackEntity.getResult().isNewEntity(), true);
        assertEquals("Problem creating the Content", trackEntity.getResult().getLogStatus(), ContentInputBean.LogStatus.OK);

        EntityGet foundEntity = new EntityGet(clientConfiguration, fdRestWriter, trackEntity.getResult().getKey());
        assertNull(foundEntity.exec());
        assertNotNull(foundEntity.getResult().getKey());

    }

    @Test
    public void trackEntityOverAmqpAndFindInSearch() throws Exception {
        assertNull(getLogin("mike", "123").exec());

        SystemUserResultBean suResult = getDefaultUser();
        assertNotNull(suResult);
        clientConfiguration.setApiKey(suResult.getApiKey());
        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("TrackEntityAmqp")
                        .setSearchActive(true))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("entityamqp"))
                .setContent(new ContentInputBean(Helper.getRandomMap()))
                .addTag(new TagInputBean("someCode", "SomeLabel"));


        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);
        entityGet.exec();
        entityGet = integrationHelper.waitForEntityKey(entityGet);

        EntityBean entityResult = entityGet.getResult();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());
        entityGet =  integrationHelper.waitForSearch(entityGet, 1);
        entityResult = entityGet.getResult();
        assertFalse("Search was incorrectly suppressed", entityResult.isSearchSuppressed());
        assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, entityResult.getSearch());
        assertEquals("Search Key was not set to the code of the entityInput", entityInputBean.getCode(), entityResult.getSearchKey());

        Thread.sleep(2000); // Give ES write time to complete
        QueryParams qp = new QueryParams(entityResult.getCode())
                .setFortress(entityInputBean.getFortress().getName());

        SearchEsPost search = new SearchEsPost(clientConfiguration, fdRestWriter, qp);
        assertNull(search.exec());
        EsSearchResult searchResults = search.getResult();
        assertEquals("Didn't get a search hit on the Entity", 1, searchResults.getResults().size());
        assertEquals("Keys do not match", entityResult.getKey(), searchResults.getResults().iterator().next().getKey());

    }


    @Test
    public void validateEntityLogs() throws Exception {
        assertNull(getLogin("mike", "123").exec());

        SystemUserResultBean suResult = getDefaultUser();
        clientConfiguration.setApiKey(suResult.getApiKey());
        EntityInputBean entityInputBean = new EntityInputBean()
                .setFortress(new FortressInputBean("validateEntityLogs", false))
                .setCode("findme")
                .setDocumentType(new DocumentTypeInputBean("validateEntityLogs"))
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "value")))
                .addTag(new TagInputBean("someCode", "SomeLabel"));

        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        EntityGet entityGet = new EntityGet(clientConfiguration, fdRestWriter, entityInputBean);
        entityGet = integrationHelper.waitForEntityKey(entityGet);

        EntityBean entityResult = entityGet.getResult();
        assertNotNull(entityResult);
        assertNotNull(entityResult.getKey());

        EntityLogsGet entityLogs = new EntityLogsGet(clientConfiguration, fdRestWriter, entityResult.getKey());
        entityLogs = integrationHelper.waitForEntityLog(entityLogs, 1);
        assertNotNull(entityLogs.getResult());
        assertEquals("Didn't find a log", 1, entityLogs.getResult().length);
        assertNotNull("No data was returned", entityLogs.getResult()[0].getData());
        assertEquals("Content property not found", "value", entityLogs.getResult()[0].getData().get("key"));

        entityInputBean.setKey(entityResult.getKey())
                .setContent(new ContentInputBean(Helper.getSimpleMap("key", "updated")));

        // Updating an existing entity
        amqpServices.publish(integrationHelper.toCollection(entityInputBean));
        entityLogs = integrationHelper.waitForEntityLog(entityLogs, 2);
        assertEquals("Didn't find the second log", 2, entityLogs.getResult().length);
        assertEquals("Didn't find the updated field as the first result", "updated", entityLogs.getResult()[0].getData().get("key"));
        assertEquals("Didn't find the original field as the second result", "value", entityLogs.getResult()[1].getData().get("key"));
    }


}
