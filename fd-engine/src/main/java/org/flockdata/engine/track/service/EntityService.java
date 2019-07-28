/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchResult;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.EntityToEntityLinkInput;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.LogDetailBean;
import org.flockdata.track.bean.TrackResultBean;

/**
 * @author mholdsworth
 * @since 5/09/2014
 */
public interface EntityService {

  Map<String, Object> getEntityDataLast(Company company, Entity entity) throws FlockException;

  Map<String, Object> getEntityDataLast(Company company, String key) throws FlockException;

  Collection<EntityKeyBean> getEntities(Company company, List<EntityKeyBean> linkTo);

  Collection<EntityKeyBean> getNestedParentEntities(Entity company, Document docType);

  Entity find(Company company, EntityKeyBean entityKeyBean);

  StoredContent getContent(EntityNode entity, LogNode change);

  EntityNode getEntity(Company company, String key);

//    Entity getEntity(Company company, String key) throws NotFoundException;

  EntityNode getEntity(Company company, String key, boolean inflate);

  Entity getEntity(EntityNode entity);

  Collection<Entity> getEntities(Fortress fortress, Long skipTo);

  Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo);

  void updateEntity(EntityNode entity);

  EntityLog getLastEntityLog(Company company, String key) throws FlockException;

  EntityLog getLastEntityLog(Long entityId);

  Collection<org.flockdata.data.EntityLog> getEntityLogs(Entity entity);

  Collection<EntityLogResult> getEntityLogs(Company company, String key) throws FlockException;

  Collection<EntityLogResult> getEntityLogs(Company company, String key, boolean withData);

  Set<EntityLog> getEntityLogs(Company company, String key, Date from, Date to) throws FlockException;

  EntitySearchChange cancelLastLog(Company company, EntityNode entity) throws IOException, FlockException;

  int getLogCount(Company company, String key) throws FlockException;

  Entity findByCode(Fortress fortress, Document documentType, String code);

  Entity findByCode(Company company, String fortress, String documentCode, String code) throws NotFoundException;

  Entity findByCodeFull(FortressNode fortress, String documentType, String code);

  Iterable<Entity> findByCode(Company company, String fortressName, String code) throws NotFoundException;

  Entity findByCode(Fortress fortress, String documentName, String code);

  EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException;

  LogDetailBean getFullDetail(Company company, String key, Long logId);

  EntityLog getLogForEntity(EntityNode entity, Long logId);

  Collection<TrackResultBean> trackEntities(DocumentNode documentType, Segment segment, Collection<EntityInputBean> inputBeans, Future<Collection<FdTagResultBean>> tags) throws InterruptedException, ExecutionException, FlockException;

  Collection<String> crossReference(Company company, String key, Collection<String> xRef, String relationshipName) throws FlockException;

  /**
   * Locates cross linked entities with the given relationship type
   *
   * @param company      Owner
   * @param key          FD UID
   * @param relationship relationship
   * @return all entities connected
   * @throws FlockException problems
   */
  Map<String, Collection<EntityNode>> getCrossReference(Company company, String key, String relationship) throws FlockException;

  Map<String, Collection<EntityNode>> getCrossReference(Company company, String fortressName, String code, String xRefName) throws FlockException;

  /**
   * Source Entity MUST exist otherwise an exception will be thrown
   *
   * @param company    who owns the data
   * @param sourceKey  what we will link from
   * @param targetKeys collection of entities we will link to
   * @param xRefName   the name to give the relationship
   * @return all targetkeys that were ignored
   * @throws FlockException problems
   */
  Collection<EntityKeyBean> linkEntities(Company company, EntityKeyBean sourceKey, Collection<EntityKeyBean> targetKeys, String xRefName) throws FlockException;

  Map<String, EntityNode> getEntities(Company company, Collection<String> keys);

  void purge(Fortress fortress, Collection<String> keys);

  void purgeFortressDocs(Fortress fortress);

  void recordSearchResult(SearchResult searchResult, Long metaId) throws FlockException;

  Collection<EntityTag> getLastLogTags(Company company, String key) throws FlockException;

  org.flockdata.data.EntityLog getEntityLog(CompanyNode company, String key, Long logId) throws FlockException;

  /**
   * It a tag is removed from an entity, then it is associated to the last log that it was known to belong to
   * This call returns those entity tags associated with
   *
   * @param company   company caller is authorized to work with
   * @param entityLog Log for which tags might exist
   * @return All entity Tags archived to the log
   */
  Collection<EntityTag> getLogTags(Company company, org.flockdata.data.EntityLog entityLog);

  Collection<EntityToEntityLinkInput> linkEntities(Company company, Collection<EntityToEntityLinkInput> entityLinks);

  EntityNode save(EntityNode entity);

  Collection<EntityNode> getEntities(Collection<Long> entities);

  Collection<String> getEntityBatch(Fortress fortress, int count);

  Collection<String> getEntityBatch(Fortress fortress, Document documentType, Segment fortressSegment, int count);

}
