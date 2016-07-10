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
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:52 PM
 */
public abstract class AbstractEntityTag implements Comparable<AbstractEntityTag>, EntityTag {

    protected DynamicProperties properties = new DynamicPropertiesContainer();

    @Transient
    protected GeoDataBeans geo;

    Boolean geoRelationship = false;

    @Override
    public Boolean isGeoRelationship() {
        if (geoRelationship == null)
            return false;
        return geoRelationship;
    }


    @Override
    public Integer getWeight() {
        return (Integer) getProperty("weight");
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoDataBeans getGeoData() {
        return geo;
    }

    private Object getProperty(String key) {
        if (properties == null)
            return null;
        return properties.getProperty(key);
    }

    @Override
    public AbstractEntityTag setGeoData(GeoDataBeans geoBeans) {
        this.geo = geoBeans;
        return this;
    }


    @Override
    public Map<String, Object> getProperties() {
        return properties.asMap();
    }

    public int compareTo(AbstractEntityTag o) {
        int val = getRelationship().compareTo(o.getRelationship());
        if (val == 0)
            return getTag().getCode().compareTo(o.getTag().getCode());
        return val;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEntityTag)) return false;

        AbstractEntityTag that = (AbstractEntityTag) o;

        if (getEntity() != null ? !getEntity().equals(that.getEntity()) : that.getEntity() != null) return false;
        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getRelationship() != null ? !getRelationship().equals(that.getRelationship()) : that.getRelationship()!= null) return false;
        return !(getTag() != null ? !getTag().getId().equals(that.getTag().getId()) : that.getTag() != null);

    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getEntity() != null ? getEntity().getId().hashCode() : 0);
        result = 31 * result + (getTag() != null ? getTag().getId().hashCode() : 0);
        result = 31 * result + (getRelationship() != null ? getRelationship().hashCode() : 0);
        return result;
    }

    @Override
    public void setGeo(Boolean geo) {
        this.geoRelationship = geo;
    }
}
