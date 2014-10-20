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

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.TrackTagInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.Log;
import org.flockdata.track.model.TrackTag;
import org.flockdata.registration.model.Company;
import org.flockdata.track.model.EntityLog;

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