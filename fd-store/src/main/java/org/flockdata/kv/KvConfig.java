/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.kv;

import org.flockdata.helper.VersionHelper;
import org.flockdata.kv.service.KvService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service
@Transactional
public class KvConfig implements FdKvConfig {


    private String rabbitHost;

    private String rabbitPort;

    private Logger logger = LoggerFactory.getLogger(KvConfig.class);

    private Boolean multiTenanted = false;

    private KvService.KV_STORE kvStore = null;


    @Value("${rabbit.host:@null}")
    protected void setRabbitHost(String rabbitHost) {
        if ("@null".equals(rabbitHost)) this.rabbitHost = null;
        else this.rabbitHost = rabbitHost;

    }

    @Value("${rabbit.port:@null}")
    protected void setRabbitPort(String rabbitPort) {
        if ("@null".equals(rabbitPort)) this.rabbitPort = null;
        else this.rabbitPort = rabbitPort;

    }

    @Override
    @Value("${fd-engine.kv.store}")
    public void setKvStore(String kvStore) {
        if ("@null".equals(kvStore) || kvStore.equalsIgnoreCase("redis"))
            this.kvStore = KvService.KV_STORE.REDIS;
        else if (kvStore.equalsIgnoreCase("riak"))
            this.kvStore = KvService.KV_STORE.RIAK;
        else if (kvStore.equalsIgnoreCase("MEMORY"))
            this.kvStore = KvService.KV_STORE.MEMORY;
        else {
            logger.error("Unable to resolve the fd-engine.kv.store property [" + kvStore + "]. Defaulting to REDIS");
        }

    }

    @Override
    public KvService.KV_STORE getKvStore() {
        return kvStore;
    }

    boolean asyncWrite = false;

    @Value("${fd-engine.kv.async:@null}")
    protected void setAsyncWrite(String kvAsync) {
        if (!"@null".equals(kvAsync))
            this.asyncWrite = Boolean.parseBoolean(kvAsync);
    }

    public boolean isAsyncWrite() {
        return asyncWrite;
    }

    /**
     * Only users with a pre-validated api-key should be calling this
     * @return system configuration details
     */
    @Override
    public Map<String, String> getHealth() {

        String version = VersionHelper.getABVersion();
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("fd-store.version", version);
        String config = System.getProperty("fd.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("fd.integration");
        healthResults.put("fd.integration", integration);
        healthResults.put("fd-engine.kv.store", String.valueOf(kvStore));

        return healthResults;

    }

}
