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
import org.flockdata.engine.matrix.EdgeResult;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.FileProcessor;
import org.flockdata.profile.*;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.Transformer;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Content Model storage and retrieval
 * Created by mike on 14/04/16.
 */
public class TestContentModel extends MvcBase {

    @Test
    public void documentTypeFlagChecksFromModel() throws Exception{
        String test = "documentTypeFlagChecksFromModel";
        ContentModel model = new ContentModelHandler();
        model.setDocumentType( new DocumentTypeInputBean(test))
                .setTrackSuppressed(true)
                .setSearchSuppressed(true)
                .setTagModel(false)
                .setFortress( new FortressInputBean(test));

        makeDataAccessProfile("test", "mike");
        makeContentModel(mike(),test,test, model, OK );
        DocumentResultBean documentResultBean = getDocument(mike(), test, test);
        assertNotNull (documentResultBean);
        assertNotNull ( "Property did not default to true from the content model to the documentType", documentResultBean.getTrackSuppressed());
        assertTrue(documentResultBean.getTrackSuppressed());
        assertNotNull ( documentResultBean.getSearchSuppressed());
        assertTrue(documentResultBean.getSearchSuppressed());

    }

    @Test
    public void testSaveRetrieveModel() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        String docName = "testSaveRetrieveModel";
        contentModel.setFortressName(docName);

