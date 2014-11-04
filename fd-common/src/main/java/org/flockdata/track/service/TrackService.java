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
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.model.Fortress;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchResult;
import org.flockdata.registration.model.Company;
import org.flockdata.track.bean.*;
import org.flockdata.track.model.*;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:22 PM
 */
public interface TrackService {
    EntityContent getWhat(Entity entity, Log change);

    @Deprecated
    Entity getEntity(@NotEmpty String metaKey);

    Entity getEntity(Company company, String metaKey);

    Entity getEntity(Company company, @NotEmpty String metaKey, boolean inflate);

    Entity getEntity(Entity entity);

    Collection<Entity> getEntities(Fortress fortress, Long skipTo);

    Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo);

    void updateEntity(Entity entity);

    EntityLog getLastEntityLog(Company company, String metaKey) throws FlockException;

    EntityLog getLastEntityLog(Long entityId);

    Set<EntityLog> getEntityLogs(Long entityId);

    Set<EntityLog> getEntityLogs(Company company, String metaKey) throws FlockException;

    Set<EntityLog> getEntityLogs(Company company, String metaKey, Date from, Date to) throws FlockException;

    EntitySearchChange cancelLastLog(Company company, Entity entity) throws IOException, FlockException;

    int getLogCount(Company company, String metaKey) throws FlockException;

    public Entity findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef);

    Entity findByCallerRef(Company company, String fortress, String documentCode, String callerRef) throws NotFoundException;

    Entity findByCallerRefFull(Long fortressId, String documentType, String callerRef);

    Entity findByCallerRefFull(Fortress fortress, String documentType, String callerRef);

    Iterable<Entity> findByCallerRef(Company company, String fortressName, String callerRef) throws NotFoundException;

    Entity findByCallerRef(Fortress fortress, String documentType, String callerRef);

    EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException;

    LogDetailBean getFullDetail(Company company, String metaKey, Long logId);

    EntityLog getLogForEntity(Entity entity, Long logId);

    Iterable<TrackResultBean> trackEntities(Fortress fortress, Iterable<EntityInputBean> inputBeans) throws InterruptedException, ExecutionException, FlockException, IOException;

    Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws FlockException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String metaKey, String xRefName) throws FlockException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws FlockException;

    List<EntityKey> crossReferenceEntities(Company company, EntityKey sourceKey, Collection<EntityKey> targetKeys, String xRefName) throws FlockException;

    Map<String, Entity> getEntities(Company company, Collection<String> metaKeys);

    void purge(Fortress fortress);

    void recordSearchResult(SearchResult searchResult, Long metaId) throws FlockException;

    Collection<EntityTag> getLastLogTags(Company company, String metaKey) throws FlockException;

    EntityLog getEntityLog(Company company, String metaKey, long logId) throws FlockException;

    /**
     *
     * It a tag is removed from an entity, then it is associated to the last log that it was known to belong to
     * This call returns those entity tags associated with
     *
     * @param company company caller is authorized to work with
     * @param entityLog Log for which tags might exist
     * @return All entity Tags archived to the log
     */

    Collection<EntityTag> getLogTags(Company company, EntityLog entityLog);

    List<CrossReferenceInputBean> crossReferenceEntities(Company company, List<CrossReferenceInputBean> crossReferenceInputBeans);
}
