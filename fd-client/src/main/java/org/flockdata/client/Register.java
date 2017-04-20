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

import org.flockdata.client.commands.RegistrationPost;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.registration.RegistrationBean;
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
 * Command that allows an authorised user to create an FD system user. This is an account that can access
 * data associated with a company
 *
 * You will normally run this via the CommandRunner with properties such as
 *  --auth.user=mike:123 --register.login=mike
 *
 *  auth.user is an FD_ADMIN account and --register.login is the data access account to create. Data access
 *  user will be associated with the same company that the auth.user belongs to.
 *
 *  In this scenario, the authorised user is being granted data reading rights.
 *
 * @author mholdsworth
 * @since 13/10/2013
 * @tag Command, SystemUser, FdClient
 */
@Profile("fd-register")
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.flockdata.integration", "org.flockdata.client"})
public class Register implements CommandLineRunner {

    private final ClientConfiguration clientConfiguration;
    private final FdClientIo fdClientIo;
    @Value("${auth.user:#{null}}")
    String authUser;
    @Value("${register.login:#{null}}")
    String login;
    private Logger logger = LoggerFactory.getLogger(Register.class);

    @Autowired
    public Register(ClientConfiguration clientConfiguration, FdClientIo fdClientIo) {
        this.clientConfiguration = clientConfiguration;
        this.fdClientIo = fdClientIo;
    }

    @Override
    public void run(String... args) throws Exception {

        if ( login == null ) {
            logger.error ("To run this command you should supply --register.login=someuser");
            logger.error ("Where someuser is a user in your authentication realm");
            System.exit(-1);
        }

        CommandRunner.configureAuth(logger, authUser, fdClientIo);

        RegistrationBean regBean = new RegistrationBean(clientConfiguration.getCompany(), login);
        RegistrationPost register = new RegistrationPost(fdClientIo, regBean);
        register.exec();
        if ( register.error()!=null)
            logger.error(register.error());
        else {
            logger.info("Registered {}", register.result());
        }
    }


}