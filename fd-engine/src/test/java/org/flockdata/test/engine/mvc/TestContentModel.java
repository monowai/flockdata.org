/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.mvc;

import au.com.bytecode.opencsv.CSVReader;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.shared.FileProcessor;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.Transformer;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Created by mike on 14/04/16.
 */
public class TestContentModel extends MvcBase {
    @Test
    public void testSaveRetrieveModel() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        contentModel.setDocumentType( new DocumentTypeInputBean("ContentStore"));
        makeDataAccessProfile("TestContentProfileStorage", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("contentFortress"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean("ContentStore"));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStore",
                contentModel,
                MockMvcResultMatchers.status().isOk());
        assertNotNull(result);
        assertNotNull(result.getDocumentType());
        assertNotNull(result.getFortress());

        ContentModel contentResult = getContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStore",
                contentModel,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(contentResult);
//        assertNull(contentResult.getPreParseRowExp());
        assertEquals("Content Profiles differed", contentModel, contentResult);
        assertFalse("Should have been an Entity ContentModel ", contentResult.isTagModel());
    }

    @Test
    public void testContentNotFound() throws Exception {

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        makeDataAccessProfile("TestContentProfileStorage", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("contentFortress"));

        exception.expect(IllegalArgumentException.class);
        getContentModel(mike(),
                fortressResultBean.getCode(),
                "NonExistent",
                contentModel,
                MockMvcResultMatchers.status().isBadRequest());


    }

    @Test
    public void validate_Profile() throws Exception {
        makeDataAccessProfile("validateContentProfile", "mike");
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-model.json");
        ContentValidationRequest validationRequest = new ContentValidationRequest(contentModel);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        assertNotNull(JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class).getContentModel());
        ContentValidationResults result = validateContentModel(mike(), validationRequest, MockMvcResultMatchers.status().isOk());
        assertNotNull(result);
        assertFalse(result.getResults().isEmpty());

    }

    @Test
    public void find_CompanyProfiles() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        contentModel.setName("SettingTheName");
        makeDataAccessProfile("find_CompanyProfiles", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("find_CompanyProfiles"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean("ContentStoreFind"));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStoreFind",
                contentModel,
                MockMvcResultMatchers.status().isOk());
        assertNotNull(result);

        assertEquals("Mismatch on name", contentModel.getName(), result.getName());

        Collection<ContentModelResult> profileResults = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertNotNull(profileResults);
        assertTrue(profileResults.size() == 1);
        for (ContentModelResult foundResult : profileResults) {
            assertNotNull(foundResult.getFortress());
            assertNotNull(foundResult.getDocumentType());
        }
        ContentModelResult foundResult = findContentModelByKey(mike(), result.getKey(), MockMvcResultMatchers.status().isOk());
        assertNotNull(foundResult);
        assertNotNull(foundResult.getFortress());
        assertNotNull(foundResult.getDocumentType());

        // Update the name
        contentModel.setName("Updated Name");
        result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStoreFind",
                contentModel,
                MockMvcResultMatchers.status().isOk());

        assertEquals("Updated name did not persist", contentModel.getName(), result.getName());

        exception.expect(NotFoundException.class);
        findContentModelByKey(sally(), result.getKey(), MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    public void create_DefaultProfile() throws Exception {
        makeDataAccessProfile("create_DefaultProfile", "mike");
//        ContentProfile profile = ContentProfileDeserializer.getContentModel("/data/test-default-content.csv");

        Reader reader = FileProcessor.getReader("/data/test-default-content.csv");
        CSVReader csvReader = new CSVReader(reader);
        List<String[]> data = csvReader.readAll();


        int count = 0;
        String[] headers=null;
        String[] dataRow=null;
        for (String[] strings : data) {
            if ( count ==0) {
                headers = strings;
            } else
                dataRow = strings;
            count++;
        }
        Map<String,Object> dataMap = Transformer.convertToMap(headers, dataRow);;
        ContentValidationRequest validationRequest = new ContentValidationRequest(dataMap);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        ContentValidationRequest valRequest = JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class);
        assertNotNull(valRequest);
        assertEquals("Rows did not reconcile after Json deserialization", 1, valRequest.getRows().size());
        ContentModel result = getDefaultContentModel(mike(), valRequest);
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertEquals("Incorrect content column definitions", 42, result.getContent().size());
        // Spot checks
        ColumnDefinition drumID = result.getColumnDef("drumID");
        assertNotNull(drumID);
        assertEquals("ColDef was not computed to a number", "number", drumID.getDataType());

        ColumnDefinition custodianID = result.getColumnDef("custodianID");
        assertNotNull(custodianID);
        // Even though it could be a number, blank values are set to a string by default
        assertEquals("Not set value did not return as a string", "string", custodianID.getDataType());

    }

    @Test
    public void create_TagProfile() throws Exception {
        makeDataAccessProfile("create_TagProfile", "mike");
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-tag-model.json");
        ContentModelResult result = makeTagModel(mike(),
                "Countries",
                contentModel,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(result);

        ContentModelResult keyResult = findContentModelByKey(mike(), result.getKey(),  MockMvcResultMatchers.status().isOk());
        assertEquals("Tag profiles are connected to a pseudo fortress - Tag", "Tag", keyResult.getFortress());
        assertNotNull (keyResult);

        ContentModel contentResult = getContentModel(mike(),
                "countries",
                MockMvcResultMatchers.status().isOk());

        assertNotNull(contentResult);
        assertTrue(contentResult.isTagModel());
//        assertEquals(keyResult.getKey());
    }

    @Test
    public void find_afterDeletingFortress() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        contentModel.setName("anyName");
        makeDataAccessProfile("find_afterDeletingFortress", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("find_afterDeletingFortress"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean("ContentStoreFind"));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStoreFind",
                contentModel,
                MockMvcResultMatchers.status().isOk());
        assertNotNull(result);

        Collection<ContentModelResult> profileResults = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertNotNull(profileResults);
        assertTrue(profileResults.size() == 1);
        for (ContentModelResult foundResult : profileResults) {
            assertNotNull(foundResult.getFortress());
            assertNotNull(foundResult.getDocumentType());
        }

        ContentModelResult foundResult = findContentModelByKey(mike(), result.getKey(), MockMvcResultMatchers.status().isOk());
        assertNotNull (foundResult);
        purgeFortress(mike(), fortressResultBean.getName(), MockMvcResultMatchers.status().isAccepted());
        Thread.sleep(500); // Purge is async
        Collection<ContentModelResult> contentModels = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertFalse(contentModels.isEmpty());
        foundResult = findContentModelByKey(mike(), result.getKey(), MockMvcResultMatchers.status().isOk());
        assertNotNull (foundResult);
        assertNotNull( "Document Type not resolved from the document it used to be associated with", foundResult.getDocumentType());

    }
}
