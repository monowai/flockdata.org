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

package org.flockdata.test.search.functional;

import static org.junit.Assert.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import org.flockdata.search.QueryParams;
import org.flockdata.search.helper.QueryGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 04/08/2014
 */

public class TestPojos {

  @Test
  public void testGetSimpleQuery_Quoted() throws Exception {
    String query = QueryGenerator.getSimpleQuery(new QueryParams("\"test quotes\""), false);
    assertTrue("Quoted string not parsed correctly", query.contains("\\\"test quotes\\\""));
    query = QueryGenerator.getSimpleQuery(new QueryParams("test quotes"), false);
    assertFalse("Text should not have been quoted", query.contains("\\\"test quotes\\\""));
  }

  @Test
  public void testGetSimpleQuery_withoutHighlight() throws Exception {
    String query = QueryGenerator.getSimpleQuery(new QueryParams("test"), false);
    assertFalse(query.contains("highlight"));
  }

  @Test
  public void testGetSimpleQuery_withHighlight() throws Exception {
    String query = QueryGenerator.getSimpleQuery(new QueryParams("test"), true);
    Assert.assertTrue(query.contains("highlight"));
  }

//    @Test
//    public void indexManager() throws Exception {
//
//        String company = "qc";
//
//        QueryParams qp = new QueryParams();
//        qp.setCompany(company);
//        qp.setSearchText("*");
//
//        String indexRoot = indexManager.getTagIndexRoot(company, null);
//        assertEquals(indexManager.getPrefix() + company.toLowerCase() , indexRoot);
//
//        // Varargs set to a 0 length array
//        qp.setTypes();
//
//        String[] indexes = indexManager.getIndices(qp);
//        TestCase.assertFalse(indexes == null);
//        TestCase.assertEquals(1, indexes.length);
//        assertEquals(indexRoot +".*", indexes[0]);
//
//        qp.setFortress("*");
//        indexes = indexManager.getIndices(qp);
//        TestCase.assertFalse(indexes == null);
//        TestCase.assertEquals(1, indexes.length);
//        assertEquals(indexRoot+".*", indexes[0]);
//
//    }

}
