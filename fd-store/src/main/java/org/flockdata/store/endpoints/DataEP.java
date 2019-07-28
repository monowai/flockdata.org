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

package org.flockdata.store.endpoints;

import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Endpoint, Store, Query
 * @since 18/02/2016
 */
@RestController
@RequestMapping("${org.fd.store.system.api:api}/v1/data")
public class DataEP {
  private final StoreService storeService;

  @Autowired
  public DataEP(StoreService storeService) {
    this.storeService = storeService;
  }

  @RequestMapping(value = "/{repo}/{index}/{type}/{key}", produces = "application/json;charset=UTF-8", method = RequestMethod.GET)
  StoredContent getData(@PathVariable("repo") String repo,
                        @PathVariable("index") String index,
                        @PathVariable("type") String type,
                        @PathVariable("key") String key) {
    Store store = Store.valueOf(repo.toUpperCase());
    return storeService.doRead(store, index.toLowerCase(), type.toLowerCase(), key);
  }
}
