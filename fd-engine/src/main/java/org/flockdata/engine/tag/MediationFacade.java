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

package org.flockdata.engine.tag;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.track.bean.TrackResultBean;

/**
 * Non transactional coordinator providing mediation functionality between services
 *
 * @author mholdsworth
 * @since 28/08/2013
 */
public interface MediationFacade {

  Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, String apiKey) throws FlockException, InterruptedException, ExecutionException;

  Collection<TrackRequestResult> trackEntities(Company company, Collection<EntityInputBean> inputBeans) throws FlockException, InterruptedException, ExecutionException;

  FdTagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException;

  Collection<FdTagResultBean> createTags(String apiKey, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

  Collection<FdTagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

  Collection<TrackResultBean> trackEntities(Segment segment, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

  Collection<TrackResultBean> trackEntities(FortressNode fortress, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

  TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, ExecutionException, InterruptedException;

  TrackResultBean trackEntity(Segment segment, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

  TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

  /**
   * Rebuilds all search documents for the supplied fortress
   *
   * @param company      resolved company the caller is authorised to reindex for
   * @param fortressCode name of the fortress to rebuild
   * @return System processing message
   * @throws org.flockdata.helper.FlockException Business exceptions
   * @tag Security, Admin
   */
  String reindex(CompanyNode company, String fortressCode) throws FlockException;

  String reindex(CompanyNode company, EntityNode entity) throws FlockException;

  String reindexByDocType(CompanyNode company, String fortressName, String docType) throws FlockException;

  EntitySummaryBean getEntitySummary(CompanyNode company, String key) throws FlockException;

  //TagCloud getTagCloud(Company company, TagCloudParams tagCloudParams) throws NotFoundException;

  void purge(Fortress fo) throws FlockException;

  void purge(Company company, String fortressCode) throws FlockException;

  void cancelLastLog(Company company, Entity entity) throws IOException, FlockException;


  void mergeTags(Company company, Long source, Long target);

  Map<String, Object> getLogContent(EntityNode entity, Long logId);

  /**
   * Iterates through all search documents and validates that an existing
   * Entity can be found for it by the key returned.
   * <p>
   * Experimental
   *
   * @param company      resolved company
   * @param fortressCode code
   * @param docType      and doc-type
   * @return null - should be a message
   * @throws FlockException business or system exception
   */

  String validateFromSearch(CompanyNode company, String fortressCode, String docType) throws FlockException;

  TrackResultBean trackEntity(FortressNode fortress, EntityInputBean inputBean) throws InterruptedException, FlockException, ExecutionException, IOException;

  void purge(Company company, String fortressCode, String docType);

  void purge(Company company, String code, String code1, String segment);
}
