package org.flockdata.engine.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * Created by mike on 20/02/16.
 */
@Configuration
@IntegrationComponentScan
public class StorageWriter {

    @Bean
    MessageChannel startStoreWrite(){
        return new DirectChannel();
    }
}
