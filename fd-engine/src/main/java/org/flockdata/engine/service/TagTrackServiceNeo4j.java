/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.engine.service;

import org.flockdata.dao.TrackTagDao;
import org.flockdata.engine.repo.neo4j.TrackTagDaoNeo;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.TrackTagInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.TrackTag;
import org.flockdata.track.service.TagService;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.registration.model.Company;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.Log;

import org.flockdata.track.service.EntityTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 5:07 PM
 */
@Service
@Transactional
public class TagTrackServiceNeo4j implements EntityTagService {

    @Autowired
    TagService tagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    TrackTagDaoNeo trackTagDao;

    private Logger logger = LoggerFactory.getLogger(TagTrackServiceNeo4j.class);

    @Override
    public void processTag(Entity entity, TrackTagInputBean tagInput) {
        String relationshipName = tagInput.getType();
        boolean existing = relationshipExists(entity, tagInput.getTagName(), relationshipName);
        if (existing)
            // We already have this tagged so get out of here
            return;
        Tag tag = tagService.findTag(entity.getFortress().getCompany(), tagInput.getTagName(), tagInput.getIndex());
        trackTagDao.save(entity, tag, relationshipName);
    }

    @Override
    public Boolean relationshipExists(Entity entity, String name, String relationshipType) {
        Tag tag = tagService.findTag(name);
        if (tag == null)
            return false;
        return trackTagDao.relationshipExists(entity, tag, relationshipType);
    }

    /**
     * Associates the supplied userTags with the EntityNode
     * <p/>
     * in JSON terms....
     * "ClientID123" :{"clientKey","prospectKey"}
     * <p/>
     * <p/>
     * The value can be null which will create a simple tag for the Entity such as
     * ClientID123
     * <p/>
     * They type can be Null, String or a Collection<String> that describes the relationship
     * types to create.
     * <p/>
     * If this scenario, ClientID123 is created as a single node with two relationships that
     * describe the association - clientKey and prospectKey
     *  @param company
     * @param ah       Entity to associate userTags with
     * @param lastLog
     * @param userTags Key/Value pair of tags. TagNode will be created if missing. Value can be a Collection
     * @param archiveRemovedTags
     */
    @Override
    public Collection<TrackTag> associateTags(Company company, Entity ah, EntityLog lastLog, Collection<TagInputBean> userTags, Boolean archiveRemovedTags) {
        Collection<TrackTag> rlxs = new ArrayList<>();
        Iterable<TrackTag> existingTags = getEntityTags(company, ah);

        for (TagInputBean tagInput : userTags) {

            Tag tag = tagService.createTag(company, tagInput);

            // Handle both simple relationships type name or a map/collection of relationships
            if (tagInput.getEntityLinks() != null) {
                rlxs.addAll(writeRelationships(ah, tag, tagInput.getEntityLinks(), tagInput.isReverse()));
            }
            if (tagInput.getEntityLink() != null) // Simple relationship to the entity
                // Makes it easier for the API to call
                rlxs.add(trackTagDao.save(ah, tag, tagInput.getEntityLink(), tagInput.isReverse()));
        }

        if (!userTags.isEmpty()) {
            // We only consider relocating tags to the log if the caller passes at least one tag set
            Collection<TrackTag> tagsToRelocate = new ArrayList<>();
            for (TrackTag existingTag : existingTags) {
                if (!rlxs.contains(existingTag))
                    tagsToRelocate.add(existingTag);
            }
            if (archiveRemovedTags)
                relocateTags(ah, lastLog, tagsToRelocate);
        }
        return rlxs;
    }

    private void relocateTags(Entity ah, EntityLog currentLog, Collection<TrackTag> tagsToRelocate) {
        if (!tagsToRelocate.isEmpty()) {
            if (currentLog != null)
                trackTagDao.moveTags(ah, currentLog.getLog(), tagsToRelocate);
        }
    }

    private Collection<TrackTag> writeRelationships(Entity entity, Tag tag, Map<String, Object> metaRelationships, boolean isReversed) {
        Collection<TrackTag> trackTags = new ArrayList<>();
        long when = entity.getFortressLastWhen();
        if ( when == 0 )
            when = entity.getWhenCreated();
        for (String key : metaRelationships.keySet()) {
            Object properties = metaRelationships.get(key);
            Map<String, Object> propMap;
            if (properties != null && properties instanceof Map) {
                propMap = (Map<String, Object>) properties;
            } else {
                propMap = new HashMap<>();
            }

            propMap.put(TrackTagDao.AB_WHEN, when);
            TrackTag trackTagRelationship = trackTagDao.save(entity, tag, key, isReversed, propMap);
            if (trackTagRelationship != null)
                trackTags.add(trackTagRelationship);

        }
        return trackTags;
    }

    /**
     * Finds both incoming and outgoing tags for the Entity
     *
     * @param entity Entity the caller is authorised to work with
     * @return TrackTags found
     */
    @Override
    public Collection<TrackTag> findEntityTags(Entity entity) {
        Company company = securityHelper.getCompany();
        return findEntityTags(company, entity);
    }

    public Collection<TrackTag> findEntityTags(Company company, Entity entity){
        return getEntityTags(company, entity);
    }

    @Override
    public Collection<TrackTag> findOutboundTags(Entity entity) {
        Company company = securityHelper.getCompany();
        return findOutboundTags(company, entity);
    }

    @Override
    public Collection<TrackTag> findOutboundTags(Company company, Entity entity) {
        return trackTagDao.getDirectedEntityTags(company, entity, true);
    }

    @Override
    public Collection<TrackTag> findInboundTags(Company company, Entity entity) {
        return trackTagDao.getDirectedEntityTags(company, entity, false);
    }

    @Override
    public Collection<TrackTag> getEntityTags(Company company, Entity entity) {
        return trackTagDao.getEntityTags(company, entity);
    }

    @Override
    public Collection<TrackTag> findLogTags(Company company, Log log) {
        return trackTagDao.findLogTags(company, log);
    }

    @Override
    public void deleteTrackTags(Entity entity, Collection<TrackTag> trackTags) throws FlockException {
        trackTagDao.deleteEntityTags(entity, trackTags);
    }

    @Override
    public void deleteTrackTags(Entity entity, TrackTag value) throws FlockException {
        Collection<TrackTag> remove = new ArrayList<>(1);
        remove.add(value);
        deleteTrackTags(entity, remove);

    }

    @Override
    public void changeType(Entity entity, TrackTag existingTag, String newType) throws FlockException {
        if (entity == null || existingTag == null || newType == null)
            throw new FlockException(("Illegal parameter"));
        trackTagDao.changeType(entity, existingTag, newType);
    }


    @Override
    public Set<Entity> findTrackTags(String tagName) throws FlockException {
        Tag tag = tagService.findTag(tagName);
        if (tag == null)
            throw new FlockException("Unable to find the tag [" + tagName + "]");
        return trackTagDao.findEntityTags(tag);

    }

    @Override
    public void moveTags(Company company, Log previousLog, Entity entity) {
        trackTagDao.moveTags(company, previousLog, entity);
    }
}