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


import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides a JSON Serializable conceptual view of a Tag
 *
 * @author mholdsworth
 * @tag Contract, Concept, Query
 * @since 20/05/2015
 */
public class ConceptResultBean {
  public static final String TAG = "T";
  private String name;
  private Collection<RelationshipResultBean> relationships = new ArrayList<>();

  ConceptResultBean() {
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

  public ConceptResultBean addRelationship(RelationshipResultBean relationship) {
    if (!relationships.contains(relationship)) {
      relationships.add(relationship);
    }
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
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConceptResultBean)) {
      return false;
    }

    ConceptResultBean that = (ConceptResultBean) o;

    return !(name != null ? !name.equals(that.name) : that.name != null);

  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

}
