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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.flockdata.authentication.FdRoles;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.LoginRequest;
import org.flockdata.registration.SystemUserResultBean;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class TestAuthenticationEP extends MvcBase {

  @Test
  public void validUserPassword_ShouldReturnUserProfile() throws Exception {
    // As per the entry in test-security.xml
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername("mike");
    loginReq.setPassword("123");

    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders.post(MvcBase.LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(loginReq)))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    SystemUserResultBean systemUserResultBean = JsonUtils.toObject(response
        .getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    assertNotNull(systemUserResultBean);
  }

  @Test
  public void invalidUserPassword_ShouldReturnUnAuthorized() throws Exception {
    // As per the entry in test-security.xml
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername("mike");
    loginReq.setPassword("1234");

    mvc().perform(
        MockMvcRequestBuilders.post(MvcBase.LOGIN_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(loginReq)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();

  }

  @Test
  public void whenLoggedInAsMike_ShouldReturn2Roles() throws Exception {
    // As per the entry in test-security.xml
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername("mike");
    loginReq.setPassword("123");

    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders.post(MvcBase.LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(loginReq)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(jsonPath("$.userRoles", hasSize(3))).andReturn();
    // FD_USER, FD_ADMIN & USER

    SystemUserResultBean systemUser = JsonUtils.toObject(response
        .getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    assertNotNull(systemUser.getUserRoles());
  }

  @Test
  public void whenLoggedInAsMike_ShouldBelongToAdminAndUserRoles()
      throws Exception {

    // As per the entry in test-security.xml
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername("mike");
    loginReq.setPassword("123");

    mvc().perform(
        MockMvcRequestBuilders.post(MvcBase.LOGIN_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(loginReq)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(jsonPath("$.userRoles[0]", is(FdRoles.FD_ROLE_ADMIN)))
        .andExpect(jsonPath("$.userRoles[1]", is(FdRoles.FD_ROLE_USER)))
        .andReturn();
  }

}
