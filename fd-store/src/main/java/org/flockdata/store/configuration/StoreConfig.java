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

package org.flockdata.store.configuration;

import org.flockdata.shared.VersionHelper;
import org.flockdata.store.Store;
import org.flockdata.store.service.FdStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Configuration
class StoreConfig implements FdStoreConfig {


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

    @Bean
    @Profile("eureka")
    public InfoEndpoint infoEndpoint() {
        return new InfoEndpoint(health());
    }

    @Autowired
    VersionHelper versionHelper;

    /**
     * Only users with a pre-validated api-key should be calling this
     *
     * @return system configuration details
     */
    @Override
    public Map<String, String> health() {

        String version =  versionHelper.getFdVersion();
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
