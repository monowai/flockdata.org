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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;

/**
 * Geographic type data
 *
 * @author mholdsworth
 * @tag Contract, Geo
 * @since 28/02/2014
 */
public class GeoDataBean {

  //private String description = null;
  private String code;
  private String name = null;
  private Map<String, String> points;


  public GeoDataBean() {
  }

//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    public Map<String, Object> getProperties() {
//        return properties;
//    }

  //Map<String, Object> properties = new HashMap<>();

  public void add(String type, String code, String name, Double lat, Double lon) {
    if (code != null) {
      this.code = code;
    }
    if (name != null) {
      this.name = name;
    }

    if (lat != null && lon != null) {
      if (points == null) {
        points = new HashMap<>();
      }
      points.put(type, lat.toString() + "," + lon.toString());
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @JsonIgnore
  public Map<String, String> getPoints() {
    return points;
  }

  @Override
  public String toString() {
    return "GeoDataBean{" +
        "code='" + code + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
