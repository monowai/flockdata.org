package com.auditbucket.search.service;

import com.auditbucket.search.model.PingResult;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.Payload;

public interface AbMonitoringGateway {
    /**
     * A required Payload must be declared due to spring Integration constraint
     * http://docs.spring.io/spring-integration/reference/html/messaging-endpoints-chapter.html#gateway-calling-no-argument-methods
     *
     * @return PingResult
     */
    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "pingAbEngineRequest", replyChannel = "pingAbEngineReply")
    public PingResult ping();
}

