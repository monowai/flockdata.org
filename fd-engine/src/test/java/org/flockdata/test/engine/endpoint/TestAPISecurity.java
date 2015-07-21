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

import org.flockdata.authentication.LoginRequest;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.functional.EngineBase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
public class TestAPISecurity extends EngineBase {

	private MockMvc mockMVC;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setUp() {
		mockMVC = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.build();
		setSecurityEmpty();
	}

	@Test
	public void invokeSecureAPIWithoutAPIKey_shouldThrowError()
			throws Exception {
		mockMVC.perform(MockMvcRequestBuilders.get("/fortress/"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andReturn();
	}

	@Test
	public void invokeSecureAPIWithoutAPIKeyButAfterValidLogin_shouldReturnOk()
			throws Exception {
		setSecurity();
		registerSystemUser("invokeSecureAPIWithoutAPIKeyButAfterValidLogin_shouldReturnOk", sally_admin);
		setSecurityEmpty();
		
		LoginRequest loginReq = new LoginRequest();
		loginReq.setUsername(sally_admin);
		loginReq.setPassword("123");

		mockMVC.perform(
				MockMvcRequestBuilders.post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(JsonUtils.getJSON(loginReq))).andReturn();
		mockMVC.perform(MockMvcRequestBuilders.get("/fortress/"))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
	}

	@Test
	public void invokeSecureAPIWithAPIKeyWithoutLogin_shouldReturnOk() throws Exception {
		setSecurity();
		SystemUser su = registerSystemUser("invokeSecureAPIWithAPIKeyWithoutLogin_shouldReturnOk", mike_admin);
		String apikey = su.getApiKey();
		setSecurityEmpty();

		mockMVC.perform(
				MockMvcRequestBuilders.get("/fortress/").header("api-key",
						apikey))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
	}
}
