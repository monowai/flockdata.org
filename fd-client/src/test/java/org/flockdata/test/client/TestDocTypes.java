package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.model.DocumentType;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 20/01/16.
 */
public class TestDocTypes extends AbstractImport  {

    @Test
    public void testDocType() throws Exception{
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/test-document-type.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/test-document-type.json");

        fileProcessor.processFile(params, "/data/pac.txt", getFdWriter(), null, configuration);

        for (EntityInputBean entityInputBean : getFdWriter().getEntities()) {
            DocumentTypeInputBean docType = entityInputBean.getDocumentType();
            assertNotNull(entityInputBean.getDocumentType());
            TestCase.assertEquals("TestDocType", docType.getName());
            TestCase.assertEquals("Version Strategy not being handled", DocumentType.VERSION.ENABLE, docType.getVersionStrategy());
            EntityService.TAG_STRUCTURE tagStructure = docType.getTagStructure();
            TestCase.assertEquals("Unable to read the custom taxonomy structure", EntityService.TAG_STRUCTURE.TAXONOMY, tagStructure);
        }

    }
}
