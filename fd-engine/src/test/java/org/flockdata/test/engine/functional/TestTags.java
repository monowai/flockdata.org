/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import org.flockdata.helper.JsonUtils;
import org.flockdata.model.SystemUser;
import org.flockdata.model.Tag;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.junit.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
public class TestTags extends EngineBase {

    @Test
    public void duplicateTagLists() throws Exception {
        SystemUser iSystemUser = registerSystemUser("duplicateTagLists", mike_admin);
        assertNotNull(iSystemUser);

        List<TagInputBean> tags = new ArrayList<>();
        tags.add(new TagInputBean("FLOP", "DUPE"));
        tags.add(new TagInputBean("FLOP", "DUPE"));
        tags.add(new TagInputBean("FLOP", "DUPE"));
        tags.add(new TagInputBean("FLOP", "DUPE"));
        tags.add(new TagInputBean("FLOP", "DUPE"));

        Iterable<TagResultBean> tagResults = tagService.createTags(iSystemUser.getCompany(), tags);
        assertNotNull(tagResults);
        int count = 0;
        Boolean oneIsNew = null;
        for (TagResultBean next : tagResults) {
            assertEquals("FLOP", next.getCode());
            //assertEquals("flop", next.getKey());
            if ( next.isNew()) {
                assertNull(oneIsNew); // we only want this set once
                oneIsNew = Boolean.TRUE;
            }

            count++;
        }
        assertNotNull(oneIsNew);
        assertEquals("Every input gets a response", 5, count);

        tags = new ArrayList<>();
        tags.add(new TagInputBean("FLOP", "DUPE"));
        tags.add(new TagInputBean("FLOPPY", "DUPE"));
        tags.add(new TagInputBean("FLOPSY", "DUPE"));
        tags.add(new TagInputBean("FLOPPO", "DUPE"));
        tags.add(new TagInputBean("FLOPER", "DUPE"));
        tagResults = tagService.createTags(iSystemUser.getCompany(), tags);
        count = 0;
        for (TagResultBean next : tagResults) {
            assertNotNull(next);
            if ( next.getCode().equals("FLOP"))
                assertFalse(next.isNew()); // created in previous run
            else
                assertTrue(next.isNew());
            count++;
        }
        assertEquals(5, count);

    }

    @Test
    public void secureMultiTenantedTags() throws Exception {
        engineConfig.setMultiTenanted(true);
        SystemUser iSystemUser = registerSystemUser("secureMultiTenantedTags", mike_admin);
        assertNotNull(iSystemUser);

        List<TagInputBean> tags = new ArrayList<>();
        TagInputBean tagInput = new TagInputBean("FLOP");
        tags.add(tagInput);
        Iterable<TagResultBean> tagResult = mediationFacade.createTags(iSystemUser.getCompany(), tags);
        assertNotNull(tagResult);
        assertTrue("We didn't create a tag", tagResult.iterator().hasNext());
        SystemUser sub = registerSystemUser("ABC", "gina");
        Authentication authGina = new UsernamePasswordAuthenticationToken("gina", "user1");
        SecurityContextHolder.getContext().setAuthentication(authGina);
        assertNull(tagService.findTag(sub.getCompany(), null, "FLOP")); // Can't see the Monowai company tag

        tagInput = new TagInputBean("FLOP");
        assertNotNull(tagService.createTag(sub.getCompany(), tagInput));
        assertNull(tagService.findTag(sub.getCompany(), null, "ABC"));
        assertNotNull(tagService.findTag(sub.getCompany(), null, "FLOP"));
    }

    @Test
    public void updateExistingTag() throws Exception {
        cleanUpGraph();
        SystemUser iSystemUser = registerSystemUser("updateExistingTag", mike_admin);
        Tag tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOP"));
        assertNotNull(tag);

        Tag result = tagService.findTag(iSystemUser.getCompany(), null, "FLOP");
        assertNotNull(result);
        tagService.findTag(iSystemUser.getCompany(), null, "FLOP");
        result = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPPY"));
        assertNotNull(result);
        assertEquals("FLOPPY", result.getCode());

    }

