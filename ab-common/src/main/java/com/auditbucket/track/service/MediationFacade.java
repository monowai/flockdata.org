package com.auditbucket.track.service;

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.EntitySummaryBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 2:46 PM
 */
public interface MediationFacade {
    Tag createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException;

    Collection<Tag> createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException;

    Future<Collection<TrackResultBean>> trackEntitiesAsync(Fortress fortress, List<EntityInputBean> inputBeans) throws FlockException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackEntities(Fortress fortress, List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Fortress fortress, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException;

    Long reindex(Company company, String fortressName) throws FlockException;

    void reindexByDocType(Company company, String fortressName, String docType) throws FlockException;

    EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException;

    EsSearchResult search(Company company, QueryParams queryParams);

    void purge(String fortressName, String apiKey) throws FlockException;

    void cancelLastLog(Company company, Entity entity) throws IOException, FlockException;

    Collection<TrackResultBean> trackEntities(Company company, List<EntityInputBean> entityInputBeans) throws InterruptedException, ExecutionException, FlockException, IOException;
}
