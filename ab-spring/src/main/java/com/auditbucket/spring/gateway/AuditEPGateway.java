package com.auditbucket.spring.gateway;

import org.springframework.integration.annotation.Gateway;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 13/08/13
 * Time: 01:26
 * To change this template use File | Settings | File Templates.
 */
public interface AuditEPGateway {

    @Gateway(requestChannel = "pingChannel")
    String get();

    @Gateway(requestChannel = "healthChannel")
    Map<String, String> getHealth();
}