    @Test
    public void tagMustExist() throws Exception {
        cleanUpGraph();
        SystemUser iSystemUser = registerSystemUser("tagMustExist", mike_admin);

        assertNotNull(iSystemUser);

        Tag tag;
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

    @Test
    public void mustExist_WorksWithKeyPrefix() throws Exception {
        cleanUpGraph();
        SystemUser iSystemUser = registerSystemUser("tagMustExist", mike_admin);

        assertNotNull(iSystemUser);

        Tag tag;
        exception.expect(RuntimeException.class);
        tagService.createTag(iSystemUser.getCompany(),
                new TagInputBean("FLOPX", "MustExistTest")
                        .setKeyPrefix("abc")
                        .setMustExist(true));

        tag = tagService.createTag(
                iSystemUser.getCompany(), new TagInputBean("FLOPX", "MustExistTest")
                        .setKeyPrefix("abc")
                        .setMustExist(false));

        assertNotNull(tag);
        Long id = tag.getId();

        tag = tagService.createTag(
                iSystemUser.getCompany(), new TagInputBean("FLOPX", "MustExistTest")
                        .setKeyPrefix("abc")
                        .setMustExist(true));

        assertNotNull(tag);
        assertEquals(id, tag.getId());

    }

    @Test
    public void mustExist_NotFoundRevertsToDefault() throws Exception {
        SystemUser iSystemUser = registerSystemUser("exists_NotFoundRevertsToDefault", mike_admin);
        // DAT-411
        assertNotNull(iSystemUser);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "NEW-TAG", null, "Testing"));
        assertNull(tagService.findTag(iSystemUser.getCompany(), "NotFound", null, "Testing"));
        TagInputBean newTag = new TagInputBean("NEW-TAG")
                .setMustExist(true, "NotFound")
                .setLabel("Testing");

        Tag tag = tagService.createTag(iSystemUser.getCompany(), newTag);
        assertNotNull(tag);
        assertEquals("NotFound", tag.getCode());
        assertEquals("Testing", tag.getLabel());

