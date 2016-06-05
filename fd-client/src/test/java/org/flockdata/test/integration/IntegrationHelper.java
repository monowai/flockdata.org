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
import org.flockdata.client.commands.*;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.shared.*;
import org.flockdata.test.integration.matchers.EntityKeyReady;
import org.flockdata.test.integration.matchers.EntityLogReady;
import org.flockdata.test.integration.matchers.EntitySearchReady;
import org.flockdata.test.integration.matchers.ReadyMatcher;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;


/**
 * Integration utils. Keeps generic functionality out of the IT class
 *
 * Created by mike on 20/04/16.
 */
@Service
@Configuration
@SpringApplicationConfiguration({
        ClientConfiguration.class,
        FdBatcher.class,
        FdRestWriter.class,
        SearchHelper.class,
        Exchanges.class,
        FileProcessor.class,
        AmqpRabbitConfig.class,
        AmqpServices.class

})
class IntegrationHelper {

    // These are defined in docker-compose.yml
    static final String ADMIN_REGRESSION_USER = "integration";
    static final String ADMIN_REGRESSION_PASS = "123";
    private static Logger logger = LoggerFactory.getLogger(IntegrationHelper.class);

    @Value("${org.fd.test.sleep.short:1500}")
    private int shortSleep;

    @Value("${org.fd.test.sleep.long:4000}")
    private int longSleep;

    @Value("${org.fd.test.attempts:100}")
    private int attempts;

    @Value("${org.fd.test.pause:150}")
    private
    int waitSeconds;

    public static DockerComposeContainer stack = FdDocker.getStack();

    static final int DEBUG_ENGINE = 61000;
    static final int DEBUG_SEARCH = 61001;
    static final int DEBUG_STORE = 61002;

    static final int SERVICE_ENGINE = 8090;
    static final int SERVICE_SEARCH = 8091;
    static final int SERVICE_STORE = 8092;

    private static boolean setupComplete = false;

    // If any one of FD's services fail to come up we can't perform integration testing
    private static boolean stackFailed = false;


    Collection<EntityInputBean> toCollection(EntityInputBean entityInputBean) throws IOException {
        Collection<EntityInputBean> entities = new ArrayList<>();
        entities.add(entityInputBean);
        return entities;
    }

