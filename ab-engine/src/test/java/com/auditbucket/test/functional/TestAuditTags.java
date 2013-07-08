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

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditTagInputBean;
import com.auditbucket.core.registration.bean.RegistrationBean;
import com.auditbucket.core.registration.bean.TagInputBean;
import com.auditbucket.core.registration.service.FortressService;
import com.auditbucket.core.registration.service.RegistrationService;
import com.auditbucket.core.registration.service.TagService;
import com.auditbucket.core.service.AuditService;
import com.auditbucket.core.service.AuditTagService;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.model.ITag;
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
 * Date: 26/06/13
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
        ISystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        assertNotNull(iSystemUser);

        IFortress fortress = fortressService.registerFortress("ABC");
        assertNotNull(fortress);

        ICompany iCompany = iSystemUser.getCompany();

        ITag tagInput = new TagInputBean(iCompany, "FLOP");

        ITag result = tagService.processTag(tagInput);
        assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new Date(), "abc");
        aib = auditService.createHeader(aib);

        AuditTagInputBean auditTag = new AuditTagInputBean(null, aib.getAuditKey(), "!!!");
        try {
            auditTagService.processTag(auditTag);
            fail("No null argument exception detected");
        } catch (IllegalArgumentException ie) {
            // This should have happened
        }
        // First auditTag created
        auditTag = new AuditTagInputBean(tagInput.getName(), aib.getAuditKey(), "!!!");

        auditTagService.processTag(auditTag);
        assertNotNull(auditTag);

        Set<ITagValue> tags = auditTagService.findTagValues(tagInput.getName(), "!!!");
        assertEquals(1, tags.size());

        auditTagService.processTag(auditTag);
        // Behaviour - Can't add the same tagValue twice for the same combo
        tags = auditTagService.findTagValues(tagInput.getName(), "!!!");
        assertEquals(1, tags.size());
    }

    @Test
    public void tagValueCRUD() throws Exception {
        ISystemUser iSystemUser = regService.registerSystemUser(new RegistrationBean(company, uid, "bah"));
        fortressService.registerFortress("ABC");

        ICompany iCompany = iSystemUser.getCompany();
        ITag tagInput = new TagInputBean(iCompany, "FLOP");

        ITag result = tagService.processTag(tagInput);
        assertNotNull(result);
        AuditHeaderInputBean aib = new AuditHeaderInputBean("ABC", "auditTest", "aTest", new Date(), "abc");
        Map<String, String> tagValues = new HashMap<String, String>();
        tagValues.put("TagA", "AAAA");
        tagValues.put("TagB", "BBBB");
        tagValues.put("TagC", "CCCC");
        tagValues.put("TagD", "DDDD");
        aib.setTagValues(tagValues);
        aib = auditService.createHeader(aib);
        IAuditHeader auditHeader = auditService.getHeader(aib.getAuditKey(), true);
        Set<ITagValue> tagSet = auditHeader.getTagValues();
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertEquals(0, auditTagService.findTagValues("TagC", "!!Twee!!").size());
        // Remove a single tag
        Iterator<ITagValue> iterator = tagSet.iterator();
        while (iterator.hasNext()) {
            ITagValue value = iterator.next();
            if (value.getTag().getName().equals("TagB"))
                iterator.remove();
            if (value.getTag().getName().equals("TagC"))
                value.setTagValue("!!Twee!!");
        }

        assertEquals(3, tagSet.size());
        auditService.updateHeader(auditHeader);
        auditHeader = auditService.getHeader(aib.getAuditKey(), true);
        tagSet = auditHeader.getTagValues();
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());
        assertNotNull(auditTagService.findTagValues("TagC", "!!Twee!!"));
    }

}
