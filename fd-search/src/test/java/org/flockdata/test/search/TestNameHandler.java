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

package org.flockdata.test.search;

import org.flockdata.search.dao.EntityChangeWriterEs;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchSchema;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * FD's default search queries return a Description so that the user can get some useful data about the
 * search doc hit. Description will default to the Name if it is provided.
 *
 * To save space in the ES index we only store the node name if it is different to the description
 *
 * Neither name nor description are mandatory
 *
 * Created by mike on 21/08/16.
 */
public class TestNameHandler {

    @Test
    public void differentNameAndDescription() throws Exception {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName("A Name");
        entitySearchChange.setDescription("A Description");
        EntityChangeWriterEs bean = new EntityChangeWriterEs(null, null);
        Map<String,Object> change =  bean.getMapFromChange(entitySearchChange);
        assertTrue(change.containsKey(SearchSchema.NAME));
        assertTrue(change.containsKey(SearchSchema.DESCRIPTION));
    }

    @Test
    public void sameNameAndDescription() throws Exception {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName("A Description");
        entitySearchChange.setDescription("A Description");
        EntityChangeWriterEs bean = new EntityChangeWriterEs(null, null);
        Map<String,Object> change =  bean.getMapFromChange(entitySearchChange);
        // If == store just one field to reduce index space
        assertFalse("When name and description are the same, only description is recorded", change.containsKey(SearchSchema.NAME));
        assertTrue(change.containsKey(SearchSchema.DESCRIPTION));
    }

    @Test
    public void nameAndNoDescription() throws Exception {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName("A Name");
        EntityChangeWriterEs bean = new EntityChangeWriterEs(null, null);
        Map<String,Object> change =  bean.getMapFromChange(entitySearchChange);
        assertFalse("When no description, name is recorded as description", change.containsKey(SearchSchema.NAME));
        assertTrue(change.containsKey(SearchSchema.DESCRIPTION));
    }

    @Test
    public void neitherNameNorDescription() throws Exception {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName(null);
        entitySearchChange.setDescription(null);
        EntityChangeWriterEs bean = new EntityChangeWriterEs(null, null);
        Map<String,Object> change =  bean.getMapFromChange(entitySearchChange);
        assertFalse("Null name should not be indexed", change.containsKey(SearchSchema.NAME));
        assertFalse("Null description should not be indexed", change.containsKey(SearchSchema.DESCRIPTION));
    }
}
