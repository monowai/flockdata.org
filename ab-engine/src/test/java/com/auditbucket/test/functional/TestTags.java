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

import com.auditbucket.engine.PropertyConversion;
import com.auditbucket.helper.DatagioTagException;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.bean.TagInputBean;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
public class TestTags extends EngineBase {

    public void duplicateTagLists() throws Exception {
        SystemUser iSystemUser = registerSystemUser("duplicateTagLists", mike_admin);
        assertNotNull(iSystemUser);

        List<TagInputBean> tags = new ArrayList<>();
        //TODo: remove company from input/TagIB <> inherit Tag
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));

        Iterable<Tag> tagResults = tagService.makeTags(iSystemUser.getCompany(),tags).get();
        assertNotNull(tagResults);
        int count = 0;
        for (Tag next : tagResults) {
            assertEquals("FLOP", next.getName());
            //assertEquals("flop", next.getKey());
            count++;
        }
        assertEquals(1, count);
        tags = new ArrayList<>();
        //TODo: remove company from input/TagIB <> inherit Tag
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOPPY"));
        tags.add(new TagInputBean("FLOPSY"));
        tags.add(new TagInputBean("FLOPPO"));
        tags.add(new TagInputBean("FLOPER"));
        tagResults = tagService.makeTags(iSystemUser.getCompany(),tags).get();
        count = 0;
        for (Tag next : tagResults) {
            assertNotNull(next);
            count++;
        }
        assertEquals(5, count);

    }

    @org.junit.Test
    @Transactional
    public void secureMultiTenantedTags() throws Exception {
        engineConfig.setMultiTenanted(true);
        SystemUser iSystemUser = registerSystemUser("secureMultiTenantedTags", mike_admin);
        assertNotNull(iSystemUser);

        List<TagInputBean> tags = new ArrayList<>();
        TagInputBean tagInput = new TagInputBean("FLOP");
        tags.add(tagInput);
        Iterable<Tag> tagResult = mediationFacade.createTags(iSystemUser.getCompany(), tags);
        assertNotNull(tagResult);
        assertTrue("We didn't create a tag", tagResult.iterator().hasNext());
        SystemUser sub = registerSystemUser("ABC", "gina");
        Authentication authGina = new UsernamePasswordAuthenticationToken("gina", "user1");
        SecurityContextHolder.getContext().setAuthentication(authGina);
        assertNull(tagService.findTag(sub.getCompany(),"FLOP")); // Can't see the Monowai company tag

        tagInput = new TagInputBean("FLOP");
        assertNotNull(tagService.createTag(sub.getCompany(), tagInput) );
        assertNull(tagService.findTag(sub.getCompany(), "ABC"));
        assertNotNull(tagService.findTag(sub.getCompany(), "FLOP"));
    }

    @Test
    public void updateExistingTag() throws Exception {

        SystemUser iSystemUser = registerSystemUser("updateExistingTag", mike_admin);
        assertNull(tagService.findTag(iSystemUser.getCompany(), "ABC"));
        Tag tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOP"));
        assertNotNull(tag);

        Tag result = tagService.findTag(iSystemUser.getCompany(), "FLOP");
        assertNotNull(result);
        tagService.findTag(iSystemUser.getCompany(),"FLOP");
        result = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPPY"));
        assertNotNull(result);
        assertEquals("FLOPPY", result.getName());

    }

    @Test
    public void tagMustExist() throws Exception {
        SystemUser iSystemUser = registerSystemUser("tagMustExist", mike_admin);

        assertNotNull(iSystemUser);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "ABC"));
        Tag tag ;
        try {
            tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setMustExist(true));
            fail("Incorrect exception");
        } catch (DatagioTagException dte) {
            logger.debug("Correct");
        }

        tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setMustExist(false));
        assertNotNull(tag);
        tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setMustExist(true));
        assertNotNull(tag);

    }


    @Test
    public void tagWithProperties() throws Exception {
        SystemUser iSystemUser = registerSystemUser("tagWithProperties", mike_admin);

        TagInputBean tagInput = new TagInputBean("ZFLOP");
        tagInput.setProperty("num", 123);
        tagInput.setProperty("dec", 123.11);
        tagInput.setProperty("string", "abc");

        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag(iSystemUser.getCompany(),"ZFLOP");

        assertNotNull(result);
        assertEquals(123l, tag.getProperty("num"));
        assertEquals(123.11, tag.getProperty("dec"));
        assertEquals("abc", tag.getProperty("string"));

    }

    @Test
    public void prohibitedPropertiesIgnored() throws Exception {
        SystemUser iSystemUser = registerSystemUser("prohibitedPropertiesIgnored", mike_admin);

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagInput.setProperty("id", 123);
        tagInput.setProperty("name", "abc");

        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput) ;

        assertNotNull(tag);
        Tag result = tagService.findTag(iSystemUser.getCompany(),"FLOP");
        assertNotNull(result);
        assertEquals("FLOP", result.getName());
        assertNotSame(123, result.getId());

    }

    @Test
    public void targetRelationships() throws Exception {
        SystemUser iSystemUser = registerSystemUser("targetRelationships", "mary");

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setTargets("testAssoc", new TagInputBean("Dest"));
        TagInputBean tag3 = new TagInputBean("Dest3");
        TagInputBean tag2 = new TagInputBean("Dest2");
        tag2.setTargets("testAssoc3", tag3);

        tagInput.setTargets("testAssoc2", tag2);

        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput) ;

        assertNotNull(tag);
        Tag result = tagService.findTag(iSystemUser.getCompany(),"Source");
        assertNotNull(result);

        result = tagService.findTag(iSystemUser.getCompany(),"Dest");
        assertNotNull(result);
        result = tagService.findTag(iSystemUser.getCompany(),"Dest2");
        assertNotNull(result);
        result = tagService.findTag(iSystemUser.getCompany(),"Dest3");
        assertNotNull(result);

    }

    @Test
    public void customLabelsSingleTenant() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser iSystemUser = registerSystemUser("customLabelsSingleTenant", mike_admin);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setLabel(":TestTag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput) ;
        assertNotNull(tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(),"TestTag");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        Boolean found = isNameFound(tagInput, results);
        assertTrue(found);
    }

    @Test
    public void tagWithSpacesWorks() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser iSystemUser = registerSystemUser("tagWithSpacesWorks", mike_admin);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setLabel(":Test Tag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput) ;
        assertNotNull(tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(),"Test Tag");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        Boolean found = isNameFound(tagInput, results);
        assertTrue(found);
    }

    // ToDo: Multi-tenanted custom tags
    public void customLabelsMultiTenant() throws Exception {
        engineConfig.setMultiTenanted(true);
        SystemUser iSystemUser = registerSystemUser("customLabelsMultiTenant", mike_admin);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setLabel(":TestTag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput) ;
        assertNotNull(tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(),"TestTag");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        boolean found = isNameFound(tagInput, results);
        assertTrue("Didn't find the taginput name in the result set", found);
    }

    private boolean isNameFound(TagInputBean tagInput, Collection<Tag> results) {
        boolean found = false;
        for (Tag result : results) {
            if (result.getName().equals(tagInput.getName())) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Test
    public void duplicateTagsForSameIndexReturnSingleTag() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser iSystemUser = registerSystemUser("duplicateTagsForSameIndexReturnSingleTag", mike_admin);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setLabel(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = mediationFacade.createTag(iSystemUser.getCompany(), tagInputA);
        assertNotNull(tagA);

        // This should work as the tag is in a different index
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setLabel(":TestTagA");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.createTag(iSystemUser.getCompany(), tagInputB);
        assertNotNull(tagB);
        assertEquals(tagA.getId(), tagB.getId());

    }

    @Test
    public void systemPropertiesHonoured() throws Exception {
        // Case insensitive test
        assertTrue(PropertyConversion.isSystemColumn("Name"));
        assertTrue(PropertyConversion.isSystemColumn("code"));
        assertTrue(PropertyConversion.isSystemColumn("keY"));

    }

    @Test
    public void tagUniqueForIndex() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser iSystemUser = registerSystemUser("tagUniqueForIndex", mike_admin);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setLabel(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = tagService.createTag(iSystemUser.getCompany(), tagInputA);
        assertNotNull(tagA);

        // Same code, but different label. Should create a new tag
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setLabel(":TestTagB");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.createTag(iSystemUser.getCompany(), tagInputB);
        Tag tagC = tagService.createTag(iSystemUser.getCompany(), tagInputB);
        assertNotNull(tagB);
        assertTrue(!tagA.getId().equals(tagB.getId()));
        assertTrue(tagC.getId().equals(tagB.getId()));
    }

    @Test
    public void tagAppleNameIssue() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("tagAppleNameIssue", mike_admin);

        // Exists in one index
        TagInputBean tagInputA = new TagInputBean("Apple");
        tagInputA.setLabel(":Law");
        Tag tagA = tagService.createTag(su.getCompany(), tagInputA);
        assertNotNull(tagA);

        // Same code, and default index. Should be found in the _Tag index
        TagInputBean tagInputB = new TagInputBean("Apple");
        tagInputB.setLabel(Tag.DEFAULT);

        TagInputBean tagInputC = new TagInputBean("Samsung");
        tagInputC.setLabel("Law");
        tagInputC.setTargets("sues", tagInputB);


        Tag tagC = tagService.createTag(su.getCompany(), tagInputC);
        assertNotNull(tagC);
        //assertTrue(tagA.getId().equals(tagB.getId()));
    }
    @Test
    public void geographyEndPoints() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("geographyEndPoints", mike_admin);

        TagInputBean tagInputBean = new TagInputBean("New Zealand").setLabel("Country");
        ArrayList<TagInputBean> countries = new ArrayList<>();
        countries.add(tagInputBean);
        mediationFacade.createTags(su.getCompany(), countries);
        Collection<Tag> co = geoService.findCountries(su.getCompany());
        assertEquals(1, co.size());

    }

    @Test
    public void sameKeyForDifferentTagTypes() throws Exception {
        engineConfig.setMultiTenanted(false);

        SystemUser iSystemUser = registerSystemUser("sameKeyForDifferentTagTypes", mike_admin);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setLabel(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = tagService.createTag(iSystemUser.getCompany(), tagInputA);
        assertNotNull(tagA);
        assertEquals(tagInputA.getCode(), tagA.getCode());
        assertEquals(tagInputA.getName(), tagA.getName());
        assertNotNull(tagA.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(), "TestTagA");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        boolean found = isNameFound(tagInputA, results);
        assertTrue(found);

        // This should work as the tag is in a different index
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setLabel(":TestTagB");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.createTag(iSystemUser.getCompany(), tagInputB);
        assertNotNull(tagB);
        assertNotSame(tagA.getId(), tagB.getId());

        assertEquals(tagInputB.getCode(), tagB.getCode());
        assertEquals(tagInputB.getName(), tagB.getName());
        assertNotNull(tagA.getKey());
        Collection<Tag> resultsB = tagService.findTags(iSystemUser.getCompany(), "TestTagB");
        assertNotNull(resultsB);
        assertFalse(resultsB.isEmpty());
        found = isNameFound(tagInputB, resultsB);
        assertTrue(found);
    }


}
