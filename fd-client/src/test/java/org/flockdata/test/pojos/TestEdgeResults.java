/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.pojos;

import org.flockdata.query.EdgeResult;
import org.flockdata.query.EdgeResults;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 29/04/15.
 */
public class TestEdgeResults {
    @Test
    public void edges_DoubleTestResults(){
        EdgeResult resultA = new EdgeResult("ABC", "123", 10d);
        EdgeResult resultB = new EdgeResult("ABC", "123", 10d);
        EdgeResults results = new EdgeResults();
        results.addResult(resultA);
        results.addResult(resultB);
        assertEquals(1, results.get().size());
        Number total = results.get().iterator().next().getCount();
        assertEquals(20d, total);
    }

    @Test
    public void edges_LongTestResults(){
        EdgeResult resultA = new EdgeResult("ABC", "123", 10l);
        EdgeResult resultB = new EdgeResult("ABC", "123", 10l);
        EdgeResults results = new EdgeResults();
        results.addResult(resultA);
        results.addResult(resultB);
        assertEquals(1, results.get().size());
        Number total = results.get().iterator().next().getCount();
        assertEquals(20d, total);
    }
}
