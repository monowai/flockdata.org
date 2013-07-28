/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.*;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.CompanyUserNode;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.SystemUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.lang.System;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestRegistration {


    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private FortressService fortressService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private Neo4jTemplate template;

    String uid = "mike@sunnybell.com";
    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(auth);
        Neo4jHelper.cleanDb(template);
    }

    @Test
    public void createPersonsTest() {
        createCompanyUsers("", 3);

    }

    private Fortress createFortress(String name, Company ownedBy) {
        Fortress fortress = new FortressNode(new FortressInputBean(name), ownedBy);
        fortress = fortressService.save(fortress);
        assertNotNull(fortress);
        return fortress;
    }

    String testCompanyName = "testco";

    private void createCompanyUsers(String userNamePrefix, int count) {
        Company company = companyService.save(new CompanyNode(testCompanyName));
        int i = 1;
        while (i <= count) {
            CompanyUser test = new CompanyUserNode(userNamePrefix + i + "@sunnybell.com", company);

            test = companyService.save(test);
            assertNotNull(test);
            i++;
        }

    }

    @Test
    public void findByName() {
        createCompanyUsers("MTest", 3);
        String name = "mtest2@sunnybell.com";
        CompanyUser p = companyService.getCompanyUser(testCompanyName, name);
        assertNotNull(p);
        assertEquals(name, p.getName());

    }

    private void createCompanies(int count) {
        int i = 1;

        while (i <= count) {
            Company test = new CompanyNode();
            test.setName(testCompanyName + i);
            test = companyService.save(test);
            assertNotNull(test);
            i++;
        }
    }


    @Test
    public void testCompanyUsers() {
        createCompanyUsers("mike", 10);
        Iterable<CompanyUser> users = companyService.getUsers(testCompanyName);
        assertTrue(users.iterator().hasNext());
    }

    @Test
    public void testFulltextIndex() {
        createCompanyUsers("mike", 3);


        Index<PropertyContainer> index = template.getIndex("companyUserName");
        IndexHits<PropertyContainer> indexHits = index.query("name", "Test*");
        for (PropertyContainer c : indexHits) {
            String name = (String) c.getProperty("name");
            System.out.println(name);
        }
    }

    @Test
    public void testRegistration() {
        String companyName = "Monowai";
        String adminName = "mike@sunnybell.com";
        String userName = "gina@hummingbird.com";

        // Create the company.
        SecurityContextHolder.getContext().setAuthentication(null);
        SystemUser systemUser = registrationService.registerSystemUser(new RegistrationBean(companyName, adminName, "password"));
        assertNotNull(systemUser);

        // Assume the user has now logged in.
        SecurityContextHolder.getContext().setAuthentication(auth);
        CompanyUser nonAdmin = registrationService.addCompanyUser(userName, companyName);
        assertNotNull(nonAdmin);

        Fortress fortress = fortressService.registerFortress("auditbucket");
        assertNotNull(fortress);

        List<Fortress> fortressList = fortressService.findFortresses(companyName);
        assertNotNull(fortressList);
        assertEquals(1, fortressList.size());


        Company company = companyService.findByName(companyName);
        assertNotNull(company);

        assertNotNull(systemUserService.findByName(adminName));
        assertNull(systemUserService.findByName(userName));
        // SystemId registration are not automatically company registration
        //assertEquals(1, company.getCompanyUserCount());
        assertNotNull(companyService.getAdminUser(company, adminName));
        assertNull(companyService.getAdminUser(company, userName));

        // Add fortress User
        FortressUser fu = fortressService.addFortressUser(fortress.getId(), "useRa");
        assertNotNull(fu);
        fu = fortressService.addFortressUser(fortress.getId(), "uAerb");
        assertNotNull(fu);
        fu = fortressService.addFortressUser(fortress.getId(), "Userc");
        assertNotNull(fu);

        fortress = fortressService.find("auditbucket");
        assertNotNull(fortress);

        fu = fortressService.getFortressUser(fortress, "useRa", false);
        assertNull(fu);
        fu = fortressService.getFortressUser(fortress, "usera");
        assertNotNull(fu);


    }

}
