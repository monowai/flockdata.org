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

import org.flockdata.model.Relationship;
import org.flockdata.model.Concept;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides a JSON Serializable view of a Tag
 *
 * Created by mike on 20/05/15.
 */
public class ConceptResultBean {
    private String name;
    private Collection<RelationshipResultBean> relationships = new ArrayList<>();
    public static final String TAG = "T";
    public static final String ENTITY = "E";

    ConceptResultBean(){}

    public ConceptResultBean(Concept concept){
        this(concept.getName());
        addRelationships(TAG, concept.getKnownTags());
        addRelationships(ENTITY, concept.getKnownEntities());
    }

    public ConceptResultBean(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<RelationshipResultBean> getRelationships() {
        return relationships;
    }

    @Override
    public String toString() {
        return "ConceptResultBean{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConceptResultBean)) return false;

        ConceptResultBean that = (ConceptResultBean) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public void addRelationships(String type, Collection<Relationship> fauxRlxs) {
        if ( fauxRlxs !=null&& !fauxRlxs.isEmpty() ){

            for (Relationship relationship : fauxRlxs) {
                relationships.add(new RelationshipResultBean(type, relationship));
            }
        }


    }
}
