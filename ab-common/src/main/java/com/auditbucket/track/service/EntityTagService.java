package com.auditbucket.track.service;

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.TrackTagInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.TrackTag;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.model.EntityLog;

import java.util.Collection;
import java.util.Set;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 5:13 PM
 */
public interface EntityTagService {
    void processTag(Entity entity, TrackTagInputBean tagInput);

    Boolean relationshipExists(Entity entity, String name, String relationshipType);

    Collection<TrackTag> associateTags(Company company, Entity ah, EntityLog lastLog, Collection<TagInputBean> userTags, Boolean archiveRemovedTags);

    Collection<TrackTag> findEntityTags(Company company, Entity entity);

    Collection<TrackTag> findEntityTags(Entity entity);

    Collection<TrackTag> findOutboundTags(Entity entity);

    Collection<TrackTag> findOutboundTags(Company company, Entity entity);

    Collection<TrackTag> findInboundTags(Company company, Entity entity);

    Collection<TrackTag> getEntityTags(Company company, Entity entity);

    void deleteTrackTags(Entity entity, Collection<TrackTag> trackTags) throws FlockException;

    void deleteTrackTags(Entity entity, TrackTag value) throws FlockException;

    void changeType(Entity entity, TrackTag existingTag, String newType) throws FlockException;

    Set<Entity> findTrackTags(String tagName) throws FlockException;

    Collection<TrackTag> findLogTags(Company company, Log log);

    void moveTags(Company company, Log previousLog, Entity entity);


}
