package com.auditbucket.engine.repo.neo4j.dao;

import com.auditbucket.engine.repo.neo4j.ProfileRepo;
import com.auditbucket.engine.repo.neo4j.model.ProfileNode;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:31 PM
 */
@Repository
public class ProfileDao {
    @Autowired
    ProfileRepo profileRepo;

    public ProfileNode find ( Fortress fortress, DocumentType documentType ){
        String key = ProfileNode.parseKey (fortress, documentType);
        return profileRepo.findBySchemaPropertyValue("profileKey", key);
    }

    public ProfileNode save(ProfileNode profile) {
        return profileRepo.save(profile);
    }
}
