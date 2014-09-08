package com.auditbucket.search.service;

import com.auditbucket.search.model.MetaSearchChanges;
import com.auditbucket.track.model.MetaHeader;

import java.io.IOException;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:57 AM
 */
public interface TrackService {

    void createSearchableChange(MetaSearchChanges changes) throws IOException;

    void delete(MetaHeader metaHeader);

    byte[] findOne(MetaHeader header);

    byte[] findOne(MetaHeader header, String id);
}
