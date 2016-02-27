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

    private ConceptInputBean() {}

    public ConceptInputBean(String name) {
        this();
        this.name = name;
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
}
