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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.dao.EntityChangeMapper;
import org.junit.Test;

/**
 * FD's default search queries return a Description so that the user can get some useful data about the
 * search doc hit. Description will default to the Name if it is provided.
 * <p>
 * To save space in the ES index we only store the node name if it is different to the description
 * <p>
 * Neither name nor description are mandatory
 *
 * @author mholdsworth
 * @since 21/08/2016
 */
public class TestNameHandler {

    @Test
    public void differentNameAndDescription() {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName("A Name");
        entitySearchChange.setDescription("A Description");
        Map<String, Object> change = EntityChangeMapper.getMapFromChange(entitySearchChange);
        assertTrue(change.containsKey(SearchSchema.NAME));
        assertTrue(change.containsKey(SearchSchema.DESCRIPTION));
    }

    @Test
    public void sameNameAndDescription() {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName("A Description");
        entitySearchChange.setDescription("A Description");
        Map<String, Object> change = EntityChangeMapper.getMapFromChange(entitySearchChange);
        // If == store just one field to reduce index space
        assertFalse("When name and description are the same, only description is recorded", change.containsKey(SearchSchema.NAME));
        assertTrue(change.containsKey(SearchSchema.DESCRIPTION));
    }

    @Test
    public void nameAndNoDescription() {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName("A Name");
        Map<String, Object> change = EntityChangeMapper.getMapFromChange(entitySearchChange);
        assertFalse("When no description, name is recorded as description", change.containsKey(SearchSchema.NAME));
        assertTrue(change.containsKey(SearchSchema.DESCRIPTION));
    }

    @Test
    public void neitherNameNorDescription() {
        EntitySearchChange entitySearchChange = new EntitySearchChange();
        entitySearchChange.setName(null);
        entitySearchChange.setDescription(null);
        Map<String, Object> change = EntityChangeMapper.getMapFromChange(entitySearchChange);
        assertFalse("Null name should not be indexed", change.containsKey(SearchSchema.NAME));
        assertFalse("Null description should not be indexed", change.containsKey(SearchSchema.DESCRIPTION));
    }
}
