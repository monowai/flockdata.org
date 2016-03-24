/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.*;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchResult;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:22 PM
 */
public interface EntityService {

    EntityKeyBean findParent(Entity entity);

    Collection<EntityKeyBean> getInboundEntities(Entity entity, boolean withEntityTags);

    public enum TAG_STRUCTURE {TAXONOMY, DEFAULT}

    StoredContent getContent(Entity entity, Log change);

    @Deprecated
    Entity getEntity(String key);

    Entity getEntity(Company company, String key) throws NotFoundException;

    Entity getEntity(Company company, String key, boolean inflate);

    Entity getEntity(Entity entity);

    Collection<Entity> getEntities(Fortress fortress, Long skipTo);

    Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo);

    void updateEntity(Entity entity);

    EntityLog getLastEntityLog(Company company, String key) throws FlockException;

    EntityLog getLastEntityLog(Long entityId);

    Set<EntityLog> getEntityLogs(Entity entity);

    Set<EntityLog> getEntityLogs(Company company, String key) throws FlockException;

    Set<EntityLog> getEntityLogs(Company company, String key, Date from, Date to) throws FlockException;

    EntitySearchChange cancelLastLog(Company company, Entity entity) throws IOException, FlockException;

    int getLogCount(Company company, String key) throws FlockException;

    Entity findByCode(Fortress fortress, DocumentType documentType, String code);

    Entity findByCode(Company company, String fortress, String documentCode, String code) throws NotFoundException;

    Entity findByCodeFull(Long fortressId, String documentType, String code);

    Entity findByCodeFull(Fortress fortress, String documentType, String code);

    Iterable<Entity> findByCode(Company company, String fortressName, String code) throws NotFoundException;

    Entity findByCode(Fortress fortress, String documentName, String code);

    EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException;

    LogDetailBean getFullDetail(Company company, String key, Long logId);

    EntityLog getLogForEntity(Entity entity, Long logId);

    Collection<TrackResultBean> trackEntities(FortressSegment segment, Collection<EntityInputBean> inputBeans, Future<Collection<TagResultBean>> tags) throws InterruptedException, ExecutionException, FlockException, IOException;

    Collection<String> crossReference(Company company, String key, Collection<String> xRef, String relationshipName) throws FlockException;

    /**
     * Locates cross linked entities with the given relationship type
     * @param company      Owner
     * @param key      FD UID
     * @param relationship relationship
     * @return all entities connected
     * @throws FlockException
     */
    Map<String, Collection<Entity>> getCrossReference(Company company, String key, String relationship) throws FlockException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String code, String xRefName) throws FlockException;

    /**
     *
     * Source Entity MUST exist otherwise an exception will be thrown
     *
     * @param company       who owns the data
     * @param sourceKey     what we will link from
     * @param targetKeys    collection of entities we will link to
     * @param xRefName      the name to give the relationship
     * @return all targetkeys that were ignored
     * @throws FlockException
     */
    Collection<EntityKeyBean> linkEntities(Company company, EntityKeyBean sourceKey, Collection<EntityKeyBean> targetKeys, String xRefName) throws FlockException;

    Map<String, Entity> getEntities(Company company, Collection<String> keys);

    void purge(Fortress fortress, Collection<String> keys);

    void purgeFortressDocs(Fortress fortress);

    void recordSearchResult(SearchResult searchResult, Long metaId) throws FlockException;

    Collection<EntityTag> getLastLogTags(Company company, String key) throws FlockException;

    EntityLog getEntityLog(Company company, String key, Long logId) throws FlockException;

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

    Collection<EntityLinkInputBean> linkEntities(Company company, Collection<EntityLinkInputBean> entityLinks);

    Entity save(Entity entity);

    Collection<Entity> getEntities(Collection<Long> entities);

    Collection<String> getEntityBatch(Fortress fortress, int count);
}
