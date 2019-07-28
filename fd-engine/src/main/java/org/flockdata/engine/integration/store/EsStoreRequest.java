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

import java.nio.charset.StandardCharsets;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.integration.AbstractIntegrationRequest;
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
 * Pulls the "data" block from ElasticSearch
 *
 * @author mholdsworth
 * @since 13/02/2016
 */

@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class EsStoreRequest extends AbstractIntegrationRequest {
  @Autowired
  @Qualifier("engineConfig")
  private PlatformConfig platformConfig;

  @Bean
  IntegrationFlow dataQuery() {
    return IntegrationFlows
        .from("doDataQuery")
        .transform(Transformers.toJson(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .handle(Http.outboundGateway(platformConfig.getFdSearch() + "/v1/query/data")
            .charset(StandardCharsets.UTF_8.name())
            .httpMethod(HttpMethod.POST)
            .mappedRequestHeaders("*")
            .extractPayload(true)
            .expectedResponseType(EsSearchRequestResult.class))
        .get();
  }

  @MessagingGateway
  public interface ContentStoreEs {
    //        @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 600, multiplier = 5, random = true))
    @Gateway(requestChannel = "doDataQuery")
    EsSearchRequestResult getData(QueryParams queryParams);
  }

}
