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

package org.flockdata.geography;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.engine.data.graph.TagNode;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

/**
 * @author mholdsworth
 * @since 19/03/2015
 */
public class GeoEvaluator implements Evaluator {
  static final Label countryLabel = DynamicLabel.label("Country");
  static final Label entityLabel = DynamicLabel.label("_Entity");

  Collection<String> seenLabels = new ArrayList<>();

  @Override
  public Evaluation evaluate(Path path) {
    if (path.endNode().hasLabel(countryLabel) || path.endNode().hasLabel(entityLabel)) {
      return Evaluation.INCLUDE_AND_PRUNE;
    }
    String thisLabel = getLabel(path.endNode());
    if (seenLabels.contains(thisLabel)) {
      return Evaluation.EXCLUDE_AND_CONTINUE;
    }

    if (thisLabel.contains("Alias")) {
      return Evaluation.EXCLUDE_AND_CONTINUE;
    }

    seenLabels.add(thisLabel);
    return Evaluation.INCLUDE_AND_CONTINUE;
  }

  private String getLabel(Node node) {
    for (Label label : node.getLabels()) {
      if (!(label.name().equals(TagNode.DEFAULT_TAG) || label.name().equals("Tag"))) {
        return label.name();

      }

    }

    return TagNode.DEFAULT_TAG;
  }
}
