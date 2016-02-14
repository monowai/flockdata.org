package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageHandler;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class TagCloudRequest {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;

    @Bean
    IntegrationFlow tagCloudQuery() {

        return IntegrationFlows.from("doTagCloudQuery")
                .handle(tagCloudHandler())
                .transform(messageSupport.objectToJson())
                .get();
    }

    private MessageHandler tagCloudHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch()+ "/v1/query/tagCloud");
        handler.setExpectedResponseType(org.flockdata.search.model.TagCloud.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }

}
