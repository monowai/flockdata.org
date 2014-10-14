package com.auditbucket.test.functional;

import com.auditbucket.helper.NotFoundException;
import com.auditbucket.profile.ImportProfile;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.Entity;
import com.auditbucket.transform.FileProcessor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 5:30 PM
 */
@Transactional
public class TestBatch extends EngineBase {
    @Autowired
    ImportProfileService importProfileService;

    @Test
    public void doBatchTest() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("doBatchTest", "mike");
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("doBatchTest", true));
        DocumentType docType = schemaService.resolveByDocCode(fortress, "test-batch");

        ImportProfile params = Helper.getImportParams("/batch-csv-profile.json");

        importProfileService.save(fortress, docType, params );
        importProfileService.process(su.getCompany(), fortress, docType, "/batch-test.csv", false);

        Entity resultBean = trackService.findByCallerRef( fortress, docType, "1");
        assertNotNull(resultBean);

    }

    @Test
    public void import_ValidateArgs() throws Exception{
        FileProcessor fileProcessor = new FileProcessor();
        ProfileConfiguration profileConfiguration = new ImportProfile();
        try {
            FileProcessor.validateArgs("/illegalFile");
            fail("Exception not thrown");
        } catch ( NotFoundException nfe){
            // Great
            assertEquals(true,true);
        }
    }


}
