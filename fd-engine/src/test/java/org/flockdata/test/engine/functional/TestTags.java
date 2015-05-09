/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.functional;

import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.junit.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

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

        Iterable<Tag> tagResults = tagService.createTags(iSystemUser.getCompany(),tags);
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
        tagResults = tagService.createTags(iSystemUser.getCompany(),tags);
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
        cleanUpGraph();
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
        cleanUpGraph();
        SystemUser iSystemUser = registerSystemUser("tagMustExist", mike_admin);

        assertNotNull(iSystemUser);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "ABC"));
        Tag tag ;
        try {
            tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setMustExist(true));
            fail("Incorrect exception");
        } catch (RuntimeException dte) {
            logger.debug("Correct");
        }

        tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setMustExist(false));
        assertNotNull(tag);
        tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setMustExist(true));
        assertNotNull(tag);

    }

    @Test (expected = AmqpRejectAndDontRequeueException.class)
    public void exists_NotFoundRevertsToDefault() throws Exception {
        SystemUser iSystemUser = registerSystemUser("exists_NotFoundRevertsToDefault", mike_admin);
        // DAT-411
        assertNotNull(iSystemUser);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "NEW-TAG", "Testing"));
        assertNull(tagService.findTag(iSystemUser.getCompany(), "NotFound", "Testing"));
        TagInputBean newTag = new TagInputBean("NEW-TAG")
                .setMustExist(true, "NotFound")
                .setLabel("Testing");

        Tag tag = tagService.createTag(iSystemUser.getCompany(), newTag);
        assertNotNull(tag);
        assertEquals("NotFound", tag.getCode());
        assertEquals("Testing", tag.getLabel());

        newTag = new TagInputBean("NEW-TAG")
                .setMustExist(true, "");

        assertNull("blank code is the same as no code", tagService.createTag(iSystemUser.getCompany(), newTag));

    }

    @Test
    public void tagWithProperties() throws Exception {
        SystemUser iSystemUser = registerSystemUser("tagWithProperties", mike_admin);

        TagInputBean tagInput = new TagInputBean("ZFLOP");
        tagInput.setProperty("num", 123l);
        tagInput.setProperty("dec", 123.11);
        tagInput.setProperty("string", "abc");

        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag(iSystemUser.getCompany(),"ZFLOP");

        assertNotNull(result);
        assertEquals(123l, result.getProperty("num"));
        assertEquals(123.11, result.getProperty("dec"));
        assertEquals("abc", result.getProperty("string"));

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

    @Test
    public void add_newRelationshipExistingTag() throws Exception {
        engineConfig.setMultiTenanted(false);

        SystemUser iSystemUser = registerSystemUser("sameKeyForDifferentTagTypes", mike_admin);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setLabel(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        List<TagInputBean> tagInputs = new ArrayList<>();
        tagInputs.add(tagInputA);
        Collection<Tag>tagResults ;
        tagResults = mediationFacade.createTags(iSystemUser.getCompany(), tagInputs);
        assertEquals(1, tagResults.size());
        Tag tagA = tagResults.iterator().next();

        tagInputA.setTargets("blah", new TagInputBean("BBBB").setLabel(":TB"));
        mediationFacade.createTags(iSystemUser.getCompany(), tagInputs);

        Tag subTag = tagService.findTag(iSystemUser.getCompany(), "TB", "BBBB");
        assertNotNull(subTag);
        tagService.findTag(iSystemUser.getCompany(), "BBBB");
        assertNotNull(subTag);

        Collection<Tag> tags = tagService.findDirectedTags(tagA);
        assertEquals(1, tags.size());
        for (Tag tag : tags) {
            assertEquals("BBBB", tag.getCode());
        }

    }

    @Test
    public void label_UserDefined() throws Exception {
        cleanUpGraph();
        SystemUser iSystemUser = registerSystemUser("label_UserDefined", mike_admin);

        assertNotNull(iSystemUser);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "ABC"));
        Tag tag ;

        tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setLabel("MyLabel"));
        assertNotNull(tag);
        assertEquals ("MyLabel", tag.getLabel());

    }

    @Test
    public void scenario_SimpleAliasFound ()throws Exception{
        SystemUser su = registerSystemUser("scenario_AliasFound", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_AliasFound", true));

        TagInputBean tag = new TagInputBean("Holdsworth, Mike")
                .setLabel("Person");

        Tag tagResult = tagService.createTag(su.getCompany(), tag);

        tagService.createAlias(su.getCompany(), tagResult, "Person", "xxx");

        Tag tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), "xxx");
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

    }

    @Test
    public void scenario_AliasCollectionCreated ()throws Exception{
        SystemUser su = registerSystemUser("scenario_AliasCollectionCreated", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_AliasCollectionCreated", true));

        TagInputBean tag = new TagInputBean("scenario_AliasCollectionCreated")
                .setLabel("Person");

        // The alias will be a "PersonAlias" - ToDo: allow for other types??
        // We can find the Tag by any of the 2 aliases we define below
        AliasInputBean alias1 = new AliasInputBean("Mikey");
        AliasInputBean alias2 = new AliasInputBean("Mike Holdsworth");
        Collection<AliasInputBean>aliases = new ArrayList<>();
        aliases.add(alias1);
        aliases.add(alias2);
        tag.setAliases(aliases);
        Tag tagResult = tagService.createTag(su.getCompany(), tag);
        assertEquals("2 Aliases should have been associated with the tag", 2, tagService.findTagAliases(su.getCompany(), tag.getLabel(), tag.getCode()).size());
        Tag tagFoundByAlias = tagService.findTag(su.getCompany(), tag.getLabel(), alias1.getCode());
        assertNotNull(tagFoundByAlias);
        assertEquals(tagFoundByAlias.getId(), tagResult.getId());

        tagFoundByAlias = tagService.findTag(su.getCompany(), tag.getLabel(), alias2.getCode());
        assertNotNull(tagFoundByAlias);
        assertEquals(tagFoundByAlias.getId(), tagResult.getId());
    }

    @Test
    public void scenario_MultipleAliases ()throws Exception{
        SystemUser su = registerSystemUser("scenario_MultipleAliases", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_MultipleAliases", true));

        TagInputBean tag = new TagInputBean("Peoples Republic of Iran")
                .setLabel("Country");

        // The alias will be a "PersonAlias" - ToDo: allow for other types??
        // We can find the Tag by any of the 2 aliases we define below
        AliasInputBean alias1 = new AliasInputBean("Iran").setDescription("TestA");
        AliasInputBean alias2 = new AliasInputBean("Islamic Republic").setDescription("TestB");
        /// Alias 3 should not be created as it's the same as alias 1
        AliasInputBean alias3 = new AliasInputBean("Iran").setDescription("TestC");
        Collection<AliasInputBean>aliases = new ArrayList<>();
        aliases.add(alias1);
        aliases.add(alias2);
        tag.setAliases(aliases);
        Tag tagResult = tagService.createTag(su.getCompany(), tag);

        Tag tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), alias1.getCode());
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

        tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), alias2.getCode());
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

        tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), alias3.getCode());
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

        tagService.findTag(su.getCompany(), tag.getLabel(), "iran");
        Collection<AliasInputBean> inputs = tagService.findTagAliases(su.getCompany(), tag.getLabel(), "iran");
        assertEquals ("Alias nodes are uniquely differentiated by code value only", 2, inputs.size());


    }


}
