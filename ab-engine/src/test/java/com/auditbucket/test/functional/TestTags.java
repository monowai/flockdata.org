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

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
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
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestTags {
    @Autowired
    FortressService fortressService;

    @Autowired
    RegistrationService regService;

    @Autowired
    TagService tagService;


    @Autowired
    private Neo4jTemplate template;
    //private Logger log = LoggerFactory.getLogger(TestTags.class);
    private String company = "Monowai";
    private String mike = "mike@monowai.com";
    private String mark = "mark@monowai.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    private Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authMike);
        Neo4jHelper.cleanDb(template);
    }

    //ToDo: disabled until Neo4j2 and indexes. @org.junit.Test
    public void duplicateTagLists() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike, "bah"));
        assertNotNull(iSystemUser);

        Company iCompany = iSystemUser.getCompany();
        List<TagInputBean> tags = new ArrayList<>();
        //TODo: remove company from input/TagIB <> inherit Tag
        tags.add(new TagInputBean(iCompany, "FLOP"));
        tags.add(new TagInputBean(iCompany, "FLOP"));
        tags.add(new TagInputBean(iCompany, "FLOP"));
        tags.add(new TagInputBean(iCompany, "FLOP"));
        tags.add(new TagInputBean(iCompany, "FLOP"));

        Iterable<Tag> tagResult = tagService.processTags(tags);
        assertNotNull(tagResult);
        int count = 0;
        for (Tag next : tagResult) {
            assertEquals("FLOP", next.getName());
            assertEquals("flop", next.getCode());
            count++;
        }
        assertEquals(1, count);
        tags = new ArrayList<>();
        //TODo: remove company from input/TagIB <> inherit Tag
        tags.add(new TagInputBean(iCompany, "FLOP"));
        tags.add(new TagInputBean(iCompany, "FLOPPY"));
        tags.add(new TagInputBean(iCompany, "FLOPSY"));
        tags.add(new TagInputBean(iCompany, "FLOPPO"));
        tags.add(new TagInputBean(iCompany, "FLOPER"));
        tagResult = tagService.processTags(tags);
        count = 0;
        for (Tag next : tagResult) {
            count++;
        }
        assertEquals(5, count);

    }

    @org.junit.Test
    public void tagCreationAndSecurity() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike, "bah"));
        assertNotNull(iSystemUser);

        Company iCompany = iSystemUser.getCompany();

        List<TagInputBean> tags = new ArrayList<>();
        TagInputBean tagInput = new TagInputBean(iCompany, "FLOP");
        tags.add(tagInput);
        Iterable<Tag> tagResult = tagService.processTags(tags);
        assertNotNull(tagResult);
        assertTrue(tagResult.iterator().hasNext());
        // ToDo: FindService assertNull(tagService.findTag("ABC"));

        // ToDo: FindService assertNotNull(tagService.findTag("FLOP"));

        iSystemUser = regService.registerSystemUser(new RegistrationBean("ABC", "gina", "bah"));
        Authentication authGina = new UsernamePasswordAuthenticationToken("gina", "user1");
        SecurityContextHolder.getContext().setAuthentication(authGina);
        assertNull(tagService.findTag("FLOP")); // Can't see the Monowai company tag

        tagInput = new TagInputBean(iSystemUser.getCompany(), "FLOP");
        assertNotNull(tagService.processTag(tagInput));
        assertNull(tagService.findTag("ABC"));
        assertNotNull(tagService.findTag("FLOP"));
    }

    @Test
    public void tagUpdate() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike, "bah"));
        assertNotNull(iSystemUser);

        Company iCompany = iSystemUser.getCompany();

        Tag tag = tagService.processTag(new TagInputBean(iCompany, "FLOP"));
        assertNotNull(tag);
        assertNull(tagService.findTag("ABC"));
        // ToDo: Find tag isn't working N4j2 Node types and CreateIndex
        // Issue is manual nodes don't get in the index.

        Tag result = tagService.findTag("FLOP");
        assertNotNull(result);
        result = tagService.processTag(new TagInputBean("FLOPPY"));
        assertNotNull(result);
        assertEquals("FLOPPY", result.getName());
        // Tag update not yet supported
        //assertNull(tagService.findTag("FLOP"));
        //assertNotNull(tagService.findTag("FLOPPY"));

    }

    @Test
    public void duplicateDocumentTypes() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, mike, "bah"));
        assertNotNull(iSystemUser);

        DocumentType dType = tagService.resolveDocType("ABC123");
        assertNotNull(dType);
        Long id = dType.getId();
        dType = tagService.resolveDocType("ABC123");
        assertEquals(id, dType.getId());

        // Company 2 gets a different tag with the same name
        SecurityContextHolder.getContext().setAuthentication(authMark);
        regService.registerSystemUser(new RegistrationBean("secondcompany", mark, "bah"));
        dType = tagService.resolveDocType("ABC123");
        assertNotNull(dType);
        assertNotSame(id, dType.getId());


    }
}
