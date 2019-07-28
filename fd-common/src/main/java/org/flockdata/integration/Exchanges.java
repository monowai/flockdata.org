/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Centralised integration configurations for serverside services
 *
 * @author mholdsworth
 * @tag Integration
 * @since 12/02/2016
 */
@Configuration
@Profile( {"fd-server"})
public class Exchanges {

  @Value("${org.fd.messaging.exchange:fd}")
  public String fdExchangeName;
  private Logger logger = LoggerFactory.getLogger("configuration");
  @Value("${org.fd.search.messaging.dlx.queue:fd.search.dlx.queue}")
  private String searchDlx;

  @Value("${org.fd.engine.messaging.queue:fd.engine.queue}")
  private String engineQueue;

  @Value("${org.fd.store.messaging.queue:fd.store.queue}")
  private String storeQueue;

  @Value("${org.fd.track.messaging.queue:fd.track.queue}")
  private String trackQueue;

  @Value("${org.fd.search.messaging.queue:fd.search.queue}")
  private String searchQueue;

  @Value("${org.fd.engine.messaging.dlx.queue:fd.engine.dlx.queue}")
  private String engineDlx;

  @Value("${org.fd.store.messaging.dlx.queue:fd.store.dlx.queue}")
  private String storeDlx;

  @Value("${org.fd.track.messaging.dlx.queue:fd.track.dlx.queue}")
  private String trackDlx;

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
  private String searchBinding;

  @Value("${org.fd.track.messaging.binding:fd.track.binding}")
  private String trackBinding;

  @Value("${org.fd.engine.messaging.binding:fd.engine.binding}")
  private String engineBinding;

  @Value("${org.fd.store.messaging.binding:fd.store.binding}")
  private String storeBinding;

  @Value("${org.fd.store.messaging.concurrentConsumers:2}")
  private int storeConcurrentConsumers;

  @Value("${org.fd.store.messaging.prefetchCount:3}")
  private int storePreFetchCount;

  private AmqpRabbitConfig rabbitConfig;

  @Autowired
  void setAmqpRabbitConfig(AmqpRabbitConfig rabbitConfig) {
    this.rabbitConfig = rabbitConfig;
  }

  public String fdExchangeName() {
    return fdExchangeName;
  }

  public String searchBinding() {
    return searchBinding;
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

  @Bean
  public Exchange fdExchange() {
    return new DirectExchange(fdExchangeName());
  }

  @Bean
  public Exchange fdExchangeDlx() {
    return new DirectExchange(rabbitConfig.fdExchangeDlxName());
  }

  @Bean
  public Queue fdTrackQueue() {
    return new Queue(trackQueue, true, false, false, rabbitConfig.getFdQueueFeatures());
  }

  @Bean
  public Queue fdStoreQueue() {
    return new Queue(storeQueue, true, false, false, rabbitConfig.getFdQueueFeatures());
  }

  @Bean
  public Queue fdSearchQueue() {
    return new Queue(searchQueue, true, false, false, rabbitConfig.getFdQueueFeatures());
  }

  @Bean
  public Queue fdEngineQueue() {
    return new Queue(engineQueue, true, false, false, rabbitConfig.getFdQueueFeatures());
  }

  @Bean
  Binding engineBinding(Queue fdEngineQueue, Exchange fdExchange) {
    return BindingBuilder.bind(fdEngineQueue).to(fdExchange).with(engineBinding).noargs();
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
  Binding trackBinding(Queue fdTrackQueue, Exchange fdExchange) {
    return BindingBuilder.bind(fdTrackQueue).to(fdExchange).with(trackBinding).noargs();
  }

  @Bean
  Binding trackDlxBinding(Queue fdTrackDlx, Exchange fdExchangeDlx) {
    return BindingBuilder.bind(fdTrackDlx).to(fdExchangeDlx).with(trackQueue).noargs();
  }

  @Bean
  Binding engineDlxBinding(Queue fdEngineDlx, Exchange fdExchangeDlx) {
    return BindingBuilder.bind(fdEngineDlx).to(fdExchangeDlx).with(engineQueue).noargs();
  }

  @Bean
  Binding searchDlxBinding(Queue fdSearchDlx, Exchange fdExchangeDlx) {
    return BindingBuilder.bind(fdSearchDlx).to(fdExchangeDlx).with(searchQueue).noargs();
  }

  @Bean
  Binding storeDlxBinding(Queue fdStoreDlx, Exchange fdExchangeDlx) {
    return BindingBuilder.bind(fdStoreDlx).to(fdExchangeDlx).with(storeQueue).noargs();
  }

  // Dead Letter Exchange Queues
  @Bean
  public Queue fdTrackDlx() {
    return new Queue(trackDlx);
  }

  @Bean
  public Queue fdSearchDlx() {
    return new Queue(searchDlx);
  }

  @Bean
  public Queue fdEngineDlx() {
    return new Queue(engineDlx);
  }

  @Bean
  public Queue fdStoreDlx() {
    return new Queue(storeDlx);
  }


  @PostConstruct
  void logStatus() {
    logger.info("**** Exchanges (ex.shared) have been initialised");
  }

  @Bean
  RetryOperationsInterceptor trackInterceptor(AmqpTemplate amqpTemplate) {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(1)
        .recoverer(new RepublishMessageRecoverer(amqpTemplate, rabbitConfig.fdExchangeDlxName(), trackQueue))
        .build();
  }


  @Bean
  RetryOperationsInterceptor searchInterceptor(AmqpTemplate amqpTemplate) {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(2)
        .recoverer(new RepublishMessageRecoverer(amqpTemplate, rabbitConfig.fdExchangeDlxName(), searchQueue))
        .build();
  }

  @Bean
  RetryOperationsInterceptor storeInterceptor(AmqpTemplate amqpTemplate) {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(2)
        .recoverer(new RepublishMessageRecoverer(amqpTemplate, rabbitConfig.fdExchangeDlxName(), storeQueue))
        .build();
  }

}
