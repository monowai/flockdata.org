package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.DocumentType;
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
public class TestSchemaManagement extends TestEngineBase {

    @Test
    public void documentTypesTrackedPerFortress() throws Exception {
        cleanUpGraph();
        SystemUser suA = registerSystemUser(monowai, mike_admin);

        Fortress fortressA = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("auditTestB", true));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = mediationFacade.trackEntity(suA.getCompany(), inputBean).getMetaKey();

        inputBean = new EntityInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = mediationFacade.trackEntity(suA.getCompany(), inputBean).getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));
        // There should be a doc type per fortress and it should have the same Id.
        // ToDo: fortress actions based on fortress api-key
//        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get("/fortress/")
//                        .header("Api-Key", su.getApiKey())
//                                //.("company", su.getCompany())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(getJSON(new FortressInputBean(fortressName, true)))
//        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

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
        Thread.sleep(100);
        //SystemUserResultBean cOtherAPI = registrationEP.registerSystemUser(new RegistrationBean("OtherCo", "harry")).getBody();
        SystemUser suA = registerSystemUser(monowai, mike_admin);
        SystemUser suB = registerSystemUser("OtherCo", harry);

        Fortress fortressA = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("auditTestB", true));

        // Same name different company
        Fortress fortressC = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("auditTestB"));

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = mediationFacade.trackEntity(suA.getCompany(), inputBean).getMetaKey();

        inputBean = new EntityInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = mediationFacade.trackEntity(suB.getCompany(), inputBean).getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));

        inputBean = new EntityInputBean(fortressC.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyC = mediationFacade.trackEntity(suA.getCompany(), inputBean).getMetaKey();
        assertFalse(metaKeyC.equals(metaKeyA));
        assertFalse(metaKeyC.equals(metaKeyB));

        // There should be a doc type per fortress and it should have the same Id.
        Collection<DocumentResultBean> documentsInUse = schemaService.getDocumentsInUse(suA.getCompany());
        assertEquals(2, documentsInUse.size());
        // Companies can't see each other stuff
        documentsInUse = schemaService.getDocumentsInUse(suB.getCompany());
        assertEquals(1, documentsInUse.size());

    }

    @Test
    public void documentTypesWork() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser(monowai, mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        String docName = "CamelCaseDoc";
        DocumentType docType = schemaService.resolveDocType(fortress, docName); // Creates if missing
        assertNotNull(docType);
        assertTrue(docType.getCode().contains(docName.toLowerCase()));
        assertEquals(docName, docType.getName());
        // Should be finding by code which is always Lower
        assertNotNull(schemaService.resolveDocCode(fortress, docType.getName().toUpperCase(), false));
        try {
            schemaService.resolveDocCode(fortress, null, false);
            fail("Null not handled correctly");
        } catch ( IllegalArgumentException e){
            // Good
        }

    }

    @Test
    public void duplicateDocumentTypes() throws Exception {
        cleanUpGraph();
        setSecurity(sally_admin);
        SystemUser su = registerSystemUser(monowai, sally_admin);
        assertNotNull(su);

        Fortress fortA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortA", true));
        Fortress fortB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortB", true));

        DocumentType dType = schemaService.resolveDocCode(fortA, "ABC123", true);
        assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocCode(fortA, "ABC123", false);
        assertEquals(id, dType.getId());

        DocumentType nextType = schemaService.resolveDocCode(fortB, "ABC123", true);
        assertNotSame("Same company + different fortresses = different document types", dType, nextType);

        // Company 2 gets a different tag with the same name
        setSecurity(sally_admin); // Register an Auth user as an engine system user
        SystemUser suHarry = registerSystemUser("secondcompany", harry);
        setSecurity(harry); // Register an Auth user as an engine system user
        // Same fortress name, but different company results in a new fortress
        dType = schemaService.resolveDocType(fortressService.registerFortress(suHarry.getCompany(), new FortressInputBean("fortA",true)), "ABC123"); // Creates if missing
        assertNotNull(dType);
        assertNotSame(id, dType.getId());
    }

}
