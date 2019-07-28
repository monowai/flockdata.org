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
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author mholdsworth
 * @tag Fortress, Segment, Entity
 * @since 13/10/2015
 */

@NodeEntity
@TypeAlias("FortressSegment")
public class FortressSegmentNode implements Segment {
  @GraphId
  private Long id;
  @RelatedTo(type = "DEFINES", direction = Direction.INCOMING)
  @Fetch
  private FortressNode fortress;
  @Indexed
  private String code;
  @Indexed(unique = true)
  private String key;

  FortressSegmentNode() {
  }

  public FortressSegmentNode(Fortress fortress) {
    this(fortress, Fortress.DEFAULT);
    this.fortress = (FortressNode) fortress;
  }

  public FortressSegmentNode(Fortress fortress, String code) {
    this();
    this.fortress = (FortressNode) fortress;
    this.code = code;
    if (fortress == null) {
      throw new IllegalArgumentException("An invalid fortress was passed in");
    }
    this.key = Fortress.key(fortress.getCode(), code);
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public Fortress getFortress() {
    return fortress;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  @JsonIgnore
  public boolean isDefault() {
    return code.equals(Fortress.DEFAULT);
  }

  @Override
  @JsonIgnore
  public Company getCompany() {
    return fortress.getCompany();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FortressSegmentNode)) {
      return false;
    }

    FortressSegmentNode segment = (FortressSegmentNode) o;

    if (id != null ? !id.equals(segment.id) : segment.id != null) {
      return false;
    }
    if (code != null ? !code.equals(segment.code) : segment.code != null) {
      return false;
    }
    if (key != null ? !key.equals(segment.key) : segment.key != null) {
      return false;
    }
    return !(fortress != null ? !fortress.getId().equals(segment.fortress.getId()) : segment.fortress.getId() != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (key != null ? key.hashCode() : 0);
    result = 31 * result + (fortress != null ? fortress.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FortressSegment{" +
        "code='" + code + '\'' +
        "key='" + key + '\'' +
        '}';
  }
}
