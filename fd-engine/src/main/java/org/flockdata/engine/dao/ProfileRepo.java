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

import org.flockdata.model.Profile;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:51 PM
 */
public interface ProfileRepo extends GraphRepository<Profile> {

    @Query( elementClass = Profile.class, value=" match (c:FDCompany)-[:OWNS]->(f:Fortress)-[:FORTRESS_PROFILE]-(profile:Profile)" +
            " where id(c)={0} " +
            " return profile " +
            " limit 100 ")

    Collection<Profile> findCompanyProfiles(Long companyId);

    @Query( elementClass = Profile.class, value=" match (profile:Profile {key:{0}})" +
            " return profile " )
    Profile findByKey( String key);

    @Query( elementClass = Profile.class, value=" match (f:Fortress)<-[:FORTRESS_PROFILE]-(profile:Profile)-[:DOCUMENT_PROFILE]-(d:DocType)" +
            " where id(f)={0} and id(d)={1}" +
            " return profile " )
    Profile findContentProfile(Long fortressId, Long documentId);
}
