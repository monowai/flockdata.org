package org.flockdata.kv.integration;

import org.flockdata.kv.KvConfig;
import org.flockdata.search.model.EsSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Pulls the "data" block from ElasticSearch
 * Created by mike on 13/02/16.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class KvEsStoreRequest extends AbstractIntegrationRequest {

    @Autowired
    KvConfig kvConfig;

    @Bean
    MessageChannel receiveContentReply(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel sendDataQuery(){
        return new DirectChannel();
    }

    @Bean
    IntegrationFlow dataQuery() {
        return IntegrationFlows.from(sendDataQuery())
                .handle(dataQueryHandler())
                .get();
    }

    private MessageHandler dataQueryHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getDataQuery());
        handler.setHttpMethod(HttpMethod.POST);
        handler.setExpectedResponseType(EsSearchResult.class);
        return handler;
    }

    public String getDataQuery() {
        // The endpoint in fd-search
        return kvConfig.getFdSearchUrl()+ "/v1/query/data";
    }

    @Bean
    MessageChannel doDataQuery(){
        return new DirectChannel();
    }

    // Seems we have to transform via this
    @Transformer(inputChannel="doDataQuery", outputChannel="sendDataQuery")
    public Message<?> transformRequest(Message theObject){
        return objectToJson().transform(theObject);
    }

}
