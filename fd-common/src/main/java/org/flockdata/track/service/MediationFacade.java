/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;

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
    TagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException;

    Collection<TagResultBean> createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

    public Collection<TrackResultBean> trackEntities(Collection<EntityInputBean> inputBeans, String apiKey) throws FlockException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackEntities(Fortress fortress, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Fortress fortress, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

    String reindex(Company company, String fortressCode) throws FlockException;

    String reindexByDocType(Company company, String fortressName, String docType) throws FlockException;

    EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException;

    //TagCloud getTagCloud(Company company, TagCloudParams tagCloudParams) throws NotFoundException;

    void purge(Fortress fo) throws FlockException;

    void purge(Company company, String fortressCode) throws FlockException;

    void cancelLastLog(Company company, Entity entity) throws IOException, FlockException;


    void mergeTags(Company company, Tag source, Tag target);

    void createAlias(Company company, String label, Tag source, String akaValue);


    Map<String,Object> getLogContent(Entity entity, Long logId);

    String validateFromSearch(Company company, String fortressName, String docType) throws FlockException;
}
