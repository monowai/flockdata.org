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

package org.flockdata.store.configuration;

import org.flockdata.helper.VersionHelper;
import org.flockdata.store.FdStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static org.flockdata.store.service.KvService.KV_STORE;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Configuration
public class StoreConfig implements FdStoreConfig {


    private Logger logger = LoggerFactory.getLogger(StoreConfig.class);

    private Boolean multiTenanted = false;

    private KV_STORE kvStore = null;

    private Boolean storeEnabled = true;

    @Value("${fd-search.url:http://localhost:8081}")
    String fdSearchUrl;

    @Value("${riak.hosts:127.0.0.1}")
    private String riakHosts;

    public String riakHosts() {
        return riakHosts;
    }

    @Value ("${redis.port:6379}")
    private int redisPort;

    @Value ("${redis.host:localhost}")
    private String redisHost;

    public String fdSearchUrl() {
        return fdSearchUrl;
    }

    @Override
    @Value("${fd-store.engine}")
    public void setKvStore(String kvStore) {
        if ( kvStore.equalsIgnoreCase(KV_STORE.REDIS.toString()))
            setKvStore( KV_STORE.REDIS);
        else if (kvStore.equalsIgnoreCase(KV_STORE.RIAK.toString()))
            setKvStore( KV_STORE.RIAK);
        else if (kvStore.equalsIgnoreCase(KV_STORE.NONE.toString()))
            setKvStore( KV_STORE.NONE);
        else if (kvStore.equalsIgnoreCase(KV_STORE.MEMORY.toString()))
            setKvStore( KV_STORE.MEMORY);
        else {
            setKvStore( KV_STORE.NONE);
            logger.error("Unable to resolve the fd-store.engine property [" + kvStore + "]. Defaulting to DEFAULT");
        }

    }

    @Override
    public KV_STORE setKvStore(KV_STORE kvStore) {
        KV_STORE current = this.kvStore;
        this.kvStore = kvStore;
        return current;

    }

    @Override
    public KV_STORE kvStore() {
        return kvStore;
    }

    /**
     * Default property for a fortress if not explicitly set.
     * When true (default) KV versions of information will be tracked
     *
     * @param storeEnabled defaults to true
     */
    @Value("${fd-store.enabled}")
    public void setStoreEnabled(String storeEnabled) {
        this.storeEnabled = "@null".equals(storeEnabled) || Boolean.parseBoolean(storeEnabled);
    }

    public Boolean storeEnabled(){
        return this.storeEnabled;
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
        healthResults.put("fd-store.version", version);
        String config = System.getProperty("fd.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("fd.integration");
        healthResults.put("fd.integration", integration);
        healthResults.put("fd-store.engine", String.valueOf(kvStore));

        return healthResults;

    }

    public int redisPort() {
        return redisPort;
    }

    public String redisHost() {
        return redisHost;
    }
}
