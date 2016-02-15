package org.flockdata.search.integration;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration and
 * Created by mike on 12/02/16.
 */
@Configuration
@IntegrationComponentScan
public class Exchanges {
    @Value("${fd-search.binding:fd.search.binding}")
    String searchBinding;

    @Value("${fd-search.exchange:fd.search.exchange}")
    String searchExchange;

    @Value("${fd-engine.binding:fd.engine.binding}")
    private String engineBinding;

    @Value("${fd-engine.exchange:fd.engine.exchange}")
    private String engineExchange;

    @Value("${fd-search.queue:fd.search.queue}")
    private String searchQueue;

    @Value("${fd-search.concurrentConsumers:2}")
    private int searchConcurrentConsumers;

    @Value("${fd-search.prefetchCount:3}")
    private int searchPreFetchCount;

    @Value("${fd-search.dlq.queue:fd.search.dlq.queue}")
    private String searchDlq;

    @Value("${fd-search.dlq.exchange:fd.search.dlq.exchange}")
    private String searchDlqExchange;

    @Value("${fd-engine.dlq.exchange:fd.engine.dlq.exchange}")
    private String engineDlqExchange;

    @Value("${fd-engine.queue:fd.engine.queue}")
    private String engineQueue;

    String engineBinding() {
        return engineBinding;
    }

    String engineExchangeName(){
        return engineExchange;
    }
    int searchConcurrentConsumers() {
        return searchConcurrentConsumers;
    }

    int searchPreFetchCount() {
        return searchPreFetchCount;
    }

    @Bean
    Queue fdSearchQueue(){
        Map<String,Object>params = new HashMap<>();
        params.put("x-dead-letter-exchange", searchDlqExchange);
        return new Queue(searchQueue, true, false, false, params);
    }

    @Bean
    Queue fdEngineQueue(){
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
    Exchange fdSearchExchange() {
        return new DirectExchange(searchExchange);
    }

    @Bean
    Exchange fdSearchDlqExchange() {
        return new DirectExchange(searchDlqExchange);
    }

}
