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

import me.tongfei.progressbar.ProgressBar;
import org.flockdata.client.FdClientIo;
import org.flockdata.client.FdTemplate;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FileProcessor;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.test.integration.matchers.*;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntityResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.AssertionErrors.*;


/**
 * Integration utils. Keeps generic functionality out of the IT class
 *
 * @author mholdsworth
 * @tag Test, Docker, Configuration
 * @since 20/04/2016
 */
@Service
@Configuration
@ContextConfiguration(classes = {
        ClientConfiguration.class,
        FileProcessor.class,
        FdTemplate.class,
        FdClientIo.class,
        FdRabbitClient.class,
        EnginePing.class,
        StorePing.class,
        SearchPing.class,
        RegistrationPost.class,
        SearchEsPost.class,
        Health.class,
        AmqpRabbitConfig.class,
        RestTemplate.class,
        SearchHelper.class,
        IntegrationHelper.class

})
class IntegrationHelper {

    // These are defined in docker-compose.yml
    static final String ADMIN_REGRESSION_USER = "integration";
    static final String ADMIN_REGRESSION_PASS = "123";
    static final int DEBUG_ENGINE = 61000;
    static final int DEBUG_SEARCH = 61001;
    static final int DEBUG_STORE = 61002;
    static final int SERVICE_ENGINE = 8090;
    static final int SERVICE_SEARCH = 8091;
    static final int SERVICE_STORE = 8092;
    private static DockerComposeContainer stack = FdDocker.getStack();
    private static Logger logger = LoggerFactory.getLogger(IntegrationHelper.class);
    private static boolean setupComplete = false;
    // If any one of FD's services fail to come up we can't perform integration testing
    private static boolean stackFailed = false;
    private FdClientIo fdClientIo;
    @Value("${org.fd.test.sleep.short:1500}")
    private int shortSleep;
    @Value("${org.fd.test.sleep.long:4000}")
    private int longSleep;
    @Value("${org.fd.test.attempts:10}")
    private int attempts;
    @Value("${org.fd.test.pause:200}")
    private int waitSeconds;
    private EnginePing enginePing;
    private SearchPing searchPing;
    private StorePing storePing;
    private RegistrationPost registrationPost;
    private ClientConfiguration clientConfiguration;
    @Autowired
    private Health health;

    private static String getUrl() {

        return "http://" + getIpAddress();
    }

    static String getEngine() throws IllegalStateException {
        return getService("fdengine_1", SERVICE_ENGINE);
    }

    static String getService(String service, int port) {
        return getUrl() + ":" + (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort(service, port) : port);
    }

    private static String getIpAddress() {
        if (stack == null) {
            return "localhost";
        } else
            return stack.getServiceHost("fdengine_1", SERVICE_ENGINE);

        //return DockerClientFactory.instance().dockerHostIpAddress();
    }

    @Autowired
    void setFdClientIo(FdClientIo fdClientIo, ClientConfiguration clientConfiguration) {
        this.fdClientIo = fdClientIo;
        this.clientConfiguration = clientConfiguration;
    }

    @Autowired
    void setRegistrationPost(RegistrationPost registrationPost) {
        this.registrationPost = registrationPost;
    }

    @Autowired
    void setEnginePing(EnginePing enginePing) {
        this.enginePing = enginePing;
    }

    @Autowired
    void setSearchPing(SearchPing searchPing) {
        this.searchPing = searchPing;
    }

    @Autowired
    void setStorePing(StorePing storePing) {
        this.storePing = storePing;
    }

    Collection<EntityInputBean> toCollection(EntityInputBean entityInputBean) throws IOException {
        Collection<EntityInputBean> entities = new ArrayList<>();
        entities.add(entityInputBean);
        return entities;
    }

