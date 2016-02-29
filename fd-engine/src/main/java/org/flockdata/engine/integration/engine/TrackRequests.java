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
import org.flockdata.engine.integration.MessageSupport;
import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Company;
import org.flockdata.model.FortressSegment;
import org.flockdata.shared.Exchanges;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.MediationFacade;
import org.flockdata.transform.ClientConfiguration;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Integration mechanism for message queue input
 * <p>
 * Created by mike on 27/12/15.
 */
@Service
public class TrackRequests {

    @Autowired
    TrackBatchSplitter batchSplitter;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    Exchanges exchanges;

    @Autowired
    SecurityHelper securityHelper;

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

    @Autowired
    MessageSupport messageSupport;


    public MessageHandler handler() throws FlockException, ExecutionException, InterruptedException {

        return message -> {
            try {
                Collection<EntityInputBean> inputBeans = JsonUtils.toCollection((byte[]) message.getPayload(), EntityInputBean.class);
                Object oKey = message.getHeaders().get(ClientConfiguration.KEY_MSG_KEY);
                if (oKey == null) {
                    throw new AmqpRejectAndDontRequeueException("No api key");
                }
                //String apiKey = oKey.toString();
                trackEntities(inputBeans, oKey.toString());
            } catch (IOException e) {
//                logger.error("Unable to de-serialize the payload. Rejecting due to [{}]", e.getMessage());
                throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
            } catch (InterruptedException | ExecutionException | FlockException e) {
                e.printStackTrace();
            }

        };
    }

    @Bean
    public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory) throws InterruptedException, FlockException, ExecutionException, IOException {
        return IntegrationFlows.from(
                Amqp.inboundAdapter(connectionFactory, exchanges.fdTrackQueue())
                        .maxConcurrentConsumers(exchanges.trackConcurrentConsumers())
                        .mappedRequestHeaders(ClientConfiguration.KEY_MSG_KEY)
                        .outputChannel(doTrackEntity())
                        .prefetchCount(exchanges.trackPreFetchCount())
        )
                .handle(handler())
                .get();
    }
    @ServiceActivator(inputChannel = "doTrackEntity")
    public Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, @Header(ClientConfiguration.KEY_MSG_KEY) String apiKey) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new AmqpRejectAndDontRequeueException("Unable to resolve the company for your ApiKey");

            Map<FortressSegment, List<EntityInputBean>> byFortress = batchSplitter.getEntitiesBySegment(c, inputBeans);
            Collection<TrackRequestResult> results = new ArrayList<>();
            for (FortressSegment segment : byFortress.keySet()) {
                Collection<TrackResultBean> tr =
                        mediationFacade.trackEntities(segment, byFortress.get(segment), 2);
                for (TrackResultBean result : tr) {
                    results.add(new TrackRequestResult(result));
                }

            }
            return results;
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
