/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackDao;
import com.auditbucket.helper.VersionHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.search.model.PingResult;
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
public class EngineConfig {

    @Autowired
    TrackDao trackDAO;

    private String abSearch;

    private String rabbitHost;

    private String rabbitPort;

    private Logger logger = LoggerFactory.getLogger(EngineConfig.class);

    private Boolean multiTenanted = false;

    private com.auditbucket.kv.service.KvService.KV_STORE kvStore = null;

    @Qualifier("abMonitoringGateway")
    @Autowired
    AbMonitoringGateway abMonitoringGateway;

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

    @Value("${absearch.make:@null}")
    protected void setAbSearch(String absearchMake) {
        if ("@null".equals(absearchMake)) this.abSearch = null;
        else this.abSearch = absearchMake;
    }

    @Value("${abengine.multiTenanted:@null}")
    protected void setMultiTenanted(String multiTenanted) {
        this.multiTenanted = !"@null".equals(multiTenanted) && Boolean.parseBoolean(multiTenanted);
    }

    @Value("${abengine.kvStore}")
    public void setKvStore(String kvStore) {
        if ("@null".equals(kvStore) || kvStore.equalsIgnoreCase("redis"))
            this.kvStore = com.auditbucket.kv.service.KvService.KV_STORE.REDIS;
        else if (kvStore.equalsIgnoreCase("riak"))
            this.kvStore = com.auditbucket.kv.service.KvService.KV_STORE.RIAK;
        else {
            logger.error("Unable to resolve the abengine.kvstore property [" + kvStore + "]. Defaulting to REDIS");
        }

    }

    public com.auditbucket.kv.service.KvService.KV_STORE getKvStore() {
        return kvStore;
    }

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
    public Map<String, String> getHealth() {
        if ( System.getProperty("neo4j")!=null )
            logger.warn("[-Dneo4j] is now an unsupported property. Ignoring this setting");
        String version = VersionHelper.getABVersion();
        Map<String, String> healthResults = new HashMap<>();
        healthResults.put("ab-engine.version", version);
        healthResults.put("ab-engine", trackDAO.ping());
        String config = System.getProperty("ab.config");
        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);
        String integration = System.getProperty("ab.integration");
        healthResults.put("ab.integration", integration);
        healthResults.put("abengine.kvStore", String.valueOf(kvStore));
        String esPingResult ;
        try {
            PingResult esPing = abMonitoringGateway.ping();
            esPingResult = (esPing == null || !esPing.getMessage().equals("Pong!")?"Problem":"Ok");
        } catch (Exception ce){
            esPingResult="!Unreachable! "+ce.getCause().getMessage();
        }
        healthResults.put("ab-search", esPingResult);

        //healthResults.put("ab.multiTenanted", multiTenanted.toString());
        if ("http".equalsIgnoreCase(integration)) {
            healthResults.put("absearch.make", abSearch);
        } else {
            healthResults.put("rabbitmq.host", rabbitHost);
            healthResults.put("rabbitmq.port", rabbitPort);
        }
        return healthResults;

    }


    public boolean isMultiTenanted() {
        return multiTenanted;
    }

    public void setMultiTenanted(boolean multiTenanted) {
        this.multiTenanted = multiTenanted;
    }

//    @CacheEvict(value = {"companyFortress", "fortressName", "trackLog", "companyKeys", "companyTag", "companyTagManager",
//            "fortressUser", "callerKey", "metaKey", "headerId" }, allEntries = true)
    @Secured({"ROLE_AB_ADMIN"})
    public void resetCache() {
        logger.info("Reset the cache");
    }

    public boolean isConceptsEnabled() {
        return conceptsEnabled;
    }

    /**
     * Should be disabled for testing purposes
     * @param conceptsEnabled if true, concepts will be created in a separate thread when headers are tracked
     */
    public void setConceptsEnabled(boolean conceptsEnabled) {
        this.conceptsEnabled = conceptsEnabled;
    }

    public void setDuplicateRegistration(boolean duplicateRegistration) {
        this.duplicateRegistration = duplicateRegistration;
    }

    public boolean isDuplicateRegistration() {
        return duplicateRegistration;
    }

}
