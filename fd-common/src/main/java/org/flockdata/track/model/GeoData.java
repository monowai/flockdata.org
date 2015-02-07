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

package org.flockdata.track.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Geographic type data
 * User: mike
 * Date: 28/02/14
 * Time: 4:00 PM
 */
public class GeoData {
    private String isoCode;
    private String country;

    private String state;
    private String city;
    private Map<String, Double>coord = new HashMap<>();

    public GeoData(String isoCode, String countryName, String city, String stateName, Double lat, Double lon) {
        this();
        if (city != null)
            setCity(city);

        if (countryName != null) {
            // ToDo: Needs to be a Country object
            setIsoCode(isoCode);
            setCountry(countryName);
            if (lon != null && lat != null)
                setLatLong(lat, lon);
        }
        if (stateName != null)
            setState(stateName);
    }

    GeoData() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    @JsonIgnore
    public Collection<Double> getGeoJson() {
        return coord.values();
    }

    public void setLatLong(Double lat, Double lon) {
        // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html
        if (lat!=null && lon !=null ) {
            coord.put("lat", lat);
            coord.put("lon", lon);
            geoPoint=  lat.toString() +","+lon.toString();
        }
    }

    String geoPoint = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getGeoPoint() {
        return geoPoint;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String,Double> getGeoMap() {
        return coord;
    }

    @JsonIgnore
    public boolean isValid() {
        if ( coord.isEmpty())
            return false;

        return coord.get("lat") !=null && coord.get("lon") !=null;
    }


}
