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

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.TimeZone;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

@Transactional
public class TestRegistration extends TestEngineBase {



    private Logger logger = LoggerFactory.getLogger(TestRegistration.class);
    
    @Test
    public void createPersonsTest() throws DatagioException {
        createCompanyUsers("", 3);

    }

    private void createCompanyUsers(String userNamePrefix, int count) throws DatagioException {
        setSecurity();
        SystemUserResultBean su = registrationEP.registerSystemUser(new RegistrationBean("CompanyA", "mike").setIsUnique(false)).getBody();

/*        int i = 1;
        while (i <= count) {
            CompanyUser test = regService.addCompanyUser(userNamePrefix + i + "@sunnybell.com", su.getCompanyName());
            test = companyService.save(test);
            assertNotNull(test);
            i++;
        }
*/
    }

    @Test
    public void companyFortressNameSearch() throws Exception {
        String companyName = "Monowai";
        String adminName = "mike";

        // Create the company.
        setSecurity();
        SystemUserResultBean systemUser = registrationEP.registerSystemUser(new RegistrationBean(companyName, adminName).setIsUnique(false)).getBody();
        assertNotNull(systemUser);

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
        String companyName = "Monowai";
        String adminName = "mike";

        // Create the company.
        setSecurity();
        SystemUserResultBean systemUser = registrationEP.registerSystemUser(new RegistrationBean(companyName, "password", adminName).setIsUnique(false)).getBody();
        assertNotNull(systemUser);
        Collection<Company> companies = companyEP.findCompanies(systemUser.getApiKey(), null);
        assertEquals(1, companies.size());
        String cKey = companies.iterator().next().getApiKey();

        SystemUserResultBean systemUserB = registrationEP.registerSystemUser(new RegistrationBean(companyName.toLowerCase(), "password", "xyz").setIsUnique(false)).getBody();
        assertNotNull(systemUserB);

        companyEP.findCompanies(systemUserB.getApiKey(), null);

        assertEquals(1, companies.size());
        assertEquals("Company keys should be the same irrespective of name case create with", companies.iterator().next().getApiKey(), cKey);
    }

    @Test
    public void uniqueFortressesForDifferentCompanies() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        setSecurity("mike");

        // ToDo: Modify this call to use the new approach
        String apiKey = registrationEP.registerSystemUser(new RegistrationBean("CompanyAA", "mike").setIsUnique(false)).getBody().getApiKey();
        Company company = securityHelper.getCompany(apiKey);
        // ToDo: Modify this call to use the new approach
        // Should we be seeing ApiKeyInterceptor being invoked?
        //this.mockMvc = webAppContextSetup(this.wac).addFilter().build();

//        ResultActions response = mockMvc.perform(post("/fortress")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(getObjectAsJsonBytes(new FortressInputBean("FortressA", true)))
//        );
//        byte[] json =null;

//        Fortress fA = getBytesAsObject(json, FortressNode.class);

        FortressInputBean fib = new FortressInputBean("blah");
        byte[] json = getObjectAsJsonBytes(fib);
        fib = getBytesAsObject(json, FortressInputBean.class);

        assertNotNull (fib);
        assertEquals("blah", fib.getName());
        Fortress fA = fortressEP.registerFortress(new FortressInputBean("FortressA"), apiKey, apiKey).getBody();
        Fortress fB = fortressEP.registerFortress(new FortressInputBean("FortressB"), apiKey, apiKey).getBody();
        Fortress fC = fortressEP.registerFortress(new FortressInputBean("FortressC"), apiKey, apiKey).getBody();

        fortressEP.registerFortress(new FortressInputBean("FortressC"), apiKey, apiKey);// Forced duplicate should be ignored

        BDDMockito.when(request.getAttribute("company")).thenReturn(company);
        Collection<Fortress> fortresses = fortressEP.findFortresses(request);
        assertFalse(fortresses.isEmpty());
        assertEquals(3, fortresses.size());

        setSecurity(harry);
        registrationEP.registerSystemUser(new RegistrationBean("CompanyBB", harry));

