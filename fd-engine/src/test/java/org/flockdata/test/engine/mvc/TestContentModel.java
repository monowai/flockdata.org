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
import org.flockdata.profile.*;
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
        ImportContentModel contentProfile = ImportContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        makeDataAccessProfile("TestContentProfileStorage", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("contentFortress"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean("ContentStore"));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStore",
                contentProfile,
                MockMvcResultMatchers.status().isOk());
        assertNotNull(result);
        assertNotNull(result.getDocumentType());
        assertNotNull(result.getFortress());

        ContentModel contentResult = getContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStore",
                contentProfile,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(contentResult);
        assertNull(contentResult.getPreParseRowExp());
        assertEquals("Content Profiles differed", contentProfile, contentResult);
    }

    @Test
    public void testContentNotFound() throws Exception {
        ImportContentModel contentProfile = ImportContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        makeDataAccessProfile("TestContentProfileStorage", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("contentFortress"));

        exception.expect(IllegalArgumentException.class);
        getContentModel(mike(),
                fortressResultBean.getCode(),
                "NonExistent",
                contentProfile,
                MockMvcResultMatchers.status().isBadRequest());


    }

    @Test
    public void validate_Profile() throws Exception {
        makeDataAccessProfile("validateContentProfile", "mike");
        ContentModel profile = ImportContentModelDeserializer.getContentModel("/models/test-model.json");
        ContentValidationRequest validationRequest = new ContentValidationRequest(profile);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        assertNotNull(JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class).getContentModel());
        ContentValidationResults result = validateContentModel(mike(), validationRequest, MockMvcResultMatchers.status().isOk());
        assertNotNull(result);
        assertFalse(result.getResults().isEmpty());

    }

    @Test
    public void find_CompanyProfiles() throws Exception {
        ImportContentModel contentProfile = ImportContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        contentProfile.setName("SettingTheName");
        makeDataAccessProfile("find_CompanyProfiles", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("find_CompanyProfiles"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean("ContentStoreFind"));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStoreFind",
                contentProfile,
                MockMvcResultMatchers.status().isOk());
        assertNotNull(result);

        assertEquals("Mismatch on name", contentProfile.getName(), result.getName());

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
        contentProfile.setName("Updated Name");
        result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                "ContentStoreFind",
                contentProfile,
                MockMvcResultMatchers.status().isOk());

        assertEquals("Updated name did not persist", contentProfile.getName(), result.getName());

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
        ImportContentModel contentProfile = ImportContentModelDeserializer.getContentModel("/models/test-tag-model.json");
        ContentModelResult result = makeTagModel(mike(),
                "Countries",
                contentProfile,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(result);

        ContentModelResult keyResult = findContentModelByKey(mike(), result.getKey(),  MockMvcResultMatchers.status().isOk());
        assertNull("Tag profiles are not connected to fortresses", keyResult.getFortress());
        assertNotNull (keyResult);

        ContentModel contentResult = getContentModel(mike(),
                "countries",
                MockMvcResultMatchers.status().isOk());

        assertNotNull(contentResult);
//        assertEquals(keyResult.getKey());
    }
}
