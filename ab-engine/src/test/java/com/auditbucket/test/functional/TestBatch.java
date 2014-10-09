package com.auditbucket.test.functional;

import com.auditbucket.profile.ImportProfile;
import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.Entity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 5:30 PM
 */
public class TestBatch extends TestEngineBase {
    @Autowired
    ImportProfileService importProfileService;

    @Test
    public void doBatchTest() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("doBatchTest", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("doBatchTest", true));
        DocumentType docType = schemaService.resolveDocType(fortress, "test-batch");

        ImportProfile params = getImportParams("/batch-csv-profile.json");

        importProfileService.save(fortress, docType, params );
        importProfileService.process(su.getCompany(), fortress, docType, "/batch-test.csv");

        Entity resultBean = trackService.findByCallerRef( fortress, docType, "1");
        assertNotNull(resultBean);

    }

    public static ImportProfile getImportParams(String profile) throws IOException {
        ImportProfile importProfile;
        ObjectMapper om = new ObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, ImportProfile.class);
            } else
                // Defaults??
                importProfile = new ImportProfile();
        }
        //importParams.setWriter(restClient);
        return importProfile;
    }

}
