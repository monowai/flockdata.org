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

package org.flockdata.test.engine.unit;

import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 21/03/15.
 */
public class TestBatchSplitter {
    @Test
    public void entitites() throws Exception{
        Collection<TrackResultBean> inputs = new ArrayList<>();
        Entity entityNewA = Helper.getEntity("blah", "abc", "123", "abc");
        assertTrue("Entity did not default to a new state", entityNewA.isNew());
        Entity entityNewB = Helper.getEntity("blah", "abc", "123", "abcd");
        Entity entityOldA = Helper.getEntity("blah", "abc", "123", "abcde");
        Entity entityOldB = Helper.getEntity("blah", "abc", "123", "abcdef");
        entityOldA.setNew(false);
        entityOldB.setNew(false);
        assertFalse(entityOldA.isNew());

        inputs.add(new TrackResultBean(entityNewA));
        inputs.add(new TrackResultBean(entityNewB));
        inputs.add(new TrackResultBean(entityOldA));
        inputs.add(new TrackResultBean(entityOldB));
        assertEquals(4, inputs.size());
        Collection<TrackResultBean>newEntities = TrackBatchSplitter.splitEntityResults(inputs);
        assertEquals(2, newEntities.size());
        assertEquals(2, inputs.size());
    }
}
