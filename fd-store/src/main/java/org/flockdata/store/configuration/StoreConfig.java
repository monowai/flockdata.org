/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.store.configuration;

import org.flockdata.helper.VersionHelper;
import org.flockdata.store.Store;
import org.flockdata.store.service.FdStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Configuration
public class StoreConfig implements FdStoreConfig {


    private Logger logger = LoggerFactory.getLogger(StoreConfig.class);

    private Store kvStore = null;

//    @Value("${org.fd.store.system.enabled}")
//    private Boolean storeEnabled = true;

    @Value("${org.fd.search.api:http://localhost:8081/api}")
    String fdSearchUrl;

    @Value("${riak.hosts:127.0.0.1}")
    private String riakHosts;

    @Value ("${redis.port:6379}")
    private int redisPort;

    @Value ("${redis.host:localhost}")
    private String redisHost;

    public String fdSearchUrl() {
        return fdSearchUrl;
    }

    public String riakHosts() {
        return riakHosts;
    }

    /**
     * Only users with a pre-validated api-key should be calling this
     *
     * @return system configuration details
     */
    @Override
    public Map<String, String> health() {

        String version = VersionHelper.getFdVersion();
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("fd.store.version", version);
        String config = System.getProperty("fd.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("fd.integration");
        healthResults.put("fd.integration", integration);
        healthResults.put("fd.store.system.engine", String.valueOf(kvStore));

        return healthResults;

    }

    public int redisPort() {
        return redisPort;
    }

    public String redisHost() {
        return redisHost;
    }
}
