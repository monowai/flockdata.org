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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.bean.GeoDataBeans;

import java.util.Map;

/**
 * Created by mike on 13/07/16.
 */
public interface EntityTag {
    // Key value indicating when, in the fortress, this relationship was established
    String SINCE = "since";

    Boolean isGeoRelationship();

    Long getId() ;

    Entity getEntity() ;

    Tag getTag() ;

    String getRelationship() ;

    boolean isGeo();

    Map<String, Object> getTagProperties() ;

    Integer getWeight();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    GeoDataBeans getGeoData();

    Boolean isReversed();

    AbstractEntityTag setGeoData(GeoDataBeans geoBeans);

    Map<String, Object> getProperties();

    void setRelationship(String name);

    void setGeo(Boolean geo);
}
