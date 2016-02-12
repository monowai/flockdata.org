package org.flockdata.engine.integration.neorest;

/**
 *
 * For SDN4 Un-managed Extensions
 *
 * Created by mike on 21/07/15.
 */

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
 * Not currently used. Waiting for binary driver to Neo4j before taking the next step
 * this approach was designed to call webservices deployed into Neo4j.
 *
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile("neorest")
public class NeoAdminRequests {

    @Autowired
    FdNeoChannels channels;

    @Bean
    IntegrationFlow doFdNeoHealth() {

        return IntegrationFlows.from("neoFdHealth")
                .handle(fdHealthRequest())
                .get();
    }

    @Bean
    IntegrationFlow doFdNeoPing() {

        return IntegrationFlows.from("neoFdPing")
                .handle(fdPingRequest())
                .get();
    }

    private MessageHandler fdPingRequest() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(channels.getUriRoot());
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.GET);

        return handler;
    }

    private MessageHandler fdHealthRequest() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getHealthUrl());
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.GET);

        return handler;
    }

    public String getHealthUrl() {
        return channels.getUriRoot()+ "health";
    }



}