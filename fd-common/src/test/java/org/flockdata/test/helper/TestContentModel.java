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

package org.flockdata.test.helper;

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.transform.ColumnDefinition;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created by mike on 12/07/16.
 */
public class TestContentModel {

    @Test
    public void serializeEntityTagRelationship() throws Exception{
        ContentModel model = ContentModelDeserializer.getContentModel("/model/entity-tag-relationship.json");
        assertNotNull ( model);
        ColumnDefinition jurisdiction = model.getContent().get("jurisdiction_description");
        assertNotNull ( jurisdiction);
        assertNotNull ( jurisdiction.getEntityTagLinks());
        assertEquals (1, jurisdiction.getEntityTagLinks().size());
        assertTrue ( "Boolean did not set", model.isSearchSuppressed());
        assertTrue ( "Boolean did not set", model.isTrackSuppressed()) ;
    }
}
