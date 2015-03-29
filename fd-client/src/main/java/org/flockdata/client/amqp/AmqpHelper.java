/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.client.amqp;

import com.rabbitmq.client.*;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

/**
 * User: mike
 * Date: 27/11/14
 * Time: 8:17 AM
 */
public class AmqpHelper {
    ConnectionFactory factory = new ConnectionFactory();
    Connection connection =null ;
    Channel channel  = null;
    String exchange;
    String queue;
    String routingKey;
    AMQP.BasicProperties.Builder builder;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AmqpHelper.class);

    public AmqpHelper (ClientConfiguration configuration){
        factory.setHost(configuration.getAmqpHostAddr());
        factory.setUsername(configuration.getRabbitUser());
        factory.setPassword(configuration.getRabbitPass());

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            this.queue = configuration.getTrackQueue();
            this.exchange = configuration.getTrackExchange();
            this.routingKey = configuration.getTrackRoutingKey();

            channel.queueBind(queue, exchange, routingKey);
            connection = factory.newConnection();
            HashMap<String,Object> headers = new HashMap<>();
            headers.put("apiKey", configuration.getApiKey());

            builder =
                    new AMQP.BasicProperties().builder()
                            .headers(headers)
                            .deliveryMode( configuration.getPersistentDelivery()?2:null)
                            .replyTo("nullChannel")
                    ;

        } catch (IOException e) {
            logger.error("Unexpected", e);
        }
    }

    public void close(){
        if ( connection != null )
            try {
                if ( channel!=null && channel.isOpen())
                    channel.close();
                if ( connection.isOpen())
                    connection.close();

            } catch (IOException e) {
                logger.error("Unexpected", e);
            }
    }


    public void publish(EntityInputBean entityInput) throws IOException {

        channel.basicPublish(exchange, routingKey, builder.build(), JsonUtils.getObjectAsJsonBytes(entityInput));
    }
}
