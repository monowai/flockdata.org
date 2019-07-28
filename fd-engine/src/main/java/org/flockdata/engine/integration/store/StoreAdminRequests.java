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

package org.flockdata.engine.integration.store;

/**
 * For SDN4 Un-managed Extensions
 *
 * @author mholdsworth
 * @since 21/07/2015
 */

import java.util.HashMap;
import java.util.Map;
import org.flockdata.engine.configure.EngineConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
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

/**
 * Ping fd-store
 *
 * @author mholdsworth
 * @since 3/07/2015
 */

@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class StoreAdminRequests {

  private final EngineConfig engineConfig;

  @Autowired
  public StoreAdminRequests(EngineConfig engineConfig) {
    this.engineConfig = engineConfig;
  }

  @Bean
  MessageChannel storePing() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel storePingEngine() {
    return new DirectChannel();
  }

  @Bean
  IntegrationFlow storePingFlow() {

    return IntegrationFlows.from(storePing())
        .handle(pingRequest())
        .get();
  }

  private MessageHandler pingRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(engineConfig.getFdStore() + "/v1/admin/ping");
    handler.setExpectedResponseType(String.class);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  @Bean
  IntegrationFlow storePingEngineFlow() {

    return IntegrationFlows.from(storePingEngine())
        .handle(pingStoreEngineRequest())
        .get();
  }

  private MessageHandler pingStoreEngineRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(engineConfig.getFdStore() + "/v1/admin/ping/{storeService}");
    SpelExpressionParser expressionParser = new SpelExpressionParser();
    Map<String, Expression> vars = new HashMap<>();
    vars.put("storeService", expressionParser.parseExpression("payload"));
    handler.setUriVariableExpressions(vars);
    handler.setExpectedResponseType(String.class);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  @MessagingGateway
  public interface StorePingGateway {
    @Gateway(requestChannel = "storePing", requestTimeout = 2000)
    String ping();

    @Gateway(requestChannel = "storePingEngine", requestTimeout = 2000)
    String ping(String storeEngine);

  }


}