/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;

import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.EntitySummaryBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.service.*;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Non transactional coordinator for mediation services
 * <p/>
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
@Service
public class MediationFacadeNeo4j implements MediationFacade {
    @Autowired
    TrackService trackService;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SearchServiceFacade searchService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    TagService tagService;

    @Autowired
    LogService logService;

    private Logger logger = LoggerFactory.getLogger(MediationFacadeNeo4j.class);

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    EngineConfig engineConfig;


    static DecimalFormat f = new DecimalFormat();

    @Override
    public Tag createTag(Company company, TagInputBean tagInput) throws DatagioException, ExecutionException, InterruptedException {
        List<TagInputBean> tags = new ArrayList<>();
        tags.add(tagInput);
        return createTags(company, tags).iterator().next();

    }

    @Override
    public Collection<Tag> createTags(Company company, List<TagInputBean> tagInputs) throws DatagioException, ExecutionException, InterruptedException {
        Collection<String> existing = tagService.getExistingIndexes();
        schemaService.ensureUniqueIndexes(company, tagInputs,existing );
        try {
            tagService.createTags(company, tagInputs);
        } catch (IOException e) {
            // Todo - how to handle??
            throw new DatagioException("Error processing your batch. Please run it again");
        }
        return tagService.makeTags(company, tagInputs).get();

    }

    /**
     * Process the Entity input for a company asynchronously
     *
     * @param fortress   system
     * @param inputBeans data
     * @return ResultBeans populated with great data
     * @throws com.auditbucket.helper.DatagioException
     */
    @Override
    @Async
    public Future<Collection<TrackResultBean>> trackHeadersAsync(final Fortress fortress, List<EntityInputBean> inputBeans) throws DatagioException, IOException, ExecutionException, InterruptedException {
        return new AsyncResult<>(trackHeaders(fortress, inputBeans, 10));
    }

    /**
     * Tracks EntityInput by fortress. Each Entity can have a different fortress value.
     * Note that the fortress is located by Name
     *
     * @param company               who owns the fortress
     * @param entityInputBeans      data to track
     * @return                      results
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws DatagioException
     * @throws IOException
     */
    @Override
    public Collection<TrackResultBean> trackHeaders(Company company, List<EntityInputBean> entityInputBeans) throws InterruptedException, ExecutionException, DatagioException, IOException {
        Map<String, List<EntityInputBean>>fortressInput = new HashMap<>();
        for (EntityInputBean entityInputBean : entityInputBeans) {
            String fortressName = entityInputBean.getFortress();
            List<EntityInputBean> input = fortressInput.get(fortressName);
            if (input == null){
                input = new ArrayList<>();
                fortressInput.put(fortressName, input);
                FortressInputBean fib = new FortressInputBean(fortressName);
                fib.setTimeZone(entityInputBean.getTimezone());
                fortressService.registerFortress(company, fib, true);
            }
            input.add(entityInputBean);
        }
        Collection<TrackResultBean> results = new ArrayList<>();
        for (String fortressName : fortressInput.keySet()) {
            Fortress f = fortressService.findByName(company,fortressName);
            results.addAll(trackHeaders(f, fortressInput.get(fortressName),10));
        }
        return results;

    }


    @Override
    public Collection<TrackResultBean> trackHeaders(final Fortress fortress, final List<EntityInputBean> inputBeans, int listSize) throws DatagioException, IOException, ExecutionException, InterruptedException {
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.debug("Starting Batch [{}] - size [{}]", id, inputBeans.size());

        // This happens before we create headers to minimize IO on the graph
        schemaService.createDocTypes(inputBeans, fortress);

        // Tune to balance against concurrency and batch transaction insert efficiency.
        List<List<EntityInputBean>> splitList = Lists.partition(inputBeans, listSize);
        Collection<TrackResultBean> results = new ArrayList<>();
        for (List<EntityInputBean> entityInputBeans : splitList) {
            Iterable<TrackResultBean> trackResultBeans = (
                    // ToDo: this should dispatch the batch to a message queue
                    trackBatch(fortress, entityInputBeans)
            );

            for (TrackResultBean resultBean : trackResultBeans) {
                results.add(resultBean);
            }
        }

        watch.stop();
        logger.debug("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));
        return results;
    }

    private Iterable<TrackResultBean> trackBatch(final Fortress fortress, List<EntityInputBean> entityInputBeans) throws InterruptedException, ExecutionException, DatagioException, IOException {
        @Deprecated
        class DLCommand implements Command {
            Iterable<EntityInputBean> headers = null;
            Iterable<TrackResultBean> resultBeans;

            DLCommand(List<EntityInputBean> processList) {
                this.headers = new CopyOnWriteArrayList<>(processList);
            }

            @Override
            public Command execute() throws DatagioException, IOException, ExecutionException, InterruptedException {
                // ToDo: DAT-169 This needs to be dealt with via SpringIntegration and persistent messaging
                //       weirdly, the integration is with ab-engine
                // DLCommand and DeadLockRetry need to be removed
                // this routine is prone to deadlocks under load

                resultBeans = trackService.trackEntities(fortress, headers);
                resultBeans = logService.processLogsSync(fortress.getCompany(), resultBeans);

                return this;
            }
        }
        DLCommand dlc = new DLCommand(entityInputBeans);
        DeadlockRetry.execute(dlc, "creating headers", 50);
        return dlc.resultBeans;

    }

    @Override
    public TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        Fortress fortress = fortressService.findByName(company, inputBean.getFortress());
        if (fortress == null)
            fortress = fortressService.registerFortress(company,
                    new FortressInputBean(inputBean.getFortress(), false)
                            .setTimeZone(inputBean.getTimezone()));
        fortress.setCompany(company);
        return trackHeader(fortress, inputBean);
    }


