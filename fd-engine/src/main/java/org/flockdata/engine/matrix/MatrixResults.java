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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;

/**
 * Encapsulates edges and nodes that make up a result suitable for matrix analysis
 *
 * @author mholdsworth
 * @tag Matrix, Query
 * @since 12/06/2014
 */
public class MatrixResults {
  private long sampleSize;
  private long totalHits;
  private Collection<FdNode> nodes;    // Lookup table of nodes in the edges
  private Collection<EdgeResult> edges;  // relationship between 2 nodes

  public MatrixResults() {
  }

  public MatrixResults(Collection<EdgeResult> edgeResults) {
    this();
    setEdges(edgeResults);
  }

  public MatrixResults(EdgeResults edgeResults) {
    setEdges(edgeResults.getEdgeResults());
  }

  public MatrixResults(EdgeResults edges, Collection<FdNode> nodes) {
    this(edges);
    this.nodes = nodes;
  }

  public Collection<EdgeResult> getEdges() {
    return edges;
  }

  public void setEdges(Collection<EdgeResult> edges) {
    this.edges = edges;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Collection<FdNode> getNodes() {
    return nodes;
  }

  public MatrixResults setNodes(Collection<FdNode> nodes) {
    this.nodes = nodes;
    return this;
  }

  public long getSampleSize() {
    return sampleSize;
  }

  public MatrixResults setSampleSize(long sampleSize) {
    this.sampleSize = sampleSize;
    return this;
  }

  public long getTotalHits() {
    return totalHits;
  }

  public void setTotalHits(long matchingResults) {
    this.totalHits = matchingResults;
  }
}
