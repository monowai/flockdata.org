package org.flockdata.store.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * Channel defintions
 * Created by mike on 13/02/16.
 */


@Configuration
@IntegrationComponentScan
public class KvChannels {
    @Bean
    MessageChannel storeErrors() {
        return new DirectChannel();
    }

    /**
     *
     * @return channel to start the transformation on
     */
    @Bean
    MessageChannel doDataQuery(){
        return new DirectChannel();
    }

    /**
     *
     * @return channel to dispatch the request on
     */
    @Bean
    MessageChannel sendDataQuery(){
        return new DirectChannel();
    }

    /**
     *
     * @return response channel
     */
    @Bean
    MessageChannel receiveContentReply(){
        return new DirectChannel();
    }


}
