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

package org.flockdata.engine.integration.store;

/**
 * For SDN4 Un-managed Extensions
 * <p/>
 * Created by mike on 21/07/15.
 */

import org.flockdata.engine.PlatformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile({"fd-server"})
public class StorePingRequest {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Bean
    MessageChannel storePing(){
        return new DirectChannel();
    }

    @Bean
    IntegrationFlow storePingFlow() {

        return IntegrationFlows.from(storePing())
                .handle(pingRequest())
                .get();
    }

    private MessageHandler pingRequest() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdStore() + "/v1/admin/ping");
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.GET);

        return handler;
    }


}