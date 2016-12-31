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

package org.flockdata.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.bean.GeoDataBeans;

import java.util.Map;

/**
 * @author mholdsworth
 * @since 13/07/2016
 */
public interface EntityTag {
    // Key value indicating when, in the fortress, this relationship was established
    String SINCE = "since";
    // Property that refers to when this relationship was introduced to FD
    String FD_WHEN = "fdWhen";

    boolean isGeoRelationship();

    Long getId() ;

    Entity getEntity() ;

    Tag getTag() ;

    String getRelationship() ;

    void setRelationship(String name);

    void setGeo(boolean geo);

    Integer getWeight();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    GeoDataBeans getGeoData();

    EntityTag setGeoData(GeoDataBeans geoBeans);

    Boolean isReversed();

    Map<String, Object> getProperties();

    enum TAG_STRUCTURE {TAXONOMY, DEFAULT}
}
