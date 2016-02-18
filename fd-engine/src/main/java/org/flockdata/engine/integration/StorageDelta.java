package org.flockdata.engine.integration;

import org.flockdata.store.bean.DeltaBean;
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
public class StorageDelta {

    @Bean
    MessageChannel startDelta(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel deltaReply(){
        return new DirectChannel();
    }

    // HTTP
    @MessagingGateway
    @Profile({"integration","production"})
    public interface DeltaGateway {
        @Gateway(requestChannel = "startDelta", requestTimeout = 40000, replyChannel = "deltaReply")
        @Retryable
        boolean isSame(DeltaBean deltaBean);
    }

}
