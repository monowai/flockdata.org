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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.bean.GeoDataBeans;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:52 PM
 */
public abstract class EntityTag implements  Comparable<EntityTag>{
    // Key value indicating when, in the fortress, this relationship was established
    public static final String SINCE = "since";

    protected DynamicProperties properties = new DynamicPropertiesContainer();

    @Transient
    protected GeoDataBeans geo;

    public abstract Long getId() ;

    public abstract Entity getEntity() ;

    public abstract Tag getTag() ;

    public abstract String getRelationship() ;

    public abstract Map<String, Object> getTagProperties() ;

    public Integer getWeight() {
        return (Integer)getProperty("weight");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoDataBeans  getGeoData() {
        return geo;
    }

    public abstract Boolean isReversed();

    private Object getProperty(String key){
        if (properties == null)
            return null;
        return properties.getProperty(key);
    }

    public EntityTag setGeoData(GeoDataBeans geoBeans ) {
        this.geo= geoBeans;
        return this;
    }


    public Map<String, Object> getProperties() {
        return properties.asMap();
    }

    public int compareTo(EntityTag o) {
        int val = getRelationship().compareTo(o.getRelationship());
        if ( val == 0 )
            return getTag().getCode().compareTo(o.getTag().getCode());
        return val;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityTag)) return false;

        EntityTag that = (EntityTag) o;

        if (getEntity() != null ? !getEntity().equals(that.getEntity()) : that.getEntity() != null) return false;
        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getRelationship() != null ? !getRelationship().equals(that.getRelationship()) : that.getRelationship()!= null) return false;
        return !(getTag() != null ? !getTag().getId().equals(that.getTag().getId()) : that.getTag() != null);

    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getEntity() != null ? getEntity().hashCode() : 0);
        result = 31 * result + (getTag() != null ? getTag().getId().hashCode() : 0);
        result = 31 * result + (getRelationship() != null ? getRelationship().hashCode() : 0);
        return result;
    }

    public abstract void setRelationship(String name);
}
