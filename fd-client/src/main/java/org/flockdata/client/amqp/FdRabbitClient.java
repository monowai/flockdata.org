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

package org.flockdata.client.amqp;

import com.rabbitmq.client.*;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Writes entity and tag payloads to FlockData over AMQP
 *
 * @author mholdsworth
 * @since 27/11/2014
 * @tag FdClient, Rabbit, Messaging
 */
@Service
@Profile({"fd-server", "fd-client"})
public class FdRabbitClient {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FdRabbitClient.class);
    private final ClientConfiguration configuration;
    private final AmqpRabbitConfig rabbitConfig;
    private AMQP.BasicProperties entityProps;
    private AMQP.BasicProperties tagProps;
    private Connection connection;

    private ConnectionFactory connectionFactory = null;

    private Channel trackChannel = null; // Channel is cached but not thread safe

    @Autowired
    public FdRabbitClient(ClientConfiguration clientConfiguration, AmqpRabbitConfig rabbitConfig) {
        this.configuration = clientConfiguration;
        this.rabbitConfig = rabbitConfig;
    }

    @PostConstruct
    public void logStatus() {
        logger.info("** Configured for Rabbit on {}", rabbitConfig.getHost());
    }

    @PreDestroy
    public void shuttingDown() {
        logger.debug("Jumping out of the rabbit hole due to shutdown request");
    }

    private void verifyConnection() {

        try {
            initConnectionFactory();

            if ( (trackChannel ==null ||connection== null) || ! (connection.isOpen() && trackChannel.isOpen())){
                openConnection();
            }
        } catch (TimeoutException | IOException e ) {
            logger.error("Unexpected error initializing Rabbit connection. Is it running @ {}:{}? {}", rabbitConfig.getHost(), rabbitConfig.getPort(), e.getMessage());
        }

    }

    private void initConnectionFactory() throws IOException, TimeoutException {
        if (connectionFactory == null ) {
            logger.debug("Initializing Rabbit connection");
            entityProps = null;
            tagProps = null;

            // Had issues with injecting amqpRabbitConfig.connectionFactory into this class
            // Connection was randomly closed in integration tests

            connectionFactory = new ConnectionFactory();
            connectionFactory.setAutomaticRecoveryEnabled(true);
            connectionFactory.setHost(rabbitConfig.getHost());
            connectionFactory.setPort(rabbitConfig.getPort());
            connectionFactory.setPassword(rabbitConfig.getPass());
            connectionFactory.setUsername(rabbitConfig.getUser());
            connectionFactory.setExceptionHandler(new ExceptionHandler() {
                @Override
                public void handleUnexpectedConnectionDriverException(Connection conn, Throwable exception) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleReturnListenerException(Channel channel, Throwable exception) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleFlowListenerException(Channel channel, Throwable exception) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleConfirmListenerException(Channel channel, Throwable exception) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleBlockedListenerException(Connection connection, Throwable exception) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleConsumerException(Channel channel, Throwable exception, Consumer consumer, String consumerTag, String methodName) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleConnectionRecoveryException(Connection conn, Throwable exception) {

                }

                @Override
                public void handleChannelRecoveryException(Channel ch, Throwable exception) {
                    logger.info(exception.getMessage());
                }

                @Override
                public void handleTopologyRecoveryException(Connection conn, Channel ch, TopologyRecoveryException exception) {
                    logger.info(exception.getMessage());
                }
            });
        }
    }

    private void openConnection() throws IOException, TimeoutException {
        if ( connectionFactory == null )
            initConnectionFactory();

        connection = connectionFactory.newConnection();
        connection.addShutdownListener(cause -> {
            Method reason = cause.getReason();
            if (cause.isHardError()) {
                if (!cause.isInitiatedByApplication()) {
                    logger.debug("Hard shutdown initiate - {} {} {}", (reason != null ? reason.protocolMethodName() : "null protocol method"), cause.getReason(), cause.getMessage());
                }
            } else {
                logger.debug("Shutdown initiated - {} {} {}", (reason != null ? reason.protocolMethodName() : "null protocol method"), cause.getReason(), cause.getMessage());
            }
            connection = null;
            connectionFactory = null;
        });

        trackChannel = connection.createChannel();
        logger.debug("{}/{}/{}", configuration.getTrackQueue(), configuration.getFdExchange(), configuration.getTrackRoutingKey());
        logger.debug(trackChannel.queueBind(configuration.getTrackQueue(), configuration.getFdExchange(), configuration.getTrackRoutingKey(), rabbitConfig.getFdQueueFeatures()).toString());

    }

    private String getApiKey() {
        if (configuration.getApiKey() == null || configuration.getApiKey().equals(""))
            throw new RuntimeException("No API key is set. Please configure one and try again");
        return configuration.getApiKey();

    }

    private AMQP.BasicProperties getTagProps() {
        if (tagProps == null)
            tagProps =
                    new AMQP.BasicProperties().builder()
                            .headers(getHeaders("T", getApiKey()))
                            .deliveryMode(rabbitConfig.getPersistentDelivery() ? 2 : null)
                            .replyTo("nullChannel").build();
        return tagProps;
    }

    private AMQP.BasicProperties getEntityProps() {
        if (entityProps == null)
            entityProps =
                    new AMQP.BasicProperties().builder()
                            .headers(getHeaders("E", getApiKey()))
                            .deliveryMode(rabbitConfig.getPersistentDelivery() ? 2 : null)
                            .replyTo("nullChannel").build();
        return entityProps;
    }

    public void publish(Collection<EntityInputBean> entityInputs) throws IOException {
        verifyConnection();
        assert trackChannel.isOpen();
        trackChannel.basicPublish(
                configuration.getFdExchange(),
                configuration.getTrackRoutingKey(),
                getEntityProps(),
                JsonUtils.toJsonBytes(entityInputs));
    }

    public void publishTags(Collection<TagInputBean> tagInputs) throws IOException {
        verifyConnection();
        if ( trackChannel == null )
            throw new RuntimeException(String.format("Failed to connect to Rabbit. Is it running @ %s:%s", rabbitConfig.getHost(), rabbitConfig.getPort()));
        else
            trackChannel.basicPublish(
                    configuration.getFdExchange(),
                    configuration.getTrackRoutingKey(),
                    getTagProps(),
                    JsonUtils.toJsonBytes(tagInputs));
    }

    private HashMap<String, Object> getHeaders(String type, String apiKey) {
        HashMap<String, Object> headers;
        headers = new HashMap<>();
        headers.put(ClientConfiguration.KEY_MSG_KEY, apiKey);
        headers.put(ClientConfiguration.KEY_MSG_TYPE, type);
        return headers;
    }

    /**
     * Used by integration tests to reset the port number.
     *
     * @param rabbitHost url
     * @param rabbitPort port
     */
    public void resetRabbitClient(String rabbitHost, Integer rabbitPort) {
        if (!(Objects.equals(rabbitHost, rabbitConfig.getHost()) && Objects.equals(rabbitPort, rabbitConfig.getPort()))) {
            rabbitConfig.resetHost(rabbitHost, rabbitPort);
            logger.info("Resetting Rabbit client clientConnection to {}:{}", rabbitConfig.getHost(), rabbitConfig.getPort());
            verifyConnection();
        }

    }


}
