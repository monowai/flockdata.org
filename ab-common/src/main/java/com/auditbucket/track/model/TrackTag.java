/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.track.model;

import com.auditbucket.registration.model.Tag;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:52 PM
 */
public interface TrackTag {

    public Long getId();

    public Tag getTag();

    //ToDo - should this be a taggable interface?
    public Long getMetaId();

    /**
     * @return relationship name
     */
    public String getTagType();

    /**
     * @return property map of custom properties associated with the tag
     */
    public Map<String, Object> getProperties();

    /**
     * useful to understand a relative weighting between the tag and the track for this
     * class of tagType.
     *
     * @return weight of the attribute in the relationship
     */
    Integer getWeight();

    public GeoData getGeoData();

}
