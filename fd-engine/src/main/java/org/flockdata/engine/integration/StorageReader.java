package org.flockdata.engine.integration;

import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.annotation.Retryable;

/**
 * Created by mike on 17/02/16.
 */
@IntegrationComponentScan
@Configuration
@Profile({"integration","production"})
public class StorageReader {

    @Bean
    MessageChannel startStoreRead(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel storeReadResult(){
        return new DirectChannel();
    }

    // HTTP
    @MessagingGateway
    public interface ReadStorageGateway {
        @Gateway(requestChannel = "startStoreRead", requestTimeout = 40000, replyChannel = "storeReadResult")
        @Retryable
        KvContent read(LogRequest readContentFor);
    }
}
