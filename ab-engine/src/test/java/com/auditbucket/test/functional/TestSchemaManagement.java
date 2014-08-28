package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.DocumentType;
import junit.framework.Assert;
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
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));

        String apiKey = su.getApiKey();
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(new FortressInputBean("auditTestB", true));

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        inputBean = new MetaInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));
        // There should be a doc type per fortress and it should have the same Id.
        // ToDo: fortress actions based on fortress api-key
//        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get("/fortress/")
//                        .header("Api-Key", su.getApiKey())
//                                //.("company", su.getCompany())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(getJSON(new FortressInputBean(fortressName, true)))
//        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        Collection<DocumentType> docTypesA = fortressService.getFortressDocumentsInUse(su.getCompany(), fortressA.getCode());
        assertEquals(1, docTypesA.size());

        Collection<DocumentType> docTypesB = fortressService.getFortressDocumentsInUse(su.getCompany(), fortressB.getCode());
        assertEquals(1, docTypesB.size());

        // Should be different key
        assertNotSame(docTypesA.iterator().next().getId(), docTypesB.iterator().next().getId());
    }

    @Test
    public void documentTypesTrackedPerCompany() throws Exception {
        cleanUpGraph();
        //SystemUserResultBean cOtherAPI = registrationEP.registerSystemUser(new RegistrationBean("OtherCo", "harry")).getBody();
        SystemUser cOtherAPI = regService.registerSystemUser(new RegistrationBean("OtherCo", harry));

        String apiKey = registrationEP.registerSystemUser(new RegistrationBean(monowai, mike_admin)).getBody().getApiKey();
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("auditTestA", true));
        Fortress fortressB = fortressService.registerFortress(new FortressInputBean("auditTestB", true));

        // Same name different company
        Fortress fortressC = createFortress(cOtherAPI, "auditTestB");

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        inputBean = new MetaInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));

        inputBean = new MetaInputBean(fortressC.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyC = trackEP.trackHeader(inputBean,  cOtherAPI.getApiKey(), cOtherAPI.getApiKey()).getBody().getMetaKey();
        assertFalse(metaKeyC.equals(metaKeyA));
        assertFalse(metaKeyC.equals(metaKeyB));

        // There should be a doc type per fortress and it should have the same Id.
        Collection<DocumentType> docTypesA = companyEP.getDocumentTypes(apiKey, apiKey);
        assertEquals(2, docTypesA.size());
        // Companies can't see each other stuff
        docTypesA = companyEP.getDocumentTypes(cOtherAPI.getApiKey(), cOtherAPI.getApiKey());
        assertEquals(1, docTypesA.size());

    }

    @Test
    public void documentTypesWork() throws Exception {
        cleanUpGraph();
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        String docName = "CamelCaseDoc";
        DocumentType docType = schemaService.resolveDocType(fortress, docName); // Creates if missing
        assertNotNull(docType);
//        Assert.assertEquals(docName.toLowerCase(), docType.getCode());
        Assert.assertTrue(docType.getCode().contains(docName.toLowerCase()));
        Assert.assertEquals(docName, docType.getName());
        // Should be finding by code which is always Lower
        Assert.assertNotNull(schemaService.resolveDocType(fortress, docType.getName().toUpperCase(), false));
        try {
            schemaService.resolveDocType(fortress, null, false);
            fail("Null not handled correctly");
        } catch ( IllegalArgumentException e){
            // Good
        }

    }

    @Test
    public void duplicateDocumentTypes() throws Exception {
        cleanUpGraph();
        setSecurity(sally_admin);
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, sally_admin));
        Assert.assertNotNull(su);

        Fortress fortA = fortressService.registerFortress("fortA");
        Fortress fortB = fortressService.registerFortress("fortB");

        DocumentType dType = schemaService.resolveDocType(fortA, "ABC123", true);
        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocType(fortA, "ABC123", false);
        Assert.assertEquals(id, dType.getId());

        DocumentType nextType = schemaService.resolveDocType(fortB, "ABC123", true);
        Assert.assertNotSame("Same company + different fortresses = different document types", dType, nextType);

        // Company 2 gets a different tag with the same name
        setSecurity(sally_admin); // Register an Auth user as an engine system user
        regService.registerSystemUser(new RegistrationBean("secondcompany", harry));
        setSecurity(harry); // Register an Auth user as an engine system user
        // Same fortress name, but different company results in a new fortress
        dType = schemaService.resolveDocType(fortressService.registerFortress("fortA"), "ABC123"); // Creates if missing
        Assert.assertNotNull(dType);
        Assert.assertNotSame(id, dType.getId());
    }

}
