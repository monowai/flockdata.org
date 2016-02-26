/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.engine.configure;

import org.flockdata.authentication.FdRoles;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.integration.search.SearchGateway;
import org.flockdata.engine.integration.store.StorageGateway;
import org.flockdata.helper.VersionHelper;
import org.flockdata.model.Company;
import org.flockdata.store.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.flockdata.authentication.FdRoles.FD_ROLE_ADMIN;
import static org.flockdata.authentication.FdRoles.FD_ROLE_USER;

/**
 * User: Mike Holdsworth
 * Since: 29/08/13
 */
@Service(value = "engineConfig")
@Transactional
@Configuration
public class EngineConfig implements PlatformConfig {

    private Logger logger = LoggerFactory.getLogger(EngineConfig.class);

    @Value("${org.fd.engine.system.storage:RIAK}")
    private String storeEngine; // The default store to write to IF a fortress allows it
                                // By default, storage engine services are not disabled
                                // and current state content is retrieved from ElasticSearch

    @Value("${org.fd.engine.fortress.store:disabled}")
    private String storeEnabled;

    @Value("${org.fd.engine.system.api:api}")
    private String apiBase ;

    public String apiBase(){
        return apiBase;
    }

    @Autowired
    SearchGateway searchGateway;

    @Autowired
    StorageGateway storageGateway;


    @Value("${org.fd.engine.system.multiTenanted:false}")
    private Boolean multiTenanted = false;
    private boolean conceptsEnabled = true;
    private boolean systemConstraints = true;
    private boolean duplicateRegistration;
    private boolean testMode;
    private boolean searchEnabled = true;
    private String fdSearch;

    @Value("${org.fd.store.api:http://localhost:8082/api}")
    private String fdStoreUrl;

    @Value("${org.fd.search.api:http://localhost:8081/api}")
    public void setFdSearch( String url) {
        fdSearch = url;
    }


    @Override
    public String getFdStore() {

        return fdStoreUrl;
    }



    private boolean timing = false;

    @Value("${org.fd.store.system.enabled}")
    public void setStoreEnabled(String storeEnabled) {
        this.storeEnabled = storeEnabled;
    }

    @Value("${org.fd.engine.fortress.search:enabled}")
    public void setSearchEnabled(String searchEnabled) {
        this.searchEnabled =searchEnabled.equalsIgnoreCase("enabled");
    }


    /**
     * Default property for a fortress if not explicitly set.
     * When true (default) KV versions of information will be tracked
     *
     * @param timing defaults to true
     */
    @Value("${org.fd.engine.system.timings:false}")
    public void setTiming(String timing) {
        this.timing = "@null".equals(timing) || Boolean.parseBoolean(timing);
    }


    // By default, we only require a reply if this is being indexed for the first time
    @Value("${org.fd.engine.search.update:false}")
    Boolean requireSearchToConfirm = false;

    public Boolean isSearchRequiredToConfirm() {
        return requireSearchToConfirm;
    }

    @Override
    public boolean isTiming() {
        return timing;
    }

    /**
     *
     * @return is fd-storeEngine part of the data processing pipeline?
     */
    public Boolean storeEnabled() {
        return Boolean.parseBoolean(this.storeEnabled);
    }

    public Boolean isSearchEnabled() {
        return searchEnabled;
    }

    /**
     * Should be disabled for testing purposes
     *
     * @param conceptsEnabled if true, concepts will be created in a separate thread when entities are tracked
     */
    @Override
    @Value("${org.fd.engine.system.concepts:@null}")
    public void setConceptsEnabled(String conceptsEnabled) {
        this.conceptsEnabled = "@null".equals(conceptsEnabled) || Boolean.parseBoolean(conceptsEnabled);
    }

    @Override
    @Value("${org.fd.engine.system.constraints:@null}")
    public void setSystemConstraints(String constraints) {
        this.systemConstraints = !"@null".equals(constraints) && Boolean.parseBoolean(constraints);

    }

    public Store setStore(Store store) {
        Store previous = Store.valueOf(storeEngine);
        this.storeEngine = store.name();
        return previous;
    }

    @Override
    public Store store() {
        return Store.valueOf(storeEngine);
    }

    @Override
    public String getTagSuffix(Company company) {
        if (company == null)
            return "";
        return (isMultiTenanted() ? company.getCode() : "");
    }

    @Secured({FD_ROLE_ADMIN, FD_ROLE_USER})
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

        String esPingResult;
        try {
            String esPing = searchGateway.ping();
            esPingResult = (esPing == null || !esPing.equals("pong") ? "Problem" : "Ok");
        } catch (Exception ce) {
            esPingResult = "!Unreachable! ";
            if (ce.getCause() != null)
                esPingResult = esPingResult + ce.getCause().getMessage();
        }
        healthResults.put("fd-search",  esPingResult + " on " +fdSearch);
        healthResults.put("fd.search.url", fdSearch);

        try {
            String esPing = storageGateway.ping();
            esPingResult = (esPing == null || !esPing.equals("pong") ? "Problem" : "Ok");
        } catch (Exception ce) {
            esPingResult = "!Unreachable! ";
            if (ce.getCause() != null)
                esPingResult = esPingResult + ce.getCause().getMessage();
        }
        healthResults.put("fd-store", esPingResult + " on " +getFdStore());
        healthResults.put("fd.store.engine", storeEngine);
        healthResults.put("fd.store.enabled", storeEnabled().toString());

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
    @PreAuthorize(FdRoles.EXP_ADMIN)
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
    @PreAuthorize(FdRoles.EXP_EITHER)
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

    public String getFdSearch() {
        return fdSearch;
    }

}
