package org.flockdata.engine.integration;

import org.flockdata.model.EntityTag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * SDN4 UMX Requests
 * Created by mike on 15/07/15.
 */
@Configuration
@IntegrationComponentScan
public class EntityTagRequests extends RequestBase{

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

        handler.setExpectedResponseType(EntityTag.class);
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
