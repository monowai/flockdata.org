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

import org.flockdata.transform.ColumnDefinition;

import java.util.Collection;

/**
 * @author mholdsworth
 * @since 19/07/2016
 * @tag Relationship, EntityTag
 */
public class EntityTagRelationshipDefinition {
    private Boolean geo ;
    private boolean reverse; // default is Entity->Tag
    private String relationshipName;
    private Collection<ColumnDefinition> properties;


    public Boolean getGeo() {
        return geo;
    }

    public boolean isReverse() {
        return reverse;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public Collection<ColumnDefinition> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "EntityTagRelationshipDefinition{" +
                "relationshipName='" + relationshipName + '\'' +
                '}';
    }
}
