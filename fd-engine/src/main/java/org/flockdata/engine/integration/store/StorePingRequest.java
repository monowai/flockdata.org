package org.flockdata.engine.integration.store;

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
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration", "production"})
public class StorePingRequest {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Bean
    MessageChannel storePing(){
        return new DirectChannel();
    }

    @Bean
    IntegrationFlow storePingFlow() {

        return IntegrationFlows.from(storePing())
                .handle(pingRequest())
                .get();
    }

    private MessageHandler pingRequest() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdStore() + "/v1/admin/ping");
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.GET);

        return handler;
    }


}