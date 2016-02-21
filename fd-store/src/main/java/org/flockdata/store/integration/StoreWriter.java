package org.flockdata.store.integration;

import org.flockdata.helper.FlockException;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.store.service.StoreService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.IOException;

import static org.flockdata.helper.FdJsonObjectMapper.getObjectMapper;

/**
 *
 * Inbound handler to write data to a store
 *
 * Created by mike on 17/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class StoreWriter {

    @Autowired
    StoreService fdStoreManager;

    @Autowired
    Exchanges exchanges;

    @Bean
    MessageChannel startStoreWrite(){
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory) throws Exception {
        return IntegrationFlows.from(
                Amqp.inboundAdapter(connectionFactory, exchanges.fdStoreQueue())
                        .outputChannel(startStoreWrite())

                        .maxConcurrentConsumers(exchanges.storeConcurrentConsumers())
                        .prefetchCount(exchanges.storePreFetchCount())
        )
                .handle(handler())
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = "startStoreWrite")
    public MessageHandler handler() throws Exception {
        return message -> {
            try {
                fdStoreManager.doWrite(
                    getObjectMapper().readValue((byte[])message.getPayload(), StorageBean.class) );
            } catch (IOException e) {
                //logger.error("Unable to de-serialize the payload");
                throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
            } catch (FlockException e) {
                throw new AmqpRejectAndDontRequeueException("Error writing the payload", e);
            }

        };
    }



}
