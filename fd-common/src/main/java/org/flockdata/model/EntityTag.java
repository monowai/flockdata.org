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
