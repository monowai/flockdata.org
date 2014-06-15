package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.DocumentType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 3/04/14
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */
@Transactional
public class TestSchemaManagement extends TestEngineBase {

    @Test
    public void documentTypesTrackedPerFortress() throws Exception {
        String apiKey = registrationEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody().getApiKey();

        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("auditTestA", true), apiKey, apiKey).getBody();
        Fortress fortressB = fortressEP.registerFortress(new FortressInputBean("auditTestB", true), apiKey, apiKey).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        inputBean = new MetaInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));
        // There should be a doc type per fortress and it should have the same Id.
        // ToDo: fortress actions based on fortress api-key
        Collection<DocumentType> docTypesA = fortressEP.getDocumentTypes (fortressA.getName(), apiKey, apiKey);
        assertEquals(1, docTypesA.size());

        Collection<DocumentType> docTypesB = fortressEP.getDocumentTypes (fortressB.getName(), apiKey, apiKey);
        assertEquals(1, docTypesB.size());

        // Should be the same key
        assertEquals(docTypesA.iterator().next().getId(), docTypesB.iterator().next().getId());
    }

    @Test
    public void documentTypesTrackedPerCompany() throws Exception {

        String cOtherAPI = registrationEP.registerSystemUser(new RegistrationBean("OtherCo", "harry")).getBody().getApiKey();


        String apiKey = registrationEP.registerSystemUser(new RegistrationBean(monowai, mike)).getBody().getApiKey();
        Fortress fortressA = fortressEP.registerFortress(new FortressInputBean("auditTestA", true), apiKey, apiKey).getBody();
        Fortress fortressB = fortressEP.registerFortress(new FortressInputBean("auditTestB", true), apiKey, apiKey).getBody();

        // Same name different company
        Fortress fortressC = fortressEP.registerFortress(new FortressInputBean("auditTestB", true), cOtherAPI, cOtherAPI).getBody();

        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyA = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        inputBean = new MetaInputBean(fortressB.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyB = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody().getMetaKey();

        assertFalse(metaKeyA.equals(metaKeyB));

        inputBean = new MetaInputBean(fortressC.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        String metaKeyC = trackEP.trackHeader(inputBean,  cOtherAPI, cOtherAPI).getBody().getMetaKey();
        assertFalse(metaKeyC.equals(metaKeyA));
        assertFalse(metaKeyC.equals(metaKeyB));

        // There should be a doc type per fortress and it should have the same Id.
        Collection<DocumentType> docTypesA = companyEP.getDocumentTypes(apiKey, apiKey);
        assertEquals(2, docTypesA.size());
        // Companies can't see each other stuff
        docTypesA = companyEP.getDocumentTypes(cOtherAPI, cOtherAPI);
        assertEquals(1, docTypesA.size());

    }
}
