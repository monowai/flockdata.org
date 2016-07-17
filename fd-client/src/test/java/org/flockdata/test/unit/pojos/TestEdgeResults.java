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

package org.flockdata.test.unit.pojos;

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
