package com.auditbucket.test.endpoint;

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

import com.auditbucket.authentication.LoginRequest;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.functional.TestEngineBase;

@WebAppConfiguration
public class TestAPISecurity extends TestEngineBase {

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
		regService.registerSystemUser(new RegistrationBean(monowai, mike_admin)
				.setIsUnique(false));

		LoginRequest loginReq = new LoginRequest();
		loginReq.setUsername("mike");
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
		SystemUser su = regService.registerSystemUser(new RegistrationBean(
				monowai, mike_admin).setIsUnique(false));
		String apikey = su.getApiKey();
		setSecurityEmpty();

		mockMVC.perform(
				MockMvcRequestBuilders.get("/fortress/").header("Api-Key",
						apikey))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
	}
}
