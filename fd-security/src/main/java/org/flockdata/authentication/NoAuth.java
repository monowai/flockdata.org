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

package org.flockdata.authentication;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Hardcoded users and passwords. Suitable for evaluation and testing
 * <p>
 * You should include the configuration to use this implementation
 *
 * @author mholdsworth
 * @since 16/02/2016
 */

@Configuration
@Profile( {"fd-no-auth"}) //
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Slf4j
public class NoAuth extends WebSecurityConfigurerAdapter {

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> ima = auth.inMemoryAuthentication();
    ima.withUser("mike")
        .password("123")
        .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN);
    ima.withUser("sally")
        .password("123")
        .roles("USER", FdRoles.FD_USER, FdRoles.FD_ADMIN);
    ima.withUser("harry")
        .password("123")
        .roles("USER", FdRoles.FD_USER);

  }

  @PostConstruct
  void dumpConfig() {
    log.info("**** [NoAuth] - requests to endpoints are not secured");
  }

  @Configuration
  @Order(10) // Preventing clash with AuthTesting deployment (100)
  @Profile( {"fd-no-auth"}) //
  public static class ApiSecurity extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
      // Security in FD take place at the service level so acess to all endpoints is granted
      // ApiKeyInterceptor is a part of the auth chain

      http.authorizeRequests()
          .antMatchers("/api/login", "/api/ping", "/api/logout", "/api/account").permitAll()
          .antMatchers("/api/v1/**").permitAll()
          .antMatchers("/").permitAll()
      ;


      //http://www.codesandnotes.be/2015/02/05/spring-securitys-csrf-protection-for-rest-services-the-client-side-and-the-server-side/
      //https://github.com/aditzel/spring-security-csrf-token-interceptor
      http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue
      http.httpBasic();
    }
  }

}