        newTag = new TagInputBean("NEW-TAG")
                .setMustExist(true, "");
        exception.expect(AmqpRejectAndDontRequeueException.class);
        assertNull("blank code is the same as no code", tagService.createTag(iSystemUser.getCompany(), newTag));

    }

    @Test
    public void mustExist_NotFoundHandlesDefaultWithKeyPrefix() throws Exception {
        SystemUser iSystemUser = registerSystemUser("exists_NotFoundRevertsToDefault", mike_admin);
        // DAT-411
        assertNotNull(iSystemUser);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "NEW-TAG", null, "Testing"));
        assertNull(tagService.findTag(iSystemUser.getCompany(), "NotFound", null, "Testing"));
        TagInputBean newTag = new TagInputBean("NEW-TAG")
                .setMustExist(true, "NotFound")
                .setLabel("Testing");

        Tag tag = tagService.createTag(iSystemUser.getCompany(), newTag);

        assertNotNull("The NotFound tag was not created for a non-existent tag", tag);
        assertEquals("NotFound", tag.getCode());
        assertEquals("Testing", tag.getLabel());

        newTag = new TagInputBean("NEW-TAG")
                .setKeyPrefix("aaa")
                .setMustExist(true, "");

        exception.expect(AmqpRejectAndDontRequeueException.class);
        assertNull("blank code is the same as no code", tagService.createTag(iSystemUser.getCompany(), newTag));

    }

    @Test
    public void mustExist_SingleTagInBatchFails() throws Exception {
        SystemUser su = registerSystemUser("mustExist_SingleTagInBatchFails", mike_admin);
        // DAT-411
        assertNotNull(su);

        assertNull(tagService.findTag(su.getCompany(), "NEW-TAG", null, "Testing"));
        assertNull(tagService.findTag(su.getCompany(), "OtherTag", null, "Testing"));

        TagInputBean mustExist = new TagInputBean("NEW-TAG")
                .setMustExist(true)
                .setLabel("Testing");

        TagInputBean tagCreates = new TagInputBean("OtherTag")
                .setLabel("Testing");

        Collection<TagInputBean>tags = new ArrayList<>();
        tags.add(mustExist);
        tags.add(tagCreates);
        Collection<TagResultBean> results = tagService.createTags(su.getCompany(), tags);
        assertEquals("Two results for two inputs", 2, results.size());

        for (TagResultBean result : results) {
            if ( result.getCode().equals(mustExist.getCode())){
                assertTrue("The tag should not have been created",result.getTag()== null );
                assertFalse("The tag should not have been created",result.isNew());
                assertNotNull(result.getMessage());
                assertEquals(mustExist.getCode(), result.getCode());
            } else if ( result.getCode().equals(tagCreates.getCode())){
                // The inverse of above
                assertFalse("The tag should have been created",result.getTag()== null );
                assertTrue("The tag should have been created", result.isNew());
                assertNull(result.getMessage());
                assertEquals(tagCreates.getCode(), result.getCode());
                assertEquals(tagCreates.getCode(), result.getTag().getCode());

            }   else {
                throw new Exception ( "Unexpected tag" + result.toString());
            }
        }

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
        Tag result = tagService.findTag(iSystemUser.getCompany(), null, "ZFLOP");

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

        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag(iSystemUser.getCompany(), null, "FLOP");
        assertNotNull(result);
        assertEquals("FLOP", result.getCode());
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

        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag(iSystemUser.getCompany(), null, "Source");
        assertNotNull(result);

        result = tagService.findTag(iSystemUser.getCompany(), null, "Dest");
        assertNotNull(result);
        result = tagService.findTag(iSystemUser.getCompany(), null, "Dest2");
        assertNotNull(result);
        result = tagService.findTag(iSystemUser.getCompany(), null, "Dest3");
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
        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);
        assertNotNull(tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(), "TestTag");
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
        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);
        assertNotNull(tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(), "Test Tag");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        Boolean found = isNameFound(tagInput, results);
        assertTrue(found);
    }

    public void customLabelsMultiTenant() throws Exception {
        // ToDo: Multi-tenanted custom tags

        engineConfig.setMultiTenanted(true);
        SystemUser iSystemUser = registerSystemUser("customLabelsMultiTenant", mike_admin);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setLabel(":TestTag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.createTag(iSystemUser.getCompany(), tagInput);
        assertNotNull(tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags(iSystemUser.getCompany(), "TestTag");
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
        tagInputA.setLabel("TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        TagResultBean tagA = mediationFacade.createTag(iSystemUser.getCompany(), tagInputA);
        assertNotNull(tagA);

        // This should work as the tag is in a different index
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setLabel("TestTagA");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.createTag(iSystemUser.getCompany(), tagInputB);
        assertNotNull(tagB);
        assertEquals(tagA.getTag().getId(), tagB.getId());

    }

    @Test
    public void unique_SameCodeDifferentLabel() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser iSystemUser = registerSystemUser("unique_SameCodeDifferentLabel", mike_admin);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setLabel("TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = tagService.createTag(iSystemUser.getCompany(), tagInputA);
        assertNotNull(tagA);

        // Same code, but different label. Should create a new tag
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setLabel("TestTagB");
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
        // Clear down issues - the Country may already be in the DB
        Collection<Tag> co = geoService.findCountries(su.getCompany());

        int existingSize = co .size();

        TagInputBean tagInputBean = new TagInputBean("Atlantis").setLabel("Country");
        ArrayList<TagInputBean> countries = new ArrayList<>();
        countries.add(tagInputBean);
        Collection<TagResultBean> results = mediationFacade.createTags(su.getCompany(), countries);
        assertEquals(1, results.size());
        assertTrue(results.iterator().next().isNew());
        co = geoService.findCountries(su.getCompany());

        assertEquals(existingSize + 1, co.size());

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
        Collection<TagResultBean> tagResults;
        tagResults = mediationFacade.createTags(iSystemUser.getCompany(), tagInputs);
        assertEquals(1, tagResults.size());
        TagResultBean tagA = tagResults.iterator().next();

        tagInputA.setTargets("blah", new TagInputBean("BBBB").setLabel(":TB"));
        mediationFacade.createTags(iSystemUser.getCompany(), tagInputs);

        Tag subTag = tagService.findTag(iSystemUser.getCompany(), "TB", null, "BBBB");
        assertNotNull(subTag);
        tagService.findTag(iSystemUser.getCompany(), null, "BBBB");
        assertNotNull(subTag);

        Collection<Tag> tags = tagService.findDirectedTags(tagA.getTag());
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

        Tag tag = tagService.createTag(iSystemUser.getCompany(), new TagInputBean("FLOPX").setLabel("MyLabel"));
        assertNotNull(tag);
        assertEquals("MyLabel", tag.getLabel());

    }

    @Test
    public void scenario_SimpleAliasFound() throws Exception {
        SystemUser su = registerSystemUser("scenario_AliasFound", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_AliasFound", true));

        TagInputBean tag = new TagInputBean("Holdsworth, Mike")
                .setLabel("Person");

        Tag tagResult = tagService.createTag(su.getCompany(), tag);

        tagService.createAlias(su.getCompany(), tagResult, "Person", "xxx");

        Tag tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), null, "xxx");
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

    }

    @Test
    public void scenario_SimpleAliasWithTagPrefix() throws Exception {
        SystemUser su = registerSystemUser("scenario_AliasFound", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_AliasFound", true));

        TagInputBean tag = new TagInputBean("Holdsworth, Mike")
                .setLabel("Person")
                .setKeyPrefix("aaa");

        Tag tagResult = tagService.createTag(su.getCompany(), tag);

        tagService.createAlias(su.getCompany(), tagResult, "Person", "xxx");

        Tag tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), "aaa", "xxx");
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

    }

    @Test
    public void scenario_differentTagsSameAliasValue() throws Exception {
        SystemUser su = registerSystemUser("scenario_AliasFound", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_AliasFound", true));

        TagInputBean tagA = new TagInputBean("Ohio")
                .setLabel("StateX")
                .setKeyPrefix("US");

        TagInputBean tagB = new TagInputBean("Ohio")
                .setLabel("StateX")
                .setKeyPrefix("ZZ");

        Tag tagResultA = tagService.createTag(su.getCompany(), tagA);
        Tag tagResultB = tagService.createTag(su.getCompany(), tagB);

        assertFalse(tagResultA.getId().equals(tagResultB.getId()));

        tagService.createAlias(su.getCompany(), tagResultA, "StateX", "xxx");
        tagService.createAlias(su.getCompany(), tagResultB, "StateX", "xxx");

        Tag tagAlias = tagService.findTag(su.getCompany(), tagA.getLabel(), "us", "xxx");
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResultA.getId());

    }


    @Test
    public void scenario_AliasCollectionCreated() throws Exception {
        SystemUser su = registerSystemUser("scenario_AliasCollectionCreated", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("scenario_AliasCollectionCreated", true));

        TagInputBean tag = new TagInputBean("scenario_AliasCollectionCreated")
                .setLabel("Person");

        // The alias will be a "PersonAlias" - ToDo: allow for other types??
        // We can find the Tag by any of the 2 aliases we define below
        AliasInputBean alias1 = new AliasInputBean("Mikey");
        AliasInputBean alias2 = new AliasInputBean("Mike Holdsworth");
        Collection<AliasInputBean> aliases = new ArrayList<>();
        aliases.add(alias1);
        aliases.add(alias2);
        tag.setAliases(aliases);
        Tag tagResult = tagService.createTag(su.getCompany(), tag);
        assertEquals("2 Aliases should have been associated with the tag", 2, tagService.findTagAliases(su.getCompany(), tag.getLabel(), null, tag.getCode()).size());
        Tag tagFoundByAlias = tagService.findTag(su.getCompany(), tag.getLabel(), null, alias1.getCode());
        assertNotNull(tagFoundByAlias);
        assertEquals(tagFoundByAlias.getId(), tagResult.getId());

        tagFoundByAlias = tagService.findTag(su.getCompany(), tag.getLabel(), null, alias2.getCode());
        assertNotNull(tagFoundByAlias);
        assertEquals(tagFoundByAlias.getId(), tagResult.getId());
    }

    @Test
    public void scenario_MultipleAliases() throws Exception {
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
        Collection<AliasInputBean> aliases = new ArrayList<>();
        aliases.add(alias1);
        aliases.add(alias2);
        tag.setAliases(aliases);
        Tag tagResult = tagService.createTag(su.getCompany(), tag);

        Tag tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), null, alias1.getCode());
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

        tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), null, alias2.getCode());
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

        tagAlias = tagService.findTag(su.getCompany(), tag.getLabel(), null, alias3.getCode());
        assertNotNull(tagAlias);
        assertEquals(tagAlias.getId(), tagResult.getId());

        tagService.findTag(su.getCompany(), tag.getLabel(), null, "iran");
        Collection<AliasInputBean> inputs = tagService.findTagAliases(su.getCompany(), tag.getLabel(), null, "iran");
        assertEquals("Alias nodes are uniquely differentiated by code value only", 2, inputs.size());


    }

    @Test
    public void serialize_tagResults() throws Exception {
        // DAT-420
        SystemUser su = registerSystemUser("serialize_tagResults", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("serialize_tagResults", true));

        TagInputBean tag = new TagInputBean("Peoples Republic of Iran")
                .setLabel("Country");

        // The alias will be a "PersonAlias" - ToDo: allow for other types??
        // We can find the Tag by any of the 2 aliases we define below
        AliasInputBean alias1 = new AliasInputBean("Iran").setDescription("TestA");
        AliasInputBean alias2 = new AliasInputBean("Islamic Republic").setDescription("TestB");
        /// Alias 3 should not be created as it's the same as alias 1
        Collection<AliasInputBean> aliases = new ArrayList<>();
        aliases.add(alias1);
        aliases.add(alias2);
        tag.setAliases(aliases);
        List<TagInputBean> tags = new ArrayList<>();
        tags.add(tag);
        Collection<TagResultBean> tagResult = tagService.createTags(su.getCompany(), tags);

        byte[] bytes = JsonUtils.getObjectAsJsonBytes(tagResult);
        Collection<TagResultBean> converted = JsonUtils.getAsCollection(bytes, TagResultBean.class);
        assertNotNull(converted);

    }

    @Test
    public void path_FindTag() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser iSystemUser = registerSystemUser("path_FindTag", mike_admin);

        TagInputBean zipCode = new TagInputBean("codeA", "ZipCode").
                setName("NameA");

        // Same code, but different label. Should create a new tag
        TagInputBean tractCode = new TagInputBean("codeB", "Tract").
                setName("NameA");

        zipCode.setTargets("located", tractCode);
        Tag tagA = tagService.createTag(iSystemUser.getCompany(), zipCode);
        Tag tagB = tagService.findTag(iSystemUser.getCompany(), tractCode.getLabel(), null, tractCode.getCode());
        assertNotNull(tagA);
        assertNotNull(tagB);

        Map<String, Collection<TagResultBean>> results = tagService.findTags(iSystemUser.getCompany(), zipCode.getLabel(), zipCode.getCode(), "*", tractCode.getLabel());
        assertEquals("didn't find by wildcard relationship", 1, results.size());
        assertEquals(tractCode.getCode(), results.get("located").iterator().next().getCode());

        results = tagService.findTags(iSystemUser.getCompany(), zipCode.getLabel(), zipCode.getCode(), "located", tractCode.getLabel());
        assertEquals("didn't find by named relationship", 1, results.size());
        assertEquals(tractCode.getCode(), results.get("located").iterator().next().getCode());

        results = tagService.findTags(iSystemUser.getCompany(), zipCode.getLabel(), zipCode.getCode(), "locatedx", tractCode.getLabel());
        assertEquals("Should have found 0 by non-existent relationship", 0, results.size());

    }

    @Test
    public void duplicate_Relationships() throws Exception {
        TagInputBean root = new TagInputBean("root", "Interest");
        root.setTargets("should-be-one", new TagInputBean("un", "Important"));
        SystemUser iSystemUser = registerSystemUser("duplicate_Relationships", mike_admin);

        Collection<TagResultBean> tags = tagService.createTags(iSystemUser.getCompany(), Collections.singleton(root));
        tagService.createTags(iSystemUser.getCompany(), Collections.singleton(root));
        // check a duplicate relationship between identical tags does not get created
        String cypher = "match (t)-[r]-(o) where id(t)={t} return count(r) as value;";
        Map<String,Object>params = new HashMap<>();
        params.put("t",tags.iterator().next().getTag().getId());
        Result<Map<String, Object>> results = template.query(cypher, params);
        for (Map<String, Object> row : results) {
            assertEquals(1l, row.get("value"));
        }
    }


    @Test
    public void keyPrefix_SameCodeSameLabel() throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("unique_SameCodeDifferentLabel", mike_admin);

        TagInputBean tagInputA = new TagInputBean("Cambridge");
        tagInputA.setLabel("City");
        tagInputA.setKeyPrefix("NZ");
        Tag tagA = tagService.createTag(su.getCompany(), tagInputA);

        assertNotNull(tagA);

        // Same code same index, different Key prefix
        TagInputBean tagInputB = new TagInputBean("Cambridge");
        tagInputB.setLabel("City");
        tagInputB.setKeyPrefix("GB");
        Tag tagB = tagService.createTag(su.getCompany(), tagInputB);
        assertNotNull(tagB);
        assertTrue(!tagA.getId().equals(tagB.getId()));

        assertNotNull("Located by prefix/code failed", tagService.findTag(su.getCompany(), "City", "gb", "cambridge"));
        assertNotNull("Located by prefix/code failed", tagService.findTag(su.getCompany(), "City", "nz", "cambridge"));
        //assertEquals(2, tagService.findTag(su.getCompany(), "cambridge"));
    }

    @Test
    public void keyPrefix_withIndirectLookup() throws Exception {
        // DAT-479
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("keyPrefix_withIndirectLookup", mike_admin);

        TagInputBean gb = new TagInputBean("GB", "Country").setName("Great Britain");
        AliasInputBean aib = new AliasInputBean("Great Britain"); // Alternate way of finding GB
        gb.addAlias(aib);
        TagInputBean nz = new TagInputBean("NZ", "Country").setName("New Zealand");
        tagService.createTag(su.getCompany(), gb);
        tagService.createTag(su.getCompany(), nz);

        TagInputBean nzCity = new TagInputBean("Cambridge");
        nzCity.setLabel("City");
        nzCity.setKeyPrefix("Country:NZ"); // Instructs the server to do a Label:Value lookup to obtain the keyPrefix
        Tag tagA = tagService.createTag(su.getCompany(), nzCity);

        assertNotNull(tagA);

        // Same code same index, different Key prefix
        TagInputBean gbCity = new TagInputBean("Cambridge");
        gbCity.setLabel("City");
        gbCity.setKeyPrefix("Country:Great Britain"); // Check it also resolved by the alias we created
        Tag tagB = tagService.createTag(su.getCompany(), gbCity);
        assertNotNull(tagB);
        assertTrue(!tagA.getId().equals(tagB.getId()));

        Tag gbTag = tagService.findTag(su.getCompany(), "City", "gb", "cambridge");
        Tag nzTag = tagService.findTag(su.getCompany(), "City", "nz", "cambridge");

        assertNotNull("Located by prefix/code failed", gbTag);
        assertEquals("gb.cambridge", gbTag.getKey());
        assertNotNull("Located by prefix/code failed", nzTag);
        assertEquals("nz.cambridge", nzTag.getKey());
        // Ensure we can't create a duplicate City
        Tag tagC = tagService.createTag(su.getCompany(), nzCity);
        assertEquals("Shouldn't have created a new Tag for an existing City", nzTag.getId(), tagC.getId());
    }

    @Test
    public void merge_PropertiesInToExistingTag() throws Exception {
        // DAT-484
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("DAT-484", mike_admin);

        TagInputBean deliveryPoint = new TagInputBean("1233210", "DeliveryPoint").setName("7 Manor Drive");
        tagService.createTag(su.getCompany(), deliveryPoint);

        // We now have an existing tag with no properties

        deliveryPoint.setProperty("bat", 123);
        deliveryPoint.setProperty("log", 456);

        Tag tag = tagService.createTag(su.getCompany(), deliveryPoint);
        assertEquals("No merge instruction was given so the properties should not have been added", 0, tag.getProperties().size());

        // Now instruct the payload that it should merge
        deliveryPoint.setMerge(true);
        tag = tagService.createTag(su.getCompany(), deliveryPoint);
        assertEquals("Merged properties were not added", 2, tag.getProperties().size());
    }

    @Test
    public void recreate_AliasRelationship () throws Exception {
        engineConfig.setMultiTenanted(false);
        SystemUser su = registerSystemUser("nested_NullPointer", mike_admin);

        TagInputBean deliveryPoint = new TagInputBean("asdf", "Nested").setName("7 Manor Drive");
        AliasInputBean dpAlias = new AliasInputBean("123", "MyAliasTest");
        deliveryPoint.addAlias(dpAlias);

        TagResultBean tagResultBean = mediationFacade.createTag(su.getCompany(), deliveryPoint);

        assertNotNull(tagService.findTag(su.getCompany(), deliveryPoint.getLabel(), null, dpAlias.getCode()));

        // Check that no errors occur when processing the payload a second time
        mediationFacade.createTag(su.getCompany(), deliveryPoint);

        // Delete the relationship and reporcess

        String cypher = "match (t)-[r]-() where id(t)= {tagId} delete r;";
        Map<String,Object>params = new HashMap<>();
        params.put("tagId",tagResultBean.getTag().getId());
        template.query(cypher, params);

        // Should re-create the relationship
        mediationFacade.createTag(su.getCompany(), deliveryPoint);
    }
}
