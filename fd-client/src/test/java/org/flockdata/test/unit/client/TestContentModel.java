package org.flockdata.test.unit.client;

import junit.framework.TestCase;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ContentModelHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.transform.ColumnDefinition;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
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

    @Test
    public void mergeColumnDefinitions() throws Exception{
        ContentModel contentModel = new ContentModelHandler();
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("String");
        Map<String,ColumnDefinition> columns = new HashMap<>();
        columns.put("Existing", columnDefinition);
        contentModel.setContent(columns);

        columns = new HashMap<>(); // Create new payload to merge
        columns.put("Second", columnDefinition);

        assertEquals("Existing model did not have 1 ColDef", 1, contentModel.getContent().size());

        contentModel.setContent(columns);
        assertEquals("Second ColDef did not merge with the existing one", 2, contentModel.getContent().size());

    }

}
