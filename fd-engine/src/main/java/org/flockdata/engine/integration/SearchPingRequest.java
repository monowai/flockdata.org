package org.flockdata.engine.integration;

/**
 * For SDN4 Un-managed Extensions
 * <p/>
 * Created by mike on 21/07/15.
 */

import org.flockdata.engine.PlatformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration", "production"})
public class SearchPingRequest {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Bean
    MessageChannel fdSearchPing(){
        return new DirectChannel();
    }

    @MessagingGateway
    public interface MonitoringGateway {
        @Payload("new java.util.Date()")
        @Gateway(requestChannel = "fdSearchPing")
        String ping();
    }

    @Bean
    IntegrationFlow doFdSearchPing() {

        return IntegrationFlows.from(fdSearchPing())
                .handle(fdPingRequest())
                .get();
    }

    private MessageHandler fdPingRequest() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch() + "/v1/admin/ping");
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.GET);

        return handler;
    }


}