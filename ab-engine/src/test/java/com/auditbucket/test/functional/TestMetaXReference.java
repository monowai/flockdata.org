package com.auditbucket.test.functional;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.MetaHeader;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 1/04/14
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
@Transactional
public class TestMetaXReference extends TestEngineBase {

    private String monowai = "Monowai";
    private String mike = "mike";
    private String what = "{\"house\": \"house";

    @Test
    public void crossReferenceMetaKeysForSameCompany() throws Exception {
        registrationEP.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest", true), null, null).getBody();

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
        registrationEP.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest", true), null, null).getBody();

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
            trackEP.postCrossReferenceByCallerRef(fortress.getName(), sourceKey, xRef, "cites", null, null);
            fail("Exactly one check failed");
        } catch ( DatagioException e ){
            // good stuff!
        }
    }

    @Test
    public void crossReferenceByCallerRefsForFortress() throws Exception {
        registrationEP.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("auditTest", true), null, null).getBody();

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

        Collection<String> notFound = trackEP.postCrossReferenceByCallerRef(fortress.getName(), "ABC123", callerRefs, "cites", null, null);
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
    @Test
    public void crossReferenceWithInputBean() throws Exception {
        registrationEP.registerSystemUser(new RegistrationBean(monowai, mike));
        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("auditTest", true),null,  null).getBody();
        //Fortress fortressB = fortressEP.registerFortress(new FortressInputBean("auditTestB", true), null).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackHeader(inputBean, null, null).getBody();

        // These are the two records that will cite the previously created header
        MetaInputBean inputBeanB = new MetaInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackHeader(inputBeanB, null, null).getBody();
        MetaInputBean inputBeanC = new MetaInputBean(fortressA.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackHeader(inputBeanC, null, null).getBody();
        Map<String,List<String>>refs = new HashMap<>();
        List<String> callerRefs = new ArrayList<>();

        callerRefs.add("ABC321");
        callerRefs.add("ABC333");

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);

        List<CrossReferenceInputBean> notFound = trackEP.putCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(1, notFound.size());
        for (CrossReferenceInputBean crossReferenceInputBean : notFound) {
            assertTrue(crossReferenceInputBean.getReferences().get("cites").isEmpty());
        }

        Map<String, Collection<MetaHeader>> results = trackEP.getCrossReferenceByCallerRef(fortressA.getName(), "ABC123", "cites", null, null);
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
