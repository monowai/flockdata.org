/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackTagDao;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.TagService;
import com.auditbucket.track.bean.TrackTagInputBean;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TrackTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 5:07 PM
 */
@Service
@Transactional
public class TagTrackService {

    @Autowired
    TagService tagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    TrackTagDao trackTagDao;

    private Logger logger = LoggerFactory.getLogger(TagTrackService.class);

    public void processTag(MetaHeader header, TrackTagInputBean tagInput) {
        String relationshipName = tagInput.getType();
        boolean existing = relationshipExists(header, tagInput.getTagName(), relationshipName);
        if (existing)
            // We already have this tagged so get out of here
            return;
        Tag tag = tagService.findTag(header.getFortress().getCompany(), tagInput.getTagName(), tagInput.getIndex());
        trackTagDao.save(header, tag, relationshipName);
    }

    public Boolean relationshipExists(MetaHeader metaHeader, String name, String relationshipType) {
        Tag tag = tagService.findTag(name);
        if (tag == null)
            return false;
        return trackTagDao.relationshipExists(metaHeader, tag, relationshipType);
    }

    /**
     * Directed tag structure hierarchy
     *
     * @param userTags input beans
     * @param company  valid company
     */
    public void createTagStructure(Collection<TagInputBean> userTags, Company company) {
        // Create a tag structure if present
        for (TagInputBean inputBean : userTags) {
            Tag t = tagService.processTag(company, inputBean);
            if (t == null) {
                logger.error("Error creating Tag " + inputBean);
            }
        }
    }

    /**
     * Associates the supplied userTags with the MetaHeaderNode
     * <p/>
     * in JSON terms....
     * "ClientID123" :{"clientKey","prospectKey"}
     * <p/>
     * <p/>
     * The value can be null which will create a simple tag for the Header such as
     * ClientID123
     * <p/>
     * They type can be Null, String or a Collection<String> that describes the relationship
     * types to create.
     * <p/>
     * If this scenario, ClientID123 is created as a single node with two relationships that
     * describe the association - clientKey and prospectKey
     *
     * @param company
     * @param ah       Header to associate userTags with
     * @param lastLog
     * @param userTags Key/Value pair of tags. TagNode will be created if missing. Value can be a Collection
     */
    public Collection<TrackTag> associateTags(Company company, MetaHeader ah, TrackLog lastLog, Collection<TagInputBean> userTags) {
        Collection<TrackTag> rlxs = new ArrayList<>();
        Iterable<TrackTag> existingTags = findTrackTags(company, ah);

        //tagService.processTags(company, userTags);

        for (TagInputBean tagInput : userTags) {

            Tag tag = tagService.processTag(company, tagInput);

            // Handle both simple relationships type name or a map/collection of relationships
            if (tagInput.getMetaLinks() != null) {
                rlxs.addAll(processRelationships(ah, tag, tagInput.getMetaLinks(), tagInput.isReverse()));
            }
            if (tagInput.getMetaLink() != null) // Simple relationship to the track header
                // Makes it easier for the API to call
                rlxs.add(trackTagDao.save(ah, tag, tagInput.getMetaLink(), tagInput.isReverse()));
        }

        Collection<TrackTag> tagsToRelocate = new ArrayList<>();
        for (TrackTag existingTag : existingTags) {
            if (!rlxs.contains(existingTag))
                tagsToRelocate.add(existingTag);
        }

        relocateTags(ah, lastLog, tagsToRelocate);
        return rlxs;
    }

    private void relocateTags(MetaHeader ah, TrackLog currentLog, Collection<TrackTag> tagsToRelocate) {


        if (!tagsToRelocate.isEmpty()) {
            if (currentLog != null)
                trackTagDao.moveTags(ah, currentLog, tagsToRelocate);
        }


    }

    private Collection<TrackTag> processRelationships(MetaHeader ah, Tag tag, Map<String, Object> metaRelationships, boolean isReversed) {
        Collection<TrackTag> trackTags = new ArrayList<>();
        for (String key : metaRelationships.keySet()) {
            Object properties = metaRelationships.get(key);
            Map<String, Object> propMap;
            if (properties != null && properties instanceof Map) {
                propMap = (Map<String, Object>) properties;
                TrackTag trackTagRelationship = trackTagDao.save(ah, tag, key, isReversed, propMap);
                if (trackTagRelationship != null)
                    trackTags.add(trackTagRelationship);
            } else {
                TrackTag trackTagRelationship = trackTagDao.save(ah, tag, key, isReversed);
                if (trackTagRelationship != null)
                    trackTags.add(trackTagRelationship);
            }
        }
        return trackTags;
    }

    /**
     * Finds both incoming and outgoing tags for the MetaHeader
     *
     * @param metaHeader Header the caller is authorised to work with
     * @return TrackTags found
     */
    public Set<TrackTag> findTrackTags(MetaHeader metaHeader) {
        Company company = securityHelper.getCompany();
        return findTrackTags(company, metaHeader);
    }

    public Set<TrackTag> findOutboundTags(MetaHeader header) {
        Company company = securityHelper.getCompany();
        return findOutboundTags(company, header);
    }

    public Set<TrackTag> findOutboundTags(Company company, MetaHeader header) {
        return trackTagDao.getMetaTrackTagsOutbound(company, header);
    }


    public Set<TrackTag> findTrackTags(Company company, MetaHeader metaHeader) {
        return trackTagDao.getMetaTrackTags(company, metaHeader);
    }

    public void deleteTrackTags(MetaHeader metaHeader, Collection<TrackTag> trackTags) throws DatagioException {
        trackTagDao.deleteAuditTags(metaHeader, trackTags);
    }

    public void deleteTrackTags(MetaHeader metaHeader, TrackTag value) throws DatagioException {
        Collection<TrackTag> remove = new ArrayList<>(1);
        remove.add(value);
        deleteTrackTags(metaHeader, remove);

    }

    public void changeType(MetaHeader metaHeader, TrackTag existingTag, String newType) throws DatagioException {
        if (metaHeader == null || existingTag == null || newType == null)
            throw new DatagioException(("Illegal parameter"));
        trackTagDao.changeType(metaHeader, existingTag, newType);
    }


    public Set<MetaHeader> findTrackTags(String tagName) throws DatagioException {
        Tag tag = tagService.findTag(tagName);
        if (tag == null)
            throw new DatagioException("Unable to find the tag [" + tagName + "]");
        return trackTagDao.findTrackTags(tag);

    }

    public Set<Tag> findLogTags(Company company, Log log) {
        return trackTagDao.findLogTags(company, log);
    }
}
