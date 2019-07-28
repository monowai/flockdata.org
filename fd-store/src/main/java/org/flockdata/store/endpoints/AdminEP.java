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

import java.util.Map;
import org.flockdata.store.Store;
import org.flockdata.store.service.StoreManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Store, Endpoint, Administration
 * @since 19/02/2016
 */
@RequestMapping("${org.fd.store.system.api:api}/v1/admin")
@RestController

public class AdminEP {
  private final StoreManager storeManager;

  @Autowired
  public AdminEP(StoreManager storeManager) {
    this.storeManager = storeManager;
  }

  @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = "text/plain")
  String ping() throws Exception {
    // curl -X GET http://localhost:8082/api/v1/admin/ping
    return "pong";
  }

  @RequestMapping(value = "/ping/{service}", method = RequestMethod.GET, produces = "text/plain")
  String ping(@PathVariable("service") String service) throws Exception {
    try {
      // Pings the underlying storage service
      Store store = Store.valueOf(service.toUpperCase());
      return storeManager.ping(store);
    } catch (IllegalArgumentException e) {
      return "Unknown store service " + service.toUpperCase();
    }
  }


  @RequestMapping(value = "/health", method = RequestMethod.GET)
  Map<String, String> health() throws Exception {
    // curl -X GET http://localhost:8082/api/v1/admin/ping
    return storeManager.health();
  }

}
