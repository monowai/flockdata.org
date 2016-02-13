package org.flockdata.search.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * Not sure we need this.
 * Created by mike on 13/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
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
