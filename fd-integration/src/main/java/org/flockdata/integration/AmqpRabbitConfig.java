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


    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String virtualHost;

    @Value("${spring.rabbitmq.port:5672}")
    private Integer rabbitPort;

    @Value("${persistentDelivery:true}")
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

    @Autowired
    Exchanges exchanges;

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

    @PostConstruct
    public void logStatus() {
        logger.info("**** FlockData RabbitAMQP configuration deployed");
    }
}
