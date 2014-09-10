package com.auditbucket.test.endpoint;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auditbucket.authentication.LoginRequest;
import com.auditbucket.authentication.UserProfile;
import com.auditbucket.helper.JsonUtils;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:root-context.xml",
		"classpath:apiDispatcher-servlet.xml" })
public class TestAuthenticationEP {

	private MockMvc mockMVC;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setUp() {
		mockMVC = MockMvcBuilders.webAppContextSetup(webApplicationContext)
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
						MockMvcRequestBuilders.post("/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(JsonUtils.getJSON(loginReq)))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

		UserProfile userProfile = JsonUtils.getBytesAsObject(response
				.getResponse().getContentAsByteArray(), UserProfile.class);
		Assert.assertNotNull(userProfile);
	}

	@Test
	public void invalidUserPassword_ShouldReturnUnAuthorized() throws Exception {
        // As per the entry in test-security.xml
		LoginRequest loginReq = new LoginRequest();
		loginReq.setUsername("mike");
		loginReq.setPassword("1234");

		mockMVC.perform(
				MockMvcRequestBuilders.post("/login")
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
						MockMvcRequestBuilders.post("/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(JsonUtils.getJSON(loginReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(jsonPath("$.userRoles", hasSize(2))).andReturn();

		UserProfile userProfile = JsonUtils.getBytesAsObject(response
				.getResponse().getContentAsByteArray(), UserProfile.class);
		Assert.assertNotNull(userProfile.getUserRoles());
	}

	@Test
	public void whenLoggedInAsMike_ShouldBelongToAdminAndUserRoles()
			throws Exception {

        // As per the entry in test-security.xml
		LoginRequest loginReq = new LoginRequest();
		loginReq.setUsername("mike");
		loginReq.setPassword("123");

		mockMVC.perform(
				MockMvcRequestBuilders.post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(JsonUtils.getJSON(loginReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(jsonPath("$.userRoles[0]", is("ROLE_AB_ADMIN")))
				.andExpect(jsonPath("$.userRoles[1]", is("ROLE_AB_USER")))
				.andReturn();
	}

}
