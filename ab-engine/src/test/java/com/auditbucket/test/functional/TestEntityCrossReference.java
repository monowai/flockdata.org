package com.auditbucket.test.functional;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityKey;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 1/04/14
 * Time: 4:12 PM
 */
@Transactional
public class TestEntityCrossReference extends TestEngineBase {

    @Test
    public void xRef_MetaKeysForSameCompany() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = trackEP.trackEntity(inputBean, null, null).getBody().getMetaKey();

        assertNotNull(sourceKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = trackEP.trackEntity(inputBean, null, null).getBody().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        xRef.add("NonExistent");
        Collection<String> notFound = trackEP.putCrossReference(sourceKey, xRef, "cites", null, null);
        assertEquals(1, notFound.size());
        assertEquals("NonExistent", notFound.iterator().next());

        Map<String, Collection<Entity>> results = trackEP.getCrossRefenceByMetaKey(sourceKey, "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<Entity> headers = results.get("cites");
        assertNotNull ( headers);
        for (Entity header : headers) {
            assertEquals(destKey, header.getMetaKey());
        }


    }

    @Test
    public void xRef_duplicateCallerRefForFortressFails() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = trackEP.trackEntity(inputBean, null, null).getBody().getMetaKey();

        assertNotNull(sourceKey);

        // Check that exception is thrown if the callerRef is not unique for the fortress
        Collection<EntityKey> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = trackEP.trackEntity(inputBean, null, null).getBody().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(new EntityKey("ABC321"));
        xRef.add(new EntityKey("Doesn't matter"));
        try {
            trackEP.postCrossReferenceByCallerRef(fortress.getName(), sourceKey, xRef, "cites", null, null);
            fail("Exactly one check failed");
        } catch ( DatagioException e ){
            // good stuff!
        }
    }

    @Test
    public void xRef_ByCallerRefsForFortress() throws Exception {
        registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackEntity(inputBean, null, null).getBody();

        Collection<EntityKey> callerRefs = new ArrayList<>();
        // These are the two records that will cite the previously created header
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackEntity(inputBean, null, null).getBody();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackEntity(inputBean, null, null).getBody();

        callerRefs.add(new EntityKey("ABC321"));
        callerRefs.add(new EntityKey("ABC333"));

        Collection<EntityKey> notFound = trackEP.postCrossReferenceByCallerRef(fortress.getName(), "ABC123", callerRefs, "cites", null, null);
        assertEquals(0, notFound.size());
        Map<String, Collection<Entity>> results = trackEP.getCrossReferenceByCallerRef(fortress.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull ( entities);
        int count = 0;
        for (Entity header : entities) {
            count ++;
        }
        assertEquals(2, count);
    }
    @Test
    public void xRef_FromInputBeans() throws Exception {
        registerSystemUser(monowai, mike_admin);
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackEntity(inputBean, null, null).getBody();

        // These are the two records that will cite the previously created header
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackEntity(inputBeanB, null, null).getBody();
        EntityInputBean inputBeanC = new EntityInputBean(fortressA.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackEntity(inputBeanC, null, null).getBody();
        Map<String, List<EntityKey>> refs = new HashMap<>();
        List<EntityKey> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKey("ABC321"));
        callerRefs.add(new EntityKey("ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);

        List<CrossReferenceInputBean> notFound = trackEP.postCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(1, notFound.size());
        for (CrossReferenceInputBean crossReferenceInputBean : notFound) {
            assertTrue(crossReferenceInputBean.getIgnored().get("cites").isEmpty());
        }

        Map<String, Collection<Entity>> results = trackEP.getCrossReferenceByCallerRef(fortressA.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<Entity> headers = results.get("cites");
        assertNotNull ( headers);
        int count = 0;
        for (Entity header : headers) {
            count ++;
        }
        assertEquals(2, count);
    }
    @Test
    public void xRef_AcrossFortressBoundaries() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestB", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        trackEP.trackEntity(inputBean, null, null).getBody();

        Map<String, List<EntityKey>> refs = new HashMap<>();
        List<EntityKey> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKey(fortressA.getName(), "DocTypeZ", "ABC321"));
        callerRefs.add(new EntityKey(fortressB.getName(), "DocTypeS", "ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);

        List<CrossReferenceInputBean> notFound = trackEP.postCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(2, notFound.iterator().next().getIgnored().get("cites").size());

        // These are the two records that will cite the previously created header
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        trackEP.trackEntity(inputBeanB, null, null).getBody();
        EntityInputBean inputBeanC = new EntityInputBean(fortressB.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        trackEP.trackEntity(inputBeanC, null, null).getBody();
        notFound = trackEP.postCrossReferenceByCallerRef(inputs, null, null);
        assertEquals(0, notFound.iterator().next().getIgnored().get("cites").size());

        Map<String, Collection<Entity>> results = trackEP.getCrossReferenceByCallerRef(fortressA.getName(), "ABC123", "cites", null, null);
        assertNotNull ( results);
        assertEquals("Unexpected cites count", 2, results.get("cites").size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull ( entities);
        int count = 0;
        for (Entity entity : entities) {
            count ++;
        }
        assertEquals(2, count);
    }

    @Test
    public void xRef_CreatesUniqueRelationships() throws Exception {
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("xRef_CreatesUniqueRelationships", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();

        assertNotNull(sourceKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        trackService.crossReference(su.getCompany(), sourceKey, xRef, "cites");
        // Try and force a duplicate relationship - only 1 should be created
        trackService.crossReference(su.getCompany(), sourceKey, xRef, "cites");

        Map<String, Collection<Entity>> results = trackEP.getCrossRefenceByMetaKey(sourceKey, "cites", null, null);
        assertNotNull(results);
        assertEquals(1, results.size());
        Collection<Entity> headers = results.get("cites");
        assertEquals("Tracking the same relationship name between two headers should not create duplicate relationships", 1, headers.size());
    }
}
