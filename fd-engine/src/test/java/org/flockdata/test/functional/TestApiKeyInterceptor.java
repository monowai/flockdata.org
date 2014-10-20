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

package org.flockdata.test.functional;

import org.flockdata.authentication.handler.ApiKeyInterceptor;
import org.flockdata.registration.model.Company;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public class TestApiKeyInterceptor extends EngineBase {

	@Autowired
	ApplicationContext context;

	MockHttpServletRequest request;

	MockHttpServletResponse response;

	ApiKeyInterceptor apiKeyInterceptor;

	@Before
	public void initialize() {
		setSecurity(mike_admin);
		apiKeyInterceptor = (ApiKeyInterceptor) context
				.getBean("apiKeyInterceptor");

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

	}

	@Test
	public void givenValidAPIKey_WhenCallingSecureAPI_ThenShouldBeAllowed()
			throws Exception {
        String companyName = "SecAPI";
		String apiKey = registerSystemUser(companyName, "abc123")
				.getApiKey();

		request.setRequestURI("/fortress/");
		request.addHeader("Api-Key", apiKey);
		boolean status = apiKeyInterceptor.preHandle(request, response, null);

		Assert.assertEquals(true, status);
        Company company = (Company) request.getAttribute("company");
        assertNotNull (company);

		Assert.assertEquals(companyName, company.getName());
	}

	@Test
	public void givenInValidAPIKey_WhenCallingSecureAPI_ThenShouldNotBeAllowed()
			throws Exception {

		request.setRequestURI("/fortress/");
		request.addHeader("Api-Key", "someKey");
        boolean status = false;
        try {
            status = apiKeyInterceptor.preHandle(request, response, null);
            fail();
        } catch (SecurityException se){

        }
	}

	@Test
	public void givenNoAPIKey_WhenCallingSecureAPI_ThenShouldNotBeAllowed()
			throws Exception {
        setSecurity(sally_admin); // Sally is Authorised but has not API Key
		request.setRequestURI("/fortress/");
        try {
            apiKeyInterceptor.preHandle(request, response, null);
            fail();
        } catch (SecurityException se){
            // Good stuff
        }

	}

    // ToDo: add a disabled user check

    // ToDo: user is enabled but has no API key in AuditBucket - still valid, but results in a null company


	@After
	public void cleanUp() {
		setSecurityEmpty();
	}

}