        contentModel.setDocumentType( new DocumentTypeInputBean(docName));
        makeDataAccessProfile("TestContentProfileStorage", "mike");

        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean(docName));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean(docName));

        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                docName,
                contentModel,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(result);
        assertNotNull(result.getDocumentType());
        assertNotNull(result.getFortress());

        ContentModel contentResult = getContentModel(mike(),
                fortressResultBean.getCode(),
                docName,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(contentResult);
//        assertNull(contentResult.getPreParseRowExp());
        assertEquals("Content Profiles differed", contentModel, contentResult);
        assertFalse("Should have been an Entity ContentModel ", contentResult.isTagModel());
    }

    @Test
    public void testContentNotFound() throws Exception {

        makeDataAccessProfile("TestContentProfileStorage", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("contentFortress"));

        exception.expect(IllegalArgumentException.class);
        getContentModel(mike(),
                fortressResultBean.getCode(),
                "NonExistent",
                MockMvcResultMatchers.status().isBadRequest());


    }

    @Test
    public void validate_Profile() throws Exception {
        makeDataAccessProfile("validateContentProfile", "mike");
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-model.json");

        String[] headers = {"Athlete", "Age", "Country", "Year", "Sport", "Gold Medals", "Silver Medals", "Bronze Medals"};
        String[] values = {"Michael Phelps", "23", "United States", "2008", "Swimming", "8", "0", "0", "8"};
        Map<String,Object> row = Transformer.convertToMap(headers, values, new ExtractProfileHandler(contentModel));

        Collection<Map<String,Object>>rows = new ArrayList<>();
        rows.add(row);

        ContentValidationRequest validationRequest = new ContentValidationRequest(contentModel,rows);

        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        assertNotNull(JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class).getContentModel());
        ContentValidationResults result = validateContentModel(mike(), validationRequest, MockMvcResultMatchers.status().isOk());

        assertNotNull(result);
        assertNotNull(result.getResults());
        assertNotNull(result.getEntity(0));
        assertFalse(result.getResults(0).isEmpty());

    }

    @Test
    public void find_CompanyProfiles() throws Exception {
        String docName = "find_CompanyProfiles";
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        contentModel.setName("SettingTheName");
        contentModel.setDocumentType(new DocumentTypeInputBean(docName));
        makeDataAccessProfile("find_CompanyProfiles", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("find_CompanyProfiles"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean(docName));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                docName,
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
                docName,
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
    public void create_DefaultProfileIllegalColNames() throws Exception {
        makeDataAccessProfile("create_DefaultProfileIllegalColNames", "mike");

        Reader reader = FileProcessor.getReader("/data/test-illegal-columns.csv");
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
        assertFalse(dataMap.isEmpty());
        ContentValidationRequest validationRequest = new ContentValidationRequest(dataMap);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        ContentValidationRequest valRequest = JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class);
        assertNotNull(valRequest);

        ContentModel model = getDefaultContentModel(mike(), valRequest);
        model.setDocumentType( new DocumentTypeInputBean("Entity"));
        assertNotNull(model);
        assertNotNull(model.getContent());

        ColumnDefinition columnDefinition = model.getContent().get("drumID.key");
        assertEquals("Elastic does not accept columns with a period. FD should remove the character", "drumIDkey", columnDefinition.getTarget());
    }

    @Test
    public void create_ContentValidationErrors() throws Exception {
        makeDataAccessProfile("create_ContentValidationErrors", "mike");

        Reader reader = FileProcessor.getReader("/data/test-illegal-columns.csv");
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
        assertFalse(dataMap.isEmpty());
        ContentValidationRequest validationRequest = new ContentValidationRequest(dataMap);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        ContentValidationRequest valRequest = JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class);
        assertNotNull(valRequest);

        ContentModel model = getDefaultContentModel(mike(), valRequest);
        assertNotNull(model);
        assertNotNull(model.getContent());

        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setValue("a+b");// Illegal expression
        columnDefinition.setTitle(true);// make sure it gets evaluated;
        model.getContent().put("illegalExpression", columnDefinition);

        model.setDocumentType( new DocumentTypeInputBean("Entity"));
        model.setTagModel(false);

        columnDefinition = model.getContent().get("drumID.key");
        columnDefinition.setTarget("drumID.key"); // We want to pickup content validation error
        ContentValidationResults valResults = validateContentModel(mike(), validationRequest.setContentModel(model), OK);
        assertNotNull(valResults);
        assertEquals("One data row, one result", 1, valResults.getResults().size());
        assertNotNull ( valResults);
        Collection<ColumnValidationResult> columnValidationResults = valResults.getResults().get(0);
        for (ColumnValidationResult columnValidationResult : columnValidationResults) {
            switch (columnValidationResult.getSourceColumn()) {
                case "drumID.key":
                    assertEquals("Expected a validation error message", 1, columnValidationResult.getMessages().size());
                    break;
                case "illegalExpression":
                    assertEquals("Expected and illegal expression message", 1, columnValidationResult.getMessages().size());
                    break;
                default:
                    assertEquals("Didn't expect a validation error message", 0, columnValidationResult.getMessages().size());
                    break;
            }
        }

    }

    @Test
    public void delete_TagProfile() throws Exception {
        makeDataAccessProfile("delete_TagProfile", "mike");
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-tag-model.json");
        ContentModelResult result = makeTagModel(mike(),
                "Countries",
                contentModel,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(result);

        OK = MockMvcResultMatchers.status().isOk();
        ContentModelResult keyResult = findContentModelByKey(mike(), result.getKey(), OK);
        assertNotNull (keyResult);

        Collection<ContentModelResult> contentModels = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertEquals("Didn't find a content model", 1, contentModels.size());

        deleteContentModel(mike(), keyResult.getKey(), MockMvcResultMatchers.status().isOk());

        contentModels = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertEquals("Model did not delete", 0, contentModels.size());
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
    public void create_BulkContentProfiles() throws Exception {
        makeDataAccessProfile("create_BulkContentProfiles", "mike");
        Collection<ContentModel>models = new ArrayList<>();

        ContentModel tagModel = ContentModelDeserializer.getContentModel("/models/test-tag-model.json");
        assertNotNull(tagModel.getCode());
        models.add(tagModel);

        ContentModel entityModel = ContentModelDeserializer.getContentModel("/models/test-entity-tag-links.json");
        models.add(entityModel);

        Collection<ContentModelResult> results = makeContentModels(mike(),
                models,
                MockMvcResultMatchers.status().isOk());

        assertNotNull(results);
        assertEquals ("Request to create multiple models failed", 2, results.size());

        Collection<String>modelKeys = new ArrayList<>();

        for (ContentModelResult result : results) {
            ContentModelResult keyResult = findContentModelByKey(mike(), result.getKey(),  MockMvcResultMatchers.status().isOk());
            assertNotNull ( keyResult);
            modelKeys.add(keyResult.getKey());
        }
        Collection<ContentModelResult>contentModelResults = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertEquals("Didn't find the two we just created", 2, contentModelResults.size());

        Collection<ContentModel> download = findContentModels(mike(),modelKeys, MockMvcResultMatchers.status().isOk());
        assertEquals("Should have downloaded two content models", 2, download.size());

    }

    @Test
    public void find_afterDeletingFortress() throws Exception {
        String docName = "find_afterDeletingFortress";
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        contentModel.setName("anyName");
        contentModel.setDocumentType(new DocumentTypeInputBean(docName));
        makeDataAccessProfile("find_afterDeletingFortress", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean("find_afterDeletingFortress"));

        makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean(docName));
        ContentModelResult result = makeContentModel(mike(),
                fortressResultBean.getCode(),
                docName,
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

    @Test
    public void trackSuppressedCreatesConceptStructure () throws Exception {
        engineConfig.setConceptsEnabled(true);
        String docName = "trackSuppressedCreatesConceptStructure";

        makeDataAccessProfile(docName, "mike");

        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean(docName));
        DocumentResultBean documentResultBean = makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean(docName));

        EntityInputBean eib = new EntityInputBean(fortressResultBean, new DocumentTypeInputBean(documentResultBean));
        eib.setTrackSuppressed(true);
        Collection<TagInputBean>tags = new ArrayList<>();
        TagInputBean tagA= new TagInputBean("MyCode", "LabelToFind", "simple");
        TagInputBean tagB= new TagInputBean("MyCode", "LabelToFind", "complex");
        tags.add(tagA);
        tags.add(tagB);
        eib.setTags(tags);
        track(mike(), eib);
        Thread.sleep(1000);// Concepts are created in a separate thread so wait a bit

        MatrixResults contentStructure = getContentStructure(mike(), docName, OK);
        assertNotNull (contentStructure);
        assertEquals("Expected 1 document and 1 concept", 2, contentStructure.getNodes().size());
        assertEquals ("2 edge relationships are expected", 2, contentStructure.getEdges().size());
        boolean simpleFound=false, complexFound = false;
        for (EdgeResult edgeResult : contentStructure.getEdges()) {
            if (edgeResult.getRelationship().equals("simple"))
                simpleFound = true;
            if (edgeResult.getRelationship().equals("complex"))
                complexFound = true;

        }

        assertTrue ("Didn't find the simple relationship name", simpleFound);
        assertTrue ("Didn't find the complex relationship name", complexFound);

    }
}
