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

package org.flockdata.engine.admin;

import org.flockdata.engine.FdConfig;
import org.flockdata.engine.track.EntityDaoNeo;
import org.flockdata.helper.VersionHelper;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Company;
import org.flockdata.search.model.PingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.security.access.annotation.Secured;
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
public class EngineConfig implements FdConfig {

    @Autowired
    EntityDaoNeo trackDAO;

    private String abSearch;

    private String rabbitHost;

    private String rabbitPort;

    private Logger logger = LoggerFactory.getLogger(EngineConfig.class);

    private Boolean multiTenanted = false;

    private KvService.KV_STORE kvStore = null;

    @Qualifier("fdMonitoringGateway")
    @Autowired
    FdMonitoringGateway fdMonitoringGateway;

    @Autowired
    Neo4jTemplate template;

    private boolean conceptsEnabled=true;
    private boolean duplicateRegistration;

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

    @Value("${fd-search.url:@null}")
    protected void setFdSearch(String fdSearchMake) {
        if ("@null".equals(fdSearchMake)) this.abSearch = null;
        else this.abSearch = fdSearchMake;
    }

    @Value("${fdengine.multiTenanted:@null}")
    protected void setMultiTenanted(String multiTenanted) {
        this.multiTenanted = !"@null".equals(multiTenanted) && Boolean.parseBoolean(multiTenanted);
    }

    @Override
    @Value("${fd-engine.kv.store}")
    public void setKvStore(String kvStore) {
        if ("@null".equals(kvStore) || kvStore.equalsIgnoreCase("redis"))
            this.kvStore = KvService.KV_STORE.REDIS;
        else if (kvStore.equalsIgnoreCase("riak"))
            this.kvStore = KvService.KV_STORE.RIAK;
        else {
            logger.error("Unable to resolve the fd-engine.kv.store property [" + kvStore + "]. Defaulting to REDIS");
        }

    }

    @Override
    public KvService.KV_STORE getKvStore() {
        return kvStore;
    }

    @Override
    public String getTagSuffix(Company company) {
        if (company == null)
            return "";
        return (isMultiTenanted() ? company.getCode() : "");
    }
//    @Secured({"ROLE_AB_ADMIN"})
//    public Map<String, String> getHealthSecured() {
//        return getHealth();
//    }

    /**
     * Only users with a pre-validated api-key should be calling this
     * @return system configuration details
     */
    @Override
    public Map<String, String> getHealth() {
        if ( System.getProperty("neo4j")!=null )
            logger.warn("[-Dneo4j] is now an unsupported property. Ignoring this setting");
        String version = VersionHelper.getABVersion();
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("fd-engine.version", version);
        healthResults.put("fd-engine", trackDAO.ping());
        String config = System.getProperty("fd.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("fd.integration");
        healthResults.put("fd.integration", integration);
        healthResults.put("fd-engine.kv.store", String.valueOf(kvStore));
        String esPingResult ;
        try {
            PingResult esPing = fdMonitoringGateway.ping();
            esPingResult = (esPing == null || !esPing.getMessage().equals("Pong!")?"Problem":"Ok");
        } catch (Exception ce){
            esPingResult="!Unreachable! ";
            if ( ce.getCause() !=null )
                esPingResult = esPingResult + ce.getCause().getMessage();
        }
        healthResults.put("fd-search", esPingResult);

        //healthResults.put("fd.multiTenanted", multiTenanted.toString());
        if ("http".equalsIgnoreCase(integration)) {
            healthResults.put("fd-search.url", abSearch);
        } else {
            healthResults.put("rabbit.host", rabbitHost);
            healthResults.put("rabbit.port", rabbitPort);
        }
        return healthResults;

    }


    @Override
    public boolean isMultiTenanted() {
        return multiTenanted;
    }

    @Override
    public void setMultiTenanted(boolean multiTenanted) {
        this.multiTenanted = multiTenanted;
    }

//    @CacheEvict(value = {"companyFortress", "fortressName", "trackLog", "companyKeys", "companyTag", "companyTagManager",
//            "fortressUser", "callerKey", "metaKey", "headerId" }, allEntries = true)
    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public void resetCache() {
        logger.info("Reset the cache");
    }

    @Override
    public boolean isConceptsEnabled() {
        return conceptsEnabled;
    }

    /**
     * Should be disabled for testing purposes
     * @param conceptsEnabled if true, concepts will be created in a separate thread when entities are tracked
     */
    @Override
    public void setConceptsEnabled(boolean conceptsEnabled) {
        this.conceptsEnabled = conceptsEnabled;
    }

    @Override
    public void setDuplicateRegistration(boolean duplicateRegistration) {
        this.duplicateRegistration = duplicateRegistration;
    }

    @Override
    public boolean isDuplicateRegistration() {
        return duplicateRegistration;
    }

}
