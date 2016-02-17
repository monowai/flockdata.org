package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
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

    @Test
    public void defaults_Serialize() throws Exception{
        // Fundamental assertions are the payload is serialized

        DocumentTypeInputBean dib = new DocumentTypeInputBean("MyDoc")
                .setTagStructure(EntityService.TAG_STRUCTURE.TAXONOMY)
                .setVersionStrategy(DocumentType.VERSION.DISABLE);

        Company company = new Company("CompanyName");
        Fortress fortress = new Fortress(new FortressInputBean("FortressName"),company)
                .setSearchEnabled(true);

        byte[] bytes = JsonUtils.getObjectAsJsonBytes(dib);
        assertEquals(dib.getTagStructure(), JsonUtils.getBytesAsObject(bytes,DocumentTypeInputBean.class).getTagStructure() );

        EntityInputBean compareFrom = new EntityInputBean(fortress, dib);
        assertEquals(dib.getTagStructure(), compareFrom.getDocumentType().getTagStructure());

        EntityInputBean deserialize
                = JsonUtils.getBytesAsObject(JsonUtils.getObjectAsJsonBytes(compareFrom), EntityInputBean.class);
        assertNotNull (deserialize);

        assertEquals(compareFrom.getDocumentType().getCode(), deserialize.getDocumentType().getCode());
        assertEquals(compareFrom.getDocumentType().getTagStructure(), deserialize.getDocumentType().getTagStructure());
        assertEquals(compareFrom.getDocumentType().getVersionStrategy(), deserialize.getDocumentType().getVersionStrategy());

    }
}
