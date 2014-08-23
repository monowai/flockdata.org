package com.auditbucket.test.functional;

import com.auditbucket.authentication.handler.APIKeyInterceptor;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Company;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static junit.framework.Assert.assertNotNull;

public class TestAPIKeyInterceptor extends TestEngineBase {

	@Autowired
	ApplicationContext context;

	MockHttpServletRequest request;

	MockHttpServletResponse response;

	APIKeyInterceptor apiKeyInterceptor;

	@Before
	public void initialize() {
		setSecurity(mike);
		apiKeyInterceptor = (APIKeyInterceptor) context
				.getBean("apiKeyInterceptor");

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

	}

	@Test
	public void givenValidAPIKey_WhenCallingSecureAPI_ThenShouldBeAllowed()
			throws Exception {
		String apiKey = regEP
				.registerSystemUser(new RegistrationBean(monowai, mike))
				.getBody().getApiKey();

		request.setRequestURI("/company/hello");
		request.addParameter("apiKey", apiKey);
		boolean status = apiKeyInterceptor.preHandle(request, response, null);

		Assert.assertEquals(true, status);
        Company company = (Company) request.getAttribute("company");
        assertNotNull (company);

		Assert.assertEquals("Monowai", company.getName());
	}

	@Test
	public void givenInValidAPIKey_WhenCallingSecureAPI_ThenShouldNotBeAllowed()
			throws Exception {

		request.setRequestURI("/company/hello");
		request.addParameter("apiKey", "someKey");
		boolean status = apiKeyInterceptor.preHandle(request, response, null);

		Assert.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
		Assert.assertEquals(false, status);
	}

	@Test
	public void givenNoAPIKey_WhenCallingSecureAPI_ThenShouldNotBeAllowed()
			throws Exception {

		request.setRequestURI("/company/hello");
		boolean status = apiKeyInterceptor.preHandle(request, response, null);

		Assert.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
		Assert.assertEquals(false, status);
	}

    // ToDo: add a disabled user check

    // ToDo: user is enabled but has no API key in AuditBucket - still valid, but results in a null company


	@After
	public void cleanUp() {
		setSecurityEmpty();
	}

}
