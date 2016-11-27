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

package org.flockdata.engine;

import org.flockdata.model.Company;
import org.flockdata.store.Store;

import java.util.Map;

/**
 * @author mholdsworth
 * @since 14/11/2014
 */
public interface PlatformConfig {

    String getTagSuffix(Company company);

    Map<String, String> getHealth();

    Map<String, String> getHealthAuth();

    boolean isMultiTenanted();

    void setMultiTenanted(boolean multiTenanted);

    Store setStore(Store kvStore);

    Store store();

    boolean isConceptsEnabled();

    Boolean setConceptsEnabled(boolean conceptsEnabled);

    boolean isTestMode();

    void setTestMode(boolean testMode);

    String authPing();

    Boolean storeEnabled();

    Boolean isSearchEnabled();

    void setSearchEnabled(String enabled);

    void setStoreEnabled(boolean enabled);

    Boolean isSearchRequiredToConfirm() ;

    String getFdSearch();

    String getFdStore();

    PlatformConfig setSearchRequiredToConfirm(boolean b);
}
