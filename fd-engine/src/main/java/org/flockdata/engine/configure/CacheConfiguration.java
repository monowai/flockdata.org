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

package org.flockdata.engine.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author mholdsworth
 * @tag Configuration, Cache
 * FD-Cache configuration settings
 * @since 23/03/2016
 */
@Configuration
@EnableCaching
@Profile("fd-server")
public class CacheConfiguration {
  private Logger logger = LoggerFactory.getLogger("configuration");

  @CacheEvict(value = {"tag", "geoQuery", "geoData", "fortressUser", "sysUserApiKey", "company", "documentType", "labels", "entityByCode", "fortressSegment"
  }, allEntries = true)
  public void resetCache() {
    logger.debug("Cache Reset");
  }
}
