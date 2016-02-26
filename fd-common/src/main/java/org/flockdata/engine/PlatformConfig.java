/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.engine;

import org.flockdata.model.Company;
import org.flockdata.store.Store;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * User: mike
 * Date: 14/11/14
 * Time: 1:22 PM
 */
public interface PlatformConfig {

    public String apiBase();

    String getTagSuffix(Company company);

    Map<String, String> getHealth();

    Map<String, String> getHealthAuth();

    boolean isMultiTenanted();

    void setMultiTenanted(boolean multiTenanted);

    void resetCache();

    @Value("${org.fd.engine.system.constraints:@null}")
    void setSystemConstraints(String constraints);

    Store setStore(Store kvStore);

    Store store();

    boolean isConceptsEnabled();

    void setConceptsEnabled(String conceptsEnabled);

    void setDuplicateRegistration(boolean duplicateRegistration);

    boolean isDuplicateRegistration();

    void setTestMode(boolean testMode);

    boolean isTestMode();

    String authPing();

    boolean createSystemConstraints();

    Boolean storeEnabled();

    Boolean isSearchEnabled();

    void setSearchEnabled(String enabled);

    void setStoreEnabled(String enabled);

    Boolean isSearchRequiredToConfirm() ;

    boolean isTiming();

    String getFdSearch();

    void setFdSearch (String url);


    String getFdStore();
}
