package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 26/01/16.
 */
public class TestContentProfile {

    @Test
    public void valid_DocumentType() throws Exception {
        String fileName = "/profile/document-type.json";
        ContentProfileImpl params = ProfileReader.getImportProfile(fileName);
        assertNotNull(params.getDocumentType());
        TestCase.assertEquals("PAC", params.getDocumentType().getName());
    }

}
