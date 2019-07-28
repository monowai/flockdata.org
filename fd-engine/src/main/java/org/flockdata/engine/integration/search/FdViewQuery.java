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
import org.flockdata.search.EsSearchRequestResult;
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
 * Striped down search support. Designed for fd-view. ToDo: Move to a "Backend for Frontend" module
 *
 * @author mholdsworth
 * @tag Query, Search, Gateway, Messaging
 * @since 14/02/2016
 */

@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class FdViewQuery {

  private final PlatformConfig engineConfig;

  @Autowired
  public FdViewQuery(@Qualifier("engineConfig") PlatformConfig engineConfig) {
    this.engineConfig = engineConfig;
  }

  @Bean
  IntegrationFlow fdViewQueryFlow() {
    return IntegrationFlows
        .from("doFdViewQuery")
        .transform(Transformers.toJson(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .handle(Http.outboundGateway(engineConfig.getFdSearch() + "/v1/query/fdView")
            .charset(StandardCharsets.UTF_8.name())
            .httpMethod(HttpMethod.POST)
            .mappedRequestHeaders("*")
            .extractPayload(true)
            .expectedResponseType(EsSearchRequestResult.class))
        .get();
  }

  @MessagingGateway
  public interface FdViewQueryGateway {

    @Gateway(requestChannel = "doFdViewQuery")
    EsSearchRequestResult fdSearch(QueryParams queryParams);

  }


}
