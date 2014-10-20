/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.functional;

import org.flockdata.search.helper.QueryGenerator;
import junit.framework.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: macpro
 * Date: 04/08/2014
 * Time: 15:31
 * To change this template use File | Settings | File Templates.
 */
public class QueryGeneratorTest {

    @Test
    public void testGetSimpleQuery_Quoted() throws Exception {
        String query = QueryGenerator.getSimpleQuery("\"test quotes\"", false);
        assertTrue("Quoted string not parsed correctly", query.contains("\\\"test quotes\\\""));
        query = QueryGenerator.getSimpleQuery("test quotes", false);
        assertFalse("Text should not have been quoted", query.contains("\\\"test quotes\\\""));
    }
    @Test
    public void testGetSimpleQuery_withoutHighlight() throws Exception {
        String query = QueryGenerator.getSimpleQuery("test", false);
        assertFalse(query.contains("highlight"));
    }

    @Test
    public void testGetSimpleQuery_withHighlight() throws Exception {
        String query = QueryGenerator.getSimpleQuery("test",true);
        Assert.assertTrue(query.contains("highlight"));
    }
}
