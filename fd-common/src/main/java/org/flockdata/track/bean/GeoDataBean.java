/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Geographic type data
 * User: mike
 * Date: 28/02/14
 * Time: 4:00 PM
 */
public class GeoDataBean {

    //private String description = null;
    private String code;
    private String name = null;
    private Map<String,String>points;


    public GeoDataBean() {
    }

//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    public Map<String, Object> getProperties() {
//        return properties;
//    }

    //Map<String, Object> properties = new HashMap<>();

    public void add(String type, String code, String name, Double lat, Double lon) {
        if ( code !=null)
            this.code = code;
        if ( name !=null )
            this.name = name;

        if (lat != null && lon != null) {
            if ( points == null )
                points = new HashMap<>();
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
