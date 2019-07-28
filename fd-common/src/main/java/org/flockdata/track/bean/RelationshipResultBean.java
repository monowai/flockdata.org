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


/**
 * Encapsulates a relationship name that is the connection between a DocType and a Concept
 *
 * @author mholdsworth
 * @tag Contract, Relationship, Query
 * @since 20/05/2015
 */
public class RelationshipResultBean {

  private String name;

  RelationshipResultBean() {
  }

  public RelationshipResultBean(String relationship) {
    this();
    this.name = relationship;
  }

  public String getName() {
    return name;
  }


  @Override
  public String toString() {
    return "RelationshipResultBean{" +
        ", name='" + name + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RelationshipResultBean)) {
      return false;
    }

    RelationshipResultBean that = (RelationshipResultBean) o;

    return name.equals(that.name);

  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}

