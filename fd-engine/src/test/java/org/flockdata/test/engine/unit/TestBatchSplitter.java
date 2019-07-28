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

package org.flockdata.test.engine.unit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.test.engine.FdNodeHelper;
import org.flockdata.track.bean.TrackResultBean;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 21/03/2015
 */
public class TestBatchSplitter {
  @Test
  public void entities() throws Exception {
    Collection<TrackResultBean> inputs = new ArrayList<>();
    EntityNode entityNewA = (EntityNode) FdNodeHelper.getEntity("blah", "abc", "123", "abc");
    DocumentNode documentType = new DocumentNode(entityNewA.getFortress(), "abc");
    assertTrue("Entity did not default to a new state", entityNewA.isNewEntity());
    EntityNode entityNewB = (EntityNode) FdNodeHelper.getEntity("blah", "abc", "123", "abcd");
    EntityNode entityOldA = (EntityNode) FdNodeHelper.getEntity("blah", "abc", "123", "abcde");
    EntityNode entityOldB = (EntityNode) FdNodeHelper.getEntity("blah", "abc", "123", "abcdef");
    entityOldA.setNewEntity(false);
    entityOldB.setNewEntity(false);
    assertFalse(entityOldA.isNewEntity());

    inputs.add(new TrackResultBean(entityNewA, documentType));
    assertTrue(inputs.iterator().next().isNewEntity());
    inputs.add(new TrackResultBean(entityNewB, documentType));
    inputs.add(new TrackResultBean(entityOldA, documentType));
    inputs.add(new TrackResultBean(entityOldB, documentType));
    assertEquals(4, inputs.size());
    Collection<TrackResultBean> newEntities = TrackBatchSplitter.getNewEntities(inputs);
    assertEquals(2, newEntities.size());

    assertEquals(2, TrackBatchSplitter.getExistingEntities(inputs).size());
  }
}
