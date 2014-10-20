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

import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.RegistrationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 20/09/14
 * Time: 8:57 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:root-context-neo-rest.xml" })
public class TestNeoRestInterface {
    static Logger logger = LoggerFactory.getLogger(TestNeoRestInterface.class);
    @Autowired
    CompanyService companyService;

    @Autowired
    RegistrationService registrationService;

    @Test
    public void neo4j_EnsureRestAPIWorks() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "mike", "123");

        SecurityContextHolder.getContext().setAuthentication(auth);

        // Create something to find
        registerSystemUser("mike", "testco");

        // Now find something - anything
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = getHttpHeaders();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        String query = "{ \"statements\":[{\"statement\":\"MATCH (n) RETURN n LIMIT 10\"}]}";
        HttpEntity<String> requestEntity = new HttpEntity<>(query, httpHeaders);

        Map result = restTemplate.exchange("http://localhost:7474/db/data/transaction/commit/", HttpMethod.POST, requestEntity, Map.class).getBody();

        assertNotNull(result.get("results"));
        Collection values = (Collection) result.get("results");
        assertFalse("No results returned from the REST call", values.size()==0);

    }

    public static HttpHeaders getHttpHeaders() {

        return new HttpHeaders() {
            {
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
            }
        };
    }

    public SystemUser registerSystemUser(String companyName, String accessUser) throws Exception{
        Company company = companyService.findByName(companyName);
        if ( company == null ) {
            logger.debug("Creating company {}", companyName);
            company = companyService.create(companyName);
        }
        SystemUser su = registrationService.registerSystemUser(company, new RegistrationBean( accessUser).setIsUnique(false));
//        SystemUser su = regService.registerSystemUser(company, new RegistrationBean(companyName, accessUser).setIsUnique(false));
        logger.debug("Returning SU {}", su);
        return su;
    }

}
