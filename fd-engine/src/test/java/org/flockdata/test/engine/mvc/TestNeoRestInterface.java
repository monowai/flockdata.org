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

package org.flockdata.test.engine.mvc;

import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 20/09/2014
 * @tag Test,Neo4j
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:root-context-neo-rest.xml" })
public class TestNeoRestInterface {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public static HttpHeaders getHttpHeaders() {

        return new HttpHeaders() {
            {
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
            }
        };
    }

    @Test
    public void neo4j_EnsureRestAPIWorks() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "neo4j", "neo4j");

        SecurityContextHolder.getContext().setAuthentication(auth);

        // Now find something - anything
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = getHttpHeaders();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        String query = "{ \"statements\":[{\"statement\":\"return 1\"}]}";
        HttpEntity<String> requestEntity = new HttpEntity<>(query, httpHeaders);
        exception.expect(HttpClientErrorException.class);
        // ToDo: Auth check. At the moment a 401 is good enough
        Map result = restTemplate.exchange("http://localhost:7474/db/data/transaction/commit/", HttpMethod.POST, requestEntity, Map.class).getBody();

        TestCase.assertNotNull(result.get("results"));
        Collection values = (Collection) result.get("results");
        TestCase.assertFalse("No results returned from the REST call", values.size() == 0);

    }

}
