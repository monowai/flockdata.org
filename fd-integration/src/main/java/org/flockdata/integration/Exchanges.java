/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised queues etc.
 *
 * Created by mike on 12/02/16.
 */
@Configuration
@Profile({"fd-server"})
public class Exchanges {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${org.fd.messaging.exchange:fd}")
    private String fdExchangeName;

    @Value("${org.fd.search.messaging.dlq.queue:fd.search.dlq.queue}")
    private String searchDlq;

    @Value("${org.fd.engine.messaging.queue:fd.engine.queue}")
    private String engineQueue;

    @Value("${org.fd.store.messaging.queue:fd.store.queue}")
    private String storeQueue;

    @Value("${org.fd.track.messaging.queue:fd.track.queue}")
    private String trackQueue;

    @Value("${org.fd.search.messaging.queue:fd.search.queue}")
    private String searchQueue;

    @Value("${org.fd.engine.messaging.dlq.queue:fd.engine.dlq.queue}")
    private String engineDlq;

    @Value("${org.fd.store.messaging.dlq.queue:fd.store.dlq.queue}")
    private String storeDlq;

    @Value("${org.fd.track.messaging.dlq.queue:fd.track.dlq.queue}")
    private String trackDlq;

    @Value("${org.fd.engine.messaging.concurrentConsumers:2}")
    private int engineConcurrentConsumers;

    @Value("${org.fd.engine.messaging.prefetchCount:3}")
    private int enginePreFetchCount;

    @Value("${org.fd.search.messaging.concurrentConsumers:2}")
    private int searchConcurrentConsumers;

    @Value("${org.fd.search.messaging.prefetchCount:3}")
    private int searchPreFetchCount;

    @Value("${org.fd.track.messaging.prefetchCount:3}")
    private int trackPreFetchCount;

    @Value("${org.fd.track.messaging.concurrentConsumers:2}")
    private int trackConcurrentConsumers;

    @Value("${org.fd.search.messaging.binding:fd.search.binding}")
    String searchBinding;

    @Value("${org.fd.track.messaging.binding:fd.track.binding}")
    String trackBinding;

    @Value("${org.fd.engine.messaging.binding:fd.engine.binding}")
    String engineBinding;

    @Value("${org.fd.store.messaging.binding:fd.store.binding}")
    String storeBinding;

    @Value("${org.fd.store.messaging.concurrentConsumers:2}")
    private int storeConcurrentConsumers;

    @Value("${org.fd.store.messaging.prefetchCount:3}")
    private int storePreFetchCount;

    public String searchBinding() {
        return searchBinding;
    }

    public String fdExchangeName() {
        return fdExchangeName;
    }

    @Bean
    public Exchange fdTrackExchange() {
        return new DirectExchange(fdExchangeName);
    }

    public Map<String,Object> getTrackQueueFeatures() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", fdExchangeName);
        return params;
    }

    @Bean
    public Queue fdTrackQueue() {
        return new Queue(trackQueue, true, false, false, getTrackQueueFeatures());
    }

    @Bean
    public Queue fdStoreQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", fdExchangeName);
        return new Queue(storeQueue, true, false, false, params);
    }

    @Bean
    public Queue fdSearchQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", fdExchangeName);
        return new Queue(searchQueue, true, false, false, params);
    }

    @Bean
    public Queue fdEngineQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", fdExchangeName);
        return new Queue(engineQueue, true, false, false, params);
    }

    @Bean
    Binding engineDlqBinding(Queue fdEngineDlq, Exchange fdExchange) {
        return BindingBuilder.bind(fdEngineDlq).to(fdExchange).with(engineDlq).noargs();
    }

    @Bean
    Binding engineBinding(Queue fdEngineQueue, Exchange fdExchange) {
        return BindingBuilder.bind(fdEngineQueue).to(fdExchange).with(engineBinding).noargs();
    }

    @Bean
    Binding trackBinding(Queue fdTrackQueue, Exchange fdExchange) {
        return BindingBuilder.bind(fdTrackQueue).to(fdExchange).with(trackBinding).noargs();
    }

    @Bean
    Binding searchBinding(Queue fdSearchQueue, Exchange fdExchange) {
        return BindingBuilder.bind(fdSearchQueue).to(fdExchange).with(searchBinding).noargs();
    }

    @Bean
    Binding storeBinding(Queue fdStoreQueue, Exchange fdExchange) {
        return BindingBuilder.bind(fdStoreQueue).to(fdExchange).with(storeBinding).noargs();
    }

    @Bean
    Binding trackDlqBinding(Queue fdTrackDlq, Exchange fdExchange) {
        return BindingBuilder.bind(fdTrackDlq).to(fdExchange).with(trackBinding).noargs();
    }

    @Bean
    Binding searchDlqBinding(Queue fdSearchDlq, Exchange fdExchange) {
        return BindingBuilder.bind(fdSearchDlq).to(fdExchange).with(searchBinding).noargs();
    }

    @Bean
    Binding storeDlqBinding(Queue fdStoreDlq, Exchange fdExchange) {
        return BindingBuilder.bind(fdStoreDlq).to(fdExchange).with(storeBinding).noargs();
    }

    // DLQ
    @Bean
    public Queue fdTrackDlq() {
        return new Queue(trackDlq);
    }

    @Bean
    public Queue fdSearchDlq() {
        return new Queue(searchDlq);
    }
    @Bean
    public Queue fdEngineDlq() {
        return new Queue(engineDlq);
    }

    @Bean
    public Queue fdStoreDlq() {
        return new Queue(storeDlq);
    }

    // GENERIC BINDINGS
    public String storeBinding() {
        return storeBinding;
    }

    public int enginePreFetchCount() {
        return enginePreFetchCount;
    }

    public int engineConcurrentConsumers() {
        return engineConcurrentConsumers;
    }

    public int trackPreFetchCount() {
        return trackPreFetchCount;
    }

    public int trackConcurrentConsumers() {
        return trackConcurrentConsumers;
    }

    public int storeConcurrentConsumers() {
        return storeConcurrentConsumers;
    }

    public int storePreFetchCount() {
        return storePreFetchCount;
    }

    public String fdEngineBinding() {
        return engineBinding;
    }

    public int searchConcurrentConsumers() {
        return searchConcurrentConsumers;
    }

    public int searchPreFetchCount() {
        return searchPreFetchCount;
    }

    @PostConstruct
    void logStatus() {
        logger.info("**** Exchanges (ex.shared) have been initialised");
    }

}
