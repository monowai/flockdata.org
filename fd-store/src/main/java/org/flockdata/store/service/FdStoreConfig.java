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

package org.flockdata.store.service;


import org.flockdata.store.Store;

import java.util.Map;

/**
 * User: mike
 * Date: 14/11/14
 * Time: 1:22 PM
 */
public interface FdStoreConfig {

    /**
     * Sets the KV store to use for ContentInputBeans
     *
     * @param kvStore kvStore to use
     * @return the previous value of the kvStore
     */
    Store setKvStore(Store kvStore);

    void setKvStore(String kvStore);

    Store kvStore();

    Map<String, String> health();

    String riakHosts();

    Boolean storeEnabled();

    void setStoreEnabled(String enabled);

    String fdSearchUrl();

}