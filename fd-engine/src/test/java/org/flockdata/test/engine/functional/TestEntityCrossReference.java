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

import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityLinkInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.model.Entity;
import org.flockdata.track.bean.EntityKeyBean;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 1/04/14
 * Time: 4:12 PM
 */
public class TestEntityCrossReference extends EngineBase {

    @Test
    public void xRef_MetaKeysForSameCompany() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_MetaKeysForSameCompany", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(trackResultBean);
        String sourceKey = trackResultBean.getEntity().getMetaKey();

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        TrackResultBean destBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(destBean);
        String destKey = destBean.getEntity().getMetaKey();
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        xRef.add("NonExistent");
        Collection<String> notFound = entityService.crossReference(su.getCompany(), sourceKey, xRef, "cites");
        assertEquals(1, notFound.size());
        assertEquals("NonExistent", notFound.iterator().next());

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), sourceKey, "cites");
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull ( entities);
        for (Entity entity : entities) {
            assertEquals(destKey, entity.getMetaKey());
        }


    }

    @Test
    public void xRef_targetDoesNotExist() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_targetDoesNotExist", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("xRef_targetDoesNotExist", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String abc123 = trackResultBean.getEntity().getMetaKey();

        assertNotNull(trackResultBean);

        List<EntityKeyBean> callerRefs = new ArrayList<>();
        // DAT-443 - Request to xreference with an entity that does not yet exists.
        // Only will work if the fortress and doctype are known
        callerRefs.add(new EntityKeyBean(fortress.getName(), trackResultBean.getDocumentType().getName(), "ABC321"));
        EntityKeyBean sourceKey = new EntityKeyBean(new EntityLinkInputBean(inputBean));
        List<EntityKeyBean> results = entityService.crossReferenceEntities(su.getCompany(), sourceKey, callerRefs, "anyrlx");
        TestCase.assertTrue(results.isEmpty());

        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC321");
        ContentInputBean cib = new ContentInputBean(Helper.getRandomMap());
        inputBean.setContent(cib);

        // The Entity that previously did not exist, can have a log added and be treated like any other entity
        trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        TestCase.assertTrue(trackResultBean.entityExists());
        TestCase.assertEquals(ContentInputBean.LogStatus.OK, trackResultBean.getLogStatus());

        // (ABC123)-[anyrlx]-(ABC321)
        // Retrieving 123 returns 321
        Map<String, Collection<Entity>> xrefResults = entityService.getCrossReference(su.getCompany(), abc123, "anyrlx");
        TestCase.assertFalse(xrefResults.isEmpty());
        Collection<Entity>entities = xrefResults.get("anyrlx");
        assertEquals(1, entities.size());
        assertEquals("ABC321", entities.iterator().next().getCode());

        // Inverse of above - 321 returns 123
        xrefResults = entityService.getCrossReference(su.getCompany(), trackResultBean.getEntity().getMetaKey(), "anyrlx");
        TestCase.assertFalse(xrefResults.isEmpty());
        entities = xrefResults.get("anyrlx");
        assertEquals(1, entities.size());
        assertEquals("ABC123", entities.iterator().next().getCode());

    }

    @Test
    public void xRef_duplicateCallerRefForFortressFails() throws Exception {
        SystemUser su = registerSystemUser("xRef_duplicateCallerRefForFortressFails", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String callerRef = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getMetaKey();

        assertNotNull(callerRef);

        // Check that exception is thrown if the callerRef is not unique for the fortress
        Collection<EntityKeyBean> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(callerRef));

        xRef.add(new EntityKeyBean("ABC321"));
        xRef.add(new EntityKeyBean("Doesn't matter"));
        try {
            EntityKeyBean entityKey = new EntityKeyBean(fortress.getName(), "*", callerRef);
            entityService.crossReferenceEntities(su.getCompany(), entityKey, xRef, "cites");
            fail("Exactly one check failed");
        } catch ( FlockException e ){
            // good stuff!
        }
    }

    @Test
    public void xRef_ByCallerRefsForFortress() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_ByCallerRefsForFortress", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        Collection<EntityKeyBean> callerRefs = new ArrayList<>();
        // These are the two records that will cite the previously created entity
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        callerRefs.add(new EntityKeyBean("ABC321"));
        callerRefs.add(new EntityKeyBean("ABC333"));

        EntityKeyBean entityKey = new EntityKeyBean(fortress.getName(), "*", "ABC123")  ;
        Collection<EntityKeyBean> notFound = entityService.crossReferenceEntities(su.getCompany(), entityKey, callerRefs, "cites");
        assertEquals(0, notFound.size());
        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), fortress.getName(), "ABC123", "cites");
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull ( entities);
        int count = 0;
        for (Entity entity : entities) {
            assertNotNull(entity);
            count ++;
        }
        assertEquals(2, count);
    }
    @Test
    public void xRef_FromInputBeans() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("xRef_FromInputBeans", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        EntityInputBean inputBeanC = new EntityInputBean(fortressA.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        Map<String, List<EntityKeyBean>> refs = new HashMap<>();
        List<EntityKeyBean> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKeyBean("ABC321"));
        callerRefs.add(new EntityKeyBean("ABC333"));

        refs.put("cites",callerRefs);
        inputBean.setEntityLinks(refs);
        EntityLinkInputBean bean = new EntityLinkInputBean(inputBean);
        List<EntityLinkInputBean> entities = new ArrayList<>();
        entities.add(bean);

        List<EntityLinkInputBean> notFound = entityService.crossReferenceEntities(su.getCompany(), entities);
        assertEquals(1, notFound.size());
        for (EntityLinkInputBean crossReferenceInputBean : notFound) {
            assertTrue(crossReferenceInputBean.getIgnored().get("cites").isEmpty());
        }

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), fortressA.getName(), "ABC123", "cites");
        assertNotNull ( results);
        assertEquals(1, results.size());
        assertEquals(2, results.get("cites").size());
    }
    @Test
    public void xRef_AcrossFortressBoundaries() throws Exception {
        SystemUser su = registerSystemUser("xRef_AcrossFortressBoundaries", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTestB", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        Map<String, List<EntityKeyBean>> refs = new HashMap<>();
        List<EntityKeyBean> entityKeys = new ArrayList<>();

        entityKeys.add(new EntityKeyBean(fortressA.getName(), "DocTypeZ", "ABC321"));
        entityKeys.add(new EntityKeyBean(fortressB.getName(), "DocTypeS", "ABC333"));

        refs.put("cites",entityKeys);
        inputBean.setEntityLinks(refs);

        EntityLinkInputBean bean = new EntityLinkInputBean(inputBean);
        List<EntityLinkInputBean> inputs = new ArrayList<>();
        inputs.add(bean);

        List<EntityLinkInputBean> notFound = entityService.crossReferenceEntities(su.getCompany(), inputs);
        assertEquals(2, notFound.iterator().next().getIgnored().get("cites").size());

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        EntityInputBean inputBeanC = new EntityInputBean(fortressB.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        notFound = entityService.crossReferenceEntities(su.getCompany(), inputs);
        assertEquals(0, notFound.iterator().next().getIgnored().get("cites").size());

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), fortressA.getName(), "ABC123", "cites");
        assertNotNull ( results);
        assertEquals("Unexpected cites count", 2, results.get("cites").size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull ( entities);
        int count = 0;
        for (Entity entity : entities) {
            assertNotNull(entity);
            count ++;
        }
        assertEquals(2, count);
    }

    @Test
    public void xRef_CreatesUniqueRelationships() throws Exception {
        SystemUser su = registerSystemUser("xRef_CreatesUniqueRelationships", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("xRef_CreatesUniqueRelationships", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String sourceMetaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getMetaKey();

        assertNotNull(sourceMetaKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceMetaKey));

        xRef.add(destKey);
        entityService.crossReference(su.getCompany(), sourceMetaKey, xRef, "cites");
        // Try and force a duplicate relationship - only 1 should be created
        entityService.crossReference(su.getCompany(), sourceMetaKey, xRef, "cites");

        Map<String, Collection<Entity>> results = entityService.getCrossReference(su.getCompany(), sourceMetaKey, "cites");
        assertNotNull(results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertEquals("Tracking the same relationship name between two entities should not create duplicate relationships", 1, entities.size());
    }
}
