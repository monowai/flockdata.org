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

import me.tongfei.progressbar.ProgressBar;
import org.flockdata.client.amqp.AmqpServices;
import org.flockdata.client.commands.Command;
import org.flockdata.client.commands.Health;
import org.flockdata.client.commands.Login;
import org.flockdata.client.commands.Ping;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.UserProfile;
import org.flockdata.shared.AmqpRabbitConfig;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.shared.Exchanges;
import org.flockdata.shared.FdBatcher;
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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Establishes the integration test environment. Descendant classes use @Test functions against
 * this established stack
 *
 * Created by mike on 3/04/16.
 */
@SpringApplicationConfiguration({
        ClientConfiguration.class,
        FdBatcher.class,
        FdRestWriter.class,
        Exchanges.class,
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
    int waitSeconds;

    @ClassRule // start is called before the tests are run and stop is called after the class
               // is finished. Given that is how Junit wants to play, all integration tests that
               // want to use this stack should be in this class, otherwise a new Stack will be created
               // increasing the time it takes to start
               // ToDo: Figure out a better way of doing this!

    // http://testcontainers.viewdocs.io/testcontainers-java/usage/docker_compose/
    public static DockerComposeContainer stack =
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
    ClientConfiguration clientConfiguration;

    /**
     * Rabbit connectivity
     */
    @Autowired
    AmqpRabbitConfig rabbitConfig;

    /**
     * Contains a RestTemplate configured to talk to FlockData. By default this is fd-engine
     * but by calling clientConfiguration.setServiceUrl(...) it can be used to talk to
     * fd-search or fd-store. Only fd-engine is secured by default
     */
    @Autowired
    FdRestWriter fdRestWriter;

    private static boolean setupComplete = false;

    // If any one of FD's services fail to come up we can't perform integration testing
    private static boolean stackFailed = false;

    @Before
    public void waitForServices() throws InterruptedException {
        if (stackFailed)
            fail("Stack has failed to startup cleanly - test will fail");
        if (setupComplete)
            return; // This method is called before every @Test - it's expensive :o)
        clientConfiguration.setServiceUrl(getEngine());
        Ping enginePing = new Ping(clientConfiguration, fdRestWriter);
        clientConfiguration.setServiceUrl(getStore());
        Ping storePing = new Ping(clientConfiguration, fdRestWriter);
        clientConfiguration.setServiceUrl(getSearch());
        Ping searchPing = new Ping(clientConfiguration, fdRestWriter);
        rabbitConfig.setServicePoint(stack.getContainerIpAddress(), getRabbitPort());

        logger.info("FDEngine will listen on {}", getEngine() );
        logger.info("FDSearch will listen on {}", getSearch() );
        logger.info(" FDStore will listen on {}", getStore() );
        logger.info("FDEngine-Debug listening on " + stack.getContainerIpAddress()+":"+ getEngineDebug());
        logger.info("FDSearch-Debug listening on " + stack.getContainerIpAddress()+":"+ getSearchDebug());
        logger.info("FDStore-Debug  listening on " + stack.getContainerIpAddress()+":"+ getStoreDebug());
        logger.info("Rabbit Admin on http://"      + stack.getContainerIpAddress()+":"+ getRabbitAdmin());
        // ToDo: Bind in yourkit profiler and expose port

        logger.info("Initial wait for docker containers to startup. --org.fd.test.pause={} seconds ..... ", waitSeconds);

        pauseUntil(enginePing, "pong", waitSeconds);

        waitForService("fd-engine", enginePing, getEngine(), 30);
        waitForService("fd-search", searchPing, getSearch(), 30);
        waitForService("fd-store", storePing, getStore(), 30);

        if (!stackFailed)
            logger.info("Stack is running");
        else
            logger.error("Failed to start the stack");

        setupComplete = true;
    }

    public void pauseUntil(Command optionalCommand, String comandResult, int waitCount) throws InterruptedException {
        // A nice little status bar to show how long we've been hanging around
        ProgressBar pb = new ProgressBar("Waiting.... ", waitCount);
        pb.start();
        int run = 0;
        do {
            run ++;
            pb.step();
            // After waiting for 40% of the waitCount will try running the command if it exists
            if ( optionalCommand!=null && run %10 ==0 && (((double)run)/waitCount) > .3  ){
                // After 1 minute we will ping to see if we can finish this early
                String result = optionalCommand.exec();
                if ( result.equals(comandResult)) {
                    // We can finish early
                    pb.stepBy( (waitCount-run));
                    return;

                }
            } else {
                Thread.sleep(1000);
            }

        } while (run != waitCount);
    }

    public void waitForService(String service, Ping pingCommand, String url, int countDown) throws InterruptedException {
        String result;
        if (stackFailed)
            return;
        logger.info("looking for {}", service);
        do {
            countDown--;
            result = pingCommand.exec();
            if (!result.equals("pong")) {
                int waitSecs = 60;
                logger.info("Waiting {} seconds for {} to come on-line", waitSecs, service);
                pauseUntil(pingCommand,"pong", waitSecs);
            }

        } while (!Objects.equals(result, "pong") && countDown > 0);

        if (!result.equals("pong")) {
            setupComplete = true;
            stackFailed = true;
            fail("Failed to ping " + service + " before timeout");
        }
        logger.info("{} is running. [{}]", service, url);
    }

    private static String getUrl() {

        return "http://" + stack.getContainerIpAddress();
    }

    private Integer getRabbitAdmin() {
        return stack.getServicePort("rabbit_1",15672);
    }

    static String getEngine() {
        return getUrl() + ":" + stack.getServicePort("fdengine_1", SERVICE_ENGINE);
    }

    private Integer getRabbitPort() {
        return stack.getServicePort("rabbit_1", 5672);
    }

    static String getSearch() {
        return getUrl() + ":" + stack.getServicePort("fdsearch_1", SERVICE_SEARCH);
    }

    static String getStore() {
        return getUrl() + ":" + stack.getServicePort("fdstore_1", SERVICE_STORE);
    }

    private Integer getEngineDebug() {
        return  stack.getServicePort("fdengine_1", DEBUG_ENGINE);
    }

    private Integer getSearchDebug() {
         return stack.getServicePort("fdsearch_1", DEBUG_SEARCH);
    }

    private Integer getStoreDebug() {
        return stack.getServicePort("fdstore_1", DEBUG_STORE);
    }


    Login getLogin(String user, String pass) {
        clientConfiguration.setServiceUrl(getEngine())
                .setHttpUser(user)
                .setHttpPass(pass);

        return new Login(clientConfiguration, fdRestWriter);
    }

    @Test
    public void pingFdEngine() {
        clientConfiguration.setServiceUrl(ITDockerStack.getEngine());
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
        clientConfiguration.setServiceUrl(getEngine());
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
        assertNull(login.exec());
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

        clientConfiguration.setServiceUrl(getEngine());
        SystemUserResultBean suResult = fdRestWriter.register("mike", "TestCompany");
        assertNotNull(suResult);
        assertNotNull(suResult.getApiKey());

    }

}
