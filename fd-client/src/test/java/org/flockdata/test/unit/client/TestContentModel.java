package org.flockdata.test.unit.client;

import junit.framework.TestCase;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.model.ContentModel;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

/**
 * Basic deserialization checks
 * Created by mike on 26/01/16.
 */
public class TestContentModel {

    @Test
    public void contentModelDeserializes() throws Exception {
        String fileName = "/model/document-type.json";
        ContentModel params = ContentModelDeserializer.getContentModel(fileName);
        assertNotNull(params.getDocumentType());
        TestCase.assertEquals("PAC", params.getDocumentType().getName());
    }

}
