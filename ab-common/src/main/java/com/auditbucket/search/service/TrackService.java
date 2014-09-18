package com.auditbucket.search.service;

import com.auditbucket.search.model.EntitySearchChanges;
import com.auditbucket.track.model.Entity;

import java.io.IOException;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:57 AM
 */
public interface TrackService {

    void createSearchableChange(EntitySearchChanges changes) throws IOException;

    void createSearchableChange(byte[] bytes) throws IOException;

    void delete(Entity entity);

    byte[] findOne(Entity entity);

    byte[] findOne(Entity entity, String id);
}
