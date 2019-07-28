/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.integration.search;

import java.util.Map;
import org.flockdata.engine.admin.PlatformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author mholdsworth
 * @tag Messaging, Administration, Search, Gateway
 * @since 3/07/2015
 */

@Configuration
@Profile("fd-server")
@IntegrationComponentScan
public class SearchAdminRequests {

  private final PlatformConfig engineConfig;

  @Autowired
  public SearchAdminRequests(@Qualifier("engineConfig") PlatformConfig engineConfig) {
    this.engineConfig = engineConfig;
  }

  @Bean
  MessageChannel searchPing() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel searchHealth() {
    return new DirectChannel();
  }


  @Bean
  IntegrationFlow searchHealthFlow() {
    return IntegrationFlows.from(searchHealth())
        .handle(fdsHealthRequest())
        .get();
  }

  private MessageHandler fdsHealthRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch() + "/v1/admin/health");
    handler.setExpectedResponseType(Map.class);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  @Bean
  IntegrationFlow searchPingFlow() {

    return IntegrationFlows.from(searchPing())
        .handle(fdsPingRequest())
        .get();
  }

  private MessageHandler fdsPingRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch() + "/v1/admin/ping");
    handler.setExpectedResponseType(String.class);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  @MessagingGateway
  public interface AdminGateway {
    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "searchPing", requestTimeout = 6000)
    String ping();

    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "searchHealth", requestTimeout = 6000)
    Map<String, Object> health();

  }


}
