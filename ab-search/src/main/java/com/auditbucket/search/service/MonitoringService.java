package com.auditbucket.search.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.PingResult;
import com.auditbucket.search.model.QueryParams;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

@Service
@MessageEndpoint
public class MonitoringService {

    @ServiceActivator(inputChannel = "doEsPingRequest",outputChannel = "doEsPingResponse") // Subscriber
    public PingResult ping() {
        PingResult pingResult = new PingResult("Pong!");
        return pingResult;
    }

}
