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

package org.flockdata.client;

import org.flockdata.client.commands.Health;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dispatches a Health command to verify inter-service connectivity based on requested
 * settings
 *
 * User must be a SystemUser. Key env variables are:
 *  auth.user - who:password, i.e demo:123
 *  org.fd.engine.api = e.g. http://localhost:8080
 *
 *
 *
 * @tag Command, FdClient, Administration
 * @see org.flockdata.registration.SystemUserResultBean
 * @see ClientConfiguration
 *
 * @author mholdsworth
 * @since 13/10/2013
 */
@Profile("fd-health")
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.flockdata.integration", "org.flockdata.authentication", "org.flockdata.client"})
public class HealthRunner implements CommandLineRunner {

    private final ClientConfiguration clientConfiguration;
    private final FdTemplate fdTemplate;
    @Value("${auth.user:#{null}}")
    String authUser;
    private Logger logger = LoggerFactory.getLogger(HealthRunner.class);

    @Autowired
    public HealthRunner(ClientConfiguration clientConfiguration, FdTemplate fdTemplate) {
        this.clientConfiguration = clientConfiguration;
        this.fdTemplate = fdTemplate;
    }

    @Override
    public void run(String... args) throws Exception {

        logger.info("Looking for Flockdata on {}", clientConfiguration.getServiceUrl());
        CommandRunner.configureAuth(logger, authUser, fdTemplate);

        Health health = new Health(fdTemplate);
        logger.info(JsonUtils.pretty(health
                .exec()
                .result()));

    }


}