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
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.endpoint.TagEP;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestTags {
    @Autowired
    FortressService fortressService;

    @Autowired
    RegistrationService regService;

    @Autowired
    TagService tagService;

    @Autowired
    TagEP tagEP;

    @Autowired
    EngineConfig engineAdmin;


    @Autowired
    private Neo4jTemplate template;

    //private Logger log = LoggerFactory.getLogger(TestTags.class);
    private String company = "Monowai";
    private String mike = "mike";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "123");


    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Neo4jHelper.cleanDb(template);
    }

    public void duplicateTagLists() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike));
        assertNotNull(iSystemUser);

        List<TagInputBean> tags = new ArrayList<>();
        //TODo: remove company from input/TagIB <> inherit Tag
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));
        tags.add(new TagInputBean("FLOP"));

        Iterable<TagInputBean> tagResults = tagService.processTags(tags);
        assertNotNull(tagResults);
        int count = 0;
        for (TagInputBean next : tagResults) {
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
        tagResults = tagService.processTags(tags);
        count = 0;
        for (TagInputBean next : tagResults) {
            assertNotNull(next);
            count++;
        }
        assertEquals(5, count);

    }

    @org.junit.Test
    @Transactional
    public void secureMultiTenantedTags() throws Exception {
        engineAdmin.setMultiTenanted(true);
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike));
        assertNotNull(iSystemUser);

        List<TagInputBean> tags = new ArrayList<>();
        TagInputBean tagInput = new TagInputBean("FLOP");
        tags.add(tagInput);
        Iterable<TagInputBean> tagResult = tagService.processTags(tags);
        assertNotNull(tagResult);
        assertFalse(tagResult.iterator().hasNext()); // No errors were detected
        regService.registerSystemUser(new RegistrationBean("ABC", "gina"));
        Authentication authGina = new UsernamePasswordAuthenticationToken("gina", "user1");
        SecurityContextHolder.getContext().setAuthentication(authGina);
        assertNull(tagService.findTag("FLOP")); // Can't see the Monowai company tag

        tagInput = new TagInputBean("FLOP");
        assertNotNull(tagService.processTag(tagInput));
        assertNull(tagService.findTag("ABC"));
        assertNotNull(tagService.findTag("FLOP"));
    }

    @Test
    public void updateExistingTag() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike).setIsUnique(false));
        assertNotNull(iSystemUser);
        SecurityContextHolder.getContext().setAuthentication(authMike);

        assertNull(tagService.findTag(iSystemUser.getCompany(), "ABC"));
        Tag tag = tagService.processTag(new TagInputBean("FLOP"));
        assertNotNull(tag);

        Tag result = tagService.findTag("FLOP");
        assertNotNull(result);
        tagService.findTag("FLOP");
        result = tagService.processTag(new TagInputBean("FLOPPY"));
        assertNotNull(result);
        assertEquals("FLOPPY", result.getName());
        // Tag update not yet supported
        //assertNull(tagService.findTag("FLOP"));
        //assertNotNull(tagService.findTag("FLOPPY"));

    }


    // @Test // Not yet supported.
    public void tagWithProperties() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike));
        assertNotNull(iSystemUser);

        TagInputBean tagInput = new TagInputBean("FLOP");
        tagInput.setProperty("num", 123);
        tagInput.setProperty("dec", 123.11);
        tagInput.setProperty("string", "abc");

        Tag tag = tagService.processTag(tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag("FLOP");

        assertNotNull(result);
        assertEquals(123l, tag.getProperty("num"));
        assertEquals(123.11, tag.getProperty("dec"));
        assertEquals("abc", tag.getProperty("string"));

        result = tagService.processTag(new TagInputBean("FLOPPY"));
        assertNotNull(result);
        assertEquals("FLOPPY", result.getName());

    }

    @Test
    public void prohibitedPropertiesIgnored() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, "mike").setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagInput.setProperty("id", 123);
        tagInput.setProperty("name", "abc");

        Tag tag = tagService.processTag(tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag("FLOP");
        assertNotNull(result);
        assertEquals("FLOP", result.getName());
        assertNotSame(123, result.getId());

    }

    @Test
    public void targetRelationships() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, "mike").setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setTargets("testAssoc", new TagInputBean("Dest"));
        TagInputBean tag3 = new TagInputBean("Dest3");
        TagInputBean tag2 = new TagInputBean("Dest2");
        tag2.setTargets("testAssoc3", tag3);

        tagInput.setTargets("testAssoc2", tag2);

        Tag tag = tagService.processTag(tagInput);

        assertNotNull(tag);
        Tag result = tagService.findTag("Source");
        assertNotNull(result);

        result = tagService.findTag("Dest");
        assertNotNull(result);
        result = tagService.findTag("Dest2");
        assertNotNull(result);
        result = tagService.findTag("Dest3");
        assertNotNull(result);

    }
    @Test
    public void customLabelsSingleTenant() throws Exception {
        engineAdmin.setMultiTenanted(false);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, "mike").setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setIndex(":TestTag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.processTag(tagInput);
        assertNotNull (tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags("TestTag");
        assertNotNull ( results);
        assertFalse(results.isEmpty());
        Boolean found = isNameFound(tagInput, results);
        assertTrue(found);
    }

    @Test
    public void tagWithSpacesWorks() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        engineAdmin.setMultiTenanted(false);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, "mike").setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setIndex(":Test Tag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.processTag(tagInput);
        assertNotNull (tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags("Test Tag");
        assertNotNull ( results);
        assertFalse(results.isEmpty());
        Boolean found = isNameFound(tagInput, results);
        assertTrue(found);
    }
    // ToDo: Multi-tenanted custom tags
    public void customLabelsMultiTenant() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authMike);
        engineAdmin.setMultiTenanted(true);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, "mike"));
        assertNotNull(iSystemUser);

        TagInputBean tagInput = new TagInputBean("Source");
        tagInput.setIndex(":TestTag");
        tagInput.setCode("CodeA");
        tagInput.setName("NameA");
        Tag tag = tagService.processTag(tagInput);
        assertNotNull (tag);
        assertEquals(tagInput.getCode(), tag.getCode());
        assertEquals(tagInput.getName(), tag.getName());
        assertNotNull(tag.getKey());
        Collection<Tag> results = tagService.findTags("TestTag");
        assertNotNull ( results);
        assertFalse(results.isEmpty());
        boolean found = isNameFound(tagInput, results);
        assertTrue("Didn't find the taginput name in the result set", found);
    }

    private boolean isNameFound(TagInputBean tagInput, Collection<Tag> results) {
        boolean found = false;
        for (Tag result : results) {
            if ( result.getName().equals(tagInput.getName())){
                found = true;
                break;
            }
        }
        return found;
    }

    @Test
    public void sameKeyForDifferentTagTypes() throws Exception {
        engineAdmin.setMultiTenanted(false);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike).setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setIndex(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = tagService.processTag(tagInputA);
        assertNotNull (tagA);
        assertEquals(tagInputA.getCode(), tagA.getCode());
        assertEquals(tagInputA.getName(), tagA.getName());
        assertNotNull(tagA.getKey());
        Collection<Tag> results = tagService.findTags("TestTagA");
        assertNotNull ( results);
        assertFalse(results.isEmpty());
        boolean found = isNameFound(tagInputA, results);
        assertTrue(found);

        // This should work as the tag is in a different index
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setIndex(":TestTagB");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.processTag(tagInputB);
        assertNotNull (tagB);
        assertNotSame(tagA.getId(), tagB.getId());

        assertEquals(tagInputB.getCode(), tagB.getCode());
        assertEquals(tagInputB.getName(), tagB.getName());
        assertNotNull(tagA.getKey());
        Collection<Tag> resultsB = tagService.findTags("TestTagB");
        assertNotNull ( resultsB);
        assertFalse(resultsB.isEmpty());
        found = isNameFound(tagInputB, resultsB);
        assertTrue(found);


    }

    @Test
    public void duplicateTagsForSameIndexReturnSingleTag() throws Exception {
        engineAdmin.setMultiTenanted(false);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike).setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setIndex(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = tagService.processTag(tagInputA);
        assertNotNull (tagA);

        // This should work as the tag is in a different index
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setIndex(":TestTagA");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.processTag(tagInputB);
        assertNotNull (tagB);
        assertEquals(tagA.getId(), tagB.getId());

    }
    @Test
    public void systemPropertiesHonoured() throws Exception{
        // Case insensitive test
        assertTrue(PropertyConversion.isSystemColumn("Name"));
        assertTrue(PropertyConversion.isSystemColumn("code"));
        assertTrue(PropertyConversion.isSystemColumn("keY"));

    }
    @Test
    public void tagUniqueForIndex() throws DatagioException {
        engineAdmin.setMultiTenanted(false);
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike).setIsUnique(false));
        assertNotNull(iSystemUser);

        TagInputBean tagInputA = new TagInputBean("Source");
        tagInputA.setIndex(":TestTagA");
        tagInputA.setCode("CodeA");
        tagInputA.setName("NameA");
        Tag tagA = tagService.processTag(tagInputA);
        assertNotNull (tagA);

        // Same code, but different label. Should create a new tag
        TagInputBean tagInputB = new TagInputBean("Source");
        tagInputB.setIndex(":TestTagB");
        tagInputB.setCode("CodeA");
        tagInputB.setName("NameA");
        Tag tagB = tagService.processTag(tagInputB);
        Tag tagC = tagService.processTag(tagInputB);
        assertNotNull (tagB);
        assertTrue(!tagA.getId().equals(tagB.getId()));
        assertTrue (tagC.getId().equals(tagB.getId()));
    }



}
