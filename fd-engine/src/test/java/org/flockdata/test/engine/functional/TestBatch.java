/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.engine.functional;

import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Entity;
import org.flockdata.transform.FileProcessor;
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
    ImportProfileService importProfileService;

    @Test
    public void doBatchTest() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("doBatchTest", "mike");
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("doBatchTest", true));
        DocumentType docType = schemaService.resolveByDocCode(fortress, "test-batch");

        ImportProfile params = Helper.getImportParams("/batch-csv-profile.json");

        importProfileService.save(fortress, docType, params );
        importProfileService.process(su.getCompany(), fortress, docType, "/batch-test.csv", false);

        Entity resultBean = trackService.findByCallerRef( fortress, docType, "1");
        assertNotNull(resultBean);

    }

    @Test
    public void import_ValidateArgs() throws Exception{
        FileProcessor fileProcessor = new FileProcessor();
        ProfileConfiguration profileConfiguration = new ImportProfile();
        try {
            FileProcessor.validateArgs("/illegalFile");
            fail("Exception not thrown");
        } catch ( NotFoundException nfe){
            // Great
            assertEquals(true,true);
        }
    }


}
