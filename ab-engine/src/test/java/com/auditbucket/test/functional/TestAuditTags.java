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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.TagValue;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.bean.AuditTagInputBean;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
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
    AuditService auditService;

    @Autowired
    RegistrationService regService;

    @Autowired
    TagService tagService;

    @Autowired
    AuditTagService auditTagService;

    @Autowired
    private Neo4jTemplate template;

    private Logger log = LoggerFactory.getLogger(TestAuditTags.class);

    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(uid, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(template);
    }

    @Test
    public void tagAuditRecords() {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        Fortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        Company iCompany = iSystemUser.getCompany();

        Tag tagInput = new TagInputBean(iCompany, "FLOP");

        Tag result = tagService.processTag(tagInput);
        assertNotNull(result);
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new Date(), "abc");
        AuditResultBean resultBean = auditService.createHeader(inputBean);
        AuditHeader header = auditService.getHeader(resultBean.getAuditKey());

        AuditTagInputBean auditTag = new AuditTagInputBean(null, resultBean.getAuditKey(), "!!!");
        try {
            auditTagService.processTag(header, auditTag);
            fail("No null argument exception detected");
        } catch (IllegalArgumentException ie) {
            // This should have happened
        }
        // First auditTag created
        auditTag = new AuditTagInputBean(tagInput.getName(), resultBean.getAuditKey(), "!!!");

        auditTagService.processTag(header, auditTag);
        assertNotNull(auditTag);

        Set<TagValue> tags = auditTagService.findTagValues(tagInput.getName(), "!!!");
        assertEquals(1, tags.size());

        auditTagService.processTag(header, auditTag);
        // Behaviour - Can't add the same tagValue twice for the same combo
        tags = auditTagService.findTagValues(tagInput.getName(), "!!!");
        assertEquals(1, tags.size());
    }

    @Test
    public void tagValueCRUD() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        Company iCompany = iSystemUser.getCompany();
        Tag tagInput = new TagInputBean(iCompany, "FLOP");

        Tag result = tagService.processTag(tagInput);
        assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new Date(), "abc");
        Map<String, Object> tagValues = new HashMap<String, Object>();
        tagValues.put("TagA", "AAAA");
        tagValues.put("TagB", "BBBB");
        tagValues.put("TagC", "CCCC");
        tagValues.put("TagD", "DDDD");
        aib.setTagValues(tagValues);
        AuditResultBean resultBean = auditService.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey(), true);
        Set<TagValue> tagSet = auditHeader.getTagValues();
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertEquals(0, auditTagService.findTagValues("TagC", "!!Twee!!").size());//
        // Remove a single tag
        Iterator<TagValue> iterator = tagSet.iterator();
        while (iterator.hasNext()) {
            TagValue value = iterator.next();
            if (value.getTag().getName().equals("TagB"))
                iterator.remove();
            if (value.getTag().getName().equals("TagC"))
                value.setTagValue("!!Twee!!");
        }

        assertEquals(3, tagSet.size());
        auditService.updateHeader(auditHeader);
        auditHeader = auditService.getHeader(resultBean.getAuditKey(), true);
        tagSet = auditHeader.getTagValues();
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());
        assertNotNull(auditTagService.findTagValues("TagC", "!!Twee!!"));
    }

    @Test
    public void nullTagValueCRUD() throws Exception {
        SystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        Company iCompany = iSystemUser.getCompany();
        Tag tagInput = new TagInputBean(iCompany, "FLOP");

        Tag result = tagService.processTag(tagInput);
        assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new Date(), "abc");
        Map<String, Object> tagValues = new HashMap<String, Object>();
        tagValues.put("TagA", null);
        tagValues.put("TagB", null);
        tagValues.put("TagC", null);
        tagValues.put("TagD", "DDDD");
        aib.setTagValues(tagValues);
        AuditResultBean resultBean = auditService.createHeader(aib);
        AuditHeader auditHeader = auditService.getHeader(resultBean.getAuditKey(), true);
        Set<TagValue> tagSet = auditHeader.getTagValues();
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        auditService.updateHeader(auditHeader);
        auditHeader = auditService.getHeader(resultBean.getAuditKey(), true);
        tagSet = auditHeader.getTagValues();
        assertNotNull(tagSet);
        Set<AuditHeader> headers = auditTagService.findTagAudits("TagA");
        assertNotNull(headers);
        assertEquals(auditHeader.getAuditKey(), headers.iterator().next().getAuditKey());
        headers = auditTagService.findTagAudits("TagC");
        assertNotNull(headers);
        assertEquals(auditHeader.getAuditKey(), headers.iterator().next().getAuditKey());
        headers = auditTagService.findTagAudits("TagD");
        assertNotNull(headers);
        assertEquals(auditHeader.getAuditKey(), headers.iterator().next().getAuditKey());
    }
}
