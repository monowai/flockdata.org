package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
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
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Striped down search support. Designed for fd-view. ToDo: Move to a "Backend for Frontend" module
 *
 * Created by mike on 14/02/16.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class FdViewQuery {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;


    @MessagingGateway
    public interface FdViewGateway {
        @Gateway(requestChannel = "sendSearchRequest", replyChannel = "receiveFdViewReply")
        EsSearchResult fdSearch(QueryParams queryParams);
    }

    @Bean
    IntegrationFlow fdViewQueryFlow() {

        return IntegrationFlows.from("doFdViewQuery")
                .handle(fdViewQueryHandler())
                .transform(messageSupport.objectToJson())
                .get();
    }

    private MessageHandler fdViewQueryHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getFdViewQuery());
        handler.setExpectedResponseType(org.flockdata.search.model.EsSearchResult.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }

    public String getFdViewQuery() {
        return engineConfig.getFdSearch()+ "/v1/query/fdView";
    }

    // Seems we have to transform via this
    @Transformer(inputChannel="sendSearchRequest", outputChannel="doFdViewQuery")
    public Message<?> fdQueryTransform(Message theObject){
        return messageSupport.toJson(theObject);
    }

}
