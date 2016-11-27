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

import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.ContentStructure;
import org.flockdata.search.model.EsColumn;
import org.flockdata.search.model.SearchSchema;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Sanity tests for ContentStructure
 *
 * @author mholdsworth
 * @since 31/08/2016
 */
public class TestContentStructure {

    @Test
    public void serializeEntityTagRelationship() throws Exception{
        ContentStructure contentStructure = new ContentStructure();
        contentStructure.addData(new EsColumn(SearchSchema.DATA_FIELD+"twee.facet", "string"));
        contentStructure.addLink(new EsColumn(SearchSchema.TAG_FIELD+"twee.facet", "string"));
        contentStructure.addLink(new EsColumn(SearchSchema.ENTITY+".twee.facet", "string"));
        contentStructure.addFd(new EsColumn("whenCreated", "date"));
        String json = JsonUtils.toJson(contentStructure);
        assertNotNull ( json);
        ContentStructure deserialzied = JsonUtils.toObject(json, ContentStructure.class);
        assertNotNull (deserialzied);

        assertEquals(2, deserialzied.getLinks().size());
        assertEquals(1, deserialzied.getSystem().size());
        assertEquals(1, deserialzied.getData().size());

        assertEquals ("data. prefix and .facet suffix should have been removed", "twee", deserialzied.getData().iterator().next().getDisplayName());
        assertEquals ("Display name didn't default to name", "whenCreated", deserialzied.getSystem().iterator().next().getDisplayName());
        for (EsColumn esColumn : deserialzied.getLinks()) {
            assertEquals ("tag. & e. prefix and .facet suffix should have been removed", "twee", esColumn.getDisplayName());
        }


    }
}
