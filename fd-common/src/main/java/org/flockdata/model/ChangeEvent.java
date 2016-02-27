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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:52 PM
 */
@NodeEntity
@TypeAlias("Event")
public class ChangeEvent {
    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String code;
    private String name;

    @Override
    public String toString() {
        return "ChangeEventNode{" +
                "id=" + id +
                ", code='" + code + '\'' +
                '}';
    }

    //@Relationship(type = "COMPANY_EVENT", direction = Relationship.INCOMING)
    @RelatedTo(type = "COMPANY_EVENT", direction = Direction.INCOMING)
    private Iterable<Company> companies;

    protected ChangeEvent() {
    }

    public ChangeEvent(String name) {
        this.name = name;
        this.code = name;
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
