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

import org.flockdata.registration.AliasInputBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.ArrayList;

/**
 * Created by mike on 1/04/15.
 */
@NodeEntity // Only in place to support projection
@TypeAlias("Alias")
public class Alias {

    @GraphId
    Long id;

    String description;
    String name;
    String key;

    @Labels
    private ArrayList<String> labels = new ArrayList<>();

    @RelatedTo(elementClass = Tag.class, type = "HAS_ALIAS", direction = Direction.INCOMING)
    //    @Relationship(type = "HAS_ALIAS", direction = Relationship.INCOMING)
    private Tag tag ;

    Alias(){
        // ToDo: Remove with SDN4
    }

    public Alias(String theLabel, AliasInputBean aliasInput, String key, Tag tag) {
        this();
        // ToDo: This should be provided by the caller
        labels.add(theLabel+"Alias");
        labels.add("Alias");
        labels.add("_Alias");
        this.key = key;
        this.name = aliasInput.getCode();
        this.description = aliasInput.getDescription();
        this.tag = tag;
    }

    public String getLabel() {
        for (String label : labels) {
            if (! (label.equals("Alias") || label.equals("_Alias")))
                return label;
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "AliasNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public String getKey() {
        return key;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
}
