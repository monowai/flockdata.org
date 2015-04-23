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

package org.flockdata.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Integration configuration for Rabbit and AMQP
 *
 * Created by mike on 21/04/15.
 */
@Configuration
@EnableRabbit
public class AmqpConfig {

    private final Logger logger = LoggerFactory.getLogger(AmqpConfig.class);

    @Value("${rabbit.host:localhost}")
    private String rabbitHost;

    @Value("${rabbit.user:guest}")
    private String rabbitUser;

    @Value("${rabbit.pass:guest}")
    private String rabbitPass;

    @Value("${fd-search.queue}")
    private String searchQueue;

    @Value("${fd-store.queue}")
    private String storeQueue;

    @Value("${rabbit.port}")
    private Integer rabbitPort;

    @Value("${rabbit.publisherCacheSize}")
    private Integer pubCache;


    private Integer heartBeat= 0;

    @Value("${rabbit.heartbeat.secs:@null}")
    protected void setHeartBeat(String heartBeat){
        if (!heartBeat.equals("@null"))
            this.heartBeat= Integer.parseInt(heartBeat);

    }

    @Value("${rabbit.publisher.confirms}")
    private Boolean confirms;

    @Value("${rabbit.publisher.returns}")
    private Boolean returns;

    @Value("${fd-engine.exchange}")
    private String trackExchange;

    @Value("${fd-track.queue}")
    private String trackQueue;

    @Value("${fd-track.binding}")
    private String trackBinding;

    @Qualifier("fd-track")
    @Autowired
    ThreadPoolTaskExecutor executor;

    public AmqpConfig (){
        logger.info( "**** FlockData AMQP Configuration deploying");
    }

    @Bean(name = "connectionFactory")
    public ConnectionFactory getConnectionFactory() {
        CachingConnectionFactory connectionFactory =  new CachingConnectionFactory(rabbitHost);
        connectionFactory.setUsername(rabbitUser);
        connectionFactory.setPassword(rabbitPass);
        connectionFactory.setPort(rabbitPort);
        connectionFactory.setRequestedHeartBeat(heartBeat);
        //connectionFactory.addConnectionListener(getConnectionListener());
        connectionFactory.setPublisherConfirms(confirms);
        connectionFactory.setPublisherReturns(returns);
        connectionFactory.setChannelCacheSize(pubCache);
        connectionFactory.setExecutor(executor);
        return connectionFactory;
    }

//    @Bean
//    public ConnectionListener getConnectionListener() {
//        ConnectionListener listener = new ConnectionListener() {
//            @Override
//            public void onCreate(Connection connection) {
//                logger.info("Open Connection");
//
//            }
//
//            @Override
//            public void onClose(Connection connection) {
//                logger.info("Close Connection");
//            }
//
//        };
//
//        return listener;
//    }



}
