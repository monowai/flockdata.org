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
import org.flockdata.model.*;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 2:46 PM
 */
public interface MediationFacade {

    Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, String apiKey) throws FlockException, InterruptedException, ExecutionException, IOException;

    TagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException;

    Collection<TagResultBean> createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackEntities(FortressSegment segment, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackEntities(Fortress fortress, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(FortressSegment segment, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

    String reindex(Company company, String fortressCode) throws FlockException;

    String reindex(Company company, Entity entity) throws FlockException;

    String reindexByDocType(Company company, String fortressName, String docType) throws FlockException;

    EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException;

    //TagCloud getTagCloud(Company company, TagCloudParams tagCloudParams) throws NotFoundException;

    void purge(Fortress fo) throws FlockException;

    void purge(Company company, String fortressCode) throws FlockException;

    void cancelLastLog(Company company, Entity entity) throws IOException, FlockException;


    void mergeTags(Company company, Long source, Long target);

    void createAlias(Company company, String label, Tag source, String akaValue);


    Map<String,Object> getLogContent(Entity entity, Long logId);

    String validateFromSearch(Company company, String fortressName, String docType) throws FlockException;

    TrackResultBean trackEntity(Fortress fortress, EntityInputBean inputBean) throws InterruptedException, FlockException, ExecutionException, IOException;
}
