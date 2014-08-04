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

package com.auditbucket.dao;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TrackTag;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:55 PM
 */
public interface TrackTagDao {
    TrackTag save(MetaHeader metaHeader, Tag tag, String relationshipName);

    TrackTag save(MetaHeader ah, Tag tag, String metaLink, boolean reverse);

    TrackTag save(MetaHeader ah, Tag tag, String relationshipName, Boolean isReversed, Map<String, Object> propMap);

    Boolean relationshipExists(MetaHeader metaHeader, Tag tag, String relationshipName);

    /**
     * Track Tags that are in either direction
     *
     * @param company    validated company
     * @param metaHeader header the caller is authorised to work with
     * @return           all TrackTags for the company in both directions
     */
    Set<TrackTag> getMetaTrackTags(Company company, MetaHeader metaHeader);

    Set<TrackTag> getMetaTrackTagsOutbound(Company company, MetaHeader header);

    void deleteTrackTags(MetaHeader metaHeader, Collection<TrackTag> trackTags) throws DatagioException;

    void changeType(MetaHeader metaHeader, TrackTag existingTag, String newType);

    Set<MetaHeader> findTrackTags(Tag tag);

    void moveTags(MetaHeader metaHeader, TrackLog currentLog, Collection<TrackTag> trackTag);

    Set<Tag> findLogTags(Company company, Log log) ;
}
