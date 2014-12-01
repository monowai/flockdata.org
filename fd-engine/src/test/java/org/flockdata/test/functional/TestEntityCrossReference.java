/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.functional;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.track.bean.CrossReferenceInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityKey;
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
        String sourceKey = trackResultBean.getEntityBean().getMetaKey();

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        TrackResultBean destBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(destBean);
        String destKey = destBean.getEntityBean().getMetaKey();
        assertFalse(destKey.equals(sourceKey));

        xRef.add(destKey);
        xRef.add("NonExistent");
        Collection<String> notFound = trackService.crossReference(su.getCompany(), sourceKey, xRef, "cites");
        assertEquals(1, notFound.size());
        assertEquals("NonExistent", notFound.iterator().next());

        Map<String, Collection<Entity>> results = trackService.getCrossReference(su.getCompany(), sourceKey, "cites");
        assertNotNull ( results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertNotNull ( entities);
        for (Entity entity : entities) {
            assertEquals(destKey, entity.getMetaKey());
        }


    }

    @Test
    public void xRef_duplicateCallerRefForFortressFails() throws Exception {
        SystemUser su = registerSystemUser("xRef_duplicateCallerRefForFortressFails", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String callerRef = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntityBean().getMetaKey();

        assertNotNull(callerRef);

        // Check that exception is thrown if the callerRef is not unique for the fortress
        Collection<EntityKey> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntityBean().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(callerRef));

        xRef.add(new EntityKey("ABC321"));
        xRef.add(new EntityKey("Doesn't matter"));
        try {
            EntityKey entityKey = new EntityKey(fortress.getName(), "*", callerRef);
            trackService.crossReferenceEntities(su.getCompany(), entityKey, xRef, "cites");
            fail("Exactly one check failed");
        } catch ( FlockException e ){
            // good stuff!
        }
    }

    @Test
    public void xRef_ByCallerRefsForFortress() throws Exception {
        SystemUser su = registerSystemUser("xRef_ByCallerRefsForFortress", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        Collection<EntityKey> callerRefs = new ArrayList<>();
        // These are the two records that will cite the previously created entity
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        callerRefs.add(new EntityKey("ABC321"));
        callerRefs.add(new EntityKey("ABC333"));

        EntityKey entityKey = new EntityKey(fortress.getName(), "*", "ABC123")  ;
        Collection<EntityKey> notFound = trackService.crossReferenceEntities(su.getCompany(), entityKey, callerRefs, "cites");
        assertEquals(0, notFound.size());
        Map<String, Collection<Entity>> results = trackService.getCrossReference(su.getCompany(), fortress.getName(), "ABC123", "cites");
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
        SystemUser su = registerSystemUser("xRef_FromInputBeans", mike_admin);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        EntityInputBean inputBeanC = new EntityInputBean(fortressA.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        Map<String, List<EntityKey>> refs = new HashMap<>();
        List<EntityKey> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKey("ABC321"));
        callerRefs.add(new EntityKey("ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > entities = new ArrayList<>();
        entities.add(bean);

        List<CrossReferenceInputBean> notFound = trackService.crossReferenceEntities(su.getCompany(), entities);
        assertEquals(1, notFound.size());
        for (CrossReferenceInputBean crossReferenceInputBean : notFound) {
            assertTrue(crossReferenceInputBean.getIgnored().get("cites").isEmpty());
        }

        Map<String, Collection<Entity>> results = trackService.getCrossReference(su.getCompany(), fortressA.getName(), "ABC123", "cites");
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

        Map<String, List<EntityKey>> refs = new HashMap<>();
        List<EntityKey> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKey(fortressA.getName(), "DocTypeZ", "ABC321"));
        callerRefs.add(new EntityKey(fortressB.getName(), "DocTypeS", "ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);

        List<CrossReferenceInputBean> notFound = trackService.crossReferenceEntities(su.getCompany(), inputs);
        assertEquals(2, notFound.iterator().next().getIgnored().get("cites").size());

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        EntityInputBean inputBeanC = new EntityInputBean(fortressB.getName(), "wally", "DocTypeS", new DateTime(), "ABC333");
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        notFound = trackService.crossReferenceEntities(su.getCompany(), inputs);
        assertEquals(0, notFound.iterator().next().getIgnored().get("cites").size());

        Map<String, Collection<Entity>> results = trackService.getCrossReference(su.getCompany(), fortressA.getName(), "ABC123", "cites");
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
        String sourceMetaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntityBean().getMetaKey();

        assertNotNull(sourceMetaKey);

        Collection<String> xRef = new ArrayList<>();
        inputBean = new EntityInputBean(fortress.getName(), "wally", "DocTypeZ", new DateTime(), "ABC321");
        String destKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntityBean().getMetaKey();
        assertNotNull(destKey);
        assertFalse(destKey.equals(sourceMetaKey));

        xRef.add(destKey);
        trackService.crossReference(su.getCompany(), sourceMetaKey, xRef, "cites");
        // Try and force a duplicate relationship - only 1 should be created
        trackService.crossReference(su.getCompany(), sourceMetaKey, xRef, "cites");

        Map<String, Collection<Entity>> results = trackService.getCrossReference(su.getCompany(), sourceMetaKey, "cites");
        assertNotNull(results);
        assertEquals(1, results.size());
        Collection<Entity> entities = results.get("cites");
        assertEquals("Tracking the same relationship name between two entities should not create duplicate relationships", 1, entities.size());
    }
}
