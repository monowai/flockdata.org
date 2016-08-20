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

package org.flockdata.engine.integration.engine;

import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.model.Company;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.MediationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * Integration mechanism for message queue input
 * <p>
 * Created by mike on 27/12/15.
 */
@Service
@Profile({"fd-server"})
public class TrackRequests {

    private final MediationFacade mediationFacade;

    private final SecurityHelper securityHelper;


    private Exchanges exchanges;

    @Autowired
    public TrackRequests(MediationFacade mediationFacade, SecurityHelper securityHelper) {
        this.mediationFacade = mediationFacade;
        this.securityHelper = securityHelper;
    }

    @Autowired (required = false)
    void setExchanges ( Exchanges exchanges){
        this.exchanges = exchanges;
    }

    @Bean
    MessageChannel doTrackEntity() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel startEntityWrite() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel trackResult() {
        return new DirectChannel();
    }

    private Logger logger = LoggerFactory.getLogger(TrackRequests.class);

    public MessageHandler handler() throws FlockException, ExecutionException, InterruptedException {

        return message -> {
            try {

                Object oKey = message.getHeaders().get(ClientConfiguration.KEY_MSG_KEY);
                if (oKey == null) {
                    throw new AmqpRejectAndDontRequeueException("No api key");
                }
                Company company = securityHelper.getCompany(oKey.toString());
                if ( company == null ) {
                    logger.error( "Could not resolve company for apiKey [{}]", oKey);
                    throw new AmqpRejectAndDontRequeueException("Illegal api key");
                }

                Object oType = message.getHeaders().get(ClientConfiguration.KEY_MSG_TYPE);
                if ( oType == null || oType.toString().equalsIgnoreCase("E")) {
                    Collection<EntityInputBean> inputBeans = JsonUtils.toCollection((byte[]) message.getPayload(), EntityInputBean.class);
                    mediationFacade.trackEntities(company, inputBeans);
                }else {
                    Collection<TagInputBean> inputBeans = JsonUtils.toCollection((byte[]) message.getPayload(), TagInputBean.class);
                    mediationFacade.createTags(company, inputBeans);
                }
            } catch (IOException e) {
                throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
            } catch (InterruptedException |
                    ExecutionException |
                    InvalidDataAccessResourceUsageException |
                    InvalidDataAccessApiUsageException |
                    FlockException e) {
                logger.error(e.getMessage());
                throw new AmqpRejectAndDontRequeueException(String.format("Processing exception %s",e.getMessage()), e);
            }

        };
    }

    @Bean
    public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory) throws InterruptedException, FlockException, ExecutionException, IOException {
        return IntegrationFlows.from(
                Amqp.inboundAdapter(connectionFactory, exchanges.fdTrackQueue())
                        .maxConcurrentConsumers(exchanges.trackConcurrentConsumers())
                        .mappedRequestHeaders(ClientConfiguration.KEY_MSG_KEY, ClientConfiguration.KEY_MSG_TYPE)
                        .outputChannel(doTrackEntity())
                        .prefetchCount(exchanges.trackPreFetchCount())
        )
                .handle(handler())
                .get();
    }

    @ServiceActivator(inputChannel = "doTrackEntity")
    Collection<TagResultBean>  sendResult() {
        return new ArrayList<>();
        // What do with the response?
    }

//
//    @MessagingGateway(errorChannel = "trackError", asyncExecutor = "fd-track")
//    @Async("fd-track")
//    public interface InboundGateway {
//        //    ToDo: where to send the reply
//        @Gateway(requestChannel = "startEntityWrite", replyTimeout = 10000, replyChannel = "trackResult")
//        Future<TrackResultBean> doTrackEntity(EntityInputBean entityInputBean, @Header(value="apiKey") String apiKey);
//
//    }

}
