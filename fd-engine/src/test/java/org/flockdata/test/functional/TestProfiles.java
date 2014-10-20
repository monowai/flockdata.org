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

package org.flockdata.test.functional;

import org.flockdata.profile.ImportProfileDeserializer;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.track.model.DocumentType;
import org.flockdata.transform.ColumnDefinition;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 8:35 PM
 */
public class TestProfiles extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(TestProfiles.class);
    @Autowired
    ImportProfileService profileService;


    @Test
    @Transactional
    public void create_Profile() throws Exception{
        SystemUser su = registerSystemUser("create_Profile", mike_admin);

        ProfileConfiguration profile = ImportProfileDeserializer.getImportParams("/test_profile.json");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("create_profile", true));
        DocumentType docType = schemaService.resolveByDocCode(fortress, "Olympic");
        profileService.save(fortress, docType, profile);
        ProfileConfiguration savedProfile = profileService.get(fortress, docType);
        assertNotNull ( savedProfile);
        assertEquals(profile.getFortressUser(), savedProfile.getFortressUser());
        assertEquals(profile.isEntityOnly(), savedProfile.isEntityOnly());
        assertEquals(profile.getContent().size(), savedProfile.getContent().size());
        ColumnDefinition column = savedProfile.getContent().get("TagVal");
        assertNotNull(column);
        assertEquals(true, column.isMustExist());
        assertEquals(true, column.isTag());
        assertNull(savedProfile.getClazz());
        column.setMustExist(false);
        profileService.save(fortress,docType, savedProfile);
        savedProfile = profileService.get(fortress, docType);
        assertNull(savedProfile.getClazz());
        assertFalse("Updating the mustExist attribute did not persist",savedProfile.getContent().get("TagVal").isMustExist());




    }



}
