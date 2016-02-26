package org.flockdata.engine.integration.engine;

import com.google.common.net.MediaType;
import org.flockdata.engine.integration.AmqpRabbitConfig;
import org.flockdata.engine.integration.Exchanges;
import org.flockdata.engine.integration.search.FdSearchChannels;
import org.flockdata.engine.track.service.SearchHandler;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.SearchResults;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * fd-search -->> fd-engine (inbound)
 *
 * Created by mike on 21/07/15.
 */
@Service
@Profile({"integration","production"})
public class WriteEntityResult {

    @Autowired
    SearchHandler searchHandler;

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    @Autowired
    FdSearchChannels channels;

    @Autowired
    Exchanges exchanges;

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();
    private ObjectToJsonTransformer transformer;

    @PostConstruct
    public void createTransformer() {
        transformer = new ObjectToJsonTransformer(
                new Jackson2JsonObjectMapper(JsonUtils.getMapper())
        );
        transformer.setContentType(MediaType.JSON_UTF_8.toString());
    }

    public ObjectToJsonTransformer getTransformer(){
        return transformer;
    }

    @Bean
    MessageChannel searchDocSyncResult () {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow writeSearchResultFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlows.from(
                Amqp.inboundAdapter(connectionFactory, exchanges.fdEngineQueue())
                        .outputChannel(searchDocSyncResult())
                        .maxConcurrentConsumers(exchanges.engineConcurrentConsumers())
                        .prefetchCount(exchanges.enginePreFetchCount())

        )
                .handle(handler())
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = "searchDocSyncResult")
    public MessageHandler handler() {
        return message -> {
            try {
                syncSearchResult(objectMapper.readValue((byte[])message.getPayload(), SearchResults.class));
            } catch (IOException e) {
                throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
            }

        };
    }

    /**
     * Callback handler that is invoked from fd-search. This routine ties the generated search document ID
     * to the Entity
     * <p/>
     * ToDo: On completion of this, an outbound message should be posted so that the caller can be made aware(?)
     *
     * @param searchResults contains keys to tie the search to the entity
     */
    public void syncSearchResult(SearchResults searchResults) {
        searchHandler.handlResults(searchResults);
    }


}
