package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.bean.TrackedSummaryBean;

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

    Future<Collection<TrackResultBean>> trackHeadersAsync(Fortress fortress, List<MetaInputBean> inputBeans) throws DatagioException, IOException, ExecutionException, InterruptedException;

    Collection<TrackResultBean> trackHeaders(Fortress fortress, List<MetaInputBean> inputBeans, int listSize) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackHeader(Company company, MetaInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean trackHeader(Fortress fortress, MetaInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException;

    TrackResultBean processLog(Company company, LogInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException;

    Long reindex(Company company, String fortressName) throws DatagioException;

    void reindexByDocType(Company company, String fortressName, String docType) throws DatagioException;

    TrackedSummaryBean getTrackedSummary(Company company, String metaKey) throws DatagioException;

    EsSearchResult search(Company company, QueryParams queryParams);

    void purge(String fortressName, String apiKey) throws DatagioException;

    void cancelLastLog(Company company, String metaKey) throws IOException, DatagioException;
}
