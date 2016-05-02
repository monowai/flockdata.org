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

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.testcontainers.containers.DockerComposeContainer;

/**
 * Starts a single DCContainer for all the integration tests to use
 *
 * Created by mike on 6/05/16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ SanityCheckTesting.class, ReadWriteTesting.class})
public class ITSuite {

    private static DockerComposeContainer stack = FdDocker.stack;

    @ClassRule
    public static ExternalResource resource= new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            stack.start();
        }

        @Override
        protected void after() {
            stack.stop();
        }
    };
}
