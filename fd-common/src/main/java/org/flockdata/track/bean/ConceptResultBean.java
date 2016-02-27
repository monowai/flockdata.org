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

package org.flockdata.track.bean;


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

    ConceptResultBean(){}

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

    public ConceptResultBean addRelationship(RelationshipResultBean relationship) {
        if ( !relationships.contains(relationship))
            relationships.add( relationship);
        return this;
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

}
