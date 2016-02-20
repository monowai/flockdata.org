package org.flockdata.engine.integration;

import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.FortressSegment;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.MediationFacade;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
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
 *
 * Created by mike on 27/12/15.
 */
@Service
public class TrackRequests {

    @Autowired
    TrackBatchSplitter batchSplitter;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    @Bean
    MessageChannel doTrackEntity () {
        return new DirectChannel();
    }

    @ServiceActivator(inputChannel = "doTrackEntity")
    public Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, @Header(value = "apiKey") String apiKey) throws FlockException, IOException, ExecutionException, InterruptedException {
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new AmqpRejectAndDontRequeueException("Unable to resolve the company for your ApiKey");
        Map<FortressSegment, List<EntityInputBean>> byFortress = batchSplitter.getEntitiesBySegment(c, inputBeans);
        Collection<TrackRequestResult> results = new ArrayList<>();
        for (FortressSegment segment : byFortress.keySet()) {
            Collection<TrackResultBean>tr=
                    mediationFacade.trackEntities(segment, byFortress.get(segment), 2);
            for (TrackResultBean result : tr) {
                results.add(new TrackRequestResult(result));
            }

        }
        return results;
    }

}
