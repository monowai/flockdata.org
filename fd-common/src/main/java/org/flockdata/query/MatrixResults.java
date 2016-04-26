/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.query;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * Encapsulates edges and nodes that make up a result suitable for matrix analysis
 *
 * User: mike
 * Date: 12/06/14
 * Time: 2:17 PM
 */
public class MatrixResults {
    private long sampleSize;
    private long totalHits;
    Collection<EdgeResult> edges;  // From To
    Collection<FdNode> nodes;    // Lookup table if edges contains just Ids

    public MatrixResults (){}

    public MatrixResults(Collection<EdgeResult> edgeResults) {
        this();
        setEdges(edgeResults);
    }

    public Collection<EdgeResult> getEdges() {
        return edges;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Collection<FdNode> getNodes() {
        return nodes;
    }

    public void setEdges(Collection<EdgeResult> edges) {
        this.edges = edges;
    }

    public void setNodes(Collection<FdNode> nodes) {
        this.nodes = nodes;
    }

    public void setSampleSize(long sampleSize) {
        this.sampleSize = sampleSize;
    }

    public long getSampleSize() {
        return sampleSize;
    }

    public void setTotalHits(long matchingResults) {
        this.totalHits = matchingResults;
    }

    public long getTotalHits() {
        return totalHits;
    }
}
