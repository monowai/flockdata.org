/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.test.functional;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;

@Transactional
public class TestRegistration extends TestEngineBase {
    @Test
    public void createPersonsTest() throws DatagioException {
        createCompanyUsers();

    }

    private void createCompanyUsers() throws DatagioException {
        setSecurity();
        regService.registerSystemUser(new RegistrationBean(monowai, mike_admin).setIsUnique(false));
    }

    @Test
    public void companyFortressNameSearch() throws Exception {
        // Create the company.
        setSecurity();
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin).setIsUnique(false));
        assertNotNull(su);

        fortressService.registerFortress("fortressA");
        fortressService.registerFortress("fortressB");
        fortressService.registerFortress("fortressC");
        fortressService.registerFortress("Fortress Space Name");

        int max = 100;
        for (int i = 0; i < max; i++) {
            Assert.assertNotNull(fortressService.findByName("fortressA"));
            Assert.assertNotNull(fortressService.findByName("fortressB"));
            Assert.assertNotNull(fortressService.findByName("fortressC"));
            Fortress fCode = fortressService.findByCode("fortressspacename");
            Assert.assertNotNull(fCode);
            assertEquals("Fortress Space Name", fCode.getName());
        }
    }

    @Test
    public void onlyOneCompanyCreatedWithMixedCase() throws Exception {

        // Create the company.
        setSecurity();
        SystemUser systemUser = regService.registerSystemUser(new RegistrationBean(monowai, "password", "user").setIsUnique(false));
        assertNotNull(systemUser);
        Collection<Company> companies = companyEP.findCompanies(systemUser.getApiKey(), null);
        assertEquals(1, companies.size());
        String cKey = companies.iterator().next().getApiKey();

        SystemUser systemUserB = regService.registerSystemUser(new RegistrationBean(monowai.toLowerCase(), "password", "xyz").setIsUnique(false));
        assertNotNull(systemUserB);

        companyEP.findCompanies(systemUserB.getApiKey(), null);

        assertEquals(1, companies.size());
        assertEquals("Company keys should be the same irrespective of name case create with", companies.iterator().next().getApiKey(), cKey);
    }

    @Test
    public void uniqueFortressesForDifferentCompanies() throws Exception {
        setSecurity("mike");

        SystemUser su = regService.registerSystemUser(new RegistrationBean("CompanyAA", mike_admin).setIsUnique(false));
        Company company = securityHelper.getCompany(su.getApiKey());

        //this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        Fortress fA = createFortress(su, "FortressA");
        Fortress fB = createFortress(su, "FortressB");
        Fortress fC = createFortress(su, "FortressC");

        createFortress(su, "FortressC");// Forced duplicate should be ignored

        Collection<Fortress> fortresses = findFortresses(su);
        assertFalse(fortresses.isEmpty());
        assertEquals(3, fortresses.size());

        setSecurity(sally_admin);
        registrationEP.registerSystemUser(new RegistrationBean("CompanyBB", harry)).getBody();
        // Switch to the newly created user
        setSecurity(harry);

        //BDDMockito.when(request.getAttribute("company")).thenReturn(company);

        //Should be seeing different fortresses
        assertNotSame(fA.getId(), fortressService.registerFortress("FortressA").getId());
        assertNotSame(fB.getId(), fortressService.registerFortress("FortressB").getId());
        assertNotSame(fC.getId(), fortressService.registerFortress("FortressC").getId());

    }

    @Test
    public void companyLocators () throws Exception{
        setSecurity(mike_admin);
        String apiKey = registrationEP.registerSystemUser(new RegistrationBean("companyLocators", "mike").setIsUnique(false)).getBody().getApiKey();

        Collection<Company> companies = companyService.findCompanies();
        assertEquals(1, companies.size());
        Company listCompany = companies.iterator().next();
        Company foundCompany = companyEP.getCompany(listCompany.getName(), apiKey, apiKey).getBody();
        assertEquals(null, listCompany.getId(), foundCompany.getId());
        try {
            assertEquals(null, companyEP.getCompany(foundCompany.getName(), "illegal", "illegal"));
            fail("Illegal API key parsed in. This should not have worked");
        } catch (DatagioException e ){
            // Illegal API key so this is good.
        }
        ResponseEntity re = companyEP.getCompany("IllegalCompany Name", apiKey, apiKey);
        assertEquals(HttpStatus.NOT_FOUND, re.getStatusCode());
        assertEquals(null, re.getBody());
    }

    @Test
    public void differentUsersCantAccessKnownCompany () throws Exception{
        setSecurity(mike_admin);
        String apiKeyMike = registrationEP.registerSystemUser(new RegistrationBean("coA123", mike_admin).setIsUnique(false)).getBody().getApiKey();

        Collection<Company> companies = companyService.findCompanies();
        assertEquals(1, companies.size());
        Company listCompany = companies.iterator().next();
        Company foundCompany = companyEP.getCompany(listCompany.getName(), apiKeyMike, apiKeyMike).getBody();
        assertEquals(null, listCompany.getId(), foundCompany.getId());

        setSecurity(sally_admin);
        String apiKeySally = registrationEP.registerSystemUser(new RegistrationBean("coB123", sally_admin).setIsUnique(false)).getBody().getApiKey();

        try {
            assertEquals("Sally's APIKey cannot see Mikes company record", null, companyEP.getCompany("coA123", apiKeySally, apiKeySally));
            fail("Security Check failed");
        } catch (DatagioException e ){
            // Illegal API key so this is good.
        }
        // Happy path
        assertNotNull ( companyEP.getCompany("coB123", apiKeySally, apiKeySally));
        assertNotNull ( companyEP.getCompany("coB123", null, null));
        setSecurity(mike_admin);
        try {
            assertEquals("Mike's APIKey cannot see Sally's company record", null, companyEP.getCompany("coB123", apiKeyMike, apiKeyMike));
            fail("Security Check failed");
        } catch (DatagioException e ){
            // Illegal API key so this is good.
        }
        // Happy path
        assertNotNull ( companyEP.getCompany("coA123", apiKeyMike, apiKeyMike));
        assertNotNull ( companyEP.getCompany("coA123", null, null));

    }

    @Test
    public void testRegistration() throws Exception {
        String companyName = "testReg";
        String adminName = "admin";
        String userName = "gina@hummingbird.com";


        // Create the company.
        setSecurityEmpty();
//        try {
//            // Unauthenticated users can't register accounts
//            regService.registerSystemUser(new RegistrationBean(companyName, userName, "Arbitrary Full Name"));
//            fail("logged in user check failed");
//        } catch (Exception e) {
//            // this is good
//        }

        // Now the user has now logged in.
        setSecurity(mike_admin);
        // So can create other users
        SystemUser systemUser = regService.registerSystemUser(new RegistrationBean(companyName, adminName).setIsUnique(false));
        assertNotNull(systemUser);

        Company company = securityHelper.getCompany(systemUser.getApiKey());
        
        Fortress fortress = createFortress(systemUser, "auditbucket");
        assertNotNull(fortress);

        Collection<Fortress> fortressList = findFortresses(systemUser);
        assertNotNull(fortressList);
        assertEquals(1, fortressList.size());

        Fortress foundFortress = fortressService.findByName(company, "auditbucket");
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
        FortressUser fu = fortressService.getFortressUser(fortress, "useRa");
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
        regService.registerSystemUser(new RegistrationBean("companya", mike_admin).setIsUnique(false));
        Fortress fortressA = fortressService.registerFortress("fortress-same");
        FortressUser fua = fortressService.getFortressUser(fortressA, mike_admin);

        setSecurity(sally_admin);
        regService.registerSystemUser(new RegistrationBean("companyb", harry).setIsUnique(false));
        setSecurity(harry);
        Fortress fortressB = fortressService.registerFortress("fortress-same");
        FortressUser fub = fortressService.getFortressUser(fortressB, mike_admin);
        FortressUser fudupe = fortressService.getFortressUser(fortressB, mike_admin);

        assertNotSame("Fortress should be different", fortressA.getId(), fortressB.getId());
        assertNotSame("FortressUsers should be different", fua.getId(), fub.getId());
        assertEquals("FortressUsers should be the same", fub.getId(), fudupe.getId());
    }

    @Test
    public void companyNameCodeWithSpaces() throws DatagioException {
        String uid = "user";
        String name = "Monowai Developments";
        SystemUser su = regService.registerSystemUser(new RegistrationBean(name, uid));
        assertNotNull(su);
        Company company = companyService.findByName(name);
        Assert.assertNotNull(company);
        assertEquals(name.replaceAll("\\s", "").toLowerCase(), company.getCode());

        Company comp = companyService.findByCode(company.getCode());
        assertNotNull(comp);
        assertEquals(comp.getId(), company.getId());

    }

    @Test
    public void fortressTZLocaleChecks() throws DatagioException {
        String uid = "user";
        regService.registerSystemUser(new RegistrationBean(monowai, uid));
        setSecurity(uid);
        // Null fortress
        Fortress fortressNull = fortressService.registerFortress(new FortressInputBean("wportfolio", true));
        assertNotNull(fortressNull.getLanguageTag());
        assertNotNull(fortressNull.getTimeZone());

        String testTimezone = TimeZone.getTimeZone("GMT").getID();
        assertNotNull(testTimezone);

        String languageTag = "en-GB";
        FortressInputBean fib = new FortressInputBean("uk-wp", true);
        fib.setLanguageTag(languageTag);
        fib.setTimeZone(testTimezone);
        Fortress custom = fortressService.registerFortress(fib);
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
        Fortress fResult = fortressService.registerFortress(fibNullSetter);
        assertNotNull("Language not set to the default", fResult.getLanguageTag());
        assertNotNull("TZ not set to the default", fResult.getTimeZone());


    }
  //TODO: Mike needs to refactor this
/*    @Test
    public void duplicateRegistrationFails() throws Exception {
        String companyA = "companya";
        String companyB = "companyb";
        try {
            engineAdmin.setDuplicateRegistration(false);
            registrationEP.registerSystemUser(new RegistrationBean(companyA, "mike"));
            registrationEP.registerSystemUser(new RegistrationBean(companyB, "mike"));
            Assert.fail("You can't have a duplicate registration");
        } catch (DatagioException e) {
            // Expected
        }

    }
*/
    @Test
    public void multipleFortressUserErrors() throws Exception {
        Long uid;
        String uname = "user";
        // Assume the user has now logged in.
        String company = "MultiFortTest";
        regService.registerSystemUser(new RegistrationBean(company, uname));
        setSecurity(uname);

        Fortress fortress = fortressService.registerFortress("auditbucket");
        assertNotNull(fortress);
        FortressUser fu = fortressService.getFortressUser(fortress, uname);
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
        regService.registerSystemUser(new RegistrationBean(company, mike_admin));
        setSecurity();
        Collection<Company> co = companyEP.findCompanies(null, null);
        Assert.assertFalse(co.isEmpty());
    }


}
