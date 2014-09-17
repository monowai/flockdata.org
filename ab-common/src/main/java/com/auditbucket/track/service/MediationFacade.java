package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
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
    Tag createTag(Company company, TagInputBean tagInput) throws DatagioException, ExecutionException, InterruptedException;

    Collection<Tag> createTags(Company company, List<TagInputBean> tagInputs) throws DatagioException, ExecutionException, InterruptedException;

    Future<Collection<TrackResultBean>> trackHeadersAsync(Fortress fortress, List<EntityInputBean> inputBeans) throws DatagioException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackHeaders(Fortress fortress, List<EntityInputBean> inputBeans, int listSize) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackHeader(Fortress fortress, EntityInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackLog(Company company, ContentInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException;

    Long reindex(Company company, String fortressName) throws DatagioException;

    void reindexByDocType(Company company, String fortressName, String docType) throws DatagioException;

    EntitySummaryBean getEntitySummary(Company company, String metaKey) throws DatagioException;

    EsSearchResult search(Company company, QueryParams queryParams);

    void purge(String fortressName, String apiKey) throws DatagioException;

    void cancelLastLog(Company company, Entity entity) throws IOException, DatagioException;

    Collection<TrackResultBean> trackHeaders(Company company, List<EntityInputBean> entityInputBeans) throws InterruptedException, ExecutionException, DatagioException, IOException;
}
