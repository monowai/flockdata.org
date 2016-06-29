/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

    String apiBase();

    String getTagSuffix(Company company);

    Map<String, String> getHealth();

    Map<String, String> getHealthAuth();

    boolean isMultiTenanted();

    void setMultiTenanted(boolean multiTenanted);

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

    void setStoreEnabled(boolean enabled);

    Boolean isSearchRequiredToConfirm() ;

    boolean isTiming();

    String getFdSearch();

    void setFdSearch (String url);


    String getFdStore();

    PlatformConfig setSearchRequiredToConfirm(boolean b);
}
