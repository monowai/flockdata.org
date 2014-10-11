package com.auditbucket.test.functional;

import com.auditbucket.profile.ImportProfileDeserializer;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.transform.ColumnDefinition;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 8:35 PM
 */
public class TestProfiles extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(TestProfiles.class);
    @Autowired
    ImportProfileService profileService;


    @Test
    @Transactional
    public void create_Profile() throws Exception{
        SystemUser su = registerSystemUser("create_Profile", mike_admin);

        ProfileConfiguration profile = ImportProfileDeserializer.getImportParams("/test_profile.json");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("create_profile", true));
        DocumentType docType = schemaService.resolveByDocCode(fortress, "Olympic");
        profileService.save(fortress, docType, profile);
        ProfileConfiguration savedProfile = profileService.get(fortress, docType);
        assertNotNull ( savedProfile);
        assertEquals(profile.getFortressUser(), savedProfile.getFortressUser());
        assertEquals(profile.isEntityOnly(), savedProfile.isEntityOnly());
        assertEquals(profile.getContent().size(), savedProfile.getContent().size());
        ColumnDefinition column = savedProfile.getContent().get("TagVal");
        assertNotNull(column);
        assertEquals(true, column.isMustExist());
        assertEquals(true, column.isTag());
        assertNull(savedProfile.getClazz());
        column.setMustExist(false);
        profileService.save(fortress,docType, savedProfile);
        savedProfile = profileService.get(fortress, docType);
        assertNull(savedProfile.getClazz());
        assertFalse("Updating the mustExist attribute did not persist",savedProfile.getContent().get("TagVal").isMustExist());




    }



}
