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

package org.flockdata.test.search;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import junit.framework.TestCase;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.QueryParams;
import org.junit.Test;

/**
 * Indexes to query are computed at runtime. This validates the generic functionality that
 * composes a valid ES index path
 *
 * @author mholdsworth
 * @since 23/07/2015
 */

public class TestIndexHelper {

    @Test
    public void compute_segmentIndex() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "FlockData";
        String fortress = "fdm";
        String segment = "2013";
        String types[] = new String[1];

        types[0] = "lar";
        String[] indexes = indexManager.getIndices(company, fortress, types, segment);
        TestCase.assertEquals(1, indexes.length);
        TestCase.assertEquals("fd.flockdata.fdm.lar.2013", indexes[0]);
    }

    @Test
    public void compute_segmentWildCard() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "FlockData";
        String fortress = "fdm";
        String segment = null;
        String types[] = new String[1];

        String[] indexes = indexManager.getIndices(company, fortress, types, segment);
        TestCase.assertEquals(1, indexes.length);
        TestCase.assertEquals("fd.flockdata.fdm*", indexes[0]);
    }

    @Test
    public void testA() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        String fortress = "123";
        String segment = "segment";
        String types[] = new String[2];

        types[0] = "Type0";
        types[1] = "Type1";
        String[] indexes = indexManager.getIndices(company, fortress, types, segment);
        int count = 0;
        for (String index : indexes) {
            validateIndex(company, fortress, types[count], segment, index);
            count++;
        }


    }

    //
    @Test
    public void kvStoreRetrievalIndex() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        String fortress = "123";
        String segment = "segment";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        qp.setFortress(fortress);
        qp.setSegment(segment);
        qp.setTypes("Type0");
        String parsedIndex = indexManager.toIndex(qp);
        TestCase.assertEquals("If this fails then locating the content when KV_NONE will fail", "fd." + company.toLowerCase() + "." + fortress.toLowerCase() + "." + segment.toLowerCase(), parsedIndex);
        //String[] indexes = IndexHelper.getIndices(qp);
        //validateIndexesForQuery(company, fortress, segment, indexes);
    }

    @Test
    public void testFromQueryParams() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        String fortress = "123";
        String segment = "segment";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);
        qp.setFortress(fortress);
        qp.setSegment(segment);
        qp.setTypes("Type0", "type1");

        String[] indexes = indexManager.getIndices(qp);
        int expectedCount = 2; // We set two type filters so should be at least two indexes
        TestCase.assertEquals(expectedCount, indexes.length);
        int count = 0;
        for (String index : indexes) {
            if (count == 0) {
                validateIndex(company, fortress, "type0", segment, index);
            } else {
                validateIndex(company, fortress, "type1", segment, index);
            }
            count++;
        }

    }

    @Test
    public void wildCardFortress() throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        String company = "abc";
        QueryParams qp = new QueryParams();
        qp.setCompany(company);

        String[] indexes = indexManager.getIndices(qp);
        TestCase.assertEquals(1, indexes.length);
        for (String index : indexes) {
            assertTrue(index.startsWith(indexManager.getPrefix() + company.toLowerCase() + ".*"));
        }
    }

    @Test
    public void blah() throws Exception {
        QueryParams qp = new QueryParams("*");
        qp.setFortress("theFortress");
        qp.setCompany("theCompany"); // normally this is set automatically by fd-engine
        qp.setTypes("thedoc");
        IndexManager indexManager = new IndexManager("fd.", true);
        String indexes[] = indexManager.getIndices(qp);
        for (String index : indexes) {
            System.out.println(index);
        }

    }

    @Test
    public void nullIndexes() throws Exception {
        QueryParams qp = new QueryParams("*");
        IndexManager indexManager = new IndexManager("fd.", true);
        String indexes[] = indexManager.getIndices(qp);
        assertEquals(1, indexes.length);
        assertEquals("Cross company filter did not work", "fd.*", indexes[0]);

        //
        qp.setCompany("MyCo");
        indexes = indexManager.getIndices(qp);
        assertEquals(1, indexes.length);
        assertEquals("fd.myco.*", indexes[0]);

    }

    /**
     * Validates that the index matches a valid ElasticSearch structure
     *
     * @param company
     * @param fortress
     * @param segment
     * @param index
     * @throws Exception
     */
    private void validateIndex(String company, String fortress, String type, String segment, String index) throws Exception {
        IndexManager indexManager = new IndexManager("fd.", false);
        assertNotNull(index);

        TestCase.assertEquals(index,
            indexManager.getPrefix() +
                company.toLowerCase() + "." +
                fortress.toLowerCase() +
                (type == null ? "" : "." + type.toLowerCase()) +
                (segment == null ? "*" : "." + segment.toLowerCase())
        );
    }


}
