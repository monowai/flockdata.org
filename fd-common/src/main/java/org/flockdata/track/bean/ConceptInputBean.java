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

package org.flockdata.track.bean;

import java.util.Collection;
import java.util.Set;

/**
 * User: mike
 * Date: 19/06/14
 * Time: 4:46 PM
 */
public class ConceptInputBean {
    private String name;

    Collection<String> relationships;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<String> getRelationships(){
        return relationships;
    }

    public void setRelationships(Set<String> relationships) {
        this.relationships = relationships;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConceptInputBean)) return false;

        ConceptInputBean that = (ConceptInputBean) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(relationships != null ? !relationships.equals(that.relationships) : that.relationships != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (relationships != null ? relationships.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConceptInputBean{" +
                "name='" + name + '\'' +
                '}';
    }
}
