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

package org.flockdata.test.engine.unit;

import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Test;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * User: mike
 * Date: 28/07/14
 * Time: 1:52 PM
 */
public class EntityKeyTests {

    @Test
    public void equalityAndDefaults() throws Exception{

        // ToDo: Case sensitivity
        EntityKeyBean entityKeyA = new EntityKeyBean("123", "abc", "456");
        EntityKeyBean entityKeyB = new EntityKeyBean("123", "abc", "456");

        assertEquals("Keys should match", entityKeyA, entityKeyB);
        assertEquals("Hashcodes should match", entityKeyA.hashCode(), entityKeyB.hashCode());

        EntityKeyBean entityKeyC = new EntityKeyBean(null, "abc", "456");
        assertEquals("WildCard document not working", "*", entityKeyC.getDocumentType());
        entityKeyC = new EntityKeyBean("code");
        assertEquals("WildCard document not working", "*", entityKeyC.getDocumentType());
    }
}
