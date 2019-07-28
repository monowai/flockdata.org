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

import static org.flockdata.test.integration.IntegrationHelper.SERVICE_ENGINE;
import static org.flockdata.test.integration.IntegrationHelper.SERVICE_SEARCH;
import static org.flockdata.test.integration.IntegrationHelper.SERVICE_STORE;

import java.io.File;
import java.time.Duration;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * see http://testcontainers.viewdocs.io/testcontainers-java/usage/docker_compose/
 * <p>
 * Centralised Docker management config in this class
 *
 * @author mholdsworth
 * @since 6/05/2016
 */
public class FdDocker extends ExternalResource {

  // To debug without test containers, i.e. externally started stack, set stack to null
//    static DockerComposeContainer stack = null;

  static DockerComposeContainer stack =
      new DockerComposeContainer(new File("src/test/resources/docker-compose.yml"))
          .withPull(false)
          .withExposedService("rabbit_1", 5672)
          .withExposedService("rabbit_1", 15672)
          .withExposedService("elasticsearch_1", 9200, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(50)))
          .withExposedService("elasticsearch_1", 9300)
          .withLogConsumer("fdengine", (new Slf4jLogConsumer(LoggerFactory.getLogger("🐳 fdengine"))))
          .withLogConsumer("fdstore", (new Slf4jLogConsumer(LoggerFactory.getLogger("🐳 fdstore"))))
          .withLogConsumer("fdsearch", (new Slf4jLogConsumer(LoggerFactory.getLogger("🐳 fdsearch"))))
          .withExposedService("fdstore_1", SERVICE_STORE, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(180)))
//                .withExposedService("fdstore_1", DEBUG_STORE, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
          .withExposedService("fdsearch_1", SERVICE_SEARCH, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(180)))
//                .withExposedService("fdsearch_1", DEBUG_SEARCH, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
          .withExposedService("fdengine_1", SERVICE_ENGINE, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)));
//                .withExposedService("fdengine_1", DEBUG_ENGINE, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)));

  private static Logger logger = LoggerFactory.getLogger(FdDocker.class);

  static DockerComposeContainer getStack() {
    logger.trace("Stack started from FdDocker [{}]", stack != null);
    return stack;
  }

  @Override
  protected void before() {
    if (stack != null) {
      stack.starting(Description.EMPTY);
    }
  }

  @Override
  protected void after() {
    logger.debug("Stopping FD full docker stack");
    if (stack != null) {
      stack.finished(Description.EMPTY);
    }
  }
}
