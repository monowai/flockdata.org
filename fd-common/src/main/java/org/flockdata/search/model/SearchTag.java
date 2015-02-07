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

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.GeoData;

import java.util.Map;

/**
 * Created by mike on 7/02/15.
 */
public class SearchTag {
    String code;
    String name;
    Map<String, Object> properties;
    String iso;
    String country;
    String city;
    GeoData geo;


    SearchTag() {
    }

    SearchTag(EntityTag tag) {
        this();
        this.code = tag.getTag().getCode();
        this.name = tag.getTag().getName();

        if (this.name != null && this.name.equalsIgnoreCase(code))
            this.name = null; // Prefer code over name

        if (tag.getProperties()!=null && !tag.getProperties().isEmpty())
            this.properties = tag.getTag().getProperties();

        if (tag.getGeoData() != null) {
            this.geo = tag.getGeoData();
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode() {
        return code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return name;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoData getGeo() {
        return geo;
    }
}
