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

package org.flockdata.integration;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Generates unique keys based on various algorithms.
 *
 * @author mholdsworth
 * @tag Entity
 * @since 11/09/2013
 */
@Service
public class KeyGenService {
  private final Base64 base64;

  @Autowired
  public KeyGenService(Base64 base64) {
    this.base64 = base64;
  }

  public String getUniqueKey() {
    return getUniqueKey(METHOD.BASE64);
  }

  String getUniqueKey(METHOD method) {
    // Snowflake?
    if (method.equals(METHOD.SNOWFLAKE)) {
      return getSnowFlake();
    } else if (method.equals(METHOD.BASE64)) {
      return base64.format(UUID.randomUUID());
    } else {
      return getUUID();
    }
  }

  //ToDo: Implement!
  private String getSnowFlake() {
    return getUUID();
  }

  private String getUUID() {
    return UUID.randomUUID().toString();
  }

  private enum METHOD {
    UUID, SNOWFLAKE, BASE64
  }


}
