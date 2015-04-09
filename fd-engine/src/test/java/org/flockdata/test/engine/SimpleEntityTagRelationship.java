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

package org.flockdata.test.engine;

import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.GeoData;

import java.util.HashMap;
import java.util.Map;

/**
 * For testing purposes only
 * Created by mike on 6/03/15.
 */
public class SimpleEntityTagRelationship implements EntityTag, Comparable<EntityTag> {
    private long id;
    private Tag tag ;
    private Entity entity;
    private String tagType;
    private Map<String,Object> properties = new HashMap<>();
    private GeoData geoData;


    public SimpleEntityTagRelationship(){}

    public SimpleEntityTagRelationship(Entity entity, Tag tag, String relationship, Map<String, Object> propMap){
        this();
        this.tag = tag;
        this.entity = entity;
        this.tagType = relationship;

        if ( propMap!= null )
            this.properties = propMap;
    }

    @Override
    public int compareTo(EntityTag o) {
        return 0;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public String getRelationship() {
        return tagType;
    }

    @Override
    public void setRelationship(String relationship) {
        tagType = relationship;
    }

    @Override
    public Map<String, Object> getTagProperties() {
        return tag.getProperties();
    }

    @Override
    public Integer getWeight() {
        return null;
    }

    @Override
    public GeoData getGeoData() {
        return geoData;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public Boolean isReversed() {
        return false;
    }

    public EntityTag setGeoData(GeoData geoData) {
        this.geoData = geoData;
        return this;
    }
}
