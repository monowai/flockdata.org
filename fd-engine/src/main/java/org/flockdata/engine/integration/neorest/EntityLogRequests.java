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

import java.util.HashMap;
import java.util.Map;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.track.EntityLogs;
import org.flockdata.track.bean.TrackResultBean;
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
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageHandler;

/**
 * SDN4 UMX Log requests
 *
 * @author mholdsworth
 * @since 6/07/2015
 */
@Configuration
@IntegrationComponentScan
@Profile("neorest")
public class EntityLogRequests extends NeoRequestBase {

  @Bean
  public IntegrationFlow makeLog() {

    return IntegrationFlows.from(channels.neoFdWriteLog())
        .transform(getTransformer())
        .handle((fdMakeLogRequest()))
        .get();
  }

  private MessageHandler fdMakeLogRequest() {
    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getLogUrl());

    handler.setExpectedResponseType(EntityLog.class);
    return handler;
  }

  @Bean
  public IntegrationFlow getEntityLogFlow() {

    return IntegrationFlows.from(channels.neoFdGetEntityLog())
        .handle(fdGetEntityLog())
        .get();
  }

  private MessageHandler fdGetEntityLog() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getEntityLog());

    handler.setExpectedResponseType(EntityLog.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("entityId", expressionParser.parseExpression("payload[0]"));
    vars.put("logId", expressionParser.parseExpression("payload[1]"));
    handler.setUriVariableExpressions(vars);
    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }


  @Bean
  public IntegrationFlow findLastLog() {

    return IntegrationFlows.from(channels.neoFdGetLastChange())
        .handle(fdFindLastChange())
        .get();
  }

  private MessageHandler fdFindLastChange() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getLastLog());

    handler.setExpectedResponseType(EntityLog.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("entityId", expressionParser.parseExpression("payload[0]"));
    handler.setUriVariableExpressions(vars);
    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

  @Bean
  public IntegrationFlow findEntityLogs() {

    return IntegrationFlows.from(channels.neoFdGetEntityLogs())
        .handle(fdEntityLogs())
        .get();
  }

  private MessageHandler fdEntityLogs() {

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getForEntity());

    handler.setExpectedResponseType(EntityLogs.class);
    SpelExpressionParser expressionParser = new SpelExpressionParser();
    Map<String, Expression> vars = new HashMap<>();
    vars.put("entityId", expressionParser.parseExpression("payload[0]"));
    handler.setUriVariableExpressions(vars);
    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }

  @Bean
  public IntegrationFlow findEntityLogsBeforeDate() {

    return IntegrationFlows.from(channels.neoFdLogsBeforeDate())
        .handle(fdEntityLogsBeforeDate())
        .get();
  }

  private MessageHandler fdEntityLogsBeforeDate() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getFindLogsBeforeUrl());

    handler.setExpectedResponseType(EntityLogs.class);
    Map<String, Expression> vars = new HashMap<>();
    vars.put("entityId", expressionParser.parseExpression("payload[0]"));
    vars.put("time", expressionParser.parseExpression("payload[1]"));
    handler.setUriVariableExpressions(vars);
    handler.setHttpMethod(HttpMethod.GET);
    return handler;
  }


  @Bean
  public IntegrationFlow cancelLastEntityLog() {

    return IntegrationFlows.from(channels.neoFdCancelLastLog())
        .transform(new ObjectToJsonTransformer(new Jackson2JsonObjectMapper()))
        .handle(fdCancelLastLog())
        .get();
  }

  private MessageHandler fdCancelLastLog() {
    SpelExpressionParser expressionParser = new SpelExpressionParser();

    HttpRequestExecutingMessageHandler handler =
        new HttpRequestExecutingMessageHandler(getLogUrl());

    handler.setExpectedResponseType(TrackResultBean.class);
//        Map<String, Expression> vars = new HashMap<>();
//        vars.put("entityId", expressionParser.parseExpression("payload[0]"));
//        handler.setUriVariableExpressions(vars);
    handler.setHttpMethod(HttpMethod.DELETE);

    return handler;
  }


}