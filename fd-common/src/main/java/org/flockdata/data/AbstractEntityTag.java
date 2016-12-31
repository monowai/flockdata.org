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
 * @since 29/06/2013
 */
public abstract class AbstractEntityTag implements Comparable<AbstractEntityTag>, EntityTag {

    protected GeoDataBeans geo;

    protected boolean geoRelationship = false;

    @Override
    public boolean isGeoRelationship() {
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

    @Override
    public AbstractEntityTag setGeoData(GeoDataBeans geoBeans) {
        this.geo = geoBeans;
        return this;
    }

    public abstract Object getProperty(String key) ;

    @Override
    public abstract Map<String, Object> getProperties();

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
    public void setGeo(boolean geo) {
        this.geoRelationship = geo;
    }
}
