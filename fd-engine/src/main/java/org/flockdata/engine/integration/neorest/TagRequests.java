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

import java.util.HashMap;
import java.util.Map;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.AliasPayload;
import org.flockdata.track.bean.TagResults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageHandler;

/**
 * SDN4 UMX requests
 *
 * @author mholdsworth
 * @since 28/06/2015
 */

@Configuration
@IntegrationComponentScan
@Profile("neorest")
public class TagRequests extends NeoRequestBase {

  @Bean
  public IntegrationFlow makeTags() {

    return IntegrationFlows.from(channels.neoFdMakeTags())
        .transform(getTransformer())
        .handle(fdMakeTagsRequest())
        .get();
  }

  private MessageHandler fdMakeTagsRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getTagUrl());

    handler.setExpectedResponseType(TagResults.class);
    return handler;
  }

  @Bean
  public IntegrationFlow findTag() {

    return IntegrationFlows.from(channels.neoFdFindTag())
        .handle(fdFindTagsRequest())
        .get();
  }

  private MessageHandler fdFindTagsRequest() {

    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getFindTagUrl());

    handler.setExpectedResponseType(TagResultBean.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("label", expressionParser.parseExpression("payload[0]"));
    vars.put("code", expressionParser.parseExpression("payload[1]"));
    handler.setUriVariableExpressions(vars);
    handler.setHttpMethod(HttpMethod.GET);

    return handler;
  }

  @Bean
  public IntegrationFlow makeAlias() {

    return IntegrationFlows.from(channels.neoFdMakeAlias())
        .transform(getTransformer())
        .handle(fdMakeAliasRequest())
        .get();
  }

  private MessageHandler fdMakeAliasRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getAliasUrl());

    handler.setExpectedResponseType(AliasPayload.class);
    return handler;
  }


}
