/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.engine.functional;

import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 28/08/14
 * Time: 2:23 PM
 */
@WebAppConfiguration
public class TestProfileRegistration extends WacBase {

    MockMvc mockMvc;


    @Test
    public void testWebRegistrationFlow() throws Exception {
        String companyName = "Public Company";
        setSecurityEmpty();
        // Unauthenticated users can't register accounts
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mockMvc.perform(MockMvcRequestBuilders.post("/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(new RegistrationBean(companyName, sally_admin)))
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // We're now authenticating
        setSecurity();  // An admin user

        // Retry the operation
        SystemUserResultBean regResult = registerSystemUser(new RegistrationBean(companyName, harry));
        assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        assertEquals(harry, regResult.getLogin());
        assertNotNull(regResult.getApiKey());
        setSecurityEmpty();

        // Check we get back a Guest
        regResult = getMe();
        assertNotNull(regResult);
        assertEquals("Guest", regResult.getName());
        assertEquals("guest", regResult.getLogin());

        setSecurity(harry);
        regResult = getMe();
        assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        assertNotNull(regResult.getApiKey());

        // Assert that harry, who is not an admin, cannot create another user
        mockMvc.perform(MockMvcRequestBuilders.post("/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(new RegistrationBean(companyName, harry)))
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized());


    }

    @Override
    public void cleanUpGraph() {
        super.cleanUpGraph();
    }

    SystemUserResultBean registerSystemUser(RegistrationBean register) throws Exception {

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(register))
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

    SystemUserResultBean getMe() throws Exception {

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get("/profiles/me/")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

}
