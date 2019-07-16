/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Functional tests for AdminServices
 *
 * @author mholdsworth
 * @since 19/02/2016
 */
public class TestAdmin extends EngineBase {

    @Test
    public void deleteFortressWithEntitiesAndTagsOnly() throws Exception {

        SystemUser su = registerSystemUser("deleteFortressWithEntitiesAndTagsOnly", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");

        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", new EntityTagRelationshipInput("deltest"));
        inputBean.addTag(tagInputBean);

        assertNotNull("Why is this null ??", mediationFacade);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String key = resultBean.getEntity().getKey();

        assertNotNull(key);
        assertNotNull(entityService.getEntity(su.getCompany(), key));

        inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");
        inputBean.addTag(tagInputBean);

        mediationFacade.trackEntity(su.getCompany(), inputBean);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            mediationFacade.purge(su.getCompany(), fortress.getName());
            fail("An authorisation exception should have been thrown");
        } catch (Exception e) {
            // This is good
        }
        setSecurity();
        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        assertNull(fortressService.findByName(su.getCompany(), fortress.getName()));
        // This should fail
        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), key);
    }

    @Test
    public void unauthorisedUserCannotDeleteFortress() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("unauthorisedUserCannotDeleteFortress", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("deleteFortressPurgesEntitiesAndLogs", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "deleteFortressPurgesEntitiesAndLogs", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String key = resultBean.getEntity().getKey();

        assertNotNull(key);
        assertNotNull(entityService.getEntity(su.getCompany(), key));

        setUnauthorized();
        // Assert that unauthorised user can't purge a fortress
        exception.expect(AuthenticationException.class);
        mediationFacade.purge(fortress);
    }

    @Test
    public void purgedFortressRemovesEntities() throws Exception {

        SystemUser su = registerSystemUser("purgedFortressRemovesEntities", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("deleteFortressPurgesEntitiesAndLogs", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "deleteFortressPurgesEntitiesAndLogs", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String entityKey = resultBean.getEntity().getKey();

        assertNotNull(entityKey);
        assertNotNull(entityService.getEntity(su.getCompany(), entityKey));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", entityKey, new DateTime(), ContentDataHelper.getRandomMap()));
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", entityKey, new DateTime(), ContentDataHelper.getRandomMap()));

        Assert.assertEquals(2, entityService.getLogCount(su.getCompany(), resultBean.getEntity().getKey()));

        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        assertNull(fortressService.findByName(su.getCompany(), fortress.getName()));
        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), entityKey);
    }

    @Test
    public void deleteFortressPurgesDataWithTags() throws Exception {

        SystemUser su = registerSystemUser("deleteFortressPurgesDataWithTags", mike_admin);
        assertNotNull(su);
        assertNotNull(su.getCompany());

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        assertNotNull(fortress);
        assertNotNull(fortress.getCompany());
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");
        inputBean.addTag(new TagInputBean("DeleteTest", "NamedTag", "deltest"));

        assertNotNull("Why is this null ??", mediationFacade);
        assertNotNull(su.getCompany());
        assertNotNull(inputBean);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        String key = resultBean.getEntity().getKey();

        assertNotNull(key);
        assertNotNull(entityService.getEntity(su.getCompany(), key));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", key, new DateTime(), ContentDataHelper.getRandomMap()));

        inputBean.setCode("123abc");
        inputBean.setKey(null);
        inputBean.setContent(new ContentInputBean("wally", key, new DateTime(), ContentDataHelper.getRandomMap()));
        mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);

        setSecurity(harry);// Harry is not an admin
        // Assert that unauthorised user can't purge a fortress
        exception.expect(AccessDeniedException.class);
        mediationFacade.purge(su.getCompany(), fortress.getName());
        setUnauthorized();
        exception.expect(AccessDeniedException.class);
        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");

        assertNull(fortressService.findByName(su.getCompany(), fortress.getName()));
        // This should fail
        exception.expect(NotFoundException.class);
        assertNull(entityService.getEntity(su.getCompany(), key));


    }

    @Test
    public void purgeFortressClearsDown() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("purgeFortressClearsDown", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeFortressClearsDown", true));

        EntityInputBean trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.addTag(new TagInputBean("anyName", "TestTag", new EntityTagRelationshipInput("rlx")));
        trackBean.addTag(new TagInputBean("otherName", "TestTag", "rlxValue").setReverse(true));
        ContentInputBean logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);
        String resultA = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        assertNotNull(resultA);

        trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.addTag(new TagInputBean("anyName", "TestTag", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "TestTag", "rlxValue").setReverse(true));
        logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);

        String resultB = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        Collection<String> others = new ArrayList<>();
        others.add(resultB);
        entityService.crossReference(su.getCompany(), resultA, others, "rlxName");

        others = new ArrayList<>();
        others.add(resultA);
        entityService.crossReference(su.getCompany(), resultB, others, "rlxNameB");

        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), resultA);

        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), resultB);


    }

    @Test
    public void conceptsDeleteAndRecreate() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("conceptsDeleteAndRecreate", mike_admin);
        FortressInputBean fib = new FortressInputBean("purgeFortressClearsDown", true);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        EntityInputBean trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.addTag(new TagInputBean("anyName", "TestTag", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "TestTag", "rlxValue").setReverse(true));
        ContentInputBean logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);
        String resultA = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        assertNotNull(resultA);

        Collection<DocumentResultBean> documents = conceptService.getDocumentsInUse(su.getCompany(), fib.getName());
        assertEquals(1, documents.size());

        mediationFacade.purge(fortress);
        Long fortressId = fortress.getId();
        Long segmentId = fortress.getDefaultSegment().getId();
        Long documentId = documents.iterator().next().getId();

        // Fortress is now invalid

        EngineBase.waitAWhile("Waiting for Async processing to complete");

        try {
            entityService.getEntity(su.getCompany(), resultA);
            fail("Expected not to find the entity after the fortress was purged");
        } catch (NotFoundException e) {
            // good
        }
        assertNull(fortressService.findByName(fib.getName()));
        assertNodeDoesNotExist("Fortress should not exist", fortressId);
        assertNodeDoesNotExist("Segment should not exist", segmentId);
        assertNodeDoesNotExist("DocumentType should not exist", documentId);
    }

    @Test
    public void purgeSegmentData() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("purgeSegmentData", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeSegmentData", true));

        EntityInputBean trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.setSegment("SegmentA");

        ContentInputBean logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);
        String resultA = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        assertNotNull(resultA);

        trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.setSegment("SegmentB");
        logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);

        String resultB = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        mediationFacade.purge(su.getCompany(), fortress.getCode(), trackBean.getDocumentType().getCode());
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        try {
            entityService.getEntity(su.getCompany(), resultA);
            fail("Entity should have been purged");
        } catch (NotFoundException e) {
            // Expected
        }
        try {
            entityService.getEntity(su.getCompany(), resultB);

            fail("Entity should have been purged");
        } catch (NotFoundException e) {
            // Expected
        }

        assertNotNull("Purging segments should not delete the fortress", fortressService.findByCode(su.getCompany(), fortress.getCode()));
    }

    @Test
    public void purgeSingleSegment() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("purgeSingleSegment", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeSingleSegment", true));
        FortressNode fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeSingleSegmentB", true));

        EntityInputBean trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.setSegment("SegmentA");

        ContentInputBean logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);
        String resultA = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        assertNotNull(resultA);

        trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.setSegment("SegmentB");
        logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);

        String resultB = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        // This Entity should not be affected
        trackBean = new EntityInputBean(fortressB, "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.setSegment("SegmentB");
        logBean = new ContentInputBean("me", DateTime.now(), ContentDataHelper.getRandomMap());
        trackBean.setContent(logBean);

        String resultC = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();


        mediationFacade.purge(su.getCompany(), fortress.getCode(),
            trackBean.getDocumentType().getCode(),
            "SegmentA");
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), resultA);

        assertNotNull(entityService.getEntity(su.getCompany(), resultB));

        assertNotNull("Purging segments should not delete the fortress", fortressService.findByCode(su.getCompany(), fortress.getCode()));
        assertEquals("Only Default and SegmentB should remain", 2, fortressService.getSegments(fortress).size());

        assertNotNull(entityService.getEntity(su.getCompany(), resultC));

        mediationFacade.purge(su.getCompany(), fortress.getCode());
        mediationFacade.purge(su.getCompany(), fortressB.getCode());
        Thread.sleep(400);
        assertNull(entityService.getEntity(su.getCompany(), resultB));

    }

    @Test
    public void purgeSingleDocTypeOnSharedSegment() throws Exception {
        // Two Fortresses sharing a single segment and DocType. Purging one fortress/type/segment should not affect the other
        setSecurity();
        SystemUser su = registerSystemUser("purgeSingleDocTypeOnSharedSegment", mike_admin);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeSingleDocTypeOnSharedSegment", true));
        FortressNode fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeSingleDocTypeOnSharedSegmentB", true));
        String docType = "CompanyNode";
        String segment = "SharedSegment";

        EntityInputBean trackBean = new EntityInputBean(fortress, "olivia@ast.com", docType, null, "abc1");
        trackBean.setSegment(segment);
        String resultA = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();
        assertNotNull(resultA);

        trackBean = new EntityInputBean(fortress, "olivia@ast.com", "DontDelete", null, "abc99");
        trackBean.setSegment(segment);
        String resultD = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();
        assertNotNull(resultD);


        trackBean = new EntityInputBean(fortress, "olivia@ast.com", docType, null, "abc2");
        trackBean.setSegment(segment);
        String resultB = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();

        trackBean = new EntityInputBean(fortressB, "olivia@ast.com", docType, null, "abc3");
        trackBean.setSegment(segment);
        String resultC = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getKey();
        EngineBase.waitAWhile("Waiting for Async processing to complete");


        mediationFacade.purge(su.getCompany(), fortress.getCode(),
            trackBean.getDocumentType().getCode(),
            segment);

        EngineBase.waitAWhile("Waiting for Async processing to complete");

        try {
            entityService.getEntity(su.getCompany(), resultA);
            fail("Expected Not Found");
        } catch (NotFoundException e) {
            // expected
        }

        try {
            entityService.getEntity(su.getCompany(), resultB);
            fail("Expected Not Found");
        } catch (NotFoundException e) {
            // expected
        }


        assertNotNull("Purging segments should not delete the fortress", fortressService.findByCode(su.getCompany(), fortress.getCode()));
        assertEquals("Shared segment should still exist", 2, fortressService.getSegments(fortress).size());

        assertNotNull("Result C should still exist; in a separate fortress", entityService.getEntity(su.getCompany(), resultC));

        assertNotNull("Result D should still exist; not a document to delete", entityService.getEntity(su.getCompany(), resultD));

        assertNotNull("Document Type was removed", conceptService.findDocumentType(fortress, docType));

    }

}
