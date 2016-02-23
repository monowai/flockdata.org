package org.flockdata.engine.integration.store;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.store.bean.StorageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles reading content from fd-store
 * Created by mike on 17/02/16.
 */
@IntegrationComponentScan
@Configuration
@Profile({"integration","production"})
public class StorageReader {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Bean
    MessageChannel startStoreRead(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel storeReadResult(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel receiveStoreReadReply() {
        return new DirectChannel();
    }

    @Bean
    IntegrationFlow storeFlow() {

        return IntegrationFlows.from(startStoreRead())
                .handle(handler())
                .get();
    }

    private MessageHandler handler() {
        SpelExpressionParser expressionParser = new SpelExpressionParser();
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdStore()+ "/v1/data/{store}/{index}/{type}/{key}");
        handler.setHttpMethod(HttpMethod.GET);
        Map<String, Expression> vars = new HashMap<>();
        vars.put("store", expressionParser.parseExpression("payload[0]"));
        vars.put("index", expressionParser.parseExpression("payload[1]"));
        vars.put("type", expressionParser.parseExpression("payload[2]"));
        vars.put("key", expressionParser.parseExpression("payload[3]"));
        handler.setUriVariableExpressions(vars);
        handler.setExpectedResponseType(StorageBean.class);
        return handler;
    }


}
