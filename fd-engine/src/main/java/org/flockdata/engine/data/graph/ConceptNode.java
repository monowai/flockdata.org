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
import org.flockdata.data.Concept;
import org.flockdata.track.bean.ConceptInputBean;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;


/**
 * A structural node that represents a type of Tag that exists in the Graph.
 *
 * @author mholdsworth
 * @tag Node, Concept, Tag
 * @since 16/06/2014
 */
@NodeEntity
@TypeAlias("Concept")
public class ConceptNode implements Concept {

  @GraphId
  private Long id;

  private String name;

  private String description;

  @Indexed(unique = true)
  private String key;

  protected ConceptNode() {
  }

  public ConceptNode(ConceptInputBean inputBean) {
    this();
    this.name = inputBean.getName();
    this.description = inputBean.getDescription();
    this.key = Concept.toKey(inputBean);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Concept{" +
        "id=" + id +
        ", name='" + name + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConceptNode)) {
      return false;
    }

    ConceptNode that = (ConceptNode) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    return !(name != null ? !name.equals(that.name) : that.name != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

}