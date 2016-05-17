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

package org.flockdata.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.annotation.PostConstruct;

/**
 * Hardcoded users and passwords. Suitable for evaluation and testing
 * <p>
 * You should include the configuration to use this implementation
 * <p>
 * Created by mike on 16/02/16.
 */

@Profile({"fd-auth-test"}) //
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SimpleAuth extends WebSecurityConfigurerAdapter {

    @Configuration
    @Order(10) // Preventing clash with AuthTesting deployment (100)
    @Profile({"fd-auth-test"}) //
    public static class ApiSecurity extends WebSecurityConfigurerAdapter {

        @Value("${org.fd.auth.simple.login.form:#{null}}")
        String loginForm;

        @Value ("${org.fd.auth.simple.login.method:basic}")
        String loginMethod ;

        @Override
        public void configure(HttpSecurity http) throws Exception {
            // Security in FD take place at the service level so acess to all endpoints is granted
            // ApiKeyInterceptor is a part of the auth chain

            http.authorizeRequests()
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS
                    .antMatchers("/api/login", "/api/ping", "/api/logout", "/api/account").permitAll()
                    .antMatchers("/api/v1/**").authenticated()
                    .antMatchers("/").permitAll()
            ;


            //http://www.codesandnotes.be/2015/02/05/spring-securitys-csrf-protection-for-rest-services-the-client-side-and-the-server-side/
            //https://github.com/aditzel/spring-security-csrf-token-interceptor
            http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue

            if ( loginMethod.equalsIgnoreCase("basic") || loginForm == null )
                http.httpBasic();
            else {
                http.httpBasic();
                http.formLogin().loginPage(loginForm).permitAll();
            }
        }
    }

    @Autowired
    SimpleUsers simpleUsers;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> ima = auth.inMemoryAuthentication();
        if (simpleUsers == null || simpleUsers.getUsers() == null) {
            logger.info("**** [fd-auth-test] - attempting to use fd-auth-test but no users have been configured. Consider starting the service with -P fd-no-auth");
            logger.info("**** [fd-auth-test] - a default user of mike will be created");
            simpleUsers.createDefault();
        }
        for (String login : simpleUsers.getUsers().keySet()) {
            SimpleUsers.UserEntry user = simpleUsers.getUsers().get(login);
            ima.withUser(login)
                    .password(user.getPass())
                    .roles(user.getRoles().toArray(new String[0]));

            logger.info("**** [fd-auth-test] - Added {}", login);
        }

    }

    private static Logger logger = LoggerFactory.getLogger("configuration");

    @PostConstruct
    void dumpConfig() {
        logger.info("**** [fd-auth-test] - Limited authorization (for testing) is being used");
    }

}
