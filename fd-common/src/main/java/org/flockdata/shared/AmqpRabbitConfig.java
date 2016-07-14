/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;

import javax.annotation.PostConstruct;

/**
 * Rabbit MQ / AMQP Configuration and channel initialization
 * <p>
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile({"fd-server"})
public class AmqpRabbitConfig {

    private Logger logger = LoggerFactory.getLogger("configuration");


    @Value("${rabbit.host:localhost}")
    String rabbitHost;

    @Value("${rabbit.virtualHost:/}")
    String virtualHost;

    @Value("${rabbit.port:5672}")
    Integer rabbitPort;

    @Value("${persistentDelivery:true}")
    boolean persistentDelivery;

    @Value("${rabbit.user:guest}")
    String rabbitUser;

    @Value("${rabbit.pass:guest}")
    String rabbitPass;

    @Value("${rabbit.publisher.confirms:false}")
    Boolean publisherConfirms;

    @Value("${rabbit.publisher.returns:false}")
    Boolean publisherReturns;

    @Value("${rabbit.publisherCacheSize:22}")
    Integer publisherCacheSize;

    @Value("${amqp.lazyConnect:false}")
    Boolean amqpLazyConnect;

    @Value("${amqp.channelCacheSize:25}")
    private int channelCacheSize;

    @PostConstruct
    public void logStatus() {
        logger.info("**** FlockData AMQP Configuration deployed");
        logger.info("**** rabbit.host: [{}], rabbit.port [{}], rabbit.user [{}]", rabbitHost, rabbitPort, rabbitUser);
    }

    @Autowired
    Exchanges exchanges;

    public Boolean getAmqpLazyConnect() {
        return amqpLazyConnect;
    }

    public boolean getPersistentDelivery() {
        return persistentDelivery;
    }

    public boolean isPersistentDelivery() {
        return persistentDelivery;
    }

    private void setPersistentDelivery(boolean persistentDelivery) {
        this.persistentDelivery = persistentDelivery;
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

    public Boolean getPublisherConfirms() {
        return publisherConfirms;
    }

    public Boolean getPublisherReturns() {
        return publisherReturns;
    }

    public int getChannelCacheSize() {
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

    private ConnectionFactory setConnectionProperties(CachingConnectionFactory connectionFactory) {
        logger.info("Setting Rabbit connection properties");
        // First load or a refresh
        connectionFactory.setHost(getHost());
        connectionFactory.setPort(getPort());
        connectionFactory.setUsername(getUser());
        connectionFactory.setPassword(getPass());
        connectionFactory.setPublisherConfirms(getPublisherConfirms());
        connectionFactory.setPublisherReturns(getPublisherReturns());
        connectionFactory.setChannelCacheSize(getChannelCacheSize());
//        connectionFactory.setVirtualHost(getVirtualHost());
        return connectionFactory;
    }
}