    /**
     * tracks a header and creates logs. Distributes changes to KV stores and search engine.
     * <p/>
     * This is synchronous and blocks until completed
     *
     * @param fortress  - system that owns the data
     * @param inputBean - input
     * @return non-null
     * @throws DatagioException illegal input
     * @throws IOException      json processing exception
     */
    @Override
    public TrackResultBean trackHeader(final Fortress fortress, final EntityInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        List<EntityInputBean> inputs = new ArrayList<>(1);
        inputs.add(inputBean);
        Collection<TrackResultBean> results = trackHeaders(fortress, inputs, 1);
        return results.iterator().next();
    }


    @Override
    @Transactional
    public TrackResultBean trackLog(Company company, ContentInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException {
        Entity entity;
        if (input.getMetaKey() != null)
            entity = trackService.getEntity(company, input.getMetaKey());
        else
            entity = trackService.findByCallerRef(input.getFortress(), input.getDocumentType(), input.getCallerRef());
        if (entity == null)
            throw new DatagioException("Unable to resolve the Entity");
        return logService.writeLog(entity, input);
    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
     */
    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public Long reindex(Company company, String fortressName) throws DatagioException {
        Fortress fortress = fortressService.findByCode(company, fortressName);
        Future<Long> result = reindexAsnc(fortress);

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unexpected", e);
            return -1l;
        }


    }

    @Async
    public Future<Long> reindexAsnc(Fortress fortress) throws DatagioException {
        if (fortress == null)
            throw new DatagioException("No fortress to reindex was supplied");
        Long skipCount = 0l;
        long result = reindex(fortress, skipCount);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortress.getName() + "]");
        return new AsyncResult<>(result);
    }

    private long reindex(Fortress fortress, Long skipCount) {
        Collection<Entity> headers = trackService.getEntities(fortress, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexEntities(fortress.getCompany(), headers, skipCount);
        return reindex(fortress, skipCount);
    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document type
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
     */
    @Override
    @Async
    @Secured({"ROLE_AB_ADMIN"})
    public void reindexByDocType(Company company, String fortressName, String docType) throws DatagioException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortressName + "] could not be found");
        Long skipCount = 0l;
        long result = reindexByDocType(skipCount, fortress, docType);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortressName + "] and document type [" + docType + "]");
    }

    private long reindexByDocType(Long skipCount, Fortress fortress, String docType) {

        Collection<Entity> entities = trackService.getEntities(fortress, docType, skipCount);
        if (entities.isEmpty())
            return skipCount;
        skipCount = reindexEntities(fortress.getCompany(), entities, skipCount);
        return reindexByDocType(skipCount, fortress, docType);

    }

    private Long reindexEntities(Company company, Collection<Entity> entities, Long skipCount) {
        Collection<SearchChange> searchDocuments = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            EntityLog lastLog = trackService.getLastEntityLog(entity.getId());
            searchDocuments.add(searchService.rebuild(company, entity, lastLog));
            skipCount++;
        }
        searchService.makeChangesSearchable(searchDocuments);
        return skipCount;
    }

    @Override
    public EntitySummaryBean getEntitySummary(Company company, String metaKey) throws DatagioException {
        return trackService.getEntitySummary(company, metaKey);
    }

    @Override
    public EsSearchResult search(Company company, QueryParams queryParams) {
        StopWatch watch = new StopWatch(queryParams.toString());
        watch.start("Get ES Query Results");
        queryParams.setCompany(company.getName());
        EsSearchResult esSearchResult = searchService.search(queryParams);
        watch.stop();
        logger.info(watch.prettyPrint());

        return esSearchResult;
    }

    @Autowired
    KvService kvService;

    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public void purge(String fortressName, String apiKey) throws DatagioException {
        if (fortressName == null)
            throw new DatagioException("Illegal value for fortress name");
        SystemUser su = registrationService.getSystemUser(apiKey);
        if (su == null || su.getCompany() == null)
            throw new SecurityException("Unable to verify that the caller can work with the requested fortress");
        Fortress fortress = fortressService.findByName(su.getCompany(), fortressName);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressName + "] does not exist");
        purge(fortress, su);
    }

    private void purge(Fortress fortress, SystemUser su) throws DatagioException {
        logger.info("Purging fortress [{}] on behalf of [{}]", fortress, su.getLogin());

        String indexName = "ab." + fortress.getCompany().getCode() + "." + fortress.getCode();

        trackService.purge(fortress);
        kvService.purge(indexName);
        fortressService.purge(fortress);
        engineConfig.resetCache();
        searchService.purge(indexName);

    }

    @Override
    @Transactional
    public void cancelLastLog(Company company, Entity entity) throws IOException, DatagioException {
        EntitySearchChange searchChange;
        // Refresh the entity
        entity = trackService.getEntity(entity);
        searchChange = trackService.cancelLastLog(company, entity);
        if (searchChange != null) {
            searchService.makeChangeSearchable(searchChange);
        } else {
            logger.info("ToDo: Delete the search document {}", entity);
        }
    }



}
