package org.flockdata.search.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@IntegrationComponentScan
public class SearchChannels {

    @Bean
    MessageChannel doMetaKeyQuery() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel doContentQuery() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel getEntity() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel doContentReply() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel contentReply(){
        return new DirectChannel();
    }

}
