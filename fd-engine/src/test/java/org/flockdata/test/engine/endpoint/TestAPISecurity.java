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

import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.functional.WacBase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebAppConfiguration
public class TestAPISecurity extends WacBase {

	private MockMvc mockMVC;

	@Before
	public void setUp() {
		mockMVC = MockMvcBuilders.webAppContextSetup(wac)
				.build();
		setSecurityEmpty();
	}

	@Test
	public void invokeSecureAPIWithoutAPIKey_shouldThrowError()
			throws Exception {
		mockMVC.perform(MockMvcRequestBuilders.get(WacBase.apiPath+"/fortress/"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andReturn();
	}

	@Test
	public void invokeSecureAPIWithoutAPIKeyButAfterValidLogin_shouldReturnOk()
			throws Exception {
		setSecurity();
		registerSystemUser("invokeSecureAPIWithoutAPIKeyButAfterValidLogin_shouldReturnOk", sally_admin);
		setSecurityEmpty();
		
		login(sally_admin, "123");

		getMockMvc()
				.perform(MockMvcRequestBuilders
						.get(WacBase.apiPath+"/fortress/"))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
	}

	@Test
	public void invokeSecureAPIWithAPIKeyWithoutLogin_shouldReturnOk() throws Exception {
		setSecurity();
		SystemUser su = registerSystemUser("invokeSecureAPIWithAPIKeyWithoutLogin_shouldReturnOk", mike_admin);
		String apikey = su.getApiKey();
		setSecurityEmpty();

		mockMVC.perform(
				MockMvcRequestBuilders.get(WacBase.apiPath+"/fortress/").header("api-key",
						apikey))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
	}
}
