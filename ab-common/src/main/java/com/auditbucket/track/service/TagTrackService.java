package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.bean.TrackTagInputBean;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TrackTag;

import java.util.Collection;
import java.util.Set;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 5:13 PM
 */
public interface TagTrackService {
    void processTag(MetaHeader header, TrackTagInputBean tagInput);

    Boolean relationshipExists(MetaHeader metaHeader, String name, String relationshipType);

    Collection<TrackTag> associateTags(Company company, MetaHeader ah, TrackLog lastLog, Collection<TagInputBean> userTags);

    Set<TrackTag> findTrackTags(MetaHeader metaHeader);

    Set<TrackTag> findOutboundTags(MetaHeader header);

    Set<TrackTag> findOutboundTags(Company company, MetaHeader header);

    Set<TrackTag> findInboundTags(Company company, MetaHeader header);

    Set<TrackTag> findTrackTags(Company company, MetaHeader metaHeader);

    void deleteTrackTags(MetaHeader metaHeader, Collection<TrackTag> trackTags) throws DatagioException;

    void deleteTrackTags(MetaHeader metaHeader, TrackTag value) throws DatagioException;

    void changeType(MetaHeader metaHeader, TrackTag existingTag, String newType) throws DatagioException;

    Set<MetaHeader> findTrackTags(String tagName) throws DatagioException;

    Set<TrackTag> findLogTags(Company company, Log log);

    void moveTags(Company company, Log previousLog, MetaHeader metaHeader);
}
