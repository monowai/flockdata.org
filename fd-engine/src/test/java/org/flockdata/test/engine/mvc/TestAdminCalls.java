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

package org.flockdata.test.engine.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author mholdsworth
 * @tag Test, Administration, MVC
 * @since 19/05/2014
 */
public class TestAdminCalls extends MvcBase {


    @Test
    public void testPing() throws Exception {
        String result = authPing(mike(), MockMvcResultMatchers.status().isOk());
        assertTrue("pong".equalsIgnoreCase(result));
        setSecurityEmpty(); // Unsecured should not work
        authPing(noUser(), MockMvcResultMatchers.status().isForbidden());
    }

    /**
     * Ensures that an authorised user, but one who is not associated with a company, can ping the service
     * while an unauthorised user cannot
     *
     * @throws Exception
     */
    @Test
    public void authPing() throws Exception {

        setSecurityEmpty(); // Unsecured should fail
        String result = authPing(noUser(), MockMvcResultMatchers.status().isForbidden());
        assertFalse(result.contains("pong"));
        result = authPing(mike(), MockMvcResultMatchers.status().isOk());
        assertEquals("pong", result);

    }

    @Test
    public void auth_Health() throws Exception {
        setSecurityEmpty();
        mvc().perform(MockMvcRequestBuilders.get(MvcBase.apiPath + "/admin/health/"))
            .andExpect(MockMvcResultMatchers
                .status()
                .isUnauthorized())
            .andReturn();

    }

    @Test
    public void testHealth() throws Exception {
        setSecurity();
        Map<String, Object> results = getHealth(mike());
        assertFalse("We didn't get back the health results for a valid api account", results.isEmpty());
        if (results.get("fd-search").toString().equalsIgnoreCase("ok")) {
            logger.warn("fd-search is running in a not unit test fashion....");
        } else {
            Map<String, Object> fdSearchProps = (Map<String, Object>) results.get("fd-search");
            assertEquals(2, fdSearchProps.size());
            assertTrue(fdSearchProps.get("status").toString(), fdSearchProps.get("status").toString().contains("Disabled"));
        }

        results = getHealth(mike());
        assertFalse("We didn't get back the health results for an admin user", results.isEmpty());

        setSecurityEmpty();

        mvc().perform(MockMvcRequestBuilders.get(MvcBase.apiPath + "/admin/health/")
            .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andReturn();
        setSecurity();

        setSecurityEmpty();
        results = getHealth(suMike);
        assertFalse("The user has no AUTH credentials but a valid APIKey - this should pass", results.isEmpty());

        // Hacking with an invalid API Key. Should fail
        mvc().perform(MockMvcRequestBuilders.get(MvcBase.apiPath + "/admin/health/")
            .header(ApiKeyInterceptor.API_KEY, "_invalidAPIKey_")
            .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()
        ).andReturn();


    }


}
