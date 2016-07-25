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

package org.flockdata.track.bean;

import org.flockdata.registration.TagResultBean;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: mike
 * Date: 19/06/14
 * Time: 4:46 PM
 */
public class ConceptInputBean {
    private String name;
    private boolean tag = true;

    Collection<String> relationships = new HashSet<>();
    private String description;

    private ConceptInputBean() {}

    public ConceptInputBean(String name) {
        this();
        this.name = name;
    }

    public ConceptInputBean(TagResultBean tagResultBean) {
        this(tagResultBean.getTag().getLabel());
        this.description = tagResultBean.getDescription();
    }

    public String getName() {
        return name;
    }

    public ConceptInputBean setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<String> getRelationships(){
        return relationships;
    }

    public ConceptInputBean setRelationships(Set<String> relationships) {
        for (String relationship : relationships) {
            if ( !this.relationships.contains(relationship))
                this.relationships.add(relationship);
        }
        return this;
    }

    /**
     * If not a Tag then it is an Entity
     *
     * @return
     */
    public boolean isTag() {
        return tag;
    }

    public ConceptInputBean setTag(boolean tag) {
        this.tag = tag;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConceptInputBean)) return false;

        ConceptInputBean that = (ConceptInputBean) o;

        if (tag != that.tag) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (tag ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConceptInputBean{" +
                "name='" + name + '\'' +
                '}';
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