    private void waitUntil(Logger logger, String function, ReadyMatcher readyMatcher) {
        ProgressBar pb = null;
        StopWatch watch = new StopWatch();
        watch.start(function);
        int count = 0;
        boolean ready = false;

        do {
            try {
                ready = readyMatcher.isReady(fdClientIo);
                if (!ready) {
                    if (pb == null && count > 5) {
                        pb = new ProgressBar(function, attempts - 5);
                        pb.start();
                    }

                    Thread.sleep(shortSleep);
                    if (pb != null)
                        pb.stepBy(1);
                    count++;

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (count <= attempts && !ready);
        if (count == attempts) {
            logger.error("Timeout of {} was hit before we got a result for {}", attempts, function);
        }
        watch.stop();
        logger.info(watch.prettyPrint());

    }

    CommandResponse<EntityLogResult[]> waitForEntityLog(Logger logger, String function, EntityLogsGet entityLogs, int waitFor, String key) {
        EntityLogReady ready = new EntityLogReady(entityLogs, waitFor, key);
        waitUntil(logger, function, ready);
        return ready.getResponse();
    }

    // Executes a GetEntity command and waits for a result. Can take some time depending on the environment that this
    // is working on.
    CommandResponse<EntityResultBean> waitForEntityKey(Logger logger, String function, EntityGet entityGet, EntityInputBean entityInputBean, String key) {
        EntityKeyReady ready = new EntityKeyReady(entityGet, entityInputBean, key);
        waitUntil(logger, function, ready);
        return ready.getResponse();
    }

    CommandResponse<EntityResultBean> waitForSearch(Logger logger, String function, EntityGet entityGet, int searchCount, EntityInputBean entityInputBean, String key) {
        EntitySearchReady ready = new EntitySearchReady(entityGet, searchCount, entityInputBean, key);
        waitUntil(logger, function, ready);
        return ready.getResponse();
    }

    CommandResponse<TagResultBean[]> waitForTagCount(Logger logger, String function, TagsGet tagGet, int searchCount, String tagLabel) {
        TagCountReady ready = new TagCountReady(tagGet, searchCount, tagLabel);
        waitUntil(logger, function, ready);
        return ready.getResponse();
    }

    private void waitForPong(Ping pingCommand, int waitCount) throws InterruptedException {
        // A nice little status bar to show how long we've been hanging around
        ProgressBar pb = new ProgressBar("Looking for services... ", waitCount);
        pb.start();
        int run = 0;
        do {
            run++;
            pb.step();
            // After waiting for 30% of the waitCount will try running the command if it exists
            if (pingCommand != null && (((double) run) / waitCount) > .05) {
                // After 1 minute we will ping to see if we can finish this early
                CommandResponse<String> pingResponse = pingCommand.exec();
                if (pingResponse.getError() == null && pingResponse.getResult().equals("pong")) {
                    logger.info("finished after {}", run); // Early finish - yay!
                    return;

                }
            } else {
                shortSleep();
            }

        } while (run != waitCount);
    }

    private void waitForService(String service, Ping pingCommand, int countDown) throws InterruptedException {

        if (stackFailed)
            return;
        logger.info("looking for [{}]", service);
        CommandResponse<String> pingResponse = pingCommand.exec();
        do {
            countDown--;

            if (pingResponse.getError() == null && !pingResponse.getResult().equals("pong")) {
                int waitSecs = (stack == null ? 2 : 60);
                logger.info("Waiting {} seconds for {} to come on-line", waitSecs, service);
                waitForPong(pingCommand, waitSecs);
            }

        } while (!Objects.equals(pingResponse.getResult(), "pong") && countDown > 0);

        if (pingResponse.worked() && !pingResponse.getResult().equals("pong")) {
            setupComplete = true;
            stackFailed = true;
            fail("Failed to ping " + service + " before timeout");
        }
        logger.info("{} is running @ [{}]", service, pingCommand.getApi());
    }

    void waitForServices() {

        if (stackFailed)
            fail("Stack has failed to startup cleanly - test will fail");
        if (setupComplete)
            return; // This method is called before every @Test - it's expensive :o)

        logger.info("Waiting for containers to come on-line. Service URL {}", fdClientIo.getUrl());

        logger.debug("Running with debug logging");
        logger.info("org.fd.test.sleep.short {}ms", shortSleep);
        logger.info("org.fd.test.sleep.long  {}ms", longSleep);
        // ToDo: Bind in yourkit profiler and expose port

        logger.info("Initial wait for docker containers to startup. --org.fd.test.pause={} seconds ..... ", waitSeconds);

        startServices();
        setupComplete = true;
        fdClientIo.resetRabbitClient(getRabbit(), getRabbitPort());
        if (!stackFailed)
            logger.info("Stack is running");
        else
            logger.error("Failed to start the stack");


    }

    void startServices() {
        if (stack != null)
            try {
                boolean gotPorts = false;
                while (!gotPorts) {
                    try {
                        getEngine();
                        getSearch();
                        getStore();
                        gotPorts = true;
                    } catch (IllegalStateException e) {
                        gotPorts = false;
                        Thread.sleep(5000);
                    }
                }
                enginePing.setApi(getEngine());
                searchPing.setApi(getSearch());
                storePing.setApi(getStore());
                logger.info("FDEngine - {} - reachable @ {}", SERVICE_ENGINE, getEngine());
                logger.info("FDSearch - {} - reachable @ {}", SERVICE_SEARCH, getSearch());
                logger.info("FDStore  - {} - reachable @ {}", SERVICE_STORE, getStore());
                logger.info("FDEngine-Debug - {} - reachable @ {}", DEBUG_ENGINE, getService("fdengine_1", DEBUG_ENGINE));
                logger.info("FDSearch-Debug - {} - reachable @ {}", DEBUG_SEARCH, getService("fdsearch_1", DEBUG_SEARCH));
                logger.info("FDStore-Debug - {} - reachable @ {}", DEBUG_STORE, getService("fdstore_1", DEBUG_STORE));
                logger.info("Rabbit Admin on http://{}:{}", getRabbit(), getRabbitAdmin());
                waitForPong(enginePing, waitSeconds);

                waitForService("fd-search", searchPing, 30);
                waitForService("fd-store", storePing, 30);
                logger.info("HealthChecks");
                // Remap the API service URL due to the proxy activity that occurred above
                fdClientIo.setServiceUrl(getEngine());
                // If the services can't see each other, its not worth proceeding
                SystemUserResultBean login = login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
                assertNotNull(login);
                CommandResponse<Map<String, Object>> healthResponse = health.exec();
                assertNull("Health Check", healthResponse.getError());

                Map<String, Object> healthResult = healthResponse.getResult();
                assertTrue("Should be more than 1 entry in the health results", healthResult.size() > 1);
                assertNotNull("Could not find an entry for fd-search", healthResult.get("fd-search"));
                String searchStatus = ((Map<String, Object>) healthResult.get("fd-search")).get("status").toString();
                assertTrue("Failure for fd-engine to connect to fd-search in the container " + searchStatus, searchStatus.toLowerCase().startsWith("ok"));
                assertNotNull("Could not find an entry for fd-store", healthResult.get("fd-store"));
                String storeStatus = ((Map<String, Object>) healthResult.get("fd-store")).get("status").toString();
                assertTrue("Failure for fd-engine to connect to fd-store in the container " + storeStatus, storeStatus.toLowerCase().startsWith("ok"));
                logger.info(JsonUtils.pretty(healthResult));


            } catch (InterruptedException | FlockException e) {
                logger.error(e.getMessage());
                setupComplete = true;
                stackFailed = true;
            }

    }

    private Integer getRabbitAdmin() throws IllegalStateException {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("rabbit_1", 15672) : 15672);
    }

    String getRabbit() throws IllegalStateException {
        if (stack != null)
            return stack.getServiceHost("rabbit_1", 5672);
        return getIpAddress();
    }

    private Integer getRabbitPort() throws IllegalStateException {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("rabbit_1", 5672) : 5672);
    }

    String getSearch() throws IllegalStateException {
        return getService("fdsearch_1", SERVICE_SEARCH);
    }

    String getStore() throws IllegalStateException {
        return getService("fdstore_1", SERVICE_STORE);
    }

    /**
     * Convenience function
     * <p>
     * Logs in with the externally configured integration account and then
     * sets that user up as a DataAccessUser.
     *
     * @param user integration
     * @param pass 123
     * @return suresult
     * @throws FlockException errors
     */
    SystemUserResultBean login(String user, String pass) throws FlockException {
        fdClientIo.setServiceUrl(getEngine());
        SystemUserResultBean result = fdClientIo.login(user, pass);
        assertThat(result)
                .isNotNull();
        if (result.getApiKey() == null) {
            // New data access user
            CommandResponse<SystemUserResultBean> suResponse = registrationPost.exec(new RegistrationBean("TestCompany", user));
            assertEquals("Error registering data access user", null, suResponse.getError());
            result = suResponse.getResult();
            assertNotNull(String.format("Failed to make the login [%s] a data access user", user), result.getApiKey());
            clientConfiguration.setSystemUser(result);
        }

        return result;
    }


    void longSleep() throws InterruptedException {
        Thread.sleep(longSleep);
    }

    void shortSleep() throws InterruptedException {
        Thread.sleep(shortSleep);
    }


}
