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

package org.flockdata.engine.matrix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.helper.CypherHelper;
import org.neo4j.graphdb.Node;

/**
 * Represents a node structure used by Cytoscape to visualise graphs
 *
 * @author mholdsworth
 * @since 2/05/2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FdNode {
  Map<String, Object> data = new HashMap<>(); // Cytoscape convention is to put node properties in a data block

  FdNode() {
  }

  public FdNode(long id) {
    this();
    data.put("id", id);
  }

  public FdNode(Node node) {
    this(node.getId());
    String nameValue;
    if (node.hasProperty("name")) {
      nameValue = node.getProperty("name").toString();
      if (node.hasProperty("code")) // Concept nodes don't have a code property :/
      {
        data.put("code", node.getProperty("code"));
      }
    } else {
      nameValue = node.getProperty("code").toString();
    }
    data.put("name", nameValue);
    data.put("label", CypherHelper.getLabel(node.getLabels()));
  }

  public Map<String, Object> getData() {
    return data;
  }

  public String getKey() {
    return data.get("id").toString();
  }

  public String getLabel() {
    if (!data.containsKey("label")) {
      return null;
    }
    return data.get("label").toString();
  }

  public Object getName() {
    return data.get("name");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FdNode)) {
      return false;
    }

    FdNode fdNode = (FdNode) o;

    if (getKey() != null ? !getKey().equals(fdNode.getKey()) : fdNode.getKey() != null) {
      return false;
    }
    return !(getLabel() != null ? !getLabel().equals(fdNode.getLabel()) : fdNode.getLabel() != null);

  }

  @Override
  public int hashCode() {
    int result = getKey() != null ? getKey().hashCode() : 0;
    result = 31 * result + (getLabel() != null ? getLabel().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FdNode{" +
        "key='" + getKey() + '\'' +
        ", name=" + getName() +
        ", label=" + getLabel() +
        '}';
  }
}