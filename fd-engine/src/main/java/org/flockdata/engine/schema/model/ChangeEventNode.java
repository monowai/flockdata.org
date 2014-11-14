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

package org.flockdata.engine.schema.model;

import org.flockdata.company.model.CompanyNode;
import org.flockdata.track.model.ChangeEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: Mike Holdsworth
 * Since: 6/09/13
 */
@NodeEntity
@TypeAlias("Event")
public class ChangeEventNode implements ChangeEvent {

    @GraphId
    private Long id;

    @Indexed (unique = true)
    private String code;
    private String name;

    @Override
    public String toString() {
        return "ChangeEventNode{" +
                "id=" + id +
                ", code='" + code + '\'' +
                '}';
    }

    @RelatedTo(type = "COMPANY_EVENT", direction = Direction.INCOMING)
    private Iterable<CompanyNode> companies;

    protected ChangeEventNode() {
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public String getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
