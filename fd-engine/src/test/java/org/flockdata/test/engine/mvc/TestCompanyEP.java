/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import org.flockdata.engine.data.graph.CompanyNode;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 16/02/2015
 */
public class TestCompanyEP extends MvcBase {

  @Test
  public void companyLocators() throws Exception {

    Collection<CompanyNode> companies = findCompanies(mike());
    assertEquals(1, companies.size());
    CompanyNode listCompany = companies.iterator().next();
    CompanyNode foundCompany = getCompany(listCompany.getName(), mike());
    assertNotNull(foundCompany);
    assertEquals(null, listCompany.getId(), foundCompany.getId());
    boolean failed = findCompanyIllegal(foundCompany.getName(), noUser());
    assertTrue("Illegal user parsed in. This should not have worked", failed);
  }

  @Test
  public void locateCompanyByApiKey() throws Exception {
    Collection<CompanyNode> companies = findCompanies(mike());
    assertEquals(1, companies.size());
    CompanyNode listCompany = companies.iterator().next();
    CompanyNode foundCompany = getCompany(listCompany.getName(), mike());
    assertEquals(null, listCompany.getId(), foundCompany.getId());

    // ToDo: We have no need to look up a company by name. For this we need a company to company relationship.
    // Until that's in place there isn't much to test

//        setSecurity(sally_admin);
//        SystemUser suSally = registerSystemUser("coB123", sally_admin);
//
//        try {
//            org.junit.Assert.assertEquals("Sally's APIKey cannot see Mikes company record", null, findCompanyIllegal("coA123", suSally));
//            fail("Security Check failed");
//        } catch (FlockException e ){
//            // Illegal API key so this is good.
//        }
//        // Happy path
//        assertNotNull ( getCompany("coB123", suSally));
//        setSecurity(mike_admin);
//        try {
//            org.junit.Assert.assertEquals("Mike's APIKey cannot see Sally's company record", null, getCompany("coB123", suMike));
//            fail("Security Check failed");
//        } catch (FlockException e ){
//            // Illegal API key so this is good.
//        }
//        // Happy path
//        assertNotNull ( findCompanyIllegal("coA123", suMike));

  }

}
