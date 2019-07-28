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

package org.flockdata.track.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains index data of geoDataBeans and points
 * <p>
 * Collection of geo data
 * Collection of points
 * A description for the collection of GeoDataBean indexed objects
 *
 * @author mholdsworth
 * @tag Geo, Contract
 * @since 4/08/2015
 */
public class GeoDataBeans {
  Map<String, GeoDataBean> geoBeans = null;
  Map<String, String> points = null;

  String description = null;

  public void add(String prefix, GeoDataBean geoBean) {
    if (geoBeans == null) {
      geoBeans = new HashMap<>();
    }


    geoBeans.put(prefix, geoBean);
    if (geoBean.getPoints() != null) {
      if (points == null) {
        points = new HashMap<>();
      }
      points.put(prefix, geoBean.getPoints().get(prefix));
    }

    if (geoBean.getName() != null) {
      if (description == null) {
        description = (prefix.equalsIgnoreCase("state") ? geoBean.getCode() : geoBean.getName());
      } else {
        description = description + ", " + (prefix.equalsIgnoreCase("state") ? geoBean.getCode() : geoBean.getName());
      }
    }

  }

  public Map<String, GeoDataBean> getGeoBeans() {
    return geoBeans;
  }

  // GeoJson map points
  public Map<String, String> getPoints() {
    return points;
  }

  public String getDescription() {
    if (geoBeans == null || geoBeans.size() == 1) {
      return null; // Value was not concatenated so can be found elsewhere
    }
    return description;
  }

}
