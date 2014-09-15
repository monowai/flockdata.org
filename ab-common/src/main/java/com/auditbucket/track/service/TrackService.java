package com.auditbucket.track.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.EntitySummaryBean;
import com.auditbucket.track.bean.LogDetailBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.*;
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
    LogWhat getWhat(Entity entity, Log change);

    @Deprecated
    Entity getEntity(@NotEmpty String metaKey);

    Entity getEntity(Company company, String metaKey);

    Entity getEntity(Company company, @NotEmpty String headerKey, boolean inflate);

    Entity getEntity(Entity entity);

    Collection<Entity> getEntities(Fortress fortress, Long skipTo);

    Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo);

    void updateHeader(Entity entity);

    TrackLog getLastLog(String metaKey) throws DatagioException;

    TrackLog getLastLog(Long headerId);

    Set<TrackLog> getLogs(Long headerId);

    Set<TrackLog> getLogs(Company company, String headerKey) throws DatagioException;

    Set<TrackLog> getLogs(String headerKey, Date from, Date to) throws DatagioException;


    EntitySearchChange cancelLastLog(Company company, Entity entity) throws IOException, DatagioException;

    int getLogCount(Company company, String headerKey) throws DatagioException;

    Entity findByCallerRef(String fortress, String documentType, String callerRef);

    Entity findByCallerRefFull(Long fortressId, String documentType, String callerRef);

    Entity findByCallerRefFull(Fortress fortress, String documentType, String callerRef);

    Iterable<Entity> findByCallerRef(Company company, String fortressName, String callerRef);

    Collection<Entity> findByCallerRef(Fortress fortress, String callerRef);

    Entity findByCallerRef(Fortress fortress, String documentType, String callerRef);

    Entity findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef);

    EntitySummaryBean getEntitySummary(Company company, String metaKey) throws DatagioException;

    LogDetailBean getFullDetail(String metaKey, Long logId);

    LogDetailBean getFullDetail(Company company, String metaKey, Long logId);

    TrackLog getLogForEntity(Entity header, Long logId);

    Iterable<TrackResultBean> trackEntities(Fortress fortress, Iterable<EntityInputBean> inputBeans) throws InterruptedException, ExecutionException, DatagioException, IOException;

    Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws DatagioException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String metaKey, String xRefName) throws DatagioException;

    Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws DatagioException;

    List<EntityKey> crossReferenceByCallerRef(Company company, EntityKey sourceKey, Collection<EntityKey> targetKeys, String xRefName) throws DatagioException;

    Map<String, Entity> getEntities(Company company, Collection<String> metaKeys);

    void purge(Fortress fortress);

    void recordSearchResult(SearchResult searchResult, Long metaId);

    Set<TrackTag> getLastLogTags(Company company, String metaKey) throws DatagioException;

    TrackLog getLastLog(Company company, String metaKey) throws DatagioException;

    TrackLog getLog(Company company, String metaKey, long logId) throws DatagioException;

    Set<TrackTag> getLogTags(Company company, TrackLog tl);


}
