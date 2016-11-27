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

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.authentication.registration.bean.AliasInputBean;
import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.authentication.registration.bean.TagResultBean;
import org.flockdata.neo4j.TagManager;
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
 * @author mholdsworth
 * @since 18/06/2015
 */
public class TestTagManager {

    @Test
    public void testMyExtension() throws Exception {
        // Given
        try (ServerControls server = TestServerBuilders.newInProcessBuilder()
                .withExtension("/flockdata", TagManager.class)
                .newServer())
        {
            TagInputBean tagInputBean = new TagInputBean("NZ", "Country").setName("New Zealand");
            AliasInputBean alias = new AliasInputBean("NZL", "Sports Code");
            tagInputBean.addAlias(alias);
            Collection<TagInputBean> tags = new ArrayList<>();
            tags.add(tagInputBean);
            TagPayload payload = new TagPayload(tags);
            String makeTags = server.httpURI().resolve("flockdata/tags").toString();

            // When
            HTTP.Builder builder = getBuilder();
            // Make tag
            HTTP.Response writeResponse =
                    builder.POST(makeTags, payload);

            assertEquals(200, writeResponse.status());

            // Read tag by code
            String getTag = server.httpURI().resolve("flockdata/tags/Country/nz").toString();
            HTTP.Response readResponse =
                    builder.GET(getTag);

            assertEquals(200, readResponse.status());
            validateTag(readResponse, "NZ", "New Zealand");

            // By alias
            getTag = server.httpURI().resolve("flockdata/tags/Country/nzl").toString();
            readResponse =
                    builder.GET(getTag);

            assertEquals(200, readResponse.status());
            validateTag(readResponse, "NZ", "New Zealand");
        }
    }

    static void validateTag(HTTP.Response readResponse, String code, String name) throws java.io.IOException {
        TagResultBean result = JsonUtils.getBytesAsObject(readResponse.rawContent().getBytes(), TagResultBean.class);
        TestCase.assertEquals(code, result.getCode());
        TestCase.assertEquals(name, result.getName());
    }

    private HTTP.Builder getBuilder() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON);

        return HTTP.withHeaders(headers);
    }
}