    private void waitUntil(Logger logger, ReadyMatcher readyMatcher) {
        ProgressBar pb = null;
        StopWatch watch = new StopWatch();
        watch.start(readyMatcher.getMessage());
        int count = 0;
        boolean ready = false;

        do {
            try {
                ready = readyMatcher.isReady();
                if ( !ready ) {
                    if ( pb == null && count > 5 ) {
                        pb = new ProgressBar(readyMatcher.getMessage(), attempts -5);
                        pb.start();
                    }

                    Thread.sleep(shortSleep);
                    if ( pb!=null )
                        pb.stepBy(1);
                    count ++;

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while ( count < attempts && !ready);
        if ( count == attempts ) {
            logger.error("Timeout of {} was hit before we got a result for {}", attempts, readyMatcher.getMessage());
        }
        watch.stop();
        logger.info(watch.prettyPrint());

    }

    void waitForEntityLog(Logger logger, EntityLogsGet entityLogs, int waitFor) {
        EntityLogReady ready = new EntityLogReady(entityLogs, waitFor);
        waitUntil(logger, ready);
    }

    // Executes a GetEntity command and waits for a result. Can take some time depending on the environment that this
    // is working on.
    void waitForEntityKey(Logger logger, EntityGet entityGet) {
        EntityKeyReady ready = new EntityKeyReady(entityGet);
        waitUntil(logger, ready);
    }

    void waitForSearch(Logger logger, EntityGet entityGet, int searchCount) {
        EntitySearchReady ready = new EntitySearchReady(entityGet, searchCount);
        waitUntil(logger, ready);
    }

    private void waitForPong(Ping pingCommand, int waitCount) throws InterruptedException {
        // A nice little status bar to show how long we've been hanging around
        ProgressBar pb = new ProgressBar("Looking for services.... ", waitCount);
        pb.start();
        int run = 0;
        do {
            run++;
            pb.step();
            // After waiting for 40% of the waitCount will try running the command if it exists
            if (pingCommand != null && run % 10 == 0 && (((double) run) / waitCount) > .3) {
                // After 1 minute we will ping to see if we can finish this early

                if (pingCommand.exec().worked() && pingCommand.result().equals("pong")) {
                    // We can finish early
//                    pb.stepBy((waitCount - run));
                    logger.info("finished after {}", run);
                    return;

                }
            } else {
                shortSleep();
            }

        } while (run != waitCount);
    }

    /**
     *
     * Executes and asserts that the command worked
     *
     * @param message assertion message
     * @param command to check
     */
    void assertWorked(String message, Command command) {
        command.exec();
        assertTrue(message + command.error(), command.worked());

    }

    private void waitForService(String service, Ping pingCommand, String url, int countDown) throws InterruptedException {

        if (stackFailed)
            return;
        logger.info("looking for {}", service);
        do {
            countDown--;

            if (pingCommand.exec().worked() && !pingCommand.result().equals("pong")) {
                int waitSecs = (stack == null ? 2 : 60);
                logger.info("Waiting {} seconds for {} to come on-line", waitSecs, service);
                waitForPong(pingCommand, waitSecs);
            }

        } while (!Objects.equals(pingCommand.result(), "pong") && countDown > 0);

        if (pingCommand.worked() && !pingCommand.result().equals("pong")) {
            setupComplete = true;
            stackFailed = true;
            fail("Failed to ping " + service + " before timeout");
        }
        logger.info("{} is running. [{}]", service, url);
    }

    @Autowired
    ClientConfiguration clientConfiguration;

    @Autowired
    FdRestWriter fdRestWriter;

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    void waitForServices()  {

        if (stackFailed)
            fail("Stack has failed to startup cleanly - test will fail");
        if (setupComplete)
            return; // This method is called before every @Test - it's expensive :o)

        logger.info("Waiting for containers to come on-line");
        clientConfiguration.setServiceUrl(getEngine());

        logger.debug("Running with debug logging");
        clientConfiguration.setServiceUrl(getEngine());
        Ping enginePing = new Ping(clientConfiguration, fdRestWriter);
        clientConfiguration.setServiceUrl(getStore());
        Ping storePing = new Ping(clientConfiguration, fdRestWriter);
        clientConfiguration.setServiceUrl(getSearch());
        Ping searchPing = new Ping(clientConfiguration, fdRestWriter);
        rabbitConfig.setServicePoint(getIpAddress(), getRabbitPort());

        logger.info("org.fd.test.sleep.short {}ms", shortSleep);
        logger.info("org.fd.test.sleep.long  {}ms", longSleep);
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
            try {
                waitForPong(enginePing, waitSeconds);
                waitForService("fd-engine", enginePing, getEngine(), 30);
                waitForService("fd-search", searchPing, getSearch(), 30);
                waitForService("fd-store", storePing, getStore(), 30);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                setupComplete=true;
                stackFailed=true;
            }


        if (!stackFailed)
            logger.info("Stack is running");
        else
            logger.error("Failed to start the stack");

        setupComplete = true;
    }


    private static String getUrl() {

        return "http://" + getIpAddress();
    }

    private Integer getRabbitAdmin() {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("rabbit_1", 15672) : 15672);
    }

    static String getEngine() {
        return getUrl() + ":" + (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("fdengine_1", SERVICE_ENGINE) : SERVICE_ENGINE);
    }

    private Integer getRabbitPort() {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("rabbit_1", 5672) : 5672);
    }

    String getSearch() {
        return getUrl() + ":" + (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("fdsearch_1", SERVICE_SEARCH) : SERVICE_SEARCH);
    }

    String getStore() {
        return getUrl() + ":" + (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("fdstore_1", SERVICE_STORE) : SERVICE_STORE);
    }

    private Integer getEngineDebug() {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("fdengine_1", DEBUG_ENGINE) : DEBUG_ENGINE);
    }

    private Integer getSearchDebug() {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("fdsearch_1", DEBUG_SEARCH) : DEBUG_SEARCH);
    }

    private Integer getStoreDebug() {
        return (FdDocker.getStack() != null ? FdDocker.getStack().getServicePort("fdstore_1", DEBUG_STORE) : DEBUG_STORE);
    }

    private static String getIpAddress() {
        if ( stack == null )
            return "192.168.99.100";
        else
            return FdDocker.getStack().getContainerIpAddress();

        //return DockerClientFactory.instance().dockerHostIpAddress();
    }

    /**
     * A login is associated with a single company. Create different fortresses to partion
     * data access users.
     * <p>
     * The user name you want to create has to exist in the security context otherwise login will fail
     *
     * @return details about the DataAcessUser
     */
    SystemUserResultBean makeDataAccessUser() {
        return fdRestWriter.register(ADMIN_REGRESSION_USER, "TestCompany");
    }

    Login login(String user, String pass) {
        return login(fdRestWriter, user, pass);
    }

    Login login(FdRestWriter fdRestWriter, String user, String pass) {
        clientConfiguration.setServiceUrl(getEngine())
                .setHttpUser(user)
                .setHttpPass(pass);

        return new Login(clientConfiguration, fdRestWriter);

    }


    void longSleep() throws InterruptedException {
        Thread.sleep(longSleep);
    }

    void shortSleep() throws InterruptedException{
        Thread.sleep(shortSleep);
    }


}
