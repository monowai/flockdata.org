package org.flockdata.engine.integration;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 12/02/16.
 */
@Configuration
public class Exchanges {
    @Value("${fd-search.messaging.binding:fd.search.binding}")
    String searchBinding;

    @Value("${fd-search.messaging.exchange:fd.search.exchange}")
    String searchExchange;

    @Value("${fd-search.messaging.dlq.queue:fd.search.dlq.queue}")
    private String searchDlq;

    @Value("${fd-search.messaging.dlq.exchange:fd.search.dlq.exchange}")
    private String searchDlqExchange;

    @Value("${fd-track.messaging.binding:fd.track.binding}")
    String trackBinding;

    @Value("${fd-track.messaging.exchange:fd.track.exchange}")
    String trackExchange;

    @Value("${fd-track.messaging.dlq.queue:fd.track.dlq.queue}")
    private String trackhDlq;

    @Value("${fd-track.messaging.dlq.exchange:fd.track.dlq.exchange}")
    private String trackDlqExchange;

    @Value("${fd-engine.binding:fd.engine.binding}")
    private String engineBinding;

    @Value("${fd-engine.messaging.exchange:fd.engine.exchange}")
    private String engineExchange;

    @Value("${fd-engine.messaging.dlq.exchange:fd.engine.dlq.exchange}")
    private String engineDlqExchange;

    @Value("${fd-engine.messaging.queue:fd.engine.queue}")
    private String engineQueue;

    @Value("${fd-engine.messaging.concurrentConsumers:2}")
    private int engineConcurrentConsumers;

    @Value("${fd-engine.messaging.prefetchCount:3}")
    private int enginePreFetchCount;


    @Value("${fd-store.binding:fd.store.binding}")
    private String storeBinding;

    @Value("${fd-store.messaging.exchange:fd.store.exchange}")
    private String storeExchange;

    @Value("${fd-store.messaging.dlq.exchange:fd.store.dlq.exchange}")
    private String storeDlqExchange;

    @Value("${fd-store.messaging.dlq.queue:fd.store.dlq.queue}")
    private String storeDlq;

    @Value("${fd-search.messaging.queue:fd.search.queue}")
    private String searchQueue;

    @Value("${fd-search.messaging.concurrentConsumers:2}")
    private int searchConcurrentConsumers;

    @Value("${fd-search.messaging.prefetchCount:3}")
    private int searchPreFetchCount;

    public String searchBinding() {
        return searchBinding;
    }

    public String searchExchange() {
        return searchExchange;
    }

    String engineBinding() {
        return engineBinding;
    }

    String engineExchangeName(){
        return engineExchange;
    }
    @Bean
    public Queue fdEngineQueue(){
        Map<String,Object>params = new HashMap<>();
        params.put("x-dead-letter-exchange", engineDlqExchange);
        return new Queue(engineQueue, true, false, false, params);

    }

    @Bean
    Exchange fdEngineExchange() {
        return new DirectExchange(engineExchangeName());
    }

    @Bean
    Queue fdSearchQueueDlq(){
        return new Queue(searchDlq);
    }

    @Bean
    Queue fdStoreQueue(){
        Map<String,Object>params = new HashMap<>();
        params.put("x-dead-letter-exchange", engineDlqExchange);
        return new Queue(engineQueue, true, false, false, params);

    }

    @Bean
    Queue fdStoreQueueDlq(){
        return new Queue(storeDlq);
    }

    @Bean
    Queue fdSearchQueue(){
        Map<String,Object>params = new HashMap<>();
        params.put("x-dead-letter-exchange", searchDlqExchange);
        return new Queue(searchQueue, true, false, false, params);
    }

    @Bean
    Exchange fdSearchExchange() {
        return new DirectExchange(searchExchange);
    }

    @Bean
    Exchange fdSearchDlqExchange() {
        return new DirectExchange(searchDlqExchange);
    }

    public String storeBinding() {
        return storeBinding;
    }

    public String storeExchange() {
        return storeExchange;
    }

    public String trackExchange(){
        return trackExchange;
    }

    public String trackBinding(){
        return trackBinding;
    }

    public int enginePreFetchCount() {
        return enginePreFetchCount;
    }

    public int engineConcurrentConsumers() {
        return engineConcurrentConsumers;
    }
}
