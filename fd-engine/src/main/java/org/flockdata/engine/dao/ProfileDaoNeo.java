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

package org.flockdata.engine.dao;

import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:31 PM
 */
@Repository
public class ProfileDaoNeo {
    @Autowired
    ProfileRepo profileRepo;

    public Profile find ( Fortress fortress, DocumentType documentType ){
        String key = Profile.parseKey (fortress, documentType);
        return profileRepo.findBySchemaPropertyValue("profileKey", key);
    }

    public Profile save(Profile profile) {
        return profileRepo.save(profile);
    }
}
