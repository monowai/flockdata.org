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

import java.util.HashMap;
import java.util.Map;

/**
 * Properties to create a relationship
 * <p>
 * @author mholdsworth
 * @since 9/07/2016
 * @tag RelationshipTag
 */
public class EntityTagRelationshipInput {
    private Boolean geo;
    private boolean reverse; // default is Entity->Tag
    private String relationshipName;
    private Map<String, Object> properties;

    EntityTagRelationshipInput() {
    }

    public EntityTagRelationshipInput(String relationshipName, Boolean geo) {
        this(relationshipName);
        this.geo = geo;
    }

    public EntityTagRelationshipInput(String relationshipName, Map<String, Object> properties) {
        this(relationshipName, (Boolean) null);
        this.properties = properties;
    }

    public EntityTagRelationshipInput(String relationshipName) {
        this();
        if (relationshipName != null) {
            relationshipName = relationshipName.trim();
            if (relationshipName.contains(" ")) {
                if (!relationshipName.startsWith("'"))
                    relationshipName = "'" + relationshipName + "'";
            }
        }

        this.relationshipName = relationshipName;
    }

    public Boolean isGeo() {
        return geo;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public void setGeo(Boolean geo) {
        this.geo = geo;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean getReverse() {
        return reverse;
    }

    public EntityTagRelationshipInput setReverse(Boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityTagRelationshipInput)) return false;

        EntityTagRelationshipInput that = (EntityTagRelationshipInput) o;

        return relationshipName != null ? relationshipName.equals(that.relationshipName) : that.relationshipName == null;

    }

    @Override
    public int hashCode() {
        return relationshipName != null ? relationshipName.hashCode() : 0;
    }

    public void addProperty(String key, Object value) {
        if (properties == null)
            properties = new HashMap<>();
        properties.put(key, value);
    }
}
