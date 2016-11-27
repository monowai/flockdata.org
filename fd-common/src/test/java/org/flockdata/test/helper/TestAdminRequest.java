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
import org.flockdata.search.AdminRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mholdsworth
 * @since 12/05/2016
 */
public class TestAdminRequest {

    @Test
    public void jsonSerialization() throws Exception {

        AdminRequest adminRequest = new AdminRequest("delete.this.index");

        assertEquals(1, adminRequest.getIndexesToDelete().size());
        String json = JsonUtils.toJson(adminRequest);
        assertNotNull ( json);
        AdminRequest deserialized = JsonUtils.toObject(json.getBytes(), AdminRequest.class);
        assertNotNull( deserialized.getIndexesToDelete());
        assertEquals(1, deserialized.getIndexesToDelete().size());
        assertEquals(adminRequest.getIndexesToDelete().iterator().next(), deserialized.getIndexesToDelete().iterator().next());

    }
}
