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

package org.flockdata.test.engine.services;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.flockdata.data.ContentModel;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.track.service.FdServerIo;
import org.flockdata.integration.FileProcessor;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.engine.MapBasedStorageProxy;
import org.flockdata.test.engine.Neo4jConfigTest;
import org.flockdata.test.unit.client.FdTemplateMock;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 13/07/2016
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    Neo4jConfigTest.class,
    FdTemplateMock.class,
    FdServerIo.class,
    MapBasedStorageProxy.class})
@ActiveProfiles( {"dev", "fd-auth-test"})
public class TestEntityTagRelationships extends EngineBase {

    @Autowired
    private FileProcessor fileProcessor;

    @Test
    public void entityTagLinksCreatedWithCorrectDirection() throws Exception {
        final SystemUser su = registerSystemUser("TestEntityTagRelationships", mike_admin);
        setSecurity();
        engineConfig.setTestMode(true);

        String file = "/models/test-entity-tag-links.json";
        ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
        assertNotNull(contentModel);
        ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

        TagInputBean hongKong = new TagInputBean("HK", "Country");
        hongKong.addAlias(new AliasInputBean("HKG", "Long Code"));
        tagService.createTag(su.getCompany(), hongKong);

        // Tag code of HK is used, but data is tracking in against the Alias of HKG
        fileProcessor.processFile(extractProfile, "/data/test-entity-tag-links.csv");
        String key = "10000002";
        Entity entity = entityService.findByCode(su.getCompany(), contentModel.getFortress().getName(), contentModel.getDocumentType().getName(), key);
        assertNotNull(entity);
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
            if (entityTag.getRelationship().equals("located")) {
                count++;
                assertTrue(entityTag.isGeoRelationship());
                assertEquals("located", entityTag.getRelationship());
            } else if (entityTag.getRelationship().equals("jurisdiction")) {
                count++;
                assertTrue(entityTag.isGeoRelationship());
            } else if (entityTag.getRelationship().equals("manages")) {
                count++;
            } else if (entityTag.getRelationship().equals("ibc")) {
                count++;
            }

        }
        assertEquals("Found more relationships than expected", 4, count);
        assertEquals(1, entityTagService.findInboundTagResults(su.getCompany(), entity).size());
        assertEquals(3, entityTagService.findOutboundTagResults(su.getCompany(), entity).size());
    }
}
