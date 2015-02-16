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

package org.flockdata.test.endpoint;

import org.flockdata.company.model.CompanyNode;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.functional.EngineBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created by mike on 16/02/15.
 */
@WebAppConfiguration
public class CompanyTestEP extends EngineBase {

    @Autowired
    WebApplicationContext wac;



    @Test
    public void companyLocators () throws Exception{
        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("companyLocators", "mike");

        EngineEndPoints eip = new EngineEndPoints(wac);

        Collection<CompanyNode> companies = eip.findCompanies(su);
        assertEquals(1, companies.size());
        Company listCompany = companies.iterator().next();
        Company foundCompany = eip.getCompany(listCompany.getName(), su);
        assertNotNull(foundCompany);
        assertEquals(null, listCompany.getId(), foundCompany.getId());
        try {
            su.setApiKey("illegal");
            boolean failed = eip.findCompanyIllegal(foundCompany.getName(), su);
            assertTrue("Illegal API key parsed in. This should not have worked", failed);
        } catch (FlockException e ){
            // Illegal API key so this is good.
        }
//        su = registerSystemUser("companyLocators", "mike");
  //      eip.getIllegalCompany("IllegalCompany Name", su);


    }

    @Test
    public void differentUsersCantAccessKnownCompany () throws Exception{
        setSecurity(mike_admin);
        SystemUser suMike = registerSystemUser("coA123", mike_admin);
        EngineEndPoints eip = new EngineEndPoints(wac);

        Collection<CompanyNode> companies = eip.findCompanies(suMike);
        assertEquals(1, companies.size());
        Company listCompany = companies.iterator().next();
        Company foundCompany = eip.getCompany(listCompany.getName(), suMike);
        assertEquals(null, listCompany.getId(), foundCompany.getId());

        // ToDo: We have no need to look up a company by name. For this we need a company to company relationship.
        // Until that's in place there isn't much to test

//        setSecurity(sally_admin);
//        SystemUser suSally = registerSystemUser("coB123", sally_admin);
//
//        try {
//            org.junit.Assert.assertEquals("Sally's APIKey cannot see Mikes company record", null, eip.findCompanyIllegal("coA123", suSally));
//            fail("Security Check failed");
//        } catch (FlockException e ){
//            // Illegal API key so this is good.
//        }
//        // Happy path
//        assertNotNull ( eip.getCompany("coB123", suSally));
//        setSecurity(mike_admin);
//        try {
//            org.junit.Assert.assertEquals("Mike's APIKey cannot see Sally's company record", null, eip.getCompany("coB123", suMike));
//            fail("Security Check failed");
//        } catch (FlockException e ){
//            // Illegal API key so this is good.
//        }
//        // Happy path
//        assertNotNull ( eip.findCompanyIllegal("coA123", suMike));

    }

}
