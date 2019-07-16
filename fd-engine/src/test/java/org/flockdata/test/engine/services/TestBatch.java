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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.track.service.BatchService;
import org.flockdata.engine.track.service.FdServerIo;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.FileProcessor;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.services.ContentModelService;
import org.flockdata.test.engine.MapBasedStorageProxy;
import org.flockdata.test.engine.Neo4jConfigTest;
import org.flockdata.test.unit.client.FdTemplateMock;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 8/10/2014
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    Neo4jConfigTest.class,
    FdTemplateMock.class,
    FdServerIo.class,
    MapBasedStorageProxy.class})
@ActiveProfiles( {"dev", "fd-auth-test"})
public class TestBatch extends EngineBase {
    @Autowired
    FileProcessor fileProcessor;
    @Autowired
    private ContentModelService contentModelService;
    @Autowired
    private BatchService batchService;

    @Test
    public void doBatchTest() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("doBatchTest", "mike");
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("doBatchTest", true));
        Document docType = conceptService.resolveByDocCode(fortress, "test-batch");

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");

        contentModelService.saveEntityModel(su.getCompany(), fortress, docType, contentModel);
        batchService.process(su.getCompany(), fortress, docType, "/data/test-batch.csv", false);

        assertNotNull(entityService.findByCode(fortress, docType, "1"));

    }

    @Test
    public void import_ValidateArgs() throws Exception {
        try {
            FileProcessor.validateArgs("/illegalFile");
            fail("Exception not thrown");
        } catch (NotFoundException nfe) {
            // Great
            assertEquals(true, true);
        }
    }


}
