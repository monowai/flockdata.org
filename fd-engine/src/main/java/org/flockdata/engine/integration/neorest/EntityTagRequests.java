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
import org.flockdata.data.AbstractEntityTag;
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
 * SDN4 UMX Requests
 *
 * @author mholdsworth
 * @since 15/07/2015
 */
@Configuration
@IntegrationComponentScan
@Profile("neorest")
public class EntityTagRequests extends NeoRequestBase {

  @Bean
  public IntegrationFlow addEntityTag() {

    return IntegrationFlows.from(channels.neoFdAddEntityTag())
        .transform(getTransformer())
        .handle(fdAddEntityTagRequest())
        .get();
  }

  private MessageHandler fdAddEntityTagRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getEntityTagUrl());

    handler.setExpectedResponseType(String.class);
    return handler;
  }

  @Bean
  public IntegrationFlow findEntityTag() {
    return IntegrationFlows.from(channels.neoFdGetEntityTag())
        //.transform(getTransformer())
        .handle(neoFindEntityTag())
        .get();
  }

  private MessageHandler neoFindEntityTag() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getEntityTag());

    handler.setExpectedResponseType(AbstractEntityTag.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("entityId", expressionParser.parseExpression("payload[0]"));
    vars.put("tagType", expressionParser.parseExpression("payload[1]"));
    vars.put("tagCode", expressionParser.parseExpression("payload[2]"));
    vars.put("relationshipType", expressionParser.parseExpression("payload[3]"));
    handler.setUriVariableExpressions(vars);

    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

  @Bean
  public IntegrationFlow findEntityTags() {
    return IntegrationFlows.from(channels.neoFdGetEntityTags())
        //.transform(getTransformer())
        .handle(neoFindEntityTags())
        .get();
  }

  private MessageHandler neoFindEntityTags() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getEntityTags());

    handler.setExpectedResponseTypeExpression(expressionParser.parseExpression("T (org.flockdata.model.EntityTag[])"));
    Map<String, Expression> vars = new HashMap<>();
    vars.put("entityId", expressionParser.parseExpression("payload[0]"));
    handler.setUriVariableExpressions(vars);

    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

}
