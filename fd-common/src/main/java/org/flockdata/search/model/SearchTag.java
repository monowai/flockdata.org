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

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.dao.EntityTagDao;
import org.flockdata.track.model.EntityTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 7/02/15.
 */
public class SearchTag {
    String code;
    String name;
    Map<String, Object> properties;
    Map<String, Object> rlx;
    Map<String,Object> geo;


    SearchTag() {
    }

    SearchTag(EntityTag entityTag) {
        this();
        this.code = entityTag.getTag().getCode();
        this.name = entityTag.getTag().getName();

        if (this.name != null && this.name.equalsIgnoreCase(code))
            this.name = null; // Prefer code over name if they are the same

        // DAT-446 - ignore the code if it it is numeric, short and we have a textual name
        if (NumberUtils.isNumber(this.code) && this.code.length()<3 &&this.name!=null )
            this.code = null;

        if (entityTag.getProperties()!=null && !entityTag.getProperties().isEmpty())
            this.properties = entityTag.getTag().getProperties();

        if (entityTag.getGeoData() != null) {
            this.geo = entityTag.getGeoData().getProperties();
        }
        if ( entityTag.getProperties()!=null && !entityTag.getProperties().isEmpty()){
            this.rlx = new HashMap<>();
            // Know one will want to see these column values. Applicable for a graph viz.
            entityTag.getProperties().keySet().stream().filter
                    (key -> !key.equals("since") && !key.equals(EntityTagDao.FD_WHEN)).
                    forEach(key -> rlx.put(key, entityTag.getProperties().get(key)));
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getRlx() {
        return rlx;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String,Object> getGeo() {
        return geo;
    }

    @Override
    public String toString() {
        return "SearchTag{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
