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

import static org.springframework.test.util.AssertionErrors.assertEquals;

import org.flockdata.track.bean.EntityKeyBean;
import org.junit.Test;

/**
 * @author mholdsworth
 * @tag Test, Entity
 * @since 28/07/2014
 */
public class TestEntityKeys {

  @Test
  public void equalityAndDefaults() throws Exception {

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
