package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class SearchQueryRequests {
    @Autowired
    FdSearchChannels channels;

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;

    // Seems we have to transform via this
    @Transformer(inputChannel="doMetaKeyQuery", outputChannel="doMetaKeyQuery")
    public Message<?> transformRequest(Message theObject){
        return messageSupport.toJson(theObject);
    }

    @Bean
    IntegrationFlow metaKeyQuery() {

        return IntegrationFlows.from("doMetaKeyQuery")
                .handle(metaKeyHandler())
                .transform(messageSupport.objectToJson())
                .get();
    }

    private MessageHandler metaKeyHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getMetaKeyQuery());
        handler.setExpectedResponseType(org.flockdata.search.model.MetaKeyResults.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }



    @Bean
    IntegrationFlow tagCloudQuery() {

        return IntegrationFlows.from("doTagCloudQuery")
                .handle(tagCloudHandler())
                .transform(messageSupport.objectToJson())
                .get();
    }

    private MessageHandler tagCloudHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getTagCloudQuery());
        handler.setExpectedResponseType(org.flockdata.search.model.TagCloud.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }


    public String getMetaKeyQuery() {
        return engineConfig.getFdSearch()+ "/v1/query/metaKeys";
    }

    public String getTagCloudQuery() {
        return engineConfig.getFdSearch()+ "/v1/query/tagCloud";
    }




}
