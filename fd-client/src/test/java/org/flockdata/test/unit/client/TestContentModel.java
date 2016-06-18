package org.flockdata.test.unit.client;

import junit.framework.TestCase;
import org.flockdata.profile.ContentModelImpl;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 26/01/16.
 */
public class TestContentModel {

    @Test
    public void valid_DocumentType() throws Exception {
        String fileName = "/model/document-type.json";
        ContentModelImpl params = ProfileReader.getContentModel(fileName);
        assertNotNull(params.getDocumentType());
        TestCase.assertEquals("PAC", params.getDocumentType().getName());
    }

}
