/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.store.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration
 *
 * @author mholdsworth
 * @since 18/02/2016
 */
@Configuration
@Controller
public class WebMvcConfig implements WebMvcConfigurer {


  @RequestMapping("/")
  String home() {
    return "index.html";
  }

  @RequestMapping("/api")
  String api() {
    return home();
  }

  @Configuration
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


      //http://www.codesandnotes.be/2015/02/05/spring-securitys-csrf-protection-for-rest-services-the-client-side-and-the-server-side
      //https://github.com/aditzel/spring-security-csrf-token-interceptor
      http.csrf().disable();// ToDO: Fix me when we figure out POST/Login issue
      http.httpBasic();
    }
  }


}
