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

import java.nio.charset.StandardCharsets;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.dsl.support.Transformers;

/**
 * Finds Keys for a given set of query parameters. This can be used to drive queries in the Graph as
 * the key will give you a starting point
 *
 * @author mholdsworth
 * @tag Messaging, Search, Entity, Query, Gateway
 * @since 14/02/2016
 */

@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class EntityKeyQuery {

  private final PlatformConfig engineConfig;

  @Autowired
  public EntityKeyQuery(@Qualifier("engineConfig") PlatformConfig engineConfig) {
    this.engineConfig = engineConfig;
  }

  @Bean
  IntegrationFlow fdKeyQueryFlow() {
    return IntegrationFlows
        .from("doKeyQuery")
        .transform(Transformers.toJson(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .handle(Http.outboundGateway(engineConfig.getFdSearch() + "/v1/query/keys")
            .charset(StandardCharsets.UTF_8.name())
            .httpMethod(HttpMethod.POST)
            .mappedRequestHeaders("*")
            .extractPayload(true)
            .expectedResponseType(EntityKeyResults.class))
        .get();
  }

  @MessagingGateway
  public interface EntityKeyGateway {
    @Gateway(requestChannel = "doKeyQuery")
    EntityKeyResults keys(QueryParams queryParams);
  }
}
