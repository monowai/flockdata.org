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

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 4:49 PM
 */

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditResultBean;
import com.auditbucket.audit.bean.AuditSummaryBean;
import com.auditbucket.audit.bean.AuditTagInputBean;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.audit.model.SearchChange;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.engine.service.AuditTagService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
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

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestAuditTags {
    @Autowired
    FortressService fortressService;

    @Autowired
    AuditManagerService auditManager;

    @Autowired
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    TagService tagService;

    @Autowired
    AuditTagService auditTagService;

    @Autowired
    private Neo4jTemplate template;

    //private Logger log = LoggerFactory.getLogger(TestAuditTags.class);

    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    private Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        if (!"http".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Autowired
    GraphDatabase graphDatabase;

    @Test
    public void tagAuditRecords() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        Company iCompany = iSystemUser.getCompany();

        TagInputBean flopTag = new TagInputBean("FLOP");

        tagService.processTag(flopTag);
        //assertNotNull(result);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());

        AuditTagInputBean auditTag = new AuditTagInputBean(resultBean.getAuditKey(), null, "!!!");
        try {
            auditTagService.processTag(header, auditTag);
            fail("No null argument exception detected");
        } catch (IllegalArgumentException ie) {
            // This should have happened
        }
        // First auditTag created
        auditTag = new AuditTagInputBean(header.getAuditKey(), flopTag.getName(), "ABC");

        auditTagService.processTag(header, auditTag);

        Boolean tagRlxExists = auditTagService.relationshipExists(header, flopTag.getName(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        auditTagService.processTag(header, auditTag);
        // Behaviour - Can't add the same tagValue twice for the same combo
        tagRlxExists = auditTagService.relationshipExists(header, flopTag.getName(), "ABC");
        assertTrue(tagRlxExists);
    }

    @Test
    public void renameRelationship() throws Exception {

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        Company iCompany = iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.setTag(new TagInputBean("TagA", "AAAA"));
        aib.setTag(new TagInputBean("TagB", "BBBB"));
        aib.setTag(new TagInputBean("TagC", "CCCC"));
        aib.setTag(new TagInputBean("TagD", "DDDD"));
        AuditResultBean resultBean = auditManager.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagSet = auditTagService.findAuditTags(auditHeader);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertFalse(auditTagService.relationshipExists(auditHeader, "TagC", "!!Twee!!"));//
        // Remove a single tag
        for (AuditTag value : tagSet) {
            if (value.getTag().getName().equals("TagC"))
                auditTagService.changeType(auditHeader, value, "!!Twee!!");
        }

        assertTrue(auditTagService.relationshipExists(auditHeader, "TagC", "!!Twee!!"));
    }

    @Test
    public void createAndDeleteAuditTags() throws Exception {

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        Company iCompany = iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        aib.setTag(new TagInputBean("TagA", "AAAA"));
        aib.setTag(new TagInputBean("TagB", "BBBB"));
        aib.setTag(new TagInputBean("TagC", "CCCC"));
        aib.setTag(new TagInputBean("TagD", "DDDD"));
        AuditResultBean resultBean = auditManager.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagSet = auditTagService.findAuditTags(auditHeader);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        // Remove a single tag
        for (AuditTag value : tagSet) {
            if (value.getTag().getName().equals("TagB"))
                auditTagService.deleteAuditTag(auditHeader, value);
        }
        tagSet = auditTagService.findAuditTags(auditHeader);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());
        // Ensure that the deleted tag is not in the results
        for (AuditTag auditTag : tagSet) {
            assertFalse(auditTag.getTag().getName().equals("TagB"));
        }
    }

    @Test
    public void nullTagValueCRUD() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        aib.setTag(new TagInputBean("TagA", null));
        aib.setTag(new TagInputBean("TagB", null));
        aib.setTag(new TagInputBean("TagC", null));
        aib.setTag(new TagInputBean("TagD", "DDDD"));
        AuditResultBean resultBean = auditManager.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagSet = auditTagService.findAuditTags(auditHeader);
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        auditService.updateHeader(auditHeader);
        auditHeader = auditService.getHeader(auditHeader.getAuditKey());
        AuditSummaryBean summaryBean = auditService.getAuditSummary(auditHeader.getAuditKey());
        tagSet = summaryBean.getTags();
        assertNotNull(tagSet);
        Set<AuditHeader> headers = auditTagService.findTagAudits("TagA");
        assertNotNull(headers);
        assertNotSame(headers.size() + " Audit headers returned!", 0, headers.size());

        assertEquals(auditHeader.getAuditKey(), headers.iterator().next().getAuditKey());
        headers = auditTagService.findTagAudits("TagC");
        assertNotNull(headers);
        assertEquals(auditHeader.getAuditKey(), headers.iterator().next().getAuditKey());
        headers = auditTagService.findTagAudits("TagD");
        assertNotNull(headers);
        assertEquals(auditHeader.getAuditKey(), headers.iterator().next().getAuditKey());
    }

    @Test
    public void duplicateTagNotCreated() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        Company iCompany = iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        aib.setTag(new TagInputBean("TagA", "camel"));
        aib.setTag(new TagInputBean("taga", "lower"));
        aib.setTag(new TagInputBean("tAgA", "mixed"));
        AuditResultBean resultBean = auditManager.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey());
        Tag tag = tagService.findTag("Taga");
        assertNotNull(tag);
        Set<AuditTag> auditTags = auditTagService.findAuditTags(auditHeader);
        for (AuditTag auditTag : auditTags) {
            assertEquals("Expected same tag for each relationship", tag.getId(), auditTag.getTag().getId());
        }
        assertEquals("Expected 3 relationships for the same tag", 3, auditTags.size());

    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        Company iCompany = iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addAuditLink("Type1");
        tag.addAuditLink("Type2");
        tag.addAuditLink("Type3");
        aib.setTag(tag);

        AuditResultBean resultBean = auditManager.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagSet = auditTagService.findAuditTags(auditHeader);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());

        AuditSummaryBean summaryBean = auditManager.getAuditSummary(auditHeader.getAuditKey());
        assertNotNull(summaryBean);
        assertEquals(3, summaryBean.getTags().size());

    }

    @Test
    public void documentTypesWork() {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        String docName = "CamelCaseDoc";
        DocumentType docType = tagService.resolveDocType(docName);
        assertNotNull(docType);
        assertEquals(docName.toLowerCase(), docType.getCode());
        assertEquals(docName, docType.getName());
        // Should be finding by code which is always Lower
        DocumentType sameDoc = tagService.resolveDocType(docType.getCode().toUpperCase());
        Assert.assertNotNull(sameDoc);
        assertEquals(sameDoc.getId(), docType.getId());

    }

    @Test
    public void tagListAndSingular() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addAuditLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.setTag(tagA);
        inputBean.setTag(tagB);

        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagResults = auditTagService.findAuditTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        AuditSummaryBean summaryBean = auditService.getAuditSummary(header.getAuditKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addAuditLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.setTag(tagA);
        inputBean.setTag(tagB);

        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagResults = auditTagService.findAuditTags(header);
        AuditSummaryBean summaryBean = auditService.getAuditSummary(header.getAuditKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        Map<String, Object> propA = new HashMap<>();
        Map<String, Object> propB = new HashMap<>();
        propA.put("myValue", 10);
        propB.put("myValue", 20);

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to", propA);
        tagA.addAuditLink("email-cc", propB);
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.setTag(tagA);
        inputBean.setTag(tagB);
        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagResults = auditTagService.findAuditTags(header);
        AuditSummaryBean summaryBean = auditService.getAuditSummary(header.getAuditKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.addAuditLink("email-to");
        tagInputBean.addAuditLink("email-to");

        inputBean.setTag(tagInputBean);

        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagResults = auditTagService.findAuditTags(header);
        // ToDo In Neo4j2 remove the generic tag
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addAuditLink("email cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.setTag(tagA);
        inputBean.setTag(tagB);

        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());
        Set<AuditTag> tagResults = auditTagService.findAuditTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        AuditSummaryBean summaryBean = auditService.getAuditSummary(header.getAuditKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void nestedStructureInHeader() throws Exception {

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);
        SecurityContextHolder.getContext().setAuthentication(authA);
        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean country = new TagInputBean("New Zealand");
        TagInputBean wellington = new TagInputBean("Wellington");
        TagInputBean auckland = new TagInputBean("Auckland");
        country.setTargets("capital-city", wellington);
        country.setTargets("city", auckland);
        TagInputBean section = new TagInputBean("Thorndon");
        wellington.setTargets("section", section);
        TagInputBean building = new TagInputBean("ABC House");
        section.setTargets("houses", building);

        inputBean.setTag(country);
        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        assertNotNull(resultBean);
        // Tags are not associated with the header rather the structure is enforced while importing
        Tag countryTag = tagService.findTag("New Zealand");
        Tag cityTag = tagService.findTag("Wellington");
        Tag sectionTag = tagService.findTag("Thorndon");
        Tag houseTag = tagService.findTag("ABC House");

        assertNotNull(countryTag);
        assertEquals(2, tagService.findDirectedTags(countryTag).size());  // Country has 2 cities
        assertNotNull(cityTag);
        assertEquals(1, tagService.findDirectedTags(cityTag).size());
        assertNotNull(sectionTag);
        assertEquals(1, tagService.findDirectedTags(sectionTag).size());
        assertNotNull(houseTag);
        assertEquals(0, tagService.findDirectedTags(houseTag).size());
    }

    @Test
    public void usGeographyStructure() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country);
        TagInputBean cityInputTag = new TagInputBean(city);
        TagInputBean stateInputTag = new TagInputBean("CA");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        inputBean.setTag(institutionTag);

        // Institution<-city<-state<-country

        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        assertNotNull(resultBean);
        Set<AuditTag> tags = auditTagService.findAuditTags(resultBean.getAuditHeader());
        assertFalse(tags.isEmpty());

        for (AuditTag tag : tags) {
            assertEquals("mikecorp", tag.getTag().getName());
            Collection<Tag> cities = tagService.findDirectedTags(tag.getTag());
            org.junit.Assert.assertFalse(cities.isEmpty());
            Tag cityTag = cities.iterator().next();
            assertEquals(cityInputTag.getName(), cityTag.getName());
            Collection<Tag> states = tagService.findDirectedTags(cityTag);
            assertFalse(states.isEmpty());
            Tag stateTag = states.iterator().next();
            assertEquals(stateInputTag.getName(), stateTag.getName());
        }

    }

    @Test
    public void tagsInSearchDoc() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country);
        TagInputBean cityInputTag = new TagInputBean(city);
        TagInputBean stateInputTag = new TagInputBean("CA");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "works");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        inputBean.setTag(institutionTag);

        // Institution<-city<-state<-country

        AuditResultBean resultBean = auditManager.createHeader(inputBean);
        assertNotNull(resultBean);
        Set<AuditTag> tags = auditTagService.findAuditTags(resultBean.getAuditHeader());
        assertFalse(tags.isEmpty());

        SearchChange searchChange = auditService.getSearchChange(resultBean, "Blah", new Date());
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void targetTagWithAuditRelationship() throws Exception {
        String name = "Doctor John";
        TagInputBean authorTag = new TagInputBean(name+":person");
        authorTag.addAuditLink("writer");
        authorTag.addAuditLink("lead");

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean countryTag = new TagInputBean("New Zealand");
        TagInputBean cityTag = new TagInputBean("Auckland");
        TagInputBean institution = new TagInputBean("Auckland University:Institution");
        cityTag.setTargets("jurisdiction", countryTag); // Auckland located in NZ

        institution.addAuditLink("located");
        cityTag.addAuditLink("city");

        inputBean.setTag(cityTag); // Not attached to audit
        inputBean.setTag(countryTag);
        inputBean.setTag(institution);

        AuditResultBean result = auditManager.createHeader(inputBean);
        assertNotNull ( result);
        Set<AuditTag> tags = auditTagService.findAuditTags(result.getAuditHeader());
        assertEquals(2, tags.size());
        for (AuditTag tag : tags) {
            assertTrue(tag.getTag().getName().equals(institution.getName())|| tag.getTag().getName().equals(cityTag.getName()));
        }

    }
    @Test
    public void geoTag() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        AuditHeaderInputBean auditBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA:Country";
        String city = "Los Angeles:City";

        TagInputBean countryInputTag = new TagInputBean(country);
        TagInputBean cityInputTag = new TagInputBean(city);
        TagInputBean stateInputTag = new TagInputBean("CA:State");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        auditBean.setTag(institutionTag);

        // Institution<-city<-state<-country

        AuditResultBean resultBean = auditManager.createHeader(auditBean);
        assertNotNull(resultBean);
        assertNotNull ( tagService.findTag("USA"));

        Set<AuditTag> tags = auditTagService.findAuditTags(resultBean.getAuditHeader());
        assertFalse(tags.isEmpty());

        for (AuditTag tag : tags) {
            assertEquals("mikecorp", tag.getTag().getName());
            assertNotNull ( tag.getGeoData());
            assertEquals("CA", tag.getGeoData().getState());
            assertEquals("Los Angeles", tag.getGeoData().getCity());
            Collection<Tag> cities = tagService.findDirectedTags(tag.getTag());
            org.junit.Assert.assertFalse(cities.isEmpty());
            Tag cityTag = cities.iterator().next();
            assertEquals(cityInputTag.getName(), cityTag.getName());
            Collection<Tag> states = tagService.findDirectedTags(cityTag);
            assertFalse(states.isEmpty());
            Tag stateTag = states.iterator().next();
            assertEquals(stateInputTag.getName(), stateTag.getName());
        }

    }

}
