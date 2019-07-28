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

package org.flockdata.engine.integration.neorest;

/**
 * For SDN4 Un-managed Extensions
 *
 * @author mholdsworth
 * @since 21/07/2015
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageHandler;

/**
 * Not currently used. Waiting for binary driver to Neo4j before taking the next step
 * this approach was designed to call webservices deployed into Neo4j.
 *
 * @author mholdsworth
 * @since 3/07/2015
 */

@Configuration
@IntegrationComponentScan
@Profile("neorest")
public class NeoAdminRequests {

  @Autowired
  FdNeoChannels channels;

  @Bean
  IntegrationFlow doFdNeoHealth() {

    return IntegrationFlows.from("neoFdHealth")
        .handle(fdHealthRequest())
        .get();
  }

  @Bean
  IntegrationFlow doFdNeoPing() {

    return IntegrationFlows.from("neoFdPing")
        .handle(fdPingRequest())
        .get();
  }

  private MessageHandler fdPingRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(channels.getUriRoot());
    handler.setExpectedResponseType(String.class);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  private MessageHandler fdHealthRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getHealthUrl());
    handler.setExpectedResponseType(String.class);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  public String getHealthUrl() {
    return channels.getUriRoot() + "health";
  }


}