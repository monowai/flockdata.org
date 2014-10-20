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

package org.flockdata.dao;

import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.TrackTag;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.track.model.Log;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:55 PM
 */
public interface TrackTagDao {

    // Property that refers to when this relationship was introduced to AB
    String AB_WHEN = "abWhen";

    TrackTag save(Entity entity, Tag tag, String relationshipName);

    TrackTag save(Entity ah, Tag tag, String metaLink, boolean reverse);

    TrackTag save(Entity ah, Tag tag, String relationshipName, Boolean isReversed, Map<String, Object> propMap);

    Boolean relationshipExists(Entity entity, Tag tag, String relationshipName);

    /**
     * Track Tags that are in either direction
     *
     * @param company    validated company
     * @param entity    entity the caller is authorised to work with
     * @return           all TrackTags for the company in both directions
     */
    Set<TrackTag> getMetaTrackTags(Company company, Entity entity);

    Set<TrackTag> getDirectedMetaTags(Company company, Entity entity, boolean outbound);

    Set<TrackTag> findLogTags(Company company, Log log) ;

    void changeType(Entity entity, TrackTag existingTag, String newType);

    Set<Entity> findTrackTags(Tag tag);

    void moveTags(Entity entity, Log log, Collection<TrackTag> trackTag);

    void deleteTrackTags(Entity entity, Collection<TrackTag> trackTags) throws FlockException;

    void moveTags(Company company, Log logToMoveFrom, Entity entity);
}