        //Should be seeing different fortresses
        assertNotSame(fA.getId(), fortressService.registerFortress("FortressA").getId());
        assertNotSame(fB.getId(), fortressService.registerFortress("FortressB").getId());
        assertNotSame(fC.getId(), fortressService.registerFortress("FortressC").getId());

    }

    @Test
    public void companyLocators () throws Exception{
        setSecurity("mike");
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
        setSecurity("mike");
        String apiKeyMike = registrationEP.registerSystemUser(new RegistrationBean("coA123", "mike").setIsUnique(false)).getBody().getApiKey();

        Collection<Company> companies = companyService.findCompanies();
        assertEquals(1, companies.size());
        Company listCompany = companies.iterator().next();
        Company foundCompany = companyEP.getCompany(listCompany.getName(), apiKeyMike, apiKeyMike).getBody();
        assertEquals(null, listCompany.getId(), foundCompany.getId());

        setSecurity(sally);
        String apiKeySally = registrationEP.registerSystemUser(new RegistrationBean("coB123", "sally").setIsUnique(false)).getBody().getApiKey();

        try {
            assertEquals("Sally's APIKey cannot see Mikes company record", null, companyEP.getCompany("coA123", apiKeySally, apiKeySally));
            fail("Security Check failed");
        } catch (DatagioException e ){
            // Illegal API key so this is good.
        }
        // Happy path
        assertNotNull ( companyEP.getCompany("coB123", apiKeySally, apiKeySally));
        assertNotNull ( companyEP.getCompany("coB123", null, null));
        setSecurity(mike);
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
    public void testRegistration() throws DatagioException {
        String companyName = "testReg";
        String adminName = "mike";
        String userName = "gina@hummingbird.com";
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Create the company.
        SecurityContextHolder.getContext().setAuthentication(null);
        try {
            // Unauthenticated users can't register accounts
            SystemUserResultBean systemUser = registrationEP.registerSystemUser(new RegistrationBean(companyName, "password", adminName)).getBody();
            assertNotNull(systemUser);
        } catch (Exception e) {
            // this is good
        }

        // Assume the user has now logged in.
        setSecurity();
        SystemUserResultBean systemUser = registrationEP.registerSystemUser(new RegistrationBean(companyName, adminName)).getBody();
        assertNotNull(systemUser);

        Company company = securityHelper.getCompany(systemUser.getApiKey());
        BDDMockito.when(request.getAttribute("company")).thenReturn(company);
        
        FortressInputBean fib = new FortressInputBean("auditbucket");
        fib.setSearchActive(false);
        Fortress fortress = fortressEP.registerFortress(fib, systemUser.getApiKey(), systemUser.getApiKey()).getBody();
        assertNotNull(fortress);

        Collection<Fortress> fortressList = fortressEP.findFortresses(request);
        assertNotNull(fortressList);
        assertEquals(1, fortressList.size());

        Fortress foundFortress = fortressEP.getFortress("auditbucket", systemUser.getApiKey(), systemUser.getApiKey()).getBody();
        assertNotNull(foundFortress);
        assertEquals(HttpStatus.NOT_FOUND, fortressEP.getFortress("auditbucketzz", systemUser.getApiKey(), systemUser.getApiKey()).getStatusCode());

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
        FortressUser fu = fortressService.getFortressUser(fortress, "useRa");
        assertNotNull(fu);
        fu = fortressService.getFortressUser(fortress, "uAerb");
        assertNotNull(fu);
        fu = fortressService.getFortressUser(fortress, "Userc");
        assertNotNull(fu);

        fortress = fortressService.findByName("auditbucket");
        assertNotNull(fortress);

        fu = fortressService.getFortressUser(fortress, "useRax", false);
        assertNull(fu);
        fu = fortressService.getFortressUser(fortress, "userax");
        assertNotNull(fu);
        fu = fortressService.getFortressUser(fortress, "useRax");
        assertNotNull(fu);
        assertEquals(fu.getId(), fortressEP.getFortressUser(fortress.getName(), "userax", null, null).getBody().getId());
        assertEquals(HttpStatus.NOT_FOUND, fortressEP.getFortressUser(fortress.getName()+"zz", "userax", null, null).getStatusCode());
    }

    @Test
    public void twoDifferentCompanyFortressSameName() throws Exception {
        regService.registerSystemUser(new RegistrationBean("companya", "mike").setIsUnique(false));
        Fortress fortressA = fortressService.registerFortress("fortress-same");
        FortressUser fua = fortressService.getFortressUser(fortressA, "mike");

        setSecurity("harry");
        regService.registerSystemUser(new RegistrationBean("companyb", "harry").setIsUnique(false));
        Fortress fortressB = fortressService.registerFortress("fortress-same");
        FortressUser fub = fortressService.getFortressUser(fortressB, "mike");
        FortressUser fudupe = fortressService.getFortressUser(fortressB, "mike");

        assertNotSame("Fortress should be different", fortressA.getId(), fortressB.getId());
        assertNotSame("FortressUsers should be different", fua.getId(), fub.getId());
        assertEquals("FortressUsers should be the same", fub.getId(), fudupe.getId());
    }

    @Test
    public void companyNameCodeWithSpaces() throws DatagioException {
        String uid = "mike";
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
        String uid = "mike";
        regService.registerSystemUser(new RegistrationBean("Monowai", uid));
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
        String uname = "mike";
        // Assume the user has now logged in.
        String company = "MultiFortTest";
        regService.registerSystemUser(new RegistrationBean(company, uname));
        setSecurity(uname);

        Fortress fortress = fortressService.registerFortress("auditbucket");
        assertNotNull(fortress);
        FortressUser fu = fortressService.getFortressUser(fortress, uname);
        assertNotNull(fu);
        uid = fu.getId();
        fu = fortressService.getFortressUser(fortress, "MIKE");
        assertEquals(uid, fu.getId());
        fu = fortressService.getFortressUser(fortress, "MikE");
        assertEquals(uid, fu.getId());
    }

    @Test
    public void findCompanyByNullApiKey() throws Exception {
        String uname = "mike";
        // Assume the user has now logged in.
        String company = "MultiFortTest";
        regService.registerSystemUser(new RegistrationBean(company, uname));
        setSecurity();
        Collection<Company> co = companyEP.findCompanies(null, null);
        Assert.assertFalse(co.isEmpty());
    }


}
