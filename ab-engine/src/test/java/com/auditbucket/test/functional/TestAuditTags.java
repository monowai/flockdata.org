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

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 4:49 PM
 */

import com.auditbucket.audit.bean.*;
import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.audit.model.SearchChange;
import com.auditbucket.audit.model.TrackTag;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TagTrackService;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
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
import static junit.framework.Assert.assertNull;
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
    MediationFacade auditManager;

    @Autowired
    TrackService trackService;

    @Autowired
    RegistrationService regService;

    @Autowired
    TagService tagService;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    TrackEP trackEp;

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

        TagInputBean flopTag = new TagInputBean("FLOP");

        tagService.processTag(flopTag);
        //assertNotNull(result);
        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());

        TrackTagInputBean auditTag = new TrackTagInputBean(resultBean.getMetaKey(), null, "!!!");
        try {
            tagTrackService.processTag(header, auditTag);
            fail("No null argument exception detected");
        } catch (IllegalArgumentException ie) {
            // This should have happened
        }
        // First trackTag created
        auditTag = new TrackTagInputBean(header.getMetaKey(), flopTag.getName(), "ABC");

        tagTrackService.processTag(header, auditTag);

        Boolean tagRlxExists = tagTrackService.relationshipExists(header, flopTag.getName(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        tagTrackService.processTag(header, auditTag);
        // Behaviour - Can't add the same tagValue twice for the same combo
        tagRlxExists = tagTrackService.relationshipExists(header, flopTag.getName(), "ABC");
        assertTrue(tagRlxExists);
    }

    @Test
    public void renameRelationship() throws Exception {

        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.setTag(new TagInputBean("TagA", "AAAA"));
        aib.setTag(new TagInputBean("TagB", "BBBB"));
        aib.setTag(new TagInputBean("TagC", "CCCC"));
        aib.setTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = auditManager.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertFalse(tagTrackService.relationshipExists(metaHeader, "TagC", "!!Twee!!"));//
        // Remove a single tag
        for (TrackTag value : tagSet) {
            if (value.getTag().getName().equals("TagC"))
                tagTrackService.changeType(metaHeader, value, "!!Twee!!");
        }

        assertTrue(tagTrackService.relationshipExists(metaHeader, "TagC", "!!Twee!!"));
    }

    @Test
    public void createAndDeleteAuditTags() throws Exception {

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        aib.setTag(new TagInputBean("TagA", "AAAA"));
        aib.setTag(new TagInputBean("TagB", "BBBB"));
        aib.setTag(new TagInputBean("TagC", "CCCC"));
        aib.setTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = auditManager.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        // Remove a single tag
        for (TrackTag value : tagSet) {
            if (value.getTag().getName().equals("TagB"))
                tagTrackService.deleteTrackTags(metaHeader, value);
        }
        tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());
        // Ensure that the deleted tag is not in the results
        for (TrackTag trackTag : tagSet) {
            assertFalse(trackTag.getTag().getName().equals("TagB"));
        }
    }

    @Test
    public void nullTagValueCRUD() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        aib.setTag(new TagInputBean("TagA", null));
        aib.setTag(new TagInputBean("TagB", null));
        aib.setTag(new TagInputBean("TagC", null));
        aib.setTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = auditManager.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        trackService.updateHeader(metaHeader);
        metaHeader = trackService.getHeader(metaHeader.getMetaKey());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(metaHeader.getMetaKey(), null);
        tagSet = summaryBean.getTags();
        assertNotNull(tagSet);
        Set<MetaHeader> headers = tagTrackService.findTrackTags("TagA");
        assertNotNull(headers);
        assertNotSame(headers.size() + " Audit headers returned!", 0, headers.size());

        assertEquals(metaHeader.getMetaKey(), headers.iterator().next().getMetaKey());
        headers = tagTrackService.findTrackTags("TagC");
        assertNotNull(headers);
        assertEquals(metaHeader.getMetaKey(), headers.iterator().next().getMetaKey());
        headers = tagTrackService.findTrackTags("TagD");
        assertNotNull(headers);
        assertEquals(metaHeader.getMetaKey(), headers.iterator().next().getMetaKey());
    }

    @Test
    public void duplicateTagNotCreated() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        aib.setTag(new TagInputBean("TagA", "camel"));
        aib.setTag(new TagInputBean("taga", "lower"));
        aib.setTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = auditManager.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Tag tag = tagService.findTag("Taga");
        assertNotNull(tag);
        Set<TrackTag> trackTags = tagTrackService.findTrackTags(metaHeader);
        for (TrackTag trackTag : trackTags) {
            assertEquals("Expected same tag for each relationship", tag.getId(), trackTag.getTag().getId());
        }
        assertEquals("Expected 3 relationships for the same tag", 3, trackTags.size());

    }

    @Test
    public void noTrackTagsAreReturned() throws Exception {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress(new FortressInputBean("ABC"));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.setTrackSuppressed(true);
        // This should create the same Tag object
        aib.setTag(new TagInputBean("TagA", "camel"));
        aib.setTag(new TagInputBean("taga", "lower"));
        aib.setTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = auditManager.createHeader(aib, null);
        assertEquals(1, resultBean.getTags().size());
        assertNull(resultBean.getMetaKey());

    }

    @Test
    public void createLogForInvalidHeader() throws Exception{
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress(new FortressInputBean("ABC"));

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        trackEp.trackHeader(aib, null, null).getBody();
        LogInputBean alib = new LogInputBean("InvalidKey", "Harry", new DateTime(),"{\"xx\":1}");
        try {
            trackEp.trackLog(alib, null, null);
            fail("Invalid audit header. This should not have worked");
        } catch (DatagioException e ){
            // Good stuff
        }

    }
    @Test
    public void createLogForValidHeaderWithNoWhatDetail() throws Exception{
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress(new FortressInputBean("ABC"));

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TrackResultBean rb = trackEp.trackHeader(aib, null, null).getBody();
        LogInputBean alib = new LogInputBean(rb.getMetaKey(), "Harry", new DateTime(),null);
        trackEp.trackLog(alib, null, null).getBody();


    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addAuditLink("Type1");
        tag.addAuditLink("Type2");
        tag.addAuditLink("Type3");
        aib.setTag(tag);

        TrackResultBean resultBean = auditManager.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());

        String apiKey = iSystemUser.getCompany().getApiKey();
        TrackedSummaryBean summaryBean = trackEp.getAuditSummary(metaHeader.getMetaKey(),apiKey, apiKey ).getBody();
        assertNotNull(summaryBean);
        assertEquals(3, summaryBean.getTags().size());

    }

    @Test
    public void documentTypesWork() {
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        Fortress fortress = fortressService.registerFortress("ABC");

        String docName = "CamelCaseDoc";
        DocumentType docType = tagService.resolveDocType(fortress, docName); // Creates if missing
        assertNotNull(docType);
        assertEquals(docName.toLowerCase(), docType.getCode());
        assertEquals(docName, docType.getName());
        // Should be finding by code which is always Lower
        Assert.assertNotNull(tagService.resolveDocType(fortress, docType.getCode().toUpperCase(), false));

    }

    @Test
    public void duplicateDocumentTypes() throws Exception {
        String mark = "mark@monowai.com";
        Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "user1");

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        Assert.assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("duplicateDocumentTypes");

        DocumentType dType = tagService.resolveDocType(fortress, "ABC123", true);
        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = tagService.resolveDocType(fortress, "ABC123", false);
        assertEquals(id, dType.getId());

        // Company 2 gets a different tag with the same name
        SecurityContextHolder.getContext().setAuthentication(authMark);
        regService.registerSystemUser(new RegistrationBean("secondcompany", mark, "bah"));
        // Same fortress name, but different company
        dType = tagService.resolveDocType(fortressService.registerFortress("duplicateDocumentTypes"), "ABC123"); // Creates if missing
        Assert.assertNotNull(dType);
        Assert.assertNotSame(id, dType.getId());
    }


    @Test
    public void tagListAndSingular() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addAuditLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.setTag(tagA);
        inputBean.setTag(tagB);

        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(header.getMetaKey(), null);
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addAuditLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.setTag(tagA);
        inputBean.setTag(tagB);

        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(header.getMetaKey(), null);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        Map<String, Object> propA = new HashMap<>();
        Map<String, Object> propB = new HashMap<>();
        propA.put("myValue", 10);
        propB.put("myValue", 20);

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to", propA);
        tagA.addAuditLink("email-cc", propB);
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.setTag(tagA);
        inputBean.setTag(tagB);
        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(header.getMetaKey(), null);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.addAuditLink("email-to");
        tagInputBean.addAuditLink("email-to");

        inputBean.setTag(tagInputBean);

        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        // ToDo In Neo4j2 remove the generic tag
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addAuditLink("email cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.setTag(tagA);
        inputBean.setTag(tagB);

        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(header.getMetaKey(), null);
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void nestedStructureInHeader() throws Exception {

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);
        SecurityContextHolder.getContext().setAuthentication(authA);
        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
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
        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
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

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
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

        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        assertNotNull(resultBean);
        Set<TrackTag> tags = tagTrackService.findTrackTags(resultBean.getMetaHeader());
        assertFalse(tags.isEmpty());

        for (TrackTag tag : tags) {
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

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
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

        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        assertNotNull(resultBean);
        Set<TrackTag> tags = tagTrackService.findTrackTags(resultBean.getMetaHeader());
        assertFalse(tags.isEmpty());

        SearchChange searchChange = trackService.getSearchChange(resultBean, "Blah", new Date());
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void targetTagWithAuditRelationship() throws Exception {
        String name = "Doctor John";
        TagInputBean authorTag = new TagInputBean(name + ":person");
        authorTag.addAuditLink("writer");
        authorTag.addAuditLink("lead");

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean countryTag = new TagInputBean("New Zealand");
        TagInputBean cityTag = new TagInputBean("Auckland");
        TagInputBean institution = new TagInputBean("Auckland University:Institution");
        cityTag.setTargets("jurisdiction", countryTag); // Auckland located in NZ

        institution.addAuditLink("located");
        cityTag.addAuditLink("city");

        inputBean.setTag(cityTag); // Not attached to audit
        inputBean.setTag(countryTag);
        inputBean.setTag(institution);

        TrackResultBean result = auditManager.createHeader(inputBean, null);
        assertNotNull(result);
        Set<TrackTag> tags = tagTrackService.findTrackTags(result.getMetaHeader());
        assertEquals(2, tags.size());
        for (TrackTag tag : tags) {
            assertTrue(tag.getTag().getName().equals(institution.getName()) || tag.getTag().getName().equals(cityTag.getName()));
        }

    }

    @Test
    public void geoTag() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean auditBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
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

        TrackResultBean resultBean = auditManager.createHeader(auditBean, null);
        assertNotNull(resultBean);
        assertNotNull(tagService.findTag("USA"));

        Set<TrackTag> tags = tagTrackService.findTrackTags(resultBean.getMetaHeader());
        assertFalse(tags.isEmpty());

        for (TrackTag tag : tags) {
            assertEquals("mikecorp", tag.getTag().getName());
            assertNotNull(tag.getGeoData());
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

    @Test
    public void tagsAreUpdatedOnAuditUpdate() throws Exception {
        org.junit.Assume.assumeTrue(false);// Skipping this until FixMe - implement rewrite of header tags
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");
        String what = "{\"house\": \"house";

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(), what + "1\"}");
        inputBean.setLog(logBean);
        // This should create the same Tag object
        inputBean.setTag(new TagInputBean("TagA", "camel"));
        TrackResultBean resultBean = auditManager.createHeader(inputBean, null);
        MetaHeader unchanged = trackService.getHeader(resultBean.getMetaKey());
        assertNotNull ( unchanged);

        MetaInputBean removeTag = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc2");
        LogInputBean alb = new LogInputBean("mike", new DateTime(), what + "1\"}");
        removeTag.setLog(alb);
        // This should create the same Tag object
        removeTag.setTag(new TagInputBean("TagA", "camel"));
        resultBean = auditManager.createHeader(removeTag, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Assert.assertNotNull(metaHeader);

        validateTag(metaHeader, "TagA", 1);

        // Replace the current tag
        removeTag.setTag(new TagInputBean("TagB", "camel"));
        removeTag.setLog(new LogInputBean("mike", new DateTime(), what + "2\"}"));
        auditManager.createHeader(removeTag, null);
        validateTag(metaHeader, "TagB", 1);

        // Make sure we didn't remove the node as it was in use by the first header we created
        validateTag(unchanged, "TagA", 1);
    }
    @Test
    public void tagsWithNoRelationshipsAreRemovedOnHeaderUpdate() throws Exception {
        org.junit.Assume.assumeTrue(false);// Skipping this until FixMe - implement rewrite of header tags
        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");
        String what = "{\"house\": \"house";

        tagService.processTag(tagInput);
        MetaInputBean removeTag = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc2");
        LogInputBean alb = new LogInputBean("mike", new DateTime(), what + "1\"}");
        removeTag.setLog(alb);
        // This should create the same Tag object
        removeTag.setTag(new TagInputBean("TagA", "camel"));
        TrackResultBean resultBean = auditManager.createHeader(removeTag, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Assert.assertNotNull(metaHeader);

        validateTag(metaHeader, "TagA", 1);

        // Replace the current tag
        removeTag.setTag(new TagInputBean("TagB", "camel"));
        removeTag.setLog(new LogInputBean("mike", new DateTime(), what + "2\"}"));
        auditManager.createHeader(removeTag, null);
        validateTag(metaHeader, "TagB", 1);

        assertTrue ( "TagA has no audit headers so should have been removed", tagService.findTag("TagA")==null);
    }
//    @Test
//    public void headerTagsAreUpdatedWithTagDetailsInAnAuditLog() throws Exception {
//        regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
//        fortressService.registerFortress("ABC");
//
//        TagInputBean tagInput = new TagInputBean("FLOP");
//        String what = "{\"house\": \"house";
//
//        tagService.processTag(tagInput);
//        //assertNotNull(result);
//        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
//        LogInputBean logBean = new LogInputBean("mike", new DateTime(), what + "1\"}");
//        inputBean.setLog(logBean);
//        // This should create the same Tag object
//        inputBean.setTag(new TagInputBean("TagA", "camel"));
//        ResponseEntity<TrackResultBean> response = auditEp.createHeader(inputBean);
//        assertNotNull (response);
//
//        TrackResultBean resultBean = response.getBody();
//        assertNotNull(resultBean);
//        MetaHeader header = auditEp.getAudit(resultBean.getMetaKey()).getBody();
//        assertNotNull(header);
//
//        // Now change the tag via the Audit Log
//        LogInputBean alb = new LogInputBean("mike", new DateTime(), what + "1\"}");
//
//        validateTag(header, "TagA", 1);
//
//
//    }
    private void validateTag(MetaHeader metaHeader,  String expected, int i) {
        Collection<TrackTag> tags;
        tags = tagTrackService.findTrackTags(metaHeader);
        assertEquals(expected, i, tags.size() );
        for (TrackTag tag : tags){
            assertEquals(expected, tag.getTag().getName());
        }
    }

}
