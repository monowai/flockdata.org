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

package org.flockdata.configure;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.integration.SearchPingRequest;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.helper.VersionHelper;
import org.flockdata.kv.FdKvConfig;
import org.flockdata.kv.service.KvService;
import org.flockdata.model.Company;
import org.flockdata.registration.service.SystemUserService;
import org.flockdata.track.service.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service(value = "engineConfig")
@Transactional
@Configuration
public class EngineConfig implements PlatformConfig {

    private Logger logger = LoggerFactory.getLogger(EngineConfig.class);

    @Autowired
    FdKvConfig kvConfig;

    @Autowired
    SchemaService schemaService;

    @Autowired
    SearchPingRequest.MonitoringGateway fdMonitoringGateway;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    SystemUserService systemUserService;

    private Boolean multiTenanted = false;
    private boolean conceptsEnabled = true;
    private boolean systemConstraints = true;
    private boolean duplicateRegistration;
    private boolean testMode;
    private boolean searchEnabled = true;
    private String fdSearch;


    @Value("${fd-search.url:http://localhost:8081}")
    public void setFdSearch( String url) {
        fdSearch = url;
    }


    @Value("${fdengine.multiTenanted:@null}")
    protected void setMultiTenanted(String multiTenanted) {
        this.multiTenanted = !"@null".equals(multiTenanted) || Boolean.parseBoolean(multiTenanted);
    }

    private boolean timing = false;

    @Value("${fd-store.enabled}")
    public void setStoreEnabled(String storeEnabled) {
        kvConfig.setStoreEnabled(storeEnabled);
    }

    /**
     * Default property for a fortress if not explicitly set.
     * When true (default) KV versions of information will be tracked
     *
     * @param timing defaults to true
     */
    @Value("${fd-engine.timings}")
    public void setTiming(String timing) {
        this.timing = "@null".equals(timing) || Boolean.parseBoolean(timing);
    }


    // By default, we only require a reply if this is being indexed for the first time
    @Value("${fd-engine.search.update:false}")
    Boolean requireSearchToConfirm = false;

    public Boolean isSearchRequiredToConfirm() {
        return requireSearchToConfirm;
    }

    @Override
    public boolean isTiming() {
        return timing;
    }

    public Boolean isStoreEnabled() {
        return kvConfig.getStoreEnabled();
    }

    public Boolean isSearchEnabled() {
        return searchEnabled;
    }

    @Value("${fd-search.enabled:@null}")
    public void setSearchEnabled(String searchEnabled) {
        this.searchEnabled = "@null".equals(searchEnabled) || Boolean.parseBoolean(searchEnabled);
    }

    /**
     * Should be disabled for testing purposes
     *
     * @param conceptsEnabled if true, concepts will be created in a separate thread when entities are tracked
     */
    @Override
    @Value("${fd-engine.concepts.enabled:@null}")
    public void setConceptsEnabled(String conceptsEnabled) {
        this.conceptsEnabled = "@null".equals(conceptsEnabled) || Boolean.parseBoolean(conceptsEnabled);
    }

    @Override
    @Value("${fd-engine.system.constraints:@null}")
    public void setSystemConstraints(String constraints) {
        this.systemConstraints = !"@null".equals(constraints) && Boolean.parseBoolean(constraints);

    }

    public KvService.KV_STORE setKvStore(KvService.KV_STORE kvStore) {
        return kvConfig.setKvStore(kvStore);
    }

    @Override
    public KvService.KV_STORE getKvStore() {
        return kvConfig.getKvStore();
    }

    @Override
    public String getTagSuffix(Company company) {
        if (company == null)
            return "";
        return (isMultiTenanted() ? company.getCode() : "");
    }

    @Secured({SecurityHelper.ADMIN, SecurityHelper.USER})
    public Map<String, String> getHealthAuth() {
        return getHealth();
    }

    /**
     * Only users with a pre-validated api-key should be calling this
     *
     * @return system configuration details
     */
    @Override
    public Map<String, String> getHealth() {

        String version = VersionHelper.getFdVersion();
        Map<String, String> healthResults = new HashMap<>();

        healthResults.put("flockdata.version", version);
        healthResults.put("fd-engine", "Neo4j is OK");

        healthResults.put("fd-store.enabled", kvConfig.getStoreEnabled().toString());

        String config = System.getProperty("fd.config");

        if (config == null || config.equals(""))
            config = "system-default";
        healthResults.put("config-file", config);

        healthResults.put("fd-store.engine", kvConfig.getKvStore().toString());
        healthResults.put("fd-store.enabled", kvConfig.getStoreEnabled().toString());
        String esPingResult;
        try {
            String esPing = fdMonitoringGateway.ping();
            esPingResult = (esPing == null || !esPing.equals("pong") ? "Problem" : "Ok");
        } catch (Exception ce) {
            esPingResult = "!Unreachable! ";
            if (ce.getCause() != null)
                esPingResult = esPingResult + ce.getCause().getMessage();
        }
        healthResults.put("fd-search", esPingResult);
        healthResults.put("fd-search.url", fdSearch);
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

    @CacheEvict(value = {"fortress", "company", "companyTag", "geoData", "fortressDocType", "fortressUser",
            "companyEvent", "labels"}, allEntries = true)
    @Override
    @Secured({SecurityHelper.ADMIN})
    public void resetCache() {
        logger.debug("Cache Reset");
    }

    @Override
    public boolean isConceptsEnabled() {
        return conceptsEnabled;
    }


    @Override
    public void setDuplicateRegistration(boolean duplicateRegistration) {
        this.duplicateRegistration = duplicateRegistration;
    }

    @Override
    public boolean isDuplicateRegistration() {
        return duplicateRegistration;
    }

    public boolean isTestMode() {
        return testMode;
    }

    @Override
    @Secured({SecurityHelper.ADMIN, SecurityHelper.USER})
    public String authPing() {
        return "pong";
    }

    @Override
    public boolean createSystemConstraints() {
        return systemConstraints;
    }


    @Override
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    @PostConstruct
    public void ensureSystemIndexes() {
        if (createSystemConstraints())
            schemaService.ensureSystemIndexes(null);

    }

    public String getFdSearch() {
        return fdSearch;
    }

}
