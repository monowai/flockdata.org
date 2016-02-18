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

package org.flockdata.test.engine.endpoint;

import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.LoginRequest;
import org.flockdata.authentication.UserProfile;
import org.flockdata.helper.JsonUtils;
import org.flockdata.test.engine.functional.WacBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TestAuthenticationEP extends WacBase{

    private MockMvc mockMVC;

    @Before
    public void setUp() {
        mockMVC = MockMvcBuilders.webAppContextSetup(wac)
                .build();
    }

    @Test
    public void validUserPassword_ShouldReturnUserProfile() throws Exception {
        // As per the entry in test-security.xml
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("mike");
        loginReq.setPassword("123");

        MvcResult response = mockMVC
                .perform(
                        MockMvcRequestBuilders.post(WacBase.LOGIN_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(loginReq)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        UserProfile userProfile = JsonUtils.getBytesAsObject(response
                .getResponse().getContentAsByteArray(), UserProfile.class);
        assertNotNull(userProfile);
    }

    @Test
    public void invalidUserPassword_ShouldReturnUnAuthorized() throws Exception {
        // As per the entry in test-security.xml
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("mike");
        loginReq.setPassword("1234");

        mockMVC.perform(
                MockMvcRequestBuilders.post(WacBase.LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(loginReq)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andReturn();

    }

    @Test
    public void whenLoggedInAsMike_ShouldReturn2Roles() throws Exception {
        // As per the entry in test-security.xml
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("mike");
        loginReq.setPassword("123");

        MvcResult response = mockMVC
                .perform(
                        MockMvcRequestBuilders.post(WacBase.LOGIN_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(loginReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.userRoles", hasSize(2))).andReturn();

        UserProfile userProfile = JsonUtils.getBytesAsObject(response
                .getResponse().getContentAsByteArray(), UserProfile.class);
        assertNotNull(userProfile.getUserRoles());
    }

    @Test
    public void whenLoggedInAsMike_ShouldBelongToAdminAndUserRoles()
            throws Exception {

        // As per the entry in test-security.xml
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("mike");
        loginReq.setPassword("123");

        mockMVC.perform(
                MockMvcRequestBuilders.post(WacBase.LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(loginReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.userRoles[0]", is(FdRoles.FD_ROLE_ADMIN)))
                .andExpect(jsonPath("$.userRoles[1]", is(FdRoles.FD_ROLE_USER)))
                .andReturn();
    }

}
