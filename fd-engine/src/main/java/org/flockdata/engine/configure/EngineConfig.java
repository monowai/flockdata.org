/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.configure;

import static org.flockdata.authentication.FdRoles.FD_ROLE_ADMIN;
import static org.flockdata.authentication.FdRoles.FD_ROLE_USER;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.flockdata.authentication.FdRoles;
import org.flockdata.data.Company;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.integration.search.SearchAdminRequests;
import org.flockdata.engine.integration.search.SearchAdminRequests.AdminGateway;
import org.flockdata.engine.integration.store.StoreAdminRequests;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.VersionHelper;
import org.flockdata.store.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @tag Engine
 * @since 29/08/2013
 */
@Transactional
@Configuration
public class EngineConfig implements PlatformConfig {

  @Value("${spring.cloud.config.discovery.enabled:false}")
  Boolean discoveryEnabled;
  // By default, storage engine services are not disabled
  // and current state content is retrieved from ElasticSearch
  // By default, we only require a reply if this is being indexed for the first time
  @Value("${org.fd.engine.search.update:true}")
  Boolean requireSearchToConfirm = false;
  @Value("${org.fd.engine.system.storage:RIAK}")
  private String storeEngine; // The default store to write to IF a fortress allows it
  @Value("${org.fd.engine.fortress.store:false}")
  private Boolean storeEnabled;
  @Value("${org.fd.engine.system.api:api}")
  private String apiBase;
  private AdminGateway pingSearchGateway;
  @Autowired(required = false)
  private SearchAdminRequests searchAdminRequests;
  @Autowired
  private StoreAdminRequests.StorePingGateway storePingGateway;
  @Autowired(required = false)
  private AmqpRabbitConfig rabbitConfig;
  @Value("${org.fd.engine.system.multiTenanted:false}")
  private Boolean multiTenanted = false;
  private boolean conceptsEnabled = true;
  @Value("${org.fd.engine.system.constraints:true}")
  private boolean systemConstraints = true;
  private boolean testMode;
  @Value("${org.fd.engine.fortress.search:true}")
  private boolean searchEnabled = true;
  @Value("${org.fd.search.api:http://localhost:8081}")
  private String fdSearch;
  @Value("${org.fd.store.api:http://localhost:8082}")
  private String fdStoreUrl;
  @Value("${eureka.client.serviceUrl.defaultZone}")
  private String eurekaUrl;
  @Value("${org.fd.engine.system.timings:false}")
  private boolean timing = false;
  private VersionHelper versionHelper;

  @Autowired(required = false)
  public void setAdminGateway(AdminGateway pingSearchGateway) {
    this.pingSearchGateway = pingSearchGateway;
  }

  @Autowired(required = false)
  public void sertVersionHelper(VersionHelper versionHelper) {
    this.versionHelper = versionHelper;
  }

  @Override
  public String getFdStore() {

    return fdStoreUrl + "/api";
  }

  @Override
  public PlatformConfig setSearchRequiredToConfirm(boolean b) {
    this.requireSearchToConfirm = b;
    return this;
  }

  public void setStoreEnabled(boolean storeEnabled) {
    this.storeEnabled = storeEnabled;
  }

  public void setSearchEnabled(String searchEnabled) {
    this.searchEnabled = Boolean.parseBoolean(searchEnabled);
  }

  public Boolean isSearchRequiredToConfirm() {
    return requireSearchToConfirm;
  }

  /**
   * @return is fd-storeEngine part of the data processing pipeline?
   */
  public Boolean storeEnabled() {
    return this.storeEnabled;
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
  @Value("${org.fd.engine.system.concepts:true}")
  public Boolean setConceptsEnabled(boolean conceptsEnabled) {
    Boolean previous = conceptsEnabled;
    this.conceptsEnabled = conceptsEnabled;
    return previous;
  }

  public Store setStore(Store store) {
    Store previous = Store.valueOf(storeEngine);
    this.storeEngine = store.name();
    return previous;
  }

  @Override
  public Store store() {
//        if ( storeEnabled)
    return Store.valueOf(storeEngine.toUpperCase());
//        else
//            return Store.NONE;
  }

  @Override
  public String getTagSuffix(Company company) {
    if (company == null) {
      return "";
    }
    return (isMultiTenanted() ? company.getCode() : "");
  }

  @Secured( {FD_ROLE_ADMIN, FD_ROLE_USER})
  public Map<String, Object> getHealthAuth() {
    return getHealth();
  }

  /**
   * Only users with a pre-validated api-key should be calling this
   *
   * @return system configuration details
   */
  @Override
  public Map<String, Object> getHealth() {
    String version = "";
    if (versionHelper != null) {
      version = versionHelper.getFdVersion();
    }
    Map<String, Object> healthResults = new TreeMap<>();

    healthResults.put("fd.version", version);

    String esPingResult = "ok";
    Map<String, Object> esHealth = null;
    try {
      if (searchAdminRequests != null) {
        esHealth = pingSearchGateway.health();
      }
      //esPingResult = (esHealth == null || !esHealth.equals("pong") ? esHealth : "Ok");
    } catch (Exception ce) {
      esPingResult = "!Unreachable ";
      if (ce.getCause() != null) {
        esPingResult = esPingResult + ce.getCause().getMessage();
      }
    }
    Map<String, Object> searchHealth = new HashMap<>();
    searchHealth.put("org.fd.search.api", fdSearch);
    if (searchAdminRequests == null) {
      searchHealth.put("status", "Disabled");
    } else {
      searchHealth.put("status", esPingResult);
    }

    if (esHealth != null) {
      searchHealth.put("health", esHealth);
    }

    healthResults.put("fd-search", searchHealth);
    String kvPingResult;
    try {
      String esPing = storePingGateway.ping(storeEngine);

      kvPingResult = (esPing == null ? "Problem" : esPing);
    } catch (Exception ce) {
      kvPingResult = "!Unreachable ";
      if (ce.getCause() != null) {
        kvPingResult = kvPingResult + ce.getCause().getMessage();
      }
    }
    Map<String, Object> storeHealth = new HashMap<>();
    healthResults.put("fd-store", storeHealth);
    storeHealth.put("org.fd.store.api", fdStoreUrl);
    storeHealth.put("status", kvPingResult);
    storeHealth.put("fd.store.engine", storeEngine);
    storeHealth.put("fd.store.enabled", storeEnabled().toString());
    if (rabbitConfig != null) {
      healthResults.put("rabbit.host", rabbitConfig.getHost());
      healthResults.put("rabbit.port", rabbitConfig.getPort().toString());
      healthResults.put("rabbit.user", rabbitConfig.getUser());
    }
    healthResults.put("eureka.client.serviceUrl.defaultZone", eurekaUrl);
    healthResults.put("spring.cloud.config.discovery.enabled", discoveryEnabled.toString());

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

  @Override
  public boolean isConceptsEnabled() {
    return conceptsEnabled;
  }

  public boolean isTestMode() {
    return testMode;
  }

  @Override
  public void setTestMode(boolean testMode) {
    this.testMode = testMode;
  }

  @Override
  @PreAuthorize(FdRoles.EXP_EITHER)
  public String authPing() {
    return "pong";
  }

  public String getFdSearch() {
    return fdSearch + "/api";
  }

}
