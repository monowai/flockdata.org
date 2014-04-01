package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.model.Fortress;
import org.joda.time.DateTime;
import org.junit.Before;
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
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 1/04/14
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional

public class TestMetaXReference {
    @Autowired
    TrackEP trackEP;

    @Autowired
    FortressEP fortressEP;

    @Autowired
    RegistrationEP registrationEP;

    @Autowired
    private Neo4jTemplate template;

    private String monowai = "Monowai";
    private String mike = "test@ab.com";
    private Authentication authMike = new UsernamePasswordAuthenticationToken(mike, "user1");
    private String what = "{\"house\": \"house";

    @Before
    public void setSecurity() {
        SecurityContextHolder.getContext().setAuthentication(authMike);
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        if (!"rest".equals(System.getProperty("neo4j")))
            Neo4jHelper.cleanDb(template);
    }

    @Test
    public void crossReferenceMetaKeysForSameCompany() throws Exception {
        registrationEP.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest", true), null).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();

        assertNotNull(sourceKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        xRef.add("NonExistent");
        Collection<String> notFound = trackEP.putCrossReference(sourceKey, xRef, "cites", null, null);
        assertEquals(1, notFound.size());
        assertEquals("NonExistent", notFound.iterator().next());

        Map<String, Collection<MetaHeader>> results = trackEP.getCrossRefenceByMetaKey(sourceKey, "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<MetaHeader> headers = results.get("cites");
        assertNotNull ( headers);
        for (MetaHeader header : headers) {
            assertEquals(destKey, header.getMetaKey());
        }


    }

    @Test
    public void duplicateCallerRefForFortressFails() throws Exception {
        registrationEP.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest", true), null).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();

        assertNotNull(sourceKey);

        // Check that exception is thrown if the callerRef is not unique for the fortress
        Collection<String> xRef = new ArrayList<>();
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = trackEP.trackHeader(inputBean, null, null).getBody().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add("ABC321");
        xRef.add("Doesn't matter");
        try {
            trackEP.putCrossReferenceByCallerRef(fortress.getName(), sourceKey, xRef, "cites", null, null);
            fail("Exactly one check failed");
        } catch ( DatagioException e ){
            // good stuff!
        }
    }

    @Test
    public void crossReferenceByCallerRefsForFortress() throws Exception {
        registrationEP.register(new RegistrationBean(monowai, mike, "bah"));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest", true), null).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackHeader(inputBean, null, null).getBody();

        Collection<String> callerRefs = new ArrayList<>();
        // These are the two records that will cite the previously created header
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackHeader(inputBean, null, null).getBody();
        inputBean = new MetaInputBean(fortress.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackHeader(inputBean, null, null).getBody();

        callerRefs.add("ABC321");
        callerRefs.add("ABC333");

        Collection<String> notFound = trackEP.putCrossReferenceByCallerRef(fortress.getName(), "ABC123", callerRefs, "cites", null, null);
        assertEquals(0, notFound.size());
        Map<String, Collection<MetaHeader>> results = trackEP.getCrossReferenceByCallerRef(fortress.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<MetaHeader> headers = results.get("cites");
        assertNotNull ( headers);
        int count = 0;
        for (MetaHeader header : headers) {
            count ++;
        }
        assertEquals(2, count);
    }
}
