/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import au.com.bytecode.opencsv.CSVReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.engine.matrix.EdgeResult;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.FileProcessor;
import org.flockdata.model.ColumnValidationResult;
import org.flockdata.model.ContentModelResult;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.model.ContentValidationResults;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ContentModelHandler;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Content Model storage and retrieval
 *
 * @author mholdsworth
 * @since 14/04/2016
 */
public class TestContentModel extends MvcBase {

    @Test
    public void models_ByCompany() throws Exception {
        // Two companies. Each with an identically named fortress and docName
        String docName = "models_ByCompany";
        ContentModel entityModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        ContentModel tagModel = ContentModelDeserializer.getContentModel("/models/test-tag-model.json");
        entityModel.setName("CompanyA");
        entityModel.setDocumentType(new DocumentTypeInputBean(docName));
        FortressResultBean companyAFortress = makeFortress(mike(), new FortressInputBean("models_ByCompany"));
        makeDataAccessProfile(mike(), "byCompanyA", "mike");

        makeDocuments(mike(), companyAFortress, new DocumentTypeInputBean(docName));
        ContentModelResult modelCompanyA = makeContentModel(mike(),
            companyAFortress.getCode(),
            docName,
            entityModel,
            MockMvcResultMatchers.status().isOk());
        assertNotNull(modelCompanyA);
        assertEquals("Mismatch on name", entityModel.getName(), modelCompanyA.getName());

        makeDataAccessProfile(sally(), "byCompanyB", sally_admin);

        FortressResultBean companyBFortress = makeFortress(sally(), new FortressInputBean("models_ByCompany"));
        makeDocuments(sally(), companyBFortress, new DocumentTypeInputBean(docName));
        ContentModelResult modelCompanyB = makeContentModel(sally(),
            companyBFortress.getCode(),
            docName,
            entityModel,
            MockMvcResultMatchers.status().isOk());
        assertNotNull(modelCompanyA);

        assertEquals("Mismatch on name", entityModel.getName(), modelCompanyB.getName());

        // Two models in the DB, but for different companies. Only one should be found for each
        Collection<ContentModelResult> companyAResults = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertNotNull(companyAResults);
        assertEquals("Only one Entity model should be found for this users company", 1, companyAResults.size());

        Collection<ContentModelResult> companyBResults = findContentModels(sally(), MockMvcResultMatchers.status().isOk());
        assertNotNull(companyBResults);
        assertEquals("Only one Entity model should be found for this users company", 1, companyBResults.size());

        makeTagModel(mike(), "Blah", tagModel, OK);
        companyAResults = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertEquals("One Entity and one Tag model", 2, companyAResults.size());

        companyBResults = findContentModels(sally(), MockMvcResultMatchers.status().isOk());
        assertEquals("Tag model should not be associated with this company", 1, companyBResults.size());
    }

    @Test
    public void documentTypeFlagChecksFromModel() throws Exception {
        String test = "documentTypeFlagChecksFromModel";
        ContentModel model = new ContentModelHandler();
        model.setDocumentType(new DocumentTypeInputBean(test)
            .setVersionStrategy(Document.VERSION.DISABLE))
            .setTrackSuppressed(true) // Suppress on a model basis
            .setSearchSuppressed(true) // Suppress on a model basis
            .setTagModel(false)
            .setFortress(new FortressInputBean(test)
                .setStoreEnabled(true)
                .setSearchEnabled(true));

        makeDataAccessProfile("test", "mike");
        makeContentModel(mike(), test, test, model, OK);
        DocumentResultBean documentResultBean = getDocument(mike(), test, test);
        assertNotNull(documentResultBean);
        assertNotNull("Property did not default to true from the content model to the documentType",
            documentResultBean.getStoreEnabled());
        assertFalse("Document did not override the fortress setting", documentResultBean.getStoreEnabled());
        assertNotNull(documentResultBean.getSearchEnabled());
        assertFalse("Search should have been disabled for the DocumentType ", documentResultBean.getSearchEnabled());

    }

    @Test
    public void testSaveRetrieveModel() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");
        String docName = "testSaveRetrieveModel";
        contentModel.setFortressName(docName);

        contentModel.setDocumentType(new DocumentTypeInputBean(docName));
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
        Map<String, Object> row = Transformer.convertToMap(headers, values, new ExtractProfileHandler(contentModel));

