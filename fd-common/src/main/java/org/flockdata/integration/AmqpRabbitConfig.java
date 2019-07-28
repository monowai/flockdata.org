/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.integration;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;

/**
 * Rabbit MQ / AMQP Configuration and channel initialization
 *
 * @author mholdsworth
 * @tag Configuration, Rabbit, Integration
 * @since 3/07/2015
 */

@Configuration
@IntegrationComponentScan
public class AmqpRabbitConfig {

  @Value("${org.fd.messaging.exchange:fd-dlx}")
  public String fdExchangeDlxName;
  private Logger logger = LoggerFactory.getLogger("configuration");
  @Value("${spring.rabbitmq.host:localhost}")
  private String rabbitHost;
  @Value("${spring.rabbitmq.virtual-host:/}")
  private String virtualHost;
  @Value("${spring.rabbitmq.port:5672}")
  private Integer rabbitPort;
  @Value("${spring.rabbitmq.persistentDelivery:true}")
  private boolean persistentDelivery;
  @Value("${spring.rabbitmq.username:guest}")
  private String rabbitUser;
  @Value("${spring.rabbitmq.password:guest}")
  private String rabbitPass;
  @Value("${spring.rabbitmq.publisher.confirms:false}")
  private Boolean publisherConfirms;
  @Value("${spring.rabbitmq.publisher.returns:false}")
  private Boolean publisherReturns;
  @Value("${spring.rabbitmq.publisherCacheSize:20}")
  private Integer publisherCacheSize;
  @Value("${amqp.lazyConnect:false}")
  private Boolean amqpLazyConnect;
  @Value("${amqp.channelCacheSize:25}")
  private int channelCacheSize;

  /**
   * failure to bind with consistent features can create new queues
   *
   * @return standard features used across FlockData work queues
   */
  public Map<String, Object> getFdQueueFeatures() {
    Map<String, Object> params = new HashMap<>();
    params.put("x-dead-letter-exchange", fdExchangeDlxName());
    return params;
  }


  private ConnectionFactory setConnectionProperties(CachingConnectionFactory connectionFactory) {
    logger.info("**** rabbitmq.host: [{}], rabbitmq.port [{}], rabbitmq.virtual-host [{}], rabbitmq.username [{}]", rabbitHost, rabbitPort, virtualHost, rabbitUser);

    // First load or a refresh
    connectionFactory.setHost(getHost());
    connectionFactory.setPort(getPort());
    connectionFactory.setUsername(getUser());
    connectionFactory.setPassword(getPass());
    connectionFactory.setPublisherConfirms(getPublisherConfirms());
    connectionFactory.setPublisherReturns(getPublisherReturns());
    connectionFactory.setChannelCacheSize(getChannelCacheSize());
    connectionFactory.setVirtualHost(getVirtualHost());
    return connectionFactory;
  }

  public Boolean getAmqpLazyConnect() {
    return amqpLazyConnect;
  }

  public boolean getPersistentDelivery() {
    return persistentDelivery;
  }

  public String getHost() {
    return rabbitHost;
  }

  public Integer getPort() {
    return rabbitPort;
  }

  public String getUser() {
    return rabbitUser;
  }

  public String getPass() {
    return rabbitPass;
  }

  private Boolean getPublisherConfirms() {
    return publisherConfirms;
  }

  private Boolean getPublisherReturns() {
    return publisherReturns;
  }

  private int getChannelCacheSize() {
    return channelCacheSize;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  // Supports docker-compose integration testing where the port is obtained and mapped dynamic
  public void resetHost(String url, Integer rabbitPort) {
    this.rabbitHost = url;
    this.rabbitPort = rabbitPort;
  }

  @Bean
  @Profile("fd-server")
  RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) throws Exception {
    return new RabbitTemplate(connectionFactory);
  }

  @Bean
  @Profile("fd-server")
  public AmqpAdmin amqpAdmin(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) throws Exception {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  @Profile("fd-server")
  ConnectionFactory connectionFactory() throws Exception {
    logger.info("Initialising Rabbit connection factory");
    return setConnectionProperties(new CachingConnectionFactory());
  }

  String fdExchangeDlxName() {
    return fdExchangeDlxName;
  }


  @PostConstruct
  public String logStatus() {
    String message = String.format("{spring.rabbitmq.host: \"%s:%s\", spring.rabbitmq.user: \"%s\"}",
        getHost(), getPort(), getUser());
    logger.info(message);
    return message;
  }
}
