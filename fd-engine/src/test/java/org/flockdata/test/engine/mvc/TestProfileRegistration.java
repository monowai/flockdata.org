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

package org.flockdata.test.engine.mvc;

import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 28/08/14
 * Time: 2:23 PM
 */
public class TestProfileRegistration extends MvcBase {

    @Test
    public void testWebRegistrationFlow() throws Exception {
        setSecurityEmpty();
        // Unauthenticated users can't register accounts
        exception.expect(AccessDeniedException.class);
        makeDataAccessProfile(noUser(), ANYCO, "a-user", MockMvcResultMatchers.status().isUnauthorized());
        // We're now authenticating
        setSecurity();  // An admin user

        // Retry the operation
        SystemUserResultBean regResult = registerSystemUser(mike(), new RegistrationBean(ANYCO, harry));
        assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        assertEquals(harry, regResult.getLogin());
        assertNotNull(regResult.getApiKey());
        setSecurityEmpty();

        // Check we get back a Guest
        regResult = getMe(noUser());
        assertNotNull(regResult);
        assertEquals("noone", regResult.getLogin().toLowerCase());


        regResult = getMe(harry());
        assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        assertNotNull(regResult.getApiKey());

        // Assert that harry, who is not an admin, cannot create another user
        makeDataAccessProfile(harry(), regResult.getCompanyName(), regResult.getCompanyName(),MockMvcResultMatchers.status().isUnauthorized());


    }

    SystemUserResultBean registerSystemUser(RequestPostProcessor user, RegistrationBean register) throws Exception {

        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(MvcBase.apiPath+"/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(register)
                        )
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

    SystemUserResultBean getMe(RequestPostProcessor user) throws Exception {

        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(MvcBase.apiPath+"/profiles/me/")
                        .contentType(MediaType.APPLICATION_JSON)
                .with(user)

        ).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

}