        Collection<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row);

        ContentValidationRequest validationRequest = new ContentValidationRequest(contentModel, rows);

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
    public void find_CompanyModels() throws Exception {
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
    public void updateEntityLinkPropertyForExistingModel() throws Exception {
        String docName = "updateEntityLinkPropertyForExistingModel";
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-entity-links-update.json");
        contentModel.setName("SettingTheName");

        DocumentTypeInputBean docType = new DocumentTypeInputBean(docName);
        contentModel.setDocumentType(docType);
        makeDataAccessProfile("find_CompanyProfiles", "mike");
        FortressResultBean fortressResultBean = makeFortress(mike(),
            new FortressInputBean("updateEntityLinkPropertyForExistingModel"));
        contentModel.setFortress(new FortressInputBean(fortressResultBean.getName()));

        makeDocuments(mike(), fortressResultBean, docType);
        ContentModelResult result = makeContentModel(mike(),
            fortressResultBean.getCode(),
            docType.getName(),
            contentModel,
            MockMvcResultMatchers.status().isOk());
        assertNotNull(result);

        assertEquals("Mismatch on name", contentModel.getName(), result.getName());

        ContentModel retrieved = getContentModel(mike(),
            fortressResultBean.getCode(),
            docType.getName(),
            MockMvcResultMatchers.status().isOk());
        assertNotNull(retrieved);
        ColumnDefinition issuer = retrieved.getContent().get("issuerID");
        assertNotNull(issuer);
        assertFalse("No entity links existed", issuer.getEntityLinks() == null);
        Collection<EntityKeyBean> links = retrieved.getContent().get("issuerID").getEntityLinks();
        assertEquals("Only 1 link was expected", 1, links.size());
        EntityKeyBean entitylink = links.iterator().next();
        assertFalse("value should be false in the model on disk", entitylink.isParent());
        entitylink.setParent(true); // Toggling the value

        // Update the content model having changed the parent entitylink value from false to true
        makeContentModel(mike(),
            fortressResultBean.getCode(),
            docType.getName(),
            retrieved,
            MockMvcResultMatchers.status().isOk());

        retrieved = getContentModel(mike(),
            fortressResultBean.getCode(),
            docType.getName(),
            MockMvcResultMatchers.status().isOk());

        links = retrieved.getContent().get("issuerID").getEntityLinks();
        assertEquals("Only 1 link was expected", 1, links.size());
        entitylink = links.iterator().next();
        assertTrue("parent property set to true did not persist", entitylink.isParent());

    }

    @Test
    public void create_DefaultProfile() throws Exception {
        makeDataAccessProfile("create_DefaultProfile", "mike");

        Reader reader = FileProcessor.getReader("/data/test-default-content.csv");
        CSVReader csvReader = new CSVReader(reader);
        List<String[]> data = csvReader.readAll();


        int count = 0;
        String[] headers = null;
        String[] dataRow = null;
        for (String[] strings : data) {
            if (count == 0) {
                headers = strings;
            } else {
                dataRow = strings;
            }
            count++;
        }
        Map<String, Object> dataMap = Transformer.convertToMap(headers, dataRow);
        ContentValidationRequest validationRequest = new ContentValidationRequest(dataMap);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);

        ContentValidationRequest valRequest = JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class);
        assertNotNull(valRequest);

        assertEquals("Rows did not reconcile after Json deserialization",
            1,
            valRequest.getRows().size());

        ContentModel contentModel = getDefaultContentModel(mike(), valRequest);

        assertThat(contentModel).isNotNull().hasFieldOrProperty("content");

        assertEquals("Incorrect content column definitions", 42, contentModel.getContent().size());
        // Spot checks
        ColumnDefinition drumID = contentModel.getColumnDef("drumID");
        assertNotNull(drumID);
        assertEquals("ColDef was not computed to a number",
            "number",
            drumID.getDataType());

        ColumnDefinition custodianID = contentModel.getColumnDef("custodianID");
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
        String[] headers = null;
        String[] dataRow = null;
        for (String[] strings : data) {
            if (count == 0) {
                headers = strings;
            } else {
                dataRow = strings;
            }
            count++;
        }
        Map<String, Object> dataMap = Transformer.convertToMap(headers, dataRow);
        assertFalse(dataMap.isEmpty());
        ContentValidationRequest validationRequest = new ContentValidationRequest(dataMap);
        String json = JsonUtils.toJson(validationRequest);
        assertNotNull(json);
        ContentValidationRequest valRequest = JsonUtils.toObject(json.getBytes(), ContentValidationRequest.class);
        assertNotNull(valRequest);

        ContentModel model = getDefaultContentModel(mike(), valRequest);
        model.setDocumentType(new DocumentTypeInputBean("Entity"));
        assertNotNull(model);
        assertNotNull(model.getContent());

        ColumnDefinition columnDefinition = model.getContent().get("drumID.key");
        assertEquals("Elastic does not accept columns with a period. FD should remove the character", "drumIDkey",
            columnDefinition.getTarget());
    }

    @Test
    public void create_ContentValidationErrors() throws Exception {
        makeDataAccessProfile("create_ContentValidationErrors", "mike");

        Reader reader = FileProcessor.getReader("/data/test-illegal-columns.csv");
        CSVReader csvReader = new CSVReader(reader);
        List<String[]> data = csvReader.readAll();

        int count = 0;
        String[] headers = null;
        String[] dataRow = null;
        for (String[] strings : data) {
            if (count == 0) {
                headers = strings;
            } else {
                dataRow = strings;
            }
            count++;
        }
        Map<String, Object> dataMap = Transformer.convertToMap(headers, dataRow);
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

        model.setDocumentType(new DocumentTypeInputBean("Entity"));
        model.setTagModel(false);

        columnDefinition = model.getContent().get("drumID.key");
        columnDefinition.setTarget("drumID.key"); // We want to pickup content validation error
        ContentValidationResults valResults = validateContentModel(mike(), validationRequest.setContentModel(model), OK);
        assertNotNull(valResults);
        assertEquals("One data row, one result", 1, valResults.getResults().size());
        assertNotNull(valResults);
        Collection<ColumnValidationResult> columnValidationResults = valResults.getResults().get(0);
        for (ColumnValidationResult columnValidationResult : columnValidationResults) {
            switch (columnValidationResult.getSourceColumn()) {
                case "drumID.key":
                    assertEquals("Expected a validation error message",
                        1,
                        columnValidationResult.getMessages().size());
                    break;
                case "illegalExpression":
                    assertEquals("Expected and illegal expression message",
                        1,
                        columnValidationResult.getMessages().size());
                    break;
                default:
                    assertEquals("Didn't expect a validation error message",
                        0,
                        columnValidationResult.getMessages().size());
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
        assertNotNull(keyResult);

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

        ContentModelResult keyResult = findContentModelByKey(mike(), result.getKey(), MockMvcResultMatchers.status().isOk());
        assertEquals("Tag profiles are connected to a pseudo fortress - Tag",
            "Tag",
            keyResult.getFortress());

        assertNotNull(keyResult);

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
        Collection<ContentModel> models = new ArrayList<>();

        ContentModel tagModel = ContentModelDeserializer.getContentModel("/models/test-tag-model.json");
        assertNotNull(tagModel.getCode());
        models.add(tagModel);

        ContentModel entityModel = ContentModelDeserializer.getContentModel("/models/test-entity-tag-links.json");
        models.add(entityModel);

        Collection<ContentModelResult> results = makeContentModels(mike(),
            models,
            MockMvcResultMatchers.status().isOk());

        assertNotNull(results);
        assertEquals("Request to create multiple models failed", 2, results.size());

        Collection<String> modelKeys = new ArrayList<>();

        for (ContentModelResult result : results) {
            ContentModelResult keyResult = findContentModelByKey(mike(), result.getKey(), MockMvcResultMatchers.status().isOk());
            assertNotNull(keyResult);
            modelKeys.add(keyResult.getKey());
        }
        Collection<ContentModelResult> contentModelResults = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertEquals("Didn't find the two we just created", 2, contentModelResults.size());

        Collection<ContentModel> download = findContentModels(mike(), modelKeys, MockMvcResultMatchers.status().isOk());
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
        assertNotNull(foundResult);
        deleteFortress(mike(), fortressResultBean.getName(), MockMvcResultMatchers.status().isAccepted());
        Thread.sleep(500); // Purge is async
        Collection<ContentModelResult> contentModels = findContentModels(mike(), MockMvcResultMatchers.status().isOk());
        assertFalse(contentModels.isEmpty());
        foundResult = findContentModelByKey(mike(), result.getKey(), MockMvcResultMatchers.status().isOk());
        assertNotNull(foundResult);
        assertNotNull("Document Type not resolved from the document it used to be associated with",
            foundResult.getDocumentType());

    }

    @Test
    public void trackSuppressedCreatesConceptStructure() throws Exception {
        engineConfig.setConceptsEnabled(true);
        String docName = "trackSuppressedCreatesConceptStructure";

        makeDataAccessProfile(docName, "mike");

        FortressResultBean fortressResultBean = makeFortress(mike(), new FortressInputBean(docName));
        DocumentResultBean documentResultBean = makeDocuments(mike(), fortressResultBean, new DocumentTypeInputBean(docName));

        EntityInputBean eib = new EntityInputBean(fortressResultBean, new DocumentTypeInputBean(documentResultBean));
        eib.setTrackSuppressed(true);
        Collection<TagInputBean> tags = new ArrayList<>();
        TagInputBean tagA = new TagInputBean("MyCode", "LabelToFind", "simple");
        TagInputBean tagB = new TagInputBean("MyCode", "LabelToFind", "complex");
        tags.add(tagA);
        tags.add(tagB);
        eib.setTags(tags);
        track(mike(), eib);
        Thread.sleep(1000);// Concepts are created in a separate thread so wait a bit

        MatrixResults contentStructure = getContentStructure(mike(), docName, OK);
        assertNotNull(contentStructure);
        assertEquals("Expected 1 document and 1 concept", 2, contentStructure.getNodes().size());
        assertEquals("2 edge relationships are expected", 2, contentStructure.getEdges().size());
        boolean simpleFound = false, complexFound = false;
        for (EdgeResult edgeResult : contentStructure.getEdges()) {
            if (edgeResult.getRelationship().equals("simple")) {
                simpleFound = true;
            }
            if (edgeResult.getRelationship().equals("complex")) {
                complexFound = true;
            }

        }

        assertTrue("Didn't find the simple relationship name", simpleFound);
        assertTrue("Didn't find the complex relationship name", complexFound);

    }
}
