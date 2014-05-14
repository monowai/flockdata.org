package com.auditbucket.engine.service;

import com.auditbucket.search.model.PingResult;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

@Service
@MessageEndpoint
public class MonitoringService {

    @ServiceActivator(inputChannel = "doAbEnginePingRequest", outputChannel = "doAbEnginePingResponse") // Subscriber
    public PingResult ping() {
        PingResult pingResult = new PingResult("Pong!");
        return pingResult;
    }

}
