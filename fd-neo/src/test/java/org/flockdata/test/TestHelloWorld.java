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

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.HelloWorld;
import org.flockdata.track.TagPayload;
import org.junit.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.test.server.HTTP;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by mike on 18/06/15.
 */
public class TestHelloWorld {

    @Test
    public void testMyExtension() throws Exception {
        // Given
        try (ServerControls server = TestServerBuilders.newInProcessBuilder()
                .withExtension("/flockdata", HelloWorld.class)
                .newServer())
        {
            TagInputBean tagInputBean = new TagInputBean("Code", "Label");
            Collection<TagInputBean> tags = new ArrayList<>();
            tags.add(tagInputBean);
            TagPayload payload = new TagPayload(tags);
            String uri = server.httpURI().resolve("flockdata/makeTags").toString();
            // When
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", MediaType.APPLICATION_JSON);
            HTTP.Builder builder = HTTP.withHeaders(headers);
            HTTP.Response response =
                    builder.POST(uri, payload);

            // Then
            assertEquals(200, response.status());
        }
    }
}
