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

import org.flockdata.registration.model.Tag;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:52 PM
 */
public interface EntityTag {
    // Key value indicating when, in the fortress, this relationship was established
    public static final String SINCE = "since";

    public Long getId();

    public Tag getTag();

    //ToDo - should this be a taggable interface?
    public Long getPrimaryKey();

    /**
     * @return relationship name
     */
    public String getRelationship();

    /**
     * @return property map of custom properties associated with the tag
     */
    public Map<String, Object> getTagProperties();

    /**
     * useful to understand a relative weighting between the tag and the track for this
     * class of tagType.
     *
     * @return weight of the attribute in the relationship
     */
    Long getWeight();

    public GeoData getGeoData();

    Map<String,Object> getProperties();

}
