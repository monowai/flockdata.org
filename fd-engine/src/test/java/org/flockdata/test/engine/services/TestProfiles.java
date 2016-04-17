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

import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.profile.ContentProfileDeserializer;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.service.ContentProfileService;
import org.flockdata.registration.FortressInputBean;
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
    ContentProfileService profileService;


    @Test
    @Transactional
    public void create_Profile() throws Exception{
        SystemUser su = registerSystemUser("create_Profile", mike_admin);

        ContentProfile profile = ContentProfileDeserializer.getContentProfile("/profiles/test-profile.json");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("create_profile", true));
        DocumentType docType = conceptService.resolveByDocCode(fortress, "Olympic");
        profileService.save(fortress, docType, profile);
        ContentProfile savedProfile = profileService.get(fortress, docType);
        assertNotNull ( savedProfile);
        assertEquals(profile.getFortressUser(), savedProfile.getFortressUser());
        assertEquals(profile.isEntityOnly(), savedProfile.isEntityOnly());
        assertEquals(profile.getContent().size(), savedProfile.getContent().size());
        ColumnDefinition column = savedProfile.getContent().get("TagVal");
        assertNotNull(column);
        assertEquals(true, column.isMustExist());
        assertEquals(true, column.isTag());
        assertNull(savedProfile.getHandler());
        column.setMustExist(false);
        profileService.save(fortress,docType, savedProfile);
        savedProfile = profileService.get(fortress, docType);
        assertNull(savedProfile.getHandler());
        assertFalse("Updating the mustExist attribute did not persist",savedProfile.getContent().get("TagVal").isMustExist());




    }



}
