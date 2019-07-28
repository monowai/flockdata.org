/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.ChangeEvent;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author mholdsworth
 * @tag Node, ChangeEvent
 * @since 29/06/2013
 */
@NodeEntity
@TypeAlias("Event")
public class ChangeEventNode implements ChangeEvent {
  @GraphId
  private Long id;

  @Indexed(unique = true)
  private String code;
  private String name;
  //@Relationship(type = "COMPANY_EVENT", direction = Relationship.INCOMING)
  @RelatedTo(type = "COMPANY_EVENT", direction = Direction.INCOMING)
  private Iterable<CompanyNode> companies;

  protected ChangeEventNode() {
  }

  public ChangeEventNode(String name) {
    this.name = name;
    this.code = name;
  }

  @Override
  public String toString() {
    return "ChangeEventNode{" +
        "id=" + id +
        ", code='" + code + '\'' +
        '}';
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return id;
  }

  @Override
  @JsonIgnore
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public String getName() {
    return name;
  }
}
