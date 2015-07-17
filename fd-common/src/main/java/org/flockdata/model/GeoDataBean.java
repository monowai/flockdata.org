/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Geographic type data
 * User: mike
 * Date: 28/02/14
 * Time: 4:00 PM
 */
public class GeoDataBean {

    public GeoDataBean() {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    public Map<String, String> getPoints() {
//        return points;
//    }
//
//    Map<String, String> points = new HashMap<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    Map<String, Object> properties = new HashMap<>();

    public void add(String prefix, String code, String name, Double lat, Double lon) {
        properties.put(prefix+".code", code);
        if ( name !=null )
            properties.put(prefix+".name", name);
        // ToDo: Map user defined properties?
        setLatLong(prefix, lat, lon);
    }

    private void setLatLong(String label, Double lat, Double lon) {
        if (lat != null && lon != null)
            properties.put("points."+label, lat.toString() + "," + lon.toString());
    }

}
