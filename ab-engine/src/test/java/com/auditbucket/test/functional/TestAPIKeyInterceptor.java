package com.auditbucket.test.functional;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.auditbucket.authentication.handler.APIKeyInterceptor;
import com.auditbucket.registration.bean.RegistrationBean;

public class TestAPIKeyInterceptor extends TestEngineBase {

	@Autowired
	ApplicationContext context;

	MockHttpServletRequest request;

	MockHttpServletResponse response;

	APIKeyInterceptor apiKeyInterceptor;

	@Test
	public void GivenValidAPIKey_WhenCalledSecureAPI_ThenShouldBeAllowed() throws Exception {
		setSecurity(mike);
		String apiKey = regEP
				.registerSystemUser(new RegistrationBean(monowai, mike))
				.getBody().getApiKey();

		apiKeyInterceptor = (APIKeyInterceptor) context
				.getBean("apiKeyInterceptor");

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		request.setRequestURI("/company/hello");
		request.addParameter("apiKey", apiKey);
		boolean status = apiKeyInterceptor.preHandle(request, response, null);

		Assert.assertEquals(true, status);
		Assert.assertEquals("Monowai", request.getAttribute("company"));
	}

	@Test
	public void GivenInValidAPIKey_WhenCalledSecureAPI_ThenShouldNotBeAllowed() throws Exception {
		setSecurity(mike);

		apiKeyInterceptor = (APIKeyInterceptor) context
				.getBean("apiKeyInterceptor");

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		request.setRequestURI("/company/hello");
		request.addParameter("apiKey", "someKey");
		boolean status = apiKeyInterceptor.preHandle(request, response, null);

		Assert.assertEquals(false, status);
		Assert.assertEquals(null, request.getAttribute("company"));
	}


}
