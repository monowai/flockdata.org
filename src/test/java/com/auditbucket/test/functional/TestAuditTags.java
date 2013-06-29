package com.auditbucket.test.functional;

/**
 * User: mike
 * Date: 27/06/13
 * Time: 4:49 PM
 */

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditTagInputBean;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.audit.service.AuditTagService;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
import com.auditbucket.registration.model.ITag;
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

import java.util.Date;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * User: mike
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

}
