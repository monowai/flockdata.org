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

package org.flockdata.engine.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.GeoDataBeans;

import java.util.Map;

/**
 * @author mholdsworth
 * @since 19/02/2016
 * @tag Contract, EntityTag, Query
 */
public class EntityTagResult {


    private GeoDataBeans geoData;
    private Map<String,Object> props;
    private TagResultBean tag;
    private String relationship    ;

    EntityTagResult(){}

    public EntityTagResult(EntityTag logTag) {
        this();
        logTag.getTag();
        props = logTag.getProperties();
        this.tag = new FdTagResultBean(logTag.getTag());
        this.relationship = logTag.getRelationship();
        this.geoData = logTag.getGeoData();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoDataBeans getGeoData() {
        return geoData;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getProps() {
        return props;
    }

    public TagResultBean getTag() {
        return tag;
    }

    public String getRelationship() {
        return relationship;
    }
}
