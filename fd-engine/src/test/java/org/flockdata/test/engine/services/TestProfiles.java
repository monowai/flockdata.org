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

package org.flockdata.test.engine.services;

import org.flockdata.integration.FileProcessor;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.transform.ColumnDefinition;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author mholdsworth
 * @since 3/10/2014
 */
public class TestProfiles extends EngineBase {

    @Autowired
    ContentModelService profileService;
    @Autowired
    FileProcessor fileProcessor;
    private Logger logger = LoggerFactory.getLogger(TestProfiles.class);

    @Test
    @Transactional
    public void create_ContentModel() throws Exception {
        SystemUser su = registerSystemUser("create_Profile", mike_admin);

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-model.json");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("create_profile", true));
        DocumentType docType = conceptService.resolveByDocCode(fortress, "Olympic");
        profileService.saveEntityModel(su.getCompany(), fortress, docType, contentModel);
        ContentModel savedProfile = profileService.get(su.getCompany(), fortress, docType);
        assertNotNull(savedProfile);
        assertEquals(contentModel.getFortressUser(), savedProfile.getFortressUser());
        assertEquals(contentModel.isEntityOnly(), savedProfile.isEntityOnly());
        assertEquals(contentModel.getContent().size(), savedProfile.getContent().size());
        ColumnDefinition column = savedProfile.getContent().get("TagVal");
        assertNotNull(column);
        assertEquals(true, column.isMustExist());
        assertEquals(true, column.isTag());
        assertNull(savedProfile.getHandler());
        column.setMustExist(false);
        profileService.saveEntityModel(su.getCompany(), fortress, docType, savedProfile);
        savedProfile = profileService.get(su.getCompany(), fortress, docType);
        assertNull(savedProfile.getHandler());
        assertFalse("Updating the mustExist attribute did not persist", savedProfile.getContent().get("TagVal").isMustExist());
    }

    @Test
    public void serverSideModelAndTagsLinkToExistingDocumentType() throws Exception {
        SystemUser su = registerSystemUser("serverSideModelAndTagsLink", mike_admin);

        engineConfig.setConceptsEnabled(true);
        ContentModel simpleModel = ContentModelDeserializer.getContentModel("/models/test-sflow.json");
        Collection<ContentModel> contentModels = new ArrayList<>();
        contentModels.add(simpleModel);
        DocumentType documentType;



        Collection<TagResultBean> tags = tagService.findTags(su.getCompany());

        for (ContentModel contentModel : contentModels) {
            Fortress fortress = fortressService.registerFortress(su.getCompany(), contentModel.getFortress());
            conceptService.save(new DocumentType(fortress.getDefaultSegment(), contentModel.getDocumentType()));

            documentType = conceptService.resolveByDocCode(fortress, contentModel.getDocumentType().getName(), Boolean.FALSE);

            if (documentType == null && contentModel.getDocumentType() != null) {
                documentType = conceptService.findOrCreate(fortress, new DocumentType(fortress, contentModel.getDocumentType()));
            }

            assertNotNull(documentType);
            ContentModelResult contentModelResult = contentModelService.saveEntityModel(su.getCompany(), fortress, documentType, contentModel);
            assertNotNull(contentModelResult);

            // Create some existing tags, we want to ensure the doctype is linked to these when an Entity is tracked
            TagInputBean topicA = new TagInputBean("javascript", "Topic");
            TagInputBean topicB = new TagInputBean("alerts", "Topic");
            TagInputBean developerI = new TagInputBean("10759", "Developer");
            TagInputBean developerJ = new TagInputBean("65663", "Developer");
            Collection<TagInputBean>tagsToMake = new ArrayList<>();
            tagsToMake.add(topicA);
            tagsToMake.add(topicB);
            tagsToMake.add(developerI);
            tagsToMake.add(developerJ);
            tagService.createTags(su.getCompany(), tagsToMake);

        }
        // Above is the setup - ContentModel is on the server with an existing DocumentType and existing tags.
        // The DocType is not yet associated with the Concept objects (Tag Labels).
        // The act of tracking the entity should establish this
        fileProcessor.processFile(new ExtractProfileHandler(ContentModelDeserializer.getContentModel("/models/test-sflow.json")), "/data/test-doc-tag-concepts.csv");
        Entity entityA = null;
        int tries = 0, max = 10;

        while (entityA == null && tries <= max) {
            Thread.sleep(200);
            entityA = entityService.findByCode(su.getCompany(), "StackOverflow", "QuestionEvent", "563890");
            tries++;
        }
        assertFalse("Timeout waiting for the entity to be created and found", tries == max);

        Collection<TagResultBean> afterTags = tagService.findTags(su.getCompany());

        assertTrue("Should be more tags after the track event", afterTags.size() > tags.size());
        Collection<DocumentResultBean> docResults = conceptService.findConcepts(su.getCompany(), "QuestionEvent", true);
        assertEquals(1, docResults.size());
        assertEquals("Should be both a Developer and a Topic tag", 2, docResults.iterator().next().getConcepts().size());
        // Check that the tags are created and connected


    }


}
