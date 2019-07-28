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

package org.flockdata.data;


/**
 * Simple interface to allow us to use multiple implementations in a generic manner
 *
 * @author mholdsworth
 * @since 19/02/2016
 */
public interface Fortress {
  String DEFAULT = "Default";

  static String key(String fortressCode, String segmentCode) {
    if (segmentCode == null) {
      return null;
    }
    return fortressCode + "-" + segmentCode.toLowerCase();
  }

  static String code(String name) {
    if (name == null) {
      return null;
    }
    return name.toLowerCase().replaceAll("\\s+", "");
  }

  String getName();

  String getCode();

  Company getCompany();

  Boolean isSearchEnabled();

  Long getId();

  Boolean isStoreEnabled();

  Boolean isSystem();

  String getRootIndex();

  Segment getDefaultSegment();

  String getTimeZone();

  boolean isEnabled();
}
