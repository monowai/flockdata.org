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
import org.flockdata.profile.ContentProfileResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:31 PM
 */
@Repository
public class ProfileDaoNeo {
    @Autowired
    ProfileRepo profileRepo;

    @Autowired
    Neo4jTemplate template;

    public Profile find ( Fortress fortress, DocumentType documentType ){
        return profileRepo.findContentProfile(fortress.getId(), documentType.getId());
    }

    public Profile save(Profile profileToSave) {
        Profile profile = profileRepo.save(profileToSave);
        template.fetch(profile.getDocument());
        template.fetch(profile.getFortress());
        return profile;
    }

    public Collection<ContentProfileResult> find(Long companyId) {
        Collection<Profile> profiles = profileRepo.findCompanyProfiles(companyId);
        Collection<ContentProfileResult>results = new ArrayList<>(profiles.size());
        for (Profile profile : profiles) {
            template.fetch(profile.getFortress());
            template.fetch(profile.getDocument());
            results.add(new ContentProfileResult(profile));
        }
        return results;
    }

    public ContentProfileResult findByKey(Long companyID, String key){
        Profile profile = profileRepo.findByKey(key);
        if ( profile == null )
            return null;

        if (!Objects.equals(profile.getCompany().getId(), companyID))
            return null; // Somehow you have a key but it ain't for this company

        // Profiles can simply be stored against the company if they just import tags
        if ( profile.getFortress()!=null )
            template.fetch(profile.getFortress());
        if ( profile.getDocument()!=null)
            template.fetch(profile.getDocument());
        if ( profile.getCompany()!=null)
            template.fetch(profile.getCompany());

        return new ContentProfileResult(profile);
    }
}
