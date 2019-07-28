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

package org.flockdata.search.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * Not sure we need this.
 *
 * @author mholdsworth
 * @since 13/02/2016
 */
@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class IntegrationHandlers {

  @Bean
  public MessageHandler logger() {
    LoggingHandler loggingHandler = new LoggingHandler("INFO");
    loggingHandler.setLoggerName("logger");
    // This is redundant because the default expression is exactly "payload"
    // loggingHandler.setExpression("payload");
    return loggingHandler;
  }

}
