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

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.TimeZone;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.RegistrationBean;
import org.junit.Assert;
import org.junit.Test;

public class TestRegistration extends EngineBase {

    @Test
    public void createPersonsTest() throws Exception {
        setSecurity();
        registerSystemUser("createPersonsTest", mike_admin);


    }

    @Test
    public void companyFortressNameSearch() throws Exception {
        // Create the company.
        setSecurity("mike");
        SystemUser su = registerSystemUser("companyFortressNameSearch", mike_admin);
        assertNotNull(su);

        fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortressA"));
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortressB"));
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortressC"));
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("Fortress Space Name"));

        int max = 100;
        for (int i = 0; i < max; i++) {
            assertNotNull(fortressService.findByName(su.getCompany(), "fortressA"));
            assertNotNull(fortressService.findByName(su.getCompany(), "fortressB"));
            assertNotNull(fortressService.findByName(su.getCompany(), "fortressC"));
            FortressNode fCode = fortressService.findByCode(su.getCompany(), "fortressspacename");
            assertNotNull(fCode);
            assertEquals("Fortress Space Name", fCode.getName());
        }
    }

    @Test
    public void onlyOneCompanyCreatedWithMixedCase() throws Exception {

        // Create the company.
        setSecurity();
        String companyName = "onlyOneCompanyCreatedWithMixedCase";
        Company company = companyService.create(companyName);
        SystemUser systemUser = regService.registerSystemUser(company,
            RegistrationBean.builder().companyName(companyName).login("password").name("user").unique(false).build());
        assertNotNull(systemUser);
        Collection<Company> companies = companyService.findCompanies(systemUser.getApiKey());
        assertEquals(1, companies.size());
        String cKey = companies.iterator().next().getApiKey();
        Company companyB = companyService.create(companyName.toLowerCase());
        SystemUser systemUserB = regService.registerSystemUser(companyB, RegistrationBean.builder()
            .companyName(companyName.toLowerCase())
            .login("password")
            .name("xyz")
            .unique(false)
            .build());
        assertNotNull(systemUserB);

        companyService.findCompanies(systemUserB.getApiKey());

        assertEquals(1, companies.size());
        assertEquals("Company keys should be the same irrespective of name case create with", companies.iterator().next().getApiKey(), cKey);
    }

    @Test
    public void uniqueFortressesForDifferentCompanies() throws Exception {
        setSecurity("mike");

        SystemUser su = registerSystemUser("uniqueFortressesForDifferentCompanies", "jimmy");
        Company company = su.getCompany();

        //this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        FortressNode fA = fortressService.registerFortress(company, new FortressInputBean("FortressA"));
        FortressNode fB = fortressService.registerFortress(company, new FortressInputBean("FortressB"));
        FortressNode fC = fortressService.registerFortress(company, new FortressInputBean("FortressC"));

        fortressService.registerFortress(company, new FortressInputBean("FortressC")); // Forced duplicate should be ignored

        Collection<FortressResultBean> fortresses = fortressService.findFortresses(company);
        assertFalse(fortresses.isEmpty());
        assertEquals(3, fortresses.size());

        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser("uniqueFortressesForDifferentCompaniesBB", harry);
        // Switch to the newly created user
        setSecurity(harry);

        //BDDMockito.when(request.getAttribute("company")).thenReturn(company);

        //Should be seeing different fortresses
        assertNotSame(fA.getId(), fortressService.registerFortress(suB.getCompany(), new FortressInputBean("FortressA")).getId());
        assertNotSame(fB.getId(), fortressService.registerFortress(suB.getCompany(), new FortressInputBean("FortressB")).getId());
        assertNotSame(fC.getId(), fortressService.registerFortress(suB.getCompany(), new FortressInputBean("FortressC")).getId());

    }

    @Test
    public void testRegistration() throws Exception {
        String companyName = "testReg";
        String adminName = "admin";
        String userName = "gina@hummingbird.com";


        // Create the company.
        setUnauthorized();

        // Now the user has now logged in.
        setSecurity(mike_admin);
        // So can create other users
        SystemUser systemUser = registerSystemUser(companyName, adminName);
        assertNotNull(systemUser);

        Company company = securityHelper.getCompany(systemUser.getApiKey());

        FortressNode fortress = fortressService.registerFortress(company, new FortressInputBean("auditbucket"));
        assertNotNull(fortress);

        Collection<FortressResultBean> fortressList = fortressService.findFortresses(company);
        assertNotNull(fortressList);
        assertEquals(1, fortressList.size());

        FortressNode foundFortress = fortressService.findByName(company, "auditbucket");
        assertNotNull(foundFortress);
        assertNull(fortressService.findByName(company, "auditbucketzz"));

        assertNotNull(company);
        assertNotNull(company.getApiKey());
        Long companyId = company.getId();
        company = companyService.findByApiKey(company.getApiKey());
        assertNotNull(company);
        assertEquals(companyId, company.getId());
        assertNotNull(systemUserService.findByLogin(adminName));
        assertNull(systemUserService.findByLogin(userName));

        assertNotNull(companyService.getAdminUser(company, adminName));
        assertNull(companyService.getAdminUser(company, userName));

        // Add fortress User
        fortress.setCompany(company);
        FortressUserNode fu = fortressService.getFortressUser(fortress, "useRa");
        assertNotNull(fu);
        fu = fortressService.getFortressUser(company, fortress.getName(), "uAerb");
        assertNotNull("Case insensitive search failed", fu);
        fu = fortressService.getFortressUser(company, fortress.getName(), "Userc");
        assertNotNull("Case insensitive search failed", fu);

        fortress = fortressService.findByName(company, "auditbucket");
        assertNotNull(fortress);

        fu = fortressService.getFortressUser(fortress, "useRax", false);
        assertNull(fu);
        fu = fortressService.getFortressUser(company, fortress.getName(), "userax");
        assertNotNull(fu);
        fu = fortressService.getFortressUser(company, fortress.getName(), "useRax");
        assertNotNull(fu);
        assertEquals(fu.getId(), fortressService.getFortressUser(company, fortress.getName(), "userax").getId());
//        assertEquals(HttpStatus.NOT_FOUND, fortressEP.getFortressUser(fortress.getName()+"zz", "userax", null, null).getStatusCode());
    }

    @Test
    public void twoDifferentCompanyFortressSameName() throws Exception {
        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("companya", mike_admin);
        FortressNode fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortress-same", true));
        FortressUserNode fua = fortressService.getFortressUser(fortressA, mike_admin);

        setSecurity(sally_admin);
        SystemUser suB = registerSystemUser("companyb", harry);
        setSecurity(harry);
        FortressNode fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("fortress-same"));
        FortressUserNode fub = fortressService.getFortressUser(fortressB, mike_admin);
        FortressUserNode fudupe = fortressService.getFortressUser(fortressB, mike_admin);

        assertNotSame("Fortress should be different", fortressA.getId(), fortressB.getId());
        assertNotSame("FortressUsers should be different", fua.getId(), fub.getId());
        assertEquals("FortressUsers should be the same", fub.getId(), fudupe.getId());
    }

    @Test
    public void companyNameCodeWithSpaces() throws Exception {
        String uid = "user";
        String name = "Monowai Developments";
        SystemUser su = registerSystemUser(name, uid);
        assertNotNull(su);
        Company company = companyService.findByName(name);
        assertNotNull(company);
        assertEquals(name.replaceAll("\\s", "").toLowerCase(), company.getCode());

        Company comp = companyService.findByCode(company.getCode());
        assertNotNull(comp);
        assertEquals(comp.getId(), company.getId());

    }

    @Test
    public void fortressTZLocaleChecks() throws Exception {
        String uid = "user";
        SystemUser su = registerSystemUser("fortressTZLocaleChecks", uid);
        setSecurity(uid);
        // Null fortress
        FortressNode fortressNull = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
        assertNotNull(fortressNull.getLanguageTag());
        assertNotNull(fortressNull.getTimeZone());

        String testTimezone = TimeZone.getTimeZone("GMT").getID();
        assertNotNull(testTimezone);

        String languageTag = "en-GB";
        FortressInputBean fib = new FortressInputBean("uk-wp", true);
        fib.setLanguageTag(languageTag);
        fib.setTimeZone(testTimezone);
        FortressNode custom = fortressService.registerFortress(su.getCompany(), fib);
        assertEquals(languageTag, custom.getLanguageTag());
        assertEquals(testTimezone, custom.getTimeZone());

        try {
            FortressInputBean fibError = new FortressInputBean("uk-wp", true);
            fibError.setTimeZone("Rubbish!");
            fail("No exception thrown for an illegal timezone");
        } catch (IllegalArgumentException e) {
            // This is what we expected
        }

        try {
            FortressInputBean fibError = new FortressInputBean("uk-wp", true);
            fibError.setLanguageTag("Rubbish!");
            fail("No exception thrown for an illegal languageTag");
        } catch (IllegalArgumentException e) {
            // This is what we expected
        }
        FortressInputBean fibNullSetter = new FortressInputBean("uk-wp", true);
        fibNullSetter.setLanguageTag(null);
        fibNullSetter.setTimeZone(null);
        FortressNode fResult = fortressService.registerFortress(su.getCompany(), fibNullSetter);
        assertNotNull("Language not set to the default", fResult.getLanguageTag());
        assertNotNull("TZ not set to the default", fResult.getTimeZone());


    }

    @Test
    public void multipleFortressUserErrors() throws Exception {
        Long uid;
        String uname = "user";
        // Assume the user has now logged in.
        String company = "MultiFortTest";
        SystemUser su = registerSystemUser(company, uname);
        setSecurity(uname);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditbucket"));
        assertNotNull(fortress);
        FortressUserNode fu = fortressService.getFortressUser(fortress, uname);
        assertNotNull(fu);
        uid = fu.getId();
        fu = fortressService.getFortressUser(fortress, "USER");
        assertEquals(uid, fu.getId());
        fu = fortressService.getFortressUser(fortress, "UsEr");
        assertEquals(uid, fu.getId());
    }

    @Test
    public void findCompanyByNullApiKey() throws Exception {
        setSecurity(mike_admin);
        // Assume the user has now logged in.
        String company = "MultiFortTest";
        registerSystemUser(company, mike_admin);
        setSecurity();
        Collection<Company> co = companyEP.findCompanies(null, null);
        Assert.assertFalse(co.isEmpty());
    }

    @Test
    public void defaults_FortressBooleanValues() throws Exception {
        setSecurity(mike_admin);
        // Assume the user has now logged in.
        String company = "defaults_FortressBooleanValues";
        SystemUser su = registerSystemUser(company, mike_admin);
        FortressNode f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestName", true));
        assertTrue(f.isEnabled());
        assertFalse(f.isSystem());

    }

    @Test
    public void delete_Fortress() throws Exception {
        setSecurity();
        String company = "defaults_FortressBooleanValues";
        SystemUser su = registerSystemUser(company, mike_admin);
        FortressNode f = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestName", true));
        assertNotNull(f);
        assertEquals("Deleting by name should work as it will resolve to the code", "OK", fortressService.delete(su.getCompany(), f.getName()));
    }
}
