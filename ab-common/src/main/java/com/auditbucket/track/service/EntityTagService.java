package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.bean.TrackTagInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TrackTag;

import java.util.Collection;
import java.util.Set;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 5:13 PM
 */
public interface EntityTagService {
    void processTag(Entity header, TrackTagInputBean tagInput);

    Boolean relationshipExists(Entity entity, String name, String relationshipType);

    Collection<TrackTag> associateTags(Company company, Entity ah, TrackLog lastLog, Collection<TagInputBean> userTags);

    Set<TrackTag> findEntityTags(Entity entity);

    Set<TrackTag> findOutboundTags(Entity header);

    Set<TrackTag> findOutboundTags(Company company, Entity header);

    Set<TrackTag> findInboundTags(Company company, Entity header);

    Set<TrackTag> findTrackTags(Company company, Entity entity);

    void deleteTrackTags(Entity entity, Collection<TrackTag> trackTags) throws DatagioException;

    void deleteTrackTags(Entity entity, TrackTag value) throws DatagioException;

    void changeType(Entity entity, TrackTag existingTag, String newType) throws DatagioException;

    Set<Entity> findTrackTags(String tagName) throws DatagioException;

    Set<TrackTag> findLogTags(Company company, Log log);

    void moveTags(Company company, Log previousLog, Entity entity);
}
