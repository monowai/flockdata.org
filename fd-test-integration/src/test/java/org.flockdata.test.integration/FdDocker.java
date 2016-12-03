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

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.flockdata.test.integration.IntegrationHelper.*;

/**
 * see http://testcontainers.viewdocs.io/testcontainers-java/usage/docker_compose/
 * <p>
 * We want to be able to run the stack either for a single test method or as a suite.
 * Easier to centralise the container config in this class to accomplish that
 * <p>
 * @author mholdsworth
 * @since 6/05/2016
 */
public class FdDocker extends ExternalResource {

    static DockerComposeContainer stack =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yml"))
                    .withPull(false)
                    .withExposedService("rabbit_1", 5672)
                    .withExposedService("rabbit_1", 15672)
                    .withExposedService("fdengine_1", SERVICE_ENGINE)
                    .withExposedService("fdengine_1", DEBUG_ENGINE)
                    .withExposedService("fdsearch_1", SERVICE_SEARCH)
                    .withExposedService("fdsearch_1", DEBUG_SEARCH)
                    .withExposedService("fdstore_1", SERVICE_STORE)
                    .withExposedService("fdstore_1", DEBUG_STORE);

    private static Logger logger = LoggerFactory.getLogger(FdDocker.class);

    static DockerComposeContainer getStack() {
        logger.debug("Stack started from FdDocker = {}", stack != null);
        return stack;
    }

    @Override
    protected void before() throws Throwable {
        if (stack != null)
            stack.starting(Description.EMPTY);
    }

    @Override
    protected void after() {
        logger.debug("Stopping FD full docker stack");
        if (stack != null)
            stack.finished(Description.EMPTY);
    }
}
