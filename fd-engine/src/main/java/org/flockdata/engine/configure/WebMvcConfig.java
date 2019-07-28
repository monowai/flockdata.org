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

package org.flockdata.engine.configure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author mholdsworth
 * @tag Controller, Configuration, MVC
 * Register any additional Interceptors for fd-engine
 * @since 18/02/2016
 */
@Configuration
@Controller
public class WebMvcConfig extends WebMvcConfigurerAdapter {

  @Value("#{'${cors.allowOrigin:http://127.0.0.1:9000,http://localhost:9000}'.split(',')}")
  String[] origins;
  @Value("#{'${cors.supportedHeaders:*}'.split(',')}")
  String[] headers;
  @Value("#{'${cors.supportedMethods:GET,POST,HEAD,OPTIONS,PUT,DELETE}'.split(',')}")
  String[] methods;
  @Value("${cors.supportsCredentials:true}")
  Boolean allowCredentials;
  @Autowired
  private ApiKeyInterceptor apiKeyInterceptor;

  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(apiKeyInterceptor);
  }

  @RequestMapping("/")
  String home() {
    return "index.html";
  }

  @RequestMapping("/api")
  String api() {
    return home();
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedHeaders(headers)
        .allowedMethods(methods)
        .allowCredentials(allowCredentials)
    ;
  }

}
