package com.auditbucket.test.unit;

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.endpoint.FortressInputBean;
import com.auditbucket.registration.model.*;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.CompanyUser;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
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

    private IFortress createFortress(String name, ICompany ownedBy) {
        IFortress fortress = new Fortress(new FortressInputBean(name), ownedBy);
        fortress = fortressService.save(fortress);
        assertNotNull(fortress);
        return fortress;
    }

    String testCompanyName = "testco";

    private void createCompanyUsers(String userNamePrefix, int count) {
        ICompany company = companyService.save(new Company(testCompanyName));
        int i = 1;
        while (i <= count) {
            ICompanyUser test = new CompanyUser(userNamePrefix + i + "@sunnybell.com", company);

            test = companyService.save(test);
            assertNotNull(test);
            i++;
        }

    }

    @Test
    public void findByName() {
        createCompanyUsers("MTest", 3);
        String name = "mtest2@sunnybell.com";
        ICompanyUser p = companyService.getCompanyUser(testCompanyName, name);
        assertNotNull(p);
        assertEquals(name, p.getName());

    }

    private void createCompanies(int count) {
        int i = 1;

        while (i <= count) {
            ICompany test = new Company();
            test.setName(testCompanyName + i);
            test = companyService.save(test);
            assertNotNull(test);
            i++;
        }
    }


    @Test
    public void testCypherQuery() {
        createCompanyUsers("mike", 10);
        Iterable<ICompanyUser> users = companyService.getUsers(testCompanyName);
        assertTrue(users.iterator().hasNext());
        for (ICompanyUser friend : users) {
            System.out.println(friend.getName());
        }
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
        ISystemUser systemUser = registrationService.registerSystemUser(new RegistrationBean(companyName, adminName, "password"));
        assertNotNull(systemUser);

        // Assume the user has now logged in.
        SecurityContextHolder.getContext().setAuthentication(auth);
        ICompanyUser nonAdmin = registrationService.addCompanyUser(userName, companyName);
        assertNotNull(nonAdmin);

        IFortress fortress = fortressService.registerFortress("auditbucket");
        assertNotNull(fortress);

        List<IFortress> fortressList = fortressService.findFortresses(companyName);
        assertNotNull(fortressList);
        assertEquals(1, fortressList.size());


        ICompany company = companyService.findByName(companyName);
        assertNotNull(company);

        assertNotNull(systemUserService.findByName(adminName));
        assertNull(systemUserService.findByName(userName));
        // System registration are not automatically company registration
        //assertEquals(1, company.getCompanyUserCount());
        assertNotNull(companyService.getAdminUser(company, adminName));
        assertNull(companyService.getAdminUser(company, userName));

        // Add fortress User
        IFortressUser fu = fortressService.addFortressUser(fortress.getId(), "useRa");
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
