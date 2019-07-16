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

package org.flockdata.test.engine.unit;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.engine.matrix.EdgeResult;
import org.flockdata.engine.matrix.EdgeResults;
import org.flockdata.engine.matrix.FdNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.JsonUtils;
import org.junit.Test;

/**
 * Matrix result objects are used to render graphs.
 *
 * @author mholdsworth
 * @since 29/04/2015
 */
public class TestEdgeResults {
    @Test
    public void edges_DoubleTestResults() {
        EdgeResult resultA = new EdgeResult("ABC", "123", 10d);
        EdgeResult resultB = new EdgeResult("ABC", "123", 10d);
        EdgeResults results = new EdgeResults();
        results.addResult(resultA);
        results.addResult(resultB);
        assertEquals(1, results.getEdgeResults().size());
        Number total = results.getEdgeResults().iterator().next().getCount();
        assertEquals(20d, total);
    }

    @Test
    public void edges_LongTestResults() {
        EdgeResult resultA = new EdgeResult("ABC", "123", 10L);
        EdgeResult resultB = new EdgeResult("ABC", "123", 10L);
        EdgeResults results = new EdgeResults();
        results.addResult(resultA);
        results.addResult(resultB);
        assertEquals(1, results.getEdgeResults().size());
        Number total = results.getEdgeResults().iterator().next().getCount();
        assertEquals(20d, total);
    }

    @Test
    public void serializeMatrix() throws Exception {
        EdgeResult resultA = new EdgeResult("ABC", "123", 10L);
        EdgeResult resultB = new EdgeResult("ABC", "123", 10L);
        EdgeResults edges = new EdgeResults();
        edges.addResult(resultA);
        edges.addResult(resultB);
        Collection<FdNode> nodes = new ArrayList<>();
        nodes.add(new FdNode(9L));
        MatrixResults matrixResults = new MatrixResults(edges, nodes);
        byte[] bytes = JsonUtils.toJsonBytes(matrixResults);
        MatrixResults deserialized = JsonUtils.toObject(bytes, MatrixResults.class);
        assertNotNull(deserialized);
        assertEquals(matrixResults.getEdges().size(), deserialized.getEdges().size());
        assertEquals(matrixResults.getNodes().size(), deserialized.getNodes().size());
    }
}
