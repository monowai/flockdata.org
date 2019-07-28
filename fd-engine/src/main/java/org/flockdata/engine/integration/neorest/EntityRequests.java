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

package org.flockdata.engine.integration.neorest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.track.bean.EntityResults;
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
 * SDN4 UMX Entity requests
 *
 * @author mholdsworth
 * @since 28/06/2015
 */
@Configuration
@IntegrationComponentScan
@Profile("neorest")
public class EntityRequests extends NeoRequestBase {

  @Bean
  public IntegrationFlow makeEntities() {
    return IntegrationFlows.from(channels.neoFdMakeEntity())
        .transform(getTransformer())
        .handle(fdMakeEntityRequest())
        .get();
  }

  private MessageHandler fdMakeEntityRequest() {

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getEntityUrl());
    handler.setExpectedResponseType(EntityResults.class);

    return handler;
  }

  @Bean
  public IntegrationFlow findEntityByKey() {
    return IntegrationFlows.from(channels.neoFdFindEntity())
        .handle(fdFindByKey())
        .get();
  }

  private MessageHandler fdFindByKey() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getKeyUrl());

    handler.setExpectedResponseType(EntityNode.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("key", expressionParser.parseExpression("payload[0]"));
    handler.setUriVariableExpressions(vars);

    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

  @Bean
  public IntegrationFlow findLabeledEntities() {
    return IntegrationFlows.from(channels.neoFdFindLabeledEntities())
        .handle(fdFindByLabeledEntities())
        .get();
  }

  private MessageHandler fdFindByLabeledEntities() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getLabelFindUrl());

    handler.setExpectedResponseType(ArrayList.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("fortressId", expressionParser.parseExpression("payload[0]"));
    vars.put("label", expressionParser.parseExpression("payload[1]"));
    vars.put("skipCount", expressionParser.parseExpression("payload[2]"));
    handler.setUriVariableExpressions(vars);

    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

  @Bean
  public IntegrationFlow findByCode() {
    return IntegrationFlows.from(channels.neoFdFindByCallerRef())
        .handle(fdFindByCode())
        .get();
  }

  private MessageHandler fdFindByCode() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getCallerRefUrl());

    handler.setExpectedResponseType(EntityNode.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("fortressId", expressionParser.parseExpression("payload[0]"));
    vars.put("docId", expressionParser.parseExpression("payload[1]"));
    vars.put("code", expressionParser.parseExpression("payload[2]"));
    handler.setUriVariableExpressions(vars);

    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

}