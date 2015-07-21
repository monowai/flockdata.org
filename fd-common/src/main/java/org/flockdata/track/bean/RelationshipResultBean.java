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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.model.Relationship;

/**
 *
 * Encapsulates a relationship instance
 *
 * Created by mike on 20/05/15.
 */
public class RelationshipResultBean  {

    RelationshipResultBean () {}

    private String name ;
    private Long id;

    public RelationshipResultBean (Relationship relationship){
        this();
        this.name = relationship.getName();
        this.id = relationship.getId();
    }
    public String getName() {
        return name;
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "RelationshipResultBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationshipResultBean)) return false;

        RelationshipResultBean that = (RelationshipResultBean) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }


}
