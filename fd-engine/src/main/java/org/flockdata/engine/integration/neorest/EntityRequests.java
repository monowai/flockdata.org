package org.flockdata.engine.integration.neorest;

import org.flockdata.model.Entity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SDN4 UMX Entity requests
 *
 * Created by mike on 28/06/15.
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
    public IntegrationFlow findEntityByMetaKey() {
        return IntegrationFlows.from(channels.neoFdFindEntity())
                .handle(fdFindByMetaKey())
                .get();
    }

    private MessageHandler fdFindByMetaKey() {
        SpelExpressionParser expressionParser = new SpelExpressionParser();

        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getMetaKeyUrl());

        handler.setExpectedResponseType(Entity.class);
        Map<String, Expression> vars = new HashMap<>();
        vars.put("metaKey", expressionParser.parseExpression("payload[0]"));
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
    public IntegrationFlow findByCallerRef() {
        return IntegrationFlows.from(channels.neoFdFindByCallerRef())
                .handle(fdFindByCallerRef())
                .get();
    }

    private MessageHandler fdFindByCallerRef() {
        SpelExpressionParser expressionParser = new SpelExpressionParser();

        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getCallerRefUrl());

        handler.setExpectedResponseType(Entity.class);
        Map<String, Expression> vars = new HashMap<>();
        vars.put("fortressId", expressionParser.parseExpression("payload[0]"));
        vars.put("docId", expressionParser.parseExpression("payload[1]"));
        vars.put("code", expressionParser.parseExpression("payload[2]"));
        handler.setUriVariableExpressions(vars);

        handler.setHttpMethod(HttpMethod.GET);
        return handler;
    }

}