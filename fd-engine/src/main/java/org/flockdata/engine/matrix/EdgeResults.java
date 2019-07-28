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

import java.util.ArrayList;
import java.util.List;

/**
 * @author mholdsworth
 * @since 29/04/2015
 */
public class EdgeResults {
  private List<EdgeResult> edgeResults = new ArrayList<>();

  public void addResult(EdgeResult edgeResult) {
    int index = edgeResults.indexOf(edgeResult);
    if (index == -1) {
      edgeResults.add(edgeResult);
    } else {
      EdgeResult edge = edgeResults.get(index);

      edge.setCount(edge.getCount().doubleValue() + edgeResult.getCount().doubleValue());
    }


  }

  public List<EdgeResult> getEdgeResults() {
    return edgeResults;
  }
}
