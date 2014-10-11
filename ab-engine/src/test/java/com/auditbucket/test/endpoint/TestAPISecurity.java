package com.auditbucket.test.endpoint;

import com.auditbucket.authentication.LoginRequest;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.functional.EngineBase;
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
				MockMvcRequestBuilders.get("/fortress/").header("Api-Key",
						apikey))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
	}
}
