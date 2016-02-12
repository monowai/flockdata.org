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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.messaging.MessageChannel;


/**
 * Search integration objects
 * Created by mike on 3/07/15.
 */
@Configuration
@IntegrationComponentScan
public class FdSearchChannels {


    @Bean
    MessageChannel sendEntityIndexRequest() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel syncSearchDocs() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel writeKvContent() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel writeEntityContent() {
        return new DirectChannel();
    }

    private MessageChannel trackResult() {
        return MessageChannels.direct("trackResult").get();
    }


}
