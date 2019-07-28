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

import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.QueryParams;
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
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Striped down search support. Designed for fd-view. ToDo: Move to a "Backend for Frontend" module
 *
 * @author mholdsworth
 * @tag Messaging, Search
 * @since 14/02/2016
 */

@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class ContentStructureRequest {

  @Autowired
  @Qualifier("engineConfig")
  PlatformConfig engineConfig;

  @Bean
  public IntegrationFlow fxValuationFlow() {
    return IntegrationFlows.from("getStructure")
        .transform(Transformers.toJson())
        .handle(contentStructureHandler())
        .get();
  }

  @Bean
  public MessageChannel getStructure() {
    return new DirectChannel();
  }

  @Bean
  public MessageHandler contentStructureHandler() {

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch() + "/v1/content/");
    handler.setExpectedResponseType(ContentStructure.class);
    handler.setHttpMethod(HttpMethod.POST);

//        handler.setErrorHandler(analyticsErrorResponseHandler());
    return handler;
  }

  @MessagingGateway
  public interface ContentStructureGateway {
    @Gateway(requestChannel = "getStructure")
    ContentStructure getStructure(QueryParams queryParams);
  }

}
