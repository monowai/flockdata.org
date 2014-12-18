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

import java.util.HashMap;

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
    Double lat, lon;

    public GeoData(String isoCode, String countryName, String city, String stateName, Double lat, Double lon) {
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public Double[] getGeoJson() {
        return new Double[]{lon,lat};
    }

    public void setLatLong(Double lat, Double lon) {
        // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html
        if (lat!=null && lon !=null ) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    public String getGeoPoint() {
        if ( lat == null ||lon ==null)
            return null;

        return lat.toString() +","+lon.toString();
    }

    public HashMap<String,Object> getGeoMap() {
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("lat", lat);
        hashMap.put("lon", lon);
        return hashMap;
    }

    public boolean isValid() {
        return lat!=null && lon!=null;
    }
}
