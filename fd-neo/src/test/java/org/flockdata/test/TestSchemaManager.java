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

package org.flockdata.test;

import org.flockdata.neo4j.TagManager;
import org.junit.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.test.server.HTTP;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * @author mholdsworth
 * @since 18/06/2015
 */
public class TestSchemaManager {

    @Test
    public void testSchema() throws Exception {

        try (ServerControls server = TestServerBuilders.newInProcessBuilder()
                .withExtension("/flockdata", TagManager.class)
                .newServer())
        {
            String schema = server.httpURI().resolve("flockdata/schema").toString();

            // When
            HTTP.Builder builder = getBuilder();
            // Make tag
            HTTP.Response writeResponse =
                    builder.POST(schema);

            assertEquals(200, writeResponse.status());


        }
    }


    private HTTP.Builder getBuilder() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON);

        return HTTP.withHeaders(headers);
    }
}
