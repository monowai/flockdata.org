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

import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.service.*;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.EntitySummaryBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;
import com.auditbucket.track.model.SearchChange;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Non transactional coordinator for mediation services
 * <p/>
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
@Service
@Configuration
//@EnableRetry
@Qualifier("mediationFacadeNeo4j")
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

    @Autowired
    EngineConfig engineConfig;

    @Autowired
    EntityRetryService entityRetry;

    @Autowired
    SecurityHelper securityHelper;

    private Logger logger = LoggerFactory.getLogger(MediationFacadeNeo4j.class);

    static DecimalFormat f = new DecimalFormat();

    @Override
    public Tag createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException {
        List<TagInputBean> tags = new ArrayList<>();
        tags.add(tagInput);
        return createTags(company, tags).iterator().next();

    }

    @Override
    public Collection<Tag> createTags(Company company, List<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {
        Collection<String> existing = tagService.getExistingIndexes();
        schemaService.ensureUniqueIndexes(company, tagInputs, existing);
        try {
            tagService.createTags(company, tagInputs);
        } catch (IOException e) {
            // Todo - how to handle??
            throw new FlockException("Error processing your batch. Please run it again");
        }
        return tagService.makeTags(company, tagInputs).get();

    }

    /**
     * tracks an entity and creates logs. Distributes changes to KV stores and search engine.
     * <p/>
     * This is synchronous and blocks until completed
     *
     * @param fortress  - system that owns the data
     * @param inputBean - input
     * @return non-null
     * @throws com.auditbucket.helper.FlockException illegal input
     * @throws IOException                           json processing exception
     */
    @Override
    public TrackResultBean trackEntity(final Fortress fortress, final EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        List<EntityInputBean> inputs = new ArrayList<>(1);
        inputs.add(inputBean);
        Collection<TrackResultBean> results = trackEntities(fortress, inputs, 1);
        return results.iterator().next();
    }

    /**
     * Process the Entity input for a company asynchronously
     *
     * @param company    system
     * @param inputBeans data
     * @return ResultBeans populated with great data
     * @throws com.auditbucket.helper.FlockException
     */
    @Override
    @Async
    public Future<Collection<TrackResultBean>> trackEntitiesAsync(final Company company, List<EntityInputBean> inputBeans) throws FlockException, IOException, ExecutionException, InterruptedException {
        // ToDo:
        // This is a promise. It should be called after the batch is persisted safely

        // ToDo: This should be a batch task
        Map<Fortress, List<EntityInputBean>> fortressInput = getEntitiesByFortress(company, inputBeans);
        Collection<TrackResultBean> results = new ArrayList<>();

        for (Fortress fortress : fortressInput.keySet()) {
            results.addAll(
                    trackEntities(fortress, fortressInput.get(fortress), 10)
            );
        }

        return new AsyncResult<>(results);
    }

    /**
     * Tracks EntityInput by fortress. Each Entity can have a different fortress value.
     * Note that the fortress is located by Name
     *
     * @param company          who owns the fortress
     * @param entityInputBeans data to track
     * @return results
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws com.auditbucket.helper.FlockException
     * @throws IOException
     */
    @Override
    public Collection<TrackResultBean> trackEntities(Company company, List<EntityInputBean> entityInputBeans) throws InterruptedException, ExecutionException, FlockException, IOException {
        Map<Fortress, List<EntityInputBean>> fortressInput = getEntitiesByFortress(company, entityInputBeans);
        Collection<TrackResultBean> results = new ArrayList<>();
        for (Fortress fortress : fortressInput.keySet()) {
            results.addAll(trackEntities(fortress, fortressInput.get(fortress), 10));
        }
        return results;

    }

    private Map<Fortress, List<EntityInputBean>> getEntitiesByFortress(Company company, List<EntityInputBean> entityInputBeans) throws NotFoundException {
        Map<Fortress, List<EntityInputBean>> fortressInput = new HashMap<>();

        // Local cache of fortress by name - never very big, often only 1
        Map<String, Fortress> resolvedFortress = new HashMap<>();
        for (EntityInputBean entityInputBean : entityInputBeans) {
            String fortressName = entityInputBean.getFortress();
            Fortress f = resolvedFortress.get(fortressName);
            if (f == null) {
                f = fortressService.findByName(company, fortressName);
                if (f != null)
                    resolvedFortress.put(fortressName, f);
            }

            List<EntityInputBean> input = null;
            if (f != null)
                input = fortressInput.get(f);// are we caching this already?

            if (input == null) {
                input = new ArrayList<>();

                FortressInputBean fib = new FortressInputBean(fortressName);
                fib.setTimeZone(entityInputBean.getTimezone());
                Fortress fortress = fortressService.registerFortress(company, fib, true);
                fortressInput.put(fortress, input);
            }
            input.add(entityInputBean);
        }
        return fortressInput;
    }


    @Override
    public Collection<TrackResultBean> trackEntities(final Fortress fortress, final List<EntityInputBean> inputBeans, int listSize) throws FlockException, IOException, ExecutionException, InterruptedException {
        Long id = DateTime.now().getMillis();

        // This happens before we create entities to minimize IO on the graph
        schemaService.createDocTypes(inputBeans, fortress);

        // Tune to balance against concurrency and batch transaction insert efficiency.
        List<List<EntityInputBean>> splitList = Lists.partition(inputBeans, listSize);
        Collection<TrackResultBean> allResults = new ArrayList<>();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.trace("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        for (List<EntityInputBean> entityInputBeans : splitList) {
            Iterable<TrackResultBean> theseResults = (
                    entityRetry.track(fortress, entityInputBeans));

            for (TrackResultBean theResult : theseResults) {
                allResults.add(theResult);
            }
        }
        watch.stop();
        logger.debug("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));

        return allResults;
    }

    @Override
    public TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        Fortress fortress = fortressService.findByName(company, inputBean.getFortress());
        if (fortress == null)
            fortress = fortressService.registerFortress(company,
                    new FortressInputBean(inputBean.getFortress(), false)
                            .setTimeZone(inputBean.getTimezone()));
        fortress.setCompany(company);
        return trackEntity(fortress, inputBean);
    }


    @Override
    @Transactional
    public TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {
        Entity entity;
        if (input.getMetaKey() != null)
            entity = trackService.getEntity(company, input.getMetaKey());
        else
            entity = trackService.findByCallerRef(company, input.getFortress(), input.getDocumentType(), input.getCallerRef());
        if (entity == null)
            throw new FlockException("Unable to resolve the Entity");
        return logService.writeLog(entity, input);
    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.FlockException
     */
    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public Future<Long> reindex(Company company, String fortressName) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressName);
        if (fortress == null)
            throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", fortressName));
        return reindexAsnc(fortress);

    }

    @Async
    public Future<Long> reindexAsnc(Fortress fortress) throws FlockException {
        Long skipCount = 0l;
        long result = reindex(fortress, skipCount);
        logger.info("Reindex Search request completed. Processed [" + result + "] entities for [" + fortress.getName() + "]");
        return new AsyncResult<>(result);
    }

    private long reindex(Fortress fortress, Long skipCount) {
        Collection<Entity> entities = trackService.getEntities(fortress, skipCount);
        if (entities.isEmpty())
            return skipCount;
        skipCount = reindexEntities(fortress.getCompany(), entities, skipCount);
        return reindex(fortress, skipCount);
    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document type
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.FlockException
     */
    @Override
    @Async
    @Secured({"ROLE_AB_ADMIN"})
    public void reindexByDocType(Company company, String fortressName, String docType) throws FlockException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new FlockException("Fortress [" + fortressName + "] could not be found");
        Long skipCount = 0l;
        long result = reindexByDocType(skipCount, fortress, docType);
        logger.info("Reindex Search request completed. Processed [" + result + "] entities for [" + fortressName + "] and document type [" + docType + "]");
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
    public EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException {
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
    public void purge(Company company, String fortressName) throws FlockException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressName + "] does not exist");

        logger.info("Purging fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
        purge(company, fortress);
    }

    @Async
    public Future<Boolean> purge(Company company, Fortress fortress) throws FlockException {

        String indexName = "ab." + company.getCode() + "." + fortress.getCode();
        trackService.purge(fortress);
        kvService.purge(indexName);
        fortressService.purge(fortress);
        engineConfig.resetCache();
        searchService.purge(indexName);
        return new AsyncResult<>(true);

    }

    @Override
    @Transactional
    public void cancelLastLog(Company company, Entity entity) throws IOException, FlockException {
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
