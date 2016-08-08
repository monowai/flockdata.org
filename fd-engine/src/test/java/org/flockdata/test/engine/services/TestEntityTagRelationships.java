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

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.SystemUser;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.FileProcessor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 13/07/16.
 */
public class TestEntityTagRelationships extends EngineBase {

    @Autowired
    FileProcessor fileProcessor;


    @Test
    public void entityTagLinksCreatedWithCorrectDirection() throws Exception {
        final SystemUser su = registerSystemUser("TestEntityTagRelationships", mike_admin);
        setSecurity();
        engineConfig.setTestMode(true);

        String file = "/models/test-entity-tag-links.json";
        ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
        assertNotNull ( contentModel);
        ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

        TagInputBean hongKong = new TagInputBean("HK", "Country");
        hongKong.addAlias(new AliasInputBean("HKG", "Long Code"));
        tagService.createTag(su.getCompany(), hongKong);

        // Tag code of HK is used, but data is tracking in against the Alias of HKG
        fileProcessor.processFile(extractProfile, "/data/test-entity-tag-links.csv");
        String key = "10000002";
        Entity entity = entityService.findByCode(su.getCompany(), contentModel.getFortress().getName(), contentModel.getDocumentType().getName(), key);
        assertNotNull (entity);
        validateEntities(su, entity);

        // If not found by the alias then an extra relationship will be created
        fileProcessor.processFile(extractProfile, "/data/test-entity-tag-links.csv");
        entity = entityService.findByCode(su.getCompany(), contentModel.getFortress().getName(), contentModel.getDocumentType().getName(), key);
        validateEntities(su, entity);


    }

    public void validateEntities(SystemUser su, Entity entity) {
        Iterable<EntityTag> entityTags = entityTagService.findEntityTagsWithGeo(entity);
        int count = 0;
        for (EntityTag entityTag : entityTags) {
            if ( entityTag.getRelationship().equals("located")){
                count++;
                assertTrue(entityTag.isGeo());
                assertEquals("located", entityTag.getRelationship());
            } else if (entityTag.getRelationship().equals("jurisdiction")){
                count++;
                assertTrue(entityTag.isGeo());
            } else if (entityTag.getRelationship().equals("manages")){
                count++;
            }  else if (entityTag.getRelationship().equals("ibc")){
                count++;
            }

        }
        assertEquals ("Found more relationships than expected", 4, count);
        assertEquals (1, entityTagService.findInboundTags(su.getCompany(), entity).size());
        assertEquals (3, entityTagService.findOutboundTags(su.getCompany(), entity).size());
    }
}
