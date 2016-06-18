/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.services;

import org.flockdata.helper.NotFoundException;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ContentModelImpl;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.shared.FileProcessor;
import org.flockdata.track.service.BatchService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 8/10/14
 * Time: 5:30 PM
 */
public class TestBatch extends EngineBase {
    @Autowired
    ContentModelService contentModelService;

    @Autowired
    BatchService batchService;

    @Autowired
    FileProcessor fileProcessor;

    @Test
    public void doBatchTest() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("doBatchTest", "mike");
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("doBatchTest", true));
        DocumentType docType = conceptService.resolveByDocCode(fortress, "test-batch");

        ContentModelImpl contentProfile = ContentModelDeserializer.getContentModel("/models/test-csv-batch.json");

        contentModelService.saveEntityModel(su.getCompany(), fortress, docType, contentProfile );
        batchService.process(su.getCompany(), fortress, docType, "/data/test-batch.csv", false);

        Entity resultBean = entityService.findByCode(fortress, docType, "1");
        assertNotNull(resultBean);

    }

    @Test
    public void import_ValidateArgs() throws Exception{
        FileProcessor fileProcessor = new FileProcessor();
        ContentModel contentModel = new ContentModelImpl();
        try {
            FileProcessor.validateArgs("/illegalFile");
            fail("Exception not thrown");
        } catch ( NotFoundException nfe){
            // Great
            assertEquals(true,true);
        }
    }


}
