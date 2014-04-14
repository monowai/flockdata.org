package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.LogInputBean;
import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.bean.TrackResultBean;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import junit.framework.Assert;
import org.joda.time.DateTime;
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

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 6:43 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TrackAPIKeys {
    @Autowired
    TrackEP trackEP;

    @Autowired
    RegistrationEP regEP;

    @Autowired
    FortressEP fortressEP;

    @Autowired
    private Neo4jTemplate template;

    private String monowai = "Monowai";
    private String mike = "test@ab.com";
    private String mark = "mark@null.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    private Authentication authMark = new UsernamePasswordAuthenticationToken(mark, "user1");

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        if (!"rest".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Test
    public void testApiKeysWorkInPrecedence() throws Exception {
        // No Security Access necessary.
        SecurityContextHolder.getContext().setAuthentication(null);
        SystemUser sysUser = regEP.registerSystemUser(new RegistrationBean(monowai, mike, "bah")).getBody();
        assertNotNull(sysUser);
        String apiKey = sysUser.getCompany().getApiKey();

        Assert.assertNotNull(apiKey);
        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("testApiKeysWorkInPrecedence"), sysUser.getCompany().getApiKey()).getBody();
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC123");

        // Fails due to NoAuth or key
        TrackResultBean result;
        try {
            result = trackEP.trackHeader(inputBean, null, null).getBody();
            assertNull(result);
            fail("Security Exception did not occur");
        } catch (SecurityException e) {
            // Good
        }

        // Should work now
        SecurityContextHolder.getContext().setAuthentication(authMike);//

        result = trackEP.trackHeader(inputBean, null, null).getBody(); // Works due to basic authz
        assertNotNull(result);  // works coz basic authz

        final MetaHeader header = trackEP.getMetaHeader(result.getMetaKey(), apiKey, apiKey).getBody();
        assertNotNull(header);
        SecurityContextHolder.getContext().setAuthentication(authMark);// Wrong user, but valid API key
        assertNotNull(trackEP.trackHeader(inputBean, apiKey, null));// works
        assertNotNull(trackEP.trackHeader(inputBean, null, apiKey));// works
        assertNotNull(trackEP.trackHeader(inputBean, "invalidApiKey", apiKey));// Header overrides request
        try {
            assertNull(trackEP.trackHeader(inputBean, apiKey, "123")); // Illegal result
            Assert.fail("this should not have worked due to invalid api key");
        } catch (DatagioException e) {
            // this should happen due to invalid api key
        }
        SecurityContextHolder.getContext().setAuthentication(null);// No user context, but valid API key
        assertNotNull(trackEP.trackHeader(inputBean, apiKey, null));// works
        assertNotNull(trackEP.trackHeader(inputBean, null, apiKey));// works
        assertNotNull(trackEP.trackHeader(inputBean, "invalidApiKey", apiKey));// Header overrides request
        try {
            assertNull(trackEP.trackHeader(inputBean, apiKey, "123")); // Illegal result
            Assert.fail("this should not have worked due to invalid api key");
        } catch (DatagioException e) {
            // this should happen due to invalid api key
        }
    }

    @Test
    public void apiCallsSecuredByAccessKey() throws Exception {
        // No authorization - only API keys
        SecurityContextHolder.getContext().setAuthentication(null);
        SystemUser sysUser = regEP.registerSystemUser(new RegistrationBean(monowai, mike, "bah")).getBody();
        assertNotNull(sysUser);
        String apiKey = sysUser.getCompany().getApiKey();

        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("apiCallsSecuredByAccessKey"), sysUser.getCompany().getApiKey()).getBody();
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "TestTrack", new DateTime(), "ABC9990");
        String what = "{\"house\": \"house\"}";
        LogInputBean log = new LogInputBean("harry", new DateTime(), what);
        inputBean.setLog(log);

        TrackResultBean result = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody(); // Works due to basic authz

        assertNotNull(trackEP.getMetaHeader(result.getMetaKey(), apiKey, apiKey).getBody());
        assertNotNull(trackEP.getAuditSummary(result.getMetaKey(), apiKey, apiKey).getBody());
        assertNotNull(trackEP.getAuditTags(result.getMetaKey(), apiKey, apiKey).getBody());
        assertNotNull(trackEP.getByCallerRef(result.getFortressName(), result.getDocumentType(), result.getCallerRef(), apiKey, apiKey));
        assertNotNull(trackEP.getByCallerRef(fortressA.getName(), inputBean.getCallerRef(), apiKey, apiKey).iterator().hasNext());
        assertNotNull(trackEP.getLogs(result.getMetaKey(), apiKey, apiKey).iterator().next());
        assertNotNull(trackEP.getLastChange(result.getMetaKey(), apiKey, apiKey));
        Long logId = trackEP.getLogs(result.getMetaKey(), apiKey, apiKey).iterator().next().getId();
        assertNotNull(trackEP.getLogWhat(result.getMetaKey(), logId, apiKey, apiKey));
        assertNotNull(trackEP.getLastChangeWhat(result.getMetaKey(), apiKey, apiKey));
        assertNotNull(trackEP.getFullLog(result.getMetaKey(), logId, apiKey, apiKey));

    }
}