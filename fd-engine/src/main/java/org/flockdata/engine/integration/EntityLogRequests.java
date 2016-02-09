package org.flockdata.engine.integration;

import org.flockdata.model.EntityLog;
import org.flockdata.track.EntityLogs;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.util.HashMap;
import java.util.Map;

/**
 * SDN4 UMX Log requests
 *
 * Created by mike on 6/07/15.
 */
@Configuration
@IntegrationComponentScan
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
        SpelExpressionParser expressionParser = new SpelExpressionParser();

        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getForEntity());

        handler.setExpectedResponseType(EntityLogs.class);
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