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

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.bean.*;
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
    public void tagAuditRecords() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        assertNotNull(iSystemUser);
        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        TagInputBean flopTag = new TagInputBean("FLOP");
        tagService.processTag(flopTag);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
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

        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.addTag(new TagInputBean("TagA", "AAAA"));
        aib.addTag(new TagInputBean("TagB", "BBBB"));
        aib.addTag(new TagInputBean("TagC", "CCCC"));
        aib.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
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

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        iSystemUser.getCompany();
        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        aib.addTag(new TagInputBean("TagA", "AAAA"));
        aib.addTag(new TagInputBean("TagB", "BBBB"));
        aib.addTag(new TagInputBean("TagC", "CCCC"));
        aib.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
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
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        aib.addTag(new TagInputBean("TagA", null));
        aib.addTag(new TagInputBean("TagB", null));
        aib.addTag(new TagInputBean("TagC", null));
        aib.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
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
        regService.registerSystemUser(new RegistrationBean(monowai, mike).setIsUnique(false));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        TagInputBean tag = new TagInputBean("TagD", "DDDD");
        tag.setCode(null ); // This gets set to null if not supplied over an endpoint
        aib.addTag(tag);
        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(1, tagSet.size());

    }
    @Test
    public void duplicateTagNotCreated() throws Exception {
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        aib.addTag(new TagInputBean("TagA", "camel"));
        aib.addTag(new TagInputBean("taga", "lower"));
        aib.addTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
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
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress(new FortressInputBean("ABC"));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        aib.setTrackSuppressed(true);
        // This should create the same Tag object
        aib.addTag(new TagInputBean("TagA", "camel"));
        aib.addTag(new TagInputBean("taga", "lower"));
        aib.addTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
        assertEquals(1, resultBean.getTags().size());
        assertNull(resultBean.getMetaKey());

    }

    @Test
    public void createLogForInvalidHeader() throws Exception{
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress(new FortressInputBean("ABC"));

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        trackEP.trackHeader(aib, null, null).getBody();
        LogInputBean alib = new LogInputBean("InvalidKey", "Harry", new DateTime(),"{\"xx\":1}");
        try {
            trackEP.trackLog(alib, null, null);
            fail("Invalid track header. This should not have worked");
        } catch (DatagioException e ){
            // Good stuff
        }

    }
    @Test
    public void createLogForValidHeaderWithNoWhatDetail() throws Exception{
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress(new FortressInputBean("ABC", true));

        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TrackResultBean rb = trackEP.trackHeader(aib, null, null).getBody();
        LogInputBean alib = new LogInputBean(rb.getMetaKey(), "Harry", new DateTime(),null);
        trackEP.trackLog(alib, null, null).getBody();
    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress( new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean aib = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addMetaLink("Type1");
        tag.addMetaLink("Type2");
        tag.addMetaLink("Type3");
        aib.addTag(tag);

        TrackResultBean resultBean = mediationFacade.createHeader(aib, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagSet = tagTrackService.findTrackTags(metaHeader);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());

        String apiKey = iSystemUser.getApiKey();
        TrackedSummaryBean summaryBean = trackEP.getAuditSummary(metaHeader.getMetaKey(),apiKey, apiKey ).getBody();
        assertNotNull(summaryBean);
        assertEquals(3, summaryBean.getTags().size());

    }

    @Test
    public void tagListAndSingular() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addMetaLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike).setIsUnique(false));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addMetaLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
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
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.addMetaLink("email-to");
        tagInputBean.addMetaLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void directedMetaTagsWork ( )throws Exception{
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.setReverse(true); // relationships will be reversed
        tagInputBean.addMetaLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        // By default, tags are inbound to the MetaHeader. This asserts the reverse also works
        Set<TrackTag> tagResults = tagTrackService.findOutboundTags(header);
        assertEquals("No tag heading out from the MetaHeader could be found", 1, tagResults.size());

    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addMetaLink("email cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader header = trackService.getHeader(resultBean.getMetaKey());
        Set<TrackTag> tagResults = tagTrackService.findTrackTags(header);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        TrackedSummaryBean summaryBean = trackService.getMetaSummary(null, header.getMetaKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void nestedStructureInHeader() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(authDefault);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
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
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
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
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
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

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
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
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
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

        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
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

        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike));
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

        TrackResultBean result = mediationFacade.createHeader(inputBean, null);
        assertNotNull(result);
        Set<TrackTag> tags = tagTrackService.findTrackTags(result.getMetaHeader());
        assertEquals(2, tags.size());
        for (TrackTag tag : tags) {
            assertTrue(tag.getTag().getName().equals(institution.getName()) || tag.getTag().getName().equals(cityTag.getName()));
        }

    }

    @Test
    public void geoTag() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(monowai, mike).setIsUnique(false));
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

        TrackResultBean resultBean = mediationFacade.createHeader(auditBean, null);
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
    public void tagsAreUpdatedOnAuditUpdate() throws Exception {
        org.junit.Assume.assumeTrue(false);// Skipping this until FixMe - implement rewrite of header tags
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");
        String what = "{\"house\": \"house";

        tagService.processTag(tagInput);
        //assertNotNull(result);
        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(), what + "1\"}");
        inputBean.setLog(logBean);
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "camel"));
        TrackResultBean resultBean = mediationFacade.createHeader(inputBean, null);
        MetaHeader unchanged = trackService.getHeader(resultBean.getMetaKey());
        assertNotNull(unchanged);

        MetaInputBean removeTag = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc2");
        LogInputBean alb = new LogInputBean("mike", new DateTime(), what + "1\"}");
        removeTag.setLog(alb);
        // This should create the same Tag object
        removeTag.addTag(new TagInputBean("TagA", "camel"));
        resultBean = mediationFacade.createHeader(removeTag, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Assert.assertNotNull(metaHeader);

        validateTag(metaHeader, "TagA", 1);

        // Replace the current tag
        removeTag.addTag(new TagInputBean("TagB", "camel"));
        removeTag.setLog(new LogInputBean("mike", new DateTime(), what + "2\"}"));
        mediationFacade.createHeader(removeTag, null);
        validateTag(metaHeader, "TagB", 1);

        // Make sure we didn't remove the node as it was in use by the first header we created
        validateTag(unchanged, "TagA", 1);
    }
    @Test
    public void tagsWithNoRelationshipsAreRemovedOnHeaderUpdate() throws Exception {
        org.junit.Assume.assumeTrue(false);// Skipping this until FixMe - implement rewrite of header tags
        regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        TagInputBean tagInput = new TagInputBean("FLOP");
        String what = "{\"house\": \"house";

        tagService.processTag(tagInput);
        MetaInputBean removeTag = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc2");
        LogInputBean alb = new LogInputBean("mike", new DateTime(), what + "1\"}");
        removeTag.setLog(alb);
        // This should create the same Tag object
        removeTag.addTag(new TagInputBean("TagA", "camel"));
        TrackResultBean resultBean = mediationFacade.createHeader(removeTag, null);
        MetaHeader metaHeader = trackService.getHeader(resultBean.getMetaKey());
        Assert.assertNotNull(metaHeader);

        validateTag(metaHeader, "TagA", 1);

        // Replace the current tag
        removeTag.addTag(new TagInputBean("TagB", "camel"));
        removeTag.setLog(new LogInputBean("mike", new DateTime(), what + "2\"}"));
        mediationFacade.createHeader(removeTag, null);
        validateTag(metaHeader, "TagB", 1);

        assertTrue ( "TagA has no track headers so should have been removed", tagService.findTag("TagA")==null);
    }

    @Test
    public void addNewTagToExistingMetaHeader() throws Exception {
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        fortressService.registerFortress("ABC");

        String what = "{\"house\": \"house";

        //assertNotNull(result);
        MetaInputBean inputBean = new MetaInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        LogInputBean logBean = new LogInputBean("mike", new DateTime(), what + "1\"}");
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
    public void addNewTagToExistingMetaHeaderWithNoLog() throws Exception {
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
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

    private void validateTag(MetaHeader metaHeader,  String mustExist, int totalExpected) {
        Collection<TrackTag> tags;
        tags = tagTrackService.findTrackTags(metaHeader);
        assertEquals("Total Expected Tags is incorrect", totalExpected, tags.size() );
        if ( mustExist == null )
            return;

        boolean expectedExisted = false;
        for (TrackTag tag : tags){
            if (tag.getTag().getName().equals(mustExist))
                expectedExisted = true;

        }

        assertTrue("The expected tag ["+mustExist +"] was not found", expectedExisted);
    }

}
