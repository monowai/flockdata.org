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

import org.flockdata.authentication.registration.bean.FortressInputBean;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Schema related tests
 * User: mike
 * Date: 3/04/14
 * Time: 9:54 AM
 */
public class TestSchemaManagement extends EngineBase {

    @Override
    public void cleanUpGraph() {
        // DAT-348
        super.cleanUpGraph();
    }

    @Test
    public void documentTypesTrackedPerFortress() throws Exception {
        cleanUpGraph();
        SystemUser suA = registerSystemUser("documentTypesTrackedPerFortress", mike_admin);

        Fortress fortressA = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("auditTestB", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA, "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = mediationFacade.trackEntity(suA.getCompany(), inputBean).getEntity().getMetaKey();

        inputBean = new EntityInputBean(fortressB, "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = mediationFacade.trackEntity(suA.getCompany(), inputBean).getEntity().getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));

        Collection<DocumentResultBean> docTypesA = fortressService.getFortressDocumentsInUse(suA.getCompany(), fortressA.getCode());
        assertEquals(1, docTypesA.size());

        Collection<DocumentResultBean> docTypesB = fortressService.getFortressDocumentsInUse(suA.getCompany(), fortressB.getCode());
        assertEquals(1, docTypesB.size());

        // Should be different key
        assertNotSame(docTypesA.iterator().next().getId(), docTypesB.iterator().next().getId());
    }

    @Test
    public void documentTypesTrackedPerCompany() throws Exception {
        cleanUpGraph();
        String sharedName = "entityTestB";
        SystemUser suA = registerSystemUser("OtherCo", harry);
        Fortress fortress = fortressService.registerFortress(suA.getCompany(), new FortressInputBean(sharedName, true));
        EntityInputBean entityInput = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = mediationFacade.trackEntity(suA.getCompany(), entityInput).getEntity().getMetaKey();


//        assertFalse(metaKeyA.equals(metaKeyB));

        // Same name different company
        SystemUser suB = registerSystemUser("documentTypesTrackedPerCompany", mike_admin);
        setSecurity(suB.getName());
        Fortress fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean(sharedName, true));

        setSecurity(suB.getName());
        entityInput = new EntityInputBean(fortressB, "wally", "DocTypeA", new DateTime(), "ABC123");
        TrackResultBean trackResult = mediationFacade.trackEntity(suB.getCompany(), entityInput);
        assertFalse(trackResult.entityExists());
        String metaKeyC = trackResult.getEntity().getMetaKey();

        assertFalse("KeyC should belong to company B", metaKeyC.equals(metaKeyA));

        // Secondary document type for company B
        entityInput = new EntityInputBean(fortressB, "wally", "DocTypeB", new DateTime(), "ABC123X");
        mediationFacade.trackEntity(suB.getCompany(), entityInput);


        // There should be a doc type per fortress and it should have the same Id.
        Collection<DocumentResultBean> documentsInUse = conceptService.getDocumentsInUse(suB.getCompany());
        assertEquals("CompanyB has should have 2 doc types", 2, documentsInUse.size());
        // Companies can't see each other stuff
        setSecurity(suA.getName());
        documentsInUse = conceptService.getDocumentsInUse(suA.getCompany());
        assertEquals(1, documentsInUse.size());


    }

    @Test
    public void documentTypesWork() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("documentTypesWork", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        String docName = "CamelCaseDoc";
        DocumentType docType = conceptService.resolveByDocCode(fortress, docName); // Creates if missing
        assertNotNull(docType);
        assertTrue(docType.getCode().contains(docName.toLowerCase()));
        assertEquals(docName, docType.getName());
        // Should be finding by code which is always Lower
        assertNotNull(conceptService.resolveByDocCode(fortress, docType.getName().toUpperCase(), false));
        try {
            conceptService.resolveByDocCode(fortress, null, false);
            fail("Null not handled correctly");
        } catch (IllegalArgumentException e) {
            // Good
        }

    }

    @Test
    public void duplicateDocumentTypes() throws Exception {
        cleanUpGraph();
        setSecurity(sally_admin);
        SystemUser su = registerSystemUser("duplicateDocumentTypes", sally_admin);
        assertNotNull(su);

        Fortress fortA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortA", true));
        Fortress fortB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortB", true));

        DocumentType dType = conceptService.resolveByDocCode(fortA, "ABC123", true);
        assertNotNull(dType);
        Long id = dType.getId();
        dType = conceptService.resolveByDocCode(fortA, "ABC123", false);
        assertEquals(id, dType.getId());

        DocumentType nextType = conceptService.resolveByDocCode(fortB, "ABC123", true);
        assertNotSame("Same company + different fortresses = different document types", dType, nextType);

        // Company 2 gets a different tag with the same name
        setSecurity(sally_admin); // Register an Auth user as an engine system user
        SystemUser suHarry = registerSystemUser("secondcompany", harry);
        setSecurity(harry); // Register an Auth user as an engine system user
        // Same fortress name, but different company results in a new fortress
        dType = conceptService.resolveByDocCode(fortressService.registerFortress(suHarry.getCompany(), new FortressInputBean("fortA", true)), "ABC123"); // Creates if missing
        assertNotNull(dType);
        assertNotSame(id, dType.getId());
    }

}
