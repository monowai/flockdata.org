package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.integration.AbstractIntegrationRequest;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Pulls the "data" block from ElasticSearch
 * Created by mike on 13/02/16.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration", "production"})
public class EsStoreRequest extends AbstractIntegrationRequest {
    @Autowired
            @Qualifier("engineConfig")
    PlatformConfig platformConfig;

    @MessagingGateway
    public interface ContentStoreEs {
        @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 600, multiplier = 5, random = true))
        @Gateway(requestChannel = "doDataQuery", replyChannel = "receiveContentReply")
        EsSearchResult getData(QueryParams queryParams);
    }


    @Bean
    MessageChannel receiveContentReply() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel sendDataQuery() {
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

    private String getDataQuery() {
        // The endpoint in fd-search
        return platformConfig.getFdSearch() + "/v1/query/data";
    }

    @Bean
    MessageChannel doDataQuery() {
        return new DirectChannel();
    }

    // Seems we have to transform via this
    @Transformer(inputChannel = "doDataQuery", outputChannel = "sendDataQuery")
    public Message<?> transformRequest(Message theObject) {
        return objectToJson().transform(theObject);
    }

}
