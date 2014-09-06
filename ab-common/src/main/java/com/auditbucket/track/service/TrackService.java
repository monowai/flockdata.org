package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.LogDetailBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.bean.TrackedSummaryBean;
import com.auditbucket.track.model.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 5/09/14
 * Time: 4:22 PM
 */
public interface TrackService {
    LogWhat getWhat(MetaHeader metaHeader, Log change);

    MetaHeader getHeader(@NotEmpty String metaKey);

    MetaHeader getHeader(Company company, String metaKey);

    MetaHeader getHeader(Company company, @NotEmpty String headerKey, boolean inflate);

    Collection<MetaHeader> getHeaders(Fortress fortress, Long skipTo);

    Collection<MetaHeader> getHeaders(Fortress fortress, String docTypeName, Long skipTo);

    void updateHeader(MetaHeader metaHeader);

    TrackLog getLastLog(String metaKey) throws DatagioException;

    TrackLog getLastLog(Long headerId);

    Set<TrackLog> getLogs(Long headerId);

    Set<TrackLog> getLogs(Company company, String headerKey) throws DatagioException;

    Set<TrackLog> getLogs(String headerKey, Date from, Date to) throws DatagioException;

    @Async
    AsyncResult<MetaSearchChange> cancelLastLog(Company company, String headerKey) throws IOException, DatagioException;

    int getLogCount(Company company, String headerKey) throws DatagioException;

    MetaHeader findByCallerRef(String fortress, String documentType, String callerRef);

    MetaHeader findByCallerRefFull(Long fortressId, String documentType, String callerRef);

    MetaHeader findByCallerRefFull(Fortress fortress, String documentType, String callerRef);

    Iterable<MetaHeader> findByCallerRef(Company company, String fortressName, String callerRef);

    Collection<MetaHeader> findByCallerRef(Fortress fortress, String callerRef);

    MetaHeader findByCallerRef(Fortress fortress, String documentType, String callerRef);

    MetaHeader findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef);

    TrackedSummaryBean getMetaSummary(Company company, String metaKey) throws DatagioException;

    LogDetailBean getFullDetail(String metaKey, Long logId);

    LogDetailBean getFullDetail(Company company, String metaKey, Long logId);

    TrackLog getLogForHeader(MetaHeader header, Long logId);

    Iterable<TrackResultBean> trackHeaders(Fortress fortress, Iterable<MetaInputBean> inputBeans) throws InterruptedException, ExecutionException, DatagioException, IOException;

    Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws DatagioException;

    Map<String, Collection<MetaHeader>> getCrossReference(Company company, String metaKey, String xRefName) throws DatagioException;

    Map<String, Collection<MetaHeader>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws DatagioException;

    List<MetaKey> crossReferenceByCallerRef(Company company, MetaKey sourceKey, Collection<MetaKey> targetKeys, String xRefName) throws DatagioException;

    Map<String, MetaHeader> getHeaders(Company company, Collection<String> metaKeys);

    void purge(Fortress fortress);

    void saveMetaData(SearchResult searchResult, Long metaId);

    Set<TrackTag> getLastLogTags(Company company, String metaKey) throws DatagioException;

    TrackLog getLastLog(Company company, String metaKey) throws DatagioException;

    TrackLog getLog(Company company, String metaKey, long logId) throws DatagioException;

    Set<TrackTag> getLogTags(Company company, TrackLog tl);
}
