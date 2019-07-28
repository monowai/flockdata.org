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

package org.flockdata.engine.matrix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.Node;

/**
 * Carries details about an edge between 2 nodes
 *
 * @author mholdsworth
 * @tag Matrix, Query
 * @since 19/04/2014
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeResult {
  private Map<String, Object> data = new HashMap<>();

  EdgeResult() {
  }

  public EdgeResult(String source, String target, Number count) {
    this();
    data.put("source", source);
    data.put("target", target);
    data.put("count", count);

  }

  public EdgeResult(Node conceptFrom, Node conceptTo, Number value) {
    this();
    data.put("source", conceptFrom.getId());
    data.put("target", conceptTo.getId());
    data.put("count", value);

  }

  public EdgeResult(FdNode doc, FdNode concept, String name) {
    data.put("source", doc.getKey());
    data.put("target", concept.getKey());
    data.put("relationship", name);

  }

  public Map<String, Object> getData() {
    return data;
  }

  public String getSource() {
    return data.get("source").toString();
  }

  public void setSource(String source) {
    data.put("source", source);
  }

  public String getTarget() {
    return getData().get("target").toString();
  }

  public void setTarget(String target) {
    getData().put("target", target);
  }

  public String getRelationship() {
    Object o = getData().get("relationship");
    if (o == null) {
      return null;
    }
    return o.toString();
  }

  public Number getCount() {
    Number result = (Number) getData().get("count");
    if (result == null) {
      return 0d;
    }
    return result;
  }

  public void setCount(Number count) {
    getData().put("count", count);
  }

  @Override
  public String toString() {
    return "EdgeResult{" +
        "source='" + getSource() + '\'' +
        ", target='" + getTarget() + '\'' +
        ", count=" + getCount() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EdgeResult)) {
      return false;
    }

    EdgeResult that = (EdgeResult) o;

    if (getSource() != null ? !getSource().equals(that.getSource()) : that.getSource() != null) {
      return false;
    }
    if (getRelationship() != null ? !getRelationship().equals(that.getRelationship()) : that.getRelationship() != null) {
      return false;
    }
    return !(getTarget() != null ? !getTarget().equals(that.getTarget()) : that.getTarget() != null);

  }

  @Override
  public int hashCode() {
    int result = getSource() != null ? getSource().hashCode() : 0;
    result = 31 * result + (getTarget() != null ? getTarget().hashCode() : 0);
    result = 31 * result + (getRelationship() != null ? getRelationship().hashCode() : 0);
    return result;
  }

  public void addProperty(String key, Object property) {
    if (!data.containsKey(key)) {
      data.put(key, property);
    }
  }
}
