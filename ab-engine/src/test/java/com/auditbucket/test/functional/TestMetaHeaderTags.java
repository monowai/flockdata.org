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

import com.auditbucket.dao.TrackTagDao;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackTag;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Transactional
public class TestMetaHeaderTags extends TestEngineBase {

    @Test
    public void simpleTagAgainstMetaHeader() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        assertNotNull(su);
        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        TagInputBean flopTag = new TagInputBean("FLOP");
        tagService.createTag(su.getCompany(), flopTag);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());

        TrackTagInputBean auditTag = new TrackTagInputBean(resultBean.getMetaKey(), null, "!!!");
        try {
            tagTrackService.processTag(header, auditTag);
            fail("Null argument exception should have been thrown");
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

        SystemUser su= registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.addTag(new TagInputBean("TagA", "AAAA"));
        aib.addTag(new TagInputBean("TagB", "BBBB"));
        aib.addTag(new TagInputBean("TagC", "CCCC"));
        aib.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), aib);
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
    public void createAndDeleteTrackTags() throws Exception {

        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(iSystemUser.getCompany(), tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        aib.addTag(new TagInputBean("TagA", "AAAA"));
        aib.addTag(new TagInputBean("TagB", "BBBB"));
        aib.addTag(new TagInputBean("TagC", "CCCC"));
        aib.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), aib);
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
        SystemUser su= registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        aib.addTag(new TagInputBean("TagA", null));
        aib.addTag(new TagInputBean("TagB", null));
        aib.addTag(new TagInputBean("TagC", null));
        aib.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), aib);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        trackService.updateHeader(metaHeader);
        metaHeader = trackService.getHeader(metaHeader.getMetaKey());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, metaHeader.getMetaKey());
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
    public void nullCodeValue() throws Exception {
        SystemUser su= registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        TagInputBean tag = new TagInputBean("TagD", "DDDD");
        tag.setCode(null ); // This gets set to null if not supplied over an endpoint
        aib.addTag(tag);
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), aib);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(1, tagSet.size());

    }
    @Test
    public void duplicateTagNotCreated() throws Exception {
        SystemUser su= registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        aib.addTag(new TagInputBean("TagA", "camel"));
        aib.addTag(new TagInputBean("taga", "lower"));
        aib.addTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), aib);
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
        SystemUser su= registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress(new FortressInputBean("ABC"));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.setTrackSuppressed(true);
        // This should create the same Tag object, but return one row for each relationships
        aib.addTag(new TagInputBean("TagA", "camel"));
        aib.addTag(new TagInputBean("taga", "lower"));
        aib.addTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), aib);

        assertEquals(3, resultBean.getTags().size());
        Long id = null;
        for (TrackTag trackTag : resultBean.getTags()) {
            if ( id == null )
                id = trackTag.getTag().getId();
            assertEquals(id, trackTag.getTag().getId());
        }
        assertNull(resultBean.getMetaKey());

    }

    @Test
    public void createLogForInvalidHeader() throws Exception{
        registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress(new FortressInputBean("ABC"));

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        trackEP.trackHeader(aib, null, null).getBody();
        LogInputBean alib = new LogInputBean("Harry", "InvalidKey", new DateTime(), TestHelper.getRandomMap());
        try {
            trackEP.trackLog(alib, null, null);
            fail("Invalid track header. This should not have worked");
        } catch (DatagioException e ){
            // Good stuff
        }

    }
    @Test
    public void createLogForValidHeaderWithNoWhatDetail() throws Exception{
        registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress(new FortressInputBean("ABC", true));

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TrackResultBean rb = trackEP.trackHeader(aib, null, null).getBody();
        LogInputBean alib = new LogInputBean("Harry", rb.getMetaKey(), new DateTime(),null);
        trackEP.trackLog(alib, null, null).getBody();
    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress( new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(iSystemUser.getCompany(), tagInput);

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addMetaLink("Type1");
        tag.addMetaLink("Type2");
        tag.addMetaLink("Type3");
        aib.addTag(tag);

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), aib);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());

        String apiKey = iSystemUser.getApiKey();
        TrackedSummaryBean summaryBean = trackEP.getTrackedSummary(metaHeader.getMetaKey(), apiKey, apiKey).getBody();
        assertNotNull(summaryBean);
        assertEquals(3, summaryBean.getTags().size());

    }

    @Test
    public void tagListAndSingular() throws Exception {
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addMetaLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addMetaLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        Map<String, Object> propA = new HashMap<>();
        Map<String, Object> propB = new HashMap<>();
        propA.put("myValue", 10);
        propB.put("myValue", 20);

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to", propA);
        tagA.addMetaLink("email-cc", propB);
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);
        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.addMetaLink("email-to");
        tagInputBean.addMetaLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void directedMetaTagsWork ( )throws Exception{
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.setReverse(true); // relationships will be reversed
        tagInputBean.addMetaLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        // By default, tags are inbound to the MetaHeader. This asserts the reverse also works
        Set<TrackTag> tagResults = tagTrackService.findOutboundTags(header);
        assertEquals("No tag heading out from the MetaHeader could be found", 1, tagResults.size());

    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {

        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addMetaLink("email cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void nestedStructureInHeader() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(authDefault);
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

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

        inputBean.addTag(country);
        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
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
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
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
        inputBean.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
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
        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
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
        inputBean.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        assertNotNull(resultBean);
        Set<TrackTag> tags = tagTrackService.findTrackTags(resultBean.getMetaHeader());
        assertFalse(tags.isEmpty());

        SearchChange searchChange = searchService.getSearchChange(fortress.getCompany(), resultBean, "Blah", new Date());
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void targetTagWithAuditRelationship() throws Exception {
        String name = "Doctor John";
        TagInputBean authorTag = new TagInputBean(name + ":person");
        authorTag.addMetaLink("writer");
        authorTag.addMetaLink("lead");

        SystemUser iSystemUser = registerSystemUser(monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean countryTag = new TagInputBean("New Zealand");
        TagInputBean cityTag = new TagInputBean("Auckland");
        TagInputBean institution = new TagInputBean("Auckland University:Institution");
        cityTag.setTargets("jurisdiction", countryTag); // Auckland located in NZ

        institution.addMetaLink("located");
        cityTag.addMetaLink("city");

        inputBean.addTag(cityTag); // Not attached to track
        inputBean.addTag(countryTag);
        inputBean.addTag(institution);

        TrackResultBean result = mediationFacade.trackHeader(iSystemUser.getCompany(), inputBean);
        assertNotNull(result);
        Set<TrackTag> tags = tagTrackService.findTrackTags(result.getMetaHeader());
        assertEquals(2, tags.size());
        for (TrackTag tag : tags) {
            assertTrue(tag.getTag().getName().equals(institution.getName()) || tag.getTag().getName().equals(cityTag.getName()));
        }

    }

    @Test
    public void geoTag() throws Exception {
        SystemUser iSystemUser = registerSystemUser( monowai, mike_admin);
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean auditBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country", "");
        TagInputBean cityInputTag = new TagInputBean(city, ":City", "");
        TagInputBean stateInputTag = new TagInputBean("CA", "State", "");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        auditBean.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackHeader(iSystemUser.getCompany(), auditBean);
        assertNotNull(resultBean);
        assertNotNull(tagService.findTag(fortress.getCompany(), "USA", "Country"));

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
    public void tagsAreUpdatedOnHeaderUpdate() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("TEST-CREATE", "rlx-test");

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(), TestHelper.getRandomMap());
        inputBean.setLog(logBean);

        inputBean.addTag(tagInput);
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), inputBean);
        MetaHeader created = trackService.getHeader(resultBean.getMetaKey());
        Log firstLog = trackEP.getLastChange(created.getMetaKey(), su.getApiKey(), su.getApiKey()).getBody().getLog();
        assertNotNull(created);

        // Test that a tag is removed
        MetaInputBean updatedHeader = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        LogInputBean alb = new LogInputBean("mike", new DateTime(),  TestHelper.getRandomMap());
        updatedHeader.setLog(alb);
        // Updating an existing MetaHeader but the tagCollection is minus TEST-CREATE tag
        // The create call should create a new Tag - TEST-UPDATE - and then remove the TEST-CREATE
        updatedHeader.addTag(new TagInputBean("TEST-UPDATE", "camel"));
        resultBean = mediationFacade.trackHeader(su.getCompany(), updatedHeader);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Assert.assertNotNull(metaHeader);

        // Should only be one tag
        validateTag(metaHeader, null, 1);
        // It should be the update tag
        validateTag(metaHeader, "TEST-UPDATE", 1);
        // The create tag should not be against the header but against the log
        validateTag(metaHeader, "TEST-CREATE", 0);

        Set<TrackTag> results = trackEP.getLastChangeTags(metaHeader.getMetaKey(), su.getApiKey(), su.getApiKey());
        assertEquals(0, results.size()); // No tags against the last change log - tags are against the header

        results = trackEP.getChangeTags(metaHeader.getMetaKey(), firstLog.getTrackLog().getId(), su.getApiKey(), su.getApiKey());
        assertEquals(1, results.size());
        assertEquals("TEST-CREATE", results.iterator().next().getTag().getName());

        // Make sure when we pass NO tags, i.e. just running an update, we don't change ANY tags

        alb = new LogInputBean("mike", new DateTime(), TestHelper.getRandomMap());
        updatedHeader.setLog(alb);
        updatedHeader.getTags().clear();
        resultBean = mediationFacade.trackHeader(su.getCompany(), updatedHeader);
        metaHeader = trackService.getHeader(resultBean.getMetaKey());

        // 3 logs
        assertEquals(3, trackService.getLogCount(su.getCompany(), metaHeader.getMetaKey()));
        // Update tag should still be against the header
        validateTag(metaHeader, "TEST-UPDATE", 1);

        // Here we will cancel the last two logs getting us back to the initial state
        // should be one tag of TEST-CREATE logged

        trackEP.cancelLastLog(metaHeader.getMetaKey(), su.getApiKey(), su.getApiKey());
        trackEP.cancelLastLog(metaHeader.getMetaKey(), su.getApiKey(), su.getApiKey());
        //ToDo: We are only adding back tags that were removed If tag as added by the cancelled log then it should
        // also be removed The answer here should be 1
        validateTag(metaHeader, null, 1);
        validateTag(metaHeader, "TEST-CREATE", 1);
    }

    @Test
    public void oneTagRemovedFromASetOfTwo() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("TAG-FIRST", "rlx-test");

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(),  TestHelper.getRandomMap());
        inputBean.setLog(logBean);

        inputBean.addTag(tagInput);
        tagInput = new TagInputBean(("TAG-SECOND"), "rlxb-test");
        inputBean.addTag(tagInput);
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), inputBean);
        MetaHeader created = trackService.getHeader(resultBean.getMetaKey());
        trackEP.getLastChange(created.getMetaKey(), su.getApiKey(), su.getApiKey()).getBody().getLog();
        assertNotNull(created);
        validateTag(created, null, 2);

        // Test that a tag is removed
        MetaInputBean updatedHeader = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        LogInputBean alb = new LogInputBean("mike", new DateTime(),  TestHelper.getRandomMap());
        updatedHeader.setLog(alb);
        // we are updating an existing header with two tags and tellin it that only one is now valid
        updatedHeader.addTag(new TagInputBean("TAG-FIRST", "rlx-test"));
        resultBean = mediationFacade.trackHeader(su.getCompany(), updatedHeader);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Assert.assertNotNull(metaHeader);

        // Should be one tag
        validateTag(metaHeader, null, 1);
        // It should be the update tag
        validateTag(metaHeader, "TAG-FIRST", 1);
        // The create tag should not be against the header but against the log
        validateTag(metaHeader, "TEST-SECOND", 0);

        Set<TrackTag> results = trackEP.getLastChangeTags(metaHeader.getMetaKey(), su.getApiKey(), su.getApiKey());
        // No tags removed for the last tag
        assertEquals(0, results.size()); // No tags against the logs
        assertEquals(1, trackEP.getTrackTags(metaHeader.getMetaKey(), su.getApiKey(), su.getApiKey()).size());

    }

    @Test
    public void addNewTagToExistingMetaHeader() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        //assertNotNull(result);
        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(),  TestHelper.getRandomMap());
        inputBean.setLog(logBean);
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "camel"));
        ResponseEntity<TrackResultBean> response = trackEP.trackHeader(inputBean, su.getApiKey(), su.getApiKey());
        // At this point we have a metaHeader, log and a tag.

        assertNotNull(response);

        TrackResultBean resultBean = response.getBody();
        assertNotNull(resultBean);
        MetaHeader header = trackEP.getMetaHeader(resultBean.getMetaKey(), su.getApiKey(), su.getApiKey()).getBody();
        assertNotNull(header);

        validateTag(header, "TagA", 1);

        //Adding a second tag (the first is already in the metaHeader
        inputBean.addTag(new TagInputBean("TagB", "horse"));
        trackEP.trackHeader(inputBean, su.getApiKey(), su.getApiKey());
        validateTag(header, "TagB", 2);

    }

    @Test
    public void directionalTagsAndRelationshipPropertiesPreserved() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress("ABC");

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(),  TestHelper.getRandomMap());
        inputBean.setLog(logBean);

        Map<String,Object>rlxProperties = new HashMap<>();
        rlxProperties.put("stringTest", "blah");
        rlxProperties.put("doubleTest", 100d);
        rlxProperties.put("weight", 99);
        rlxProperties.put("abAdded", "z");

        TagInputBean inBound = new TagInputBean("TAG-IN", "rlx-test", rlxProperties);
        inputBean.addTag(inBound);

        TagInputBean outBound = new TagInputBean(("TAG-OUT"), "rlxb-test").setReverse(true);

        inputBean.addTag(outBound);
        TrackResultBean resultBean = mediationFacade.trackHeader(su.getCompany(), inputBean);
        MetaHeader created = trackService.getHeader(resultBean.getMetaKey());
        trackEP.getLastChange(created.getMetaKey(), su.getApiKey(), su.getApiKey()).getBody().getLog();

        // Total of two tags
        validateTag(created, null, 2);

        Set<TrackTag> outboundTags = tagTrackService.findInboundTags(su.getCompany(), created);
        assertEquals("One tag should be reversed", 1, outboundTags.size());
        TrackTag trackOut = outboundTags.iterator().next();
        assertEquals("TAG-IN", trackOut.getTag().getName());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals((Integer)99, trackOut.getWeight());
        Long currentWhen = (Long)trackOut.getProperties().get(TrackTagDao.AB_WHEN);
        assertTrue(currentWhen>0);

        logBean = new LogInputBean("mike", new DateTime(),  TestHelper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(outBound);
        inputBean.setLog(logBean);

        // Removing the inbound tag
        mediationFacade.trackHeader(su.getCompany(), inputBean);
        validateTag(created, null, 1);
        outboundTags = tagTrackService.findOutboundTags(su.getCompany(), created);

        // One is reversed
        assertEquals(1, outboundTags.size());

        // Cancelling last change should restore the inbound tag
        mediationFacade.cancelLastLog(su.getCompany(), created);
        // Total of two tags
        validateTag(created, null, 1);
        // One of which is outbound and the other inbound
        assertEquals(1, outboundTags.size());

        // Check that we still have our custom properties
        outboundTags = tagTrackService.findTrackTags(su.getCompany(), created);
        trackOut = outboundTags.iterator().next();
        assertEquals("TAG-IN", trackOut.getTag().getName());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals((Integer)99, trackOut.getWeight());
        assertEquals(currentWhen, trackOut.getProperties().get(TrackTagDao.AB_WHEN));
    }

    @Test
    public void addNewTagToExistingMetaHeaderWithNoLog() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        fortressService.registerFortress(new FortressInputBean("ABC", true));

        //assertNotNull(result);
        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "camel"));
        ResponseEntity<TrackResultBean> response = trackEP.trackHeader(inputBean, su.getApiKey(), su.getApiKey());

        // At this point we have a metaHeader, log and a tag.
        assertNotNull(response);

        TrackResultBean resultBean = response.getBody();
        assertNotNull(resultBean);
        MetaHeader header = trackEP.getMetaHeader(resultBean.getMetaKey(), su.getApiKey(), su.getApiKey()).getBody();
        assertNotNull(header);

        validateTag(header, "TagA", 1);

        //Adding a second tag (the first is already in the metaHeader
        inputBean.addTag(new TagInputBean("TagB", "horse"));
        trackEP.trackHeader(inputBean, su.getApiKey(), su.getApiKey());
        validateTag(header, "TagB", 2);

    }
    @Test
    public void search() throws Exception{
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fo = fortressService.registerFortress(new FortressInputBean("cancelLogTag", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "CancelDoc", new DateTime(), "ABC123");
        LogInputBean log = new LogInputBean("wally", new DateTime(),  TestHelper.getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addMetaLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addMetaLink("testingb"));
        inputBean.setLog(log);
        TrackResultBean result;
        mediationFacade.trackHeader(su.getCompany(), inputBean);

        // We now have 1 log with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        log = new LogInputBean("wally", new DateTime(),  TestHelper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(new TagInputBean("Sad Days").addMetaLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addMetaLink("testingc"));
        inputBean.setLog(log);
        result = mediationFacade.trackHeader(su.getCompany(), inputBean);
        // We now have 2 logs, sad tags, no happy tags

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), result.getMetaHeader());
        Set<TrackTag>tags = tagTrackService.findTrackTags(result.getMetaHeader());
        assertEquals(2, tags.size());

    }

    private void validateTag(MetaHeader metaHeader,  String tagName, int totalExpected) {
        Collection<TrackTag> tags;
        tags = tagTrackService.findTrackTags(metaHeader);
        if ( tagName == null ) {
            assertEquals("Total Expected Tags is incorrect", totalExpected, tags.size() );
            return;
        }

        boolean found = false;
        for (TrackTag tag : tags){
            if (tag.getTag().getName().equals(tagName)){
                found = true;
                break;
            }
        }
        if ( totalExpected == 0 && !found)
            return ;
        if ( totalExpected == 0 ){
            fail("The expected tag [" + tagName + "] was found when it was not expected to exist");
            return;
        }
        assertTrue("The expected tag ["+ tagName +"] was not found", found);
    }

}
