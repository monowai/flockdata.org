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

package org.flockdata.engine.integration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised queues etc.
 *
 * Created by mike on 12/02/16.
 */
@Configuration
public class Exchanges {

    @Value("${org.fd.track.messaging.exchange:fd.track.exchange}")
    String trackExchange;

    @Value("${org.fd.engine.messaging.exchange:fd.engine.exchange}")
    private String engineExchange;

    @Value("${org.fd.search.messaging.exchange:fd.search.exchange}")
    String searchExchange;

    @Value("${org.fd.store.messaging.exchange:fd.store.exchange}")
    private String storeExchange;

    @Value("${org.fd.search.messaging.dlq.queue:fd.search.dlq.queue}")
    private String searchDlq;

    @Value("${org.fd.search.messaging.dlq.exchange:fd.search.dlq.exchange}")
    private String searchDlqExchange;

    @Value("${org.fd.store.messaging.dlq.exchange:fd.store.dlq.exchange}")
    private String storeDlqExchange;

    @Value("${org.fd.track.messaging.dlq.exchange:fd.track.dlq.exchange}")
    private String trackDlqExchange;

    @Value("${org.fd.engine.messaging.dlq.exchange:fd.engine.dlq.exchange}")
    private String engineDlqExchange;

    @Value("${org.fd.engine.messaging.queue:fd.engine.queue}")
    private String engineQueue;

    @Value("${org.fd.store.messaging.queue:fd.store.queue}")
    private String storeQueue;

    @Value("${org.fd.track.messaging.queue:fd.track.queue}")
    private String trackQueue;

    @Value("${org.fd.search.messaging.queue:fd.search.queue}")
    private String searchQueue;

    @Value("${org.fd.track.messaging.dlq.queue:fd.track.dlq.queue}")
    private String trackhDlq;

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

    @Value("${org.fd.engine.binding:fd.engine.binding}")
    String engineBinding;

    @Value("${org.fd.store.binding:fd.store.binding}")
    String storeBinding;

    @Value("${org.fd.store.messaging.concurrentConsumers:2}")
    private int storeConcurrentConsumers;

    @Value("${org.fd.store.messaging.prefetchCount:3}")
    private int storePreFetchCount;


    public String searchBinding() {
        return searchBinding;
    }

    public String searchExchange() {
        return searchExchange;
    }

    @Bean
    public Queue fdTrackQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", trackDlqExchange);
        // ToDo: Figure out DLQ Binding
        return new Queue(trackQueue, true, false, false, params);
    }

    @Bean
    public Queue fdStoreQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", storeDlqExchange);
        return new Queue(storeQueue, true, false, false, params);
    }

    @Bean
    public Queue fdSearchQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", searchDlqExchange);
        return new Queue(searchQueue, true, false, false, params);
    }

    @Bean
    public Queue fdEngineQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put("x-dead-letter-exchange", engineDlqExchange);
        return new Queue(engineQueue, true, false, false, params);
    }

    @Bean
    Binding engineDlqBinding(Queue fdEngineDlq, Exchange fdEngineDlqExchange) {
        return BindingBuilder.bind(fdEngineDlq).to(fdEngineDlqExchange).with(engineDlq).noargs();
    }

    @Bean
    Binding trackDlqBinding(Queue fdTrackDlq, Exchange fdTrackDlqExchange) {
        return BindingBuilder.bind(fdTrackDlq).to(fdTrackDlqExchange).with(trackDlq).noargs();
    }

    @Bean
    Binding searchDlqBinding(Queue fdSearchDlq, Exchange fdSearchDlqExchange) {
        return BindingBuilder.bind(fdSearchDlq).to(fdSearchDlqExchange).with(searchDlq).noargs();
    }

    @Bean
    Binding storeDlqBinding(Queue fdStoreDlq, Exchange fdStoreDlqExchange) {
        return BindingBuilder.bind(fdStoreDlq).to(fdStoreDlqExchange).with(storeDlq).noargs();
    }

    // DLQ
    @Bean
    public Queue fdTrackDlq() {
        // ToDo: Figure out DLQ Binding
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

    // Exchanges
    @Bean
    public Exchange fdTrackExchange() {
        return new DirectExchange(trackExchange);
    }

    @Bean
    public Exchange fdEngineExchange() {
        return new DirectExchange(engineExchange);
    }

    @Bean
    public Exchange fdSearchExchange() {
        return new DirectExchange(searchExchange);
    }

    @Bean
    public Exchange fdStoreExchange() {
        return new DirectExchange(storeExchange);
    }

    @Bean
    public Exchange fdTrackDlqExchange() {
        return new DirectExchange(trackDlqExchange);
    }

    @Bean
    public Exchange fdEngineDlqExchange() {
        return new DirectExchange(engineDlqExchange);
    }

    @Bean
    public Exchange fdSearchDlqExchange() {
        return new DirectExchange(searchDlqExchange);
    }

    @Bean
    public Exchange fdStoreDlqExchange() {
        return new DirectExchange(storeDlqExchange);
    }

    // GENERIC BINDINGS
    public String storeBinding() {
        return storeBinding;
    }

    public String storeExchange() {
        return storeExchange;
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

}
