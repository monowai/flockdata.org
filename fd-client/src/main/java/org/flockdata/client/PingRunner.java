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

import org.flockdata.client.commands.Ping;
import org.flockdata.integration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

/**
 * Pings the service and outputs the connectivity results
 *
 * @tag Command, FdClient
 * @see org.flockdata.integration.ClientConfiguration
 *
 * @author mholdsworth
 * @since 13/10/2013
 */
@Profile("fd-ping")
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.flockdata.integration", "org.flockdata.authentication", "org.flockdata.client"})
public class PingRunner {

    private final ClientConfiguration clientConfiguration;
    private final FdTemplate fdTemplate;
    private Logger logger = LoggerFactory.getLogger(PingRunner.class);

    @Autowired
    public PingRunner(ClientConfiguration clientConfiguration, FdTemplate fdTemplate) {
        this.clientConfiguration = clientConfiguration;
        this.fdTemplate = fdTemplate;
    }

    @PostConstruct
    void register() {
        Ping pingCmd = new Ping(fdTemplate);
        pingCmd.exec();
        logger.info("FlockData endpoint [{}] responded with [{}]", clientConfiguration.getServiceUrl(), pingCmd.error()!=null?pingCmd.error():pingCmd.result());

    }


}