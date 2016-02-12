package org.flockdata.engine.integration;

/**
 *
 * For SDN4 Un-managed Extensions
 *
 * Created by mike on 21/07/15.
 */

import org.flockdata.engine.PlatformConfig;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class SearchAdminRequests {

    @Autowired
    FdSearchChannels channels;

    @Autowired
    PlatformConfig engineConfig;

    @Bean
    IntegrationFlow doFdSearchPing() {

        return IntegrationFlows.from("fdSearchPing")
                .handle(fdPingRequest())
                .get();
    }

    private MessageHandler fdPingRequest() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getHealthUrl());
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.GET);

        return handler;
    }


    public String getHealthUrl() {
        return engineConfig.getFdSearch()+ "/v1/admin/ping";
    }



}