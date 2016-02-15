/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.integration;

import org.flockdata.configure.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;

import javax.annotation.PostConstruct;

/**
 *
 * Rabbit MQ / AMQP
 *
 * Created by mike on 3/07/15.
 */

@Configuration
@EnableRabbit
@IntegrationComponentScan
@Profile({"integration","production"})
public class AmqpRabbitConfig {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${rabbit.host:localhost}")
    String rabbitHost;

    @Value("${rabbit.port:5672}")
    Integer rabbitPort;

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

    @Autowired
    AsyncConfig asyncConfig;

    @PostConstruct
    public void logStatus (){
        logger.info( "**** FlockData AMQP Configuration deployed");
        logger.info ( "rabbit.host: {}, rabbit.port {}, rabbit.user {}",rabbitHost, rabbitPort, rabbitUser);
    }

    @Bean
    RabbitTemplate rabbitTemplate () throws Exception {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    ConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory connect = new CachingConnectionFactory();
        connect.setHost(rabbitHost);
        connect.setPort(rabbitPort);
        connect.setUsername(rabbitUser);
        connect.setPassword(rabbitPass);
        connect.setPublisherConfirms(publisherConfirms);
        connect.setPublisherReturns(publisherReturns);
        connect.setExecutor(asyncConfig.engineExecutor());
        connect.setChannelCacheSize(publisherCacheSize);
        return connect;
    }

    public Boolean getAmqpLazyConnect() {
        return amqpLazyConnect;
    }

    public String getHost() {
        return rabbitHost;
    }

    public Integer getPort() {
        return rabbitPort;
    }
}
