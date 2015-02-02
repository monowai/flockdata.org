/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.engine.track.service;

import com.google.common.collect.Lists;
import org.flockdata.engine.FdEngineConfig;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.schema.service.IndexRetryService;
import org.flockdata.engine.schema.service.SchemaRetryService;
import org.flockdata.engine.tag.service.TagRetryService;
import org.flockdata.engine.track.endpoint.TrackGateway;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.FlockServiceException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.Tag;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.SearchChange;
import org.flockdata.track.service.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
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
@Qualifier("mediationFacadeNeo4j")
public class MediationFacadeNeo4j implements MediationFacade {

    // Default behaviour when creating a new fortress
    private static final boolean IGNORE_SEARCH_ENGINE = false;
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
    SchemaRetryService schemaRetryService;

    @Autowired
    TagService tagService;

    @Autowired
    LogService logService;

    @Autowired
    FdEngineConfig engineConfig;

    @Autowired
    EntityRetryService entityRetry;

    @Autowired
    ConceptRetryService conceptRetryService;


    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    TagRetryService tagRetryService;

    @Autowired
    IndexRetryService indexRetryService;

    @Autowired
    KvService kvService;

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
        indexRetryService.ensureUniqueIndexes(company, tagInputs);
        Collection<Tag> results;
        try {
            results = tagRetryService.createTags(company, tagInputs);
        } catch (IOException e) {
            logger.error("Unexpected", e);
            throw new FlockException("IO Exception", e);
        }
        return results;
    }


    /**
     * tracks an entity and creates logs. Distributes changes to KV stores and search engine.
     * <p/>
     * This is synchronous and blocks until completed. Designed to be accessed as a ServiceActivtory via integration mechanisms
     * APIKey should be set in the EntityInputBean as it will be used resolve access to the Company and Fortress in the payload
     *
     * @param inputBean - input to track
     * @return non-null
     * @throws org.flockdata.helper.FlockException illegal input
     * @throws IOException                         json processing exception
     */
    @ServiceActivator(inputChannel = "doTrackEntity", adviceChain = {"fde.retry"})
    public TrackResultBean trackEntity(EntityInputBean inputBean, @Header(value = "apiKey") String apiKey) throws FlockException, IOException, ExecutionException, InterruptedException {
        // ToDo: A collection??
        logger.debug("trackEntity activation");
        //EntityInputBean inputBean = JsonUtils.getBytesAsObject(payload, EntityInputBean.class);
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new FlockServiceException("Unable to resolve the company for your ApiKey");
        Fortress fortress = fortressService.registerFortress(c, new FortressInputBean(inputBean.getFortress()), true);
        assert fortress != null;
        return trackEntity(fortress, inputBean);
    }

    /**
     * tracks an entity and creates logs. Distributes changes to KV stores and search engine.
     * <p/>
     * This is synchronous and blocks until completed
     *
     * @param fortress  - system that owns the data
     * @param inputBean - input
     * @return non-null
     * @throws org.flockdata.helper.FlockException illegal input
     * @throws IOException                         json processing exception
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
     * Result set should only be relied on for testing purposes
     *
     * @param company    system
     * @param inputBeans data
     * @return ResultBeans populated with great data
     * @throws org.flockdata.helper.FlockException
     */
    @Override
    @Async("fd-track")
    public Future<Collection<TrackResultBean>> trackEntitiesAsync(final Company company, List<EntityInputBean> inputBeans) throws FlockException, IOException, ExecutionException, InterruptedException {
        Map<Fortress, List<EntityInputBean>> fortressInput = getEntitiesByFortress(company, inputBeans);
        Collection<TrackResultBean> results = new ArrayList<>();

        for (Fortress fortress : fortressInput.keySet()) {
            results.addAll(
                    trackEntities(fortress, fortressInput.get(fortress), 10)
            );
        }

        return new AsyncResult<>(results);
    }

    @Autowired
    TrackGateway trackGateway;

    @Override
//    @Transactional
    public void trackEntities(String userApiKey, List<EntityInputBean> inputBeans) {
        logger.debug("Request to process {} entities", inputBeans.size());
        for (EntityInputBean inputBean : inputBeans) {
            Future<?> r = trackGateway.doTrackEntity(inputBean, userApiKey);

        }
        logger.debug("Dispatched {} entities", inputBeans.size());
    }

    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public void mergeTags(Company company, Tag source, Tag target) {
        // ToDo: Transactional?
        // Update the search docs for the affected entities
        Collection<Long> entities = entityTagService.mergeTags(source, target);
        searchService.refresh(company, entities);

    }

    @Override
    public void createAlias(Company company, String label, Tag tag, String akaValue) {
        tagService.createAlias(company, tag, label, akaValue);
    }

    @Override
    public Map<String, Object> getLogContent(Entity entity, Long logId) {
        EntityLog log = trackService.getLogForEntity(entity, logId);
        if (log != null)
            return kvService.getContent(entity, log.getLog()).getWhat();

        return new HashMap<>();
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
    public Collection<TrackResultBean> trackEntities(final Fortress fortress, final List<EntityInputBean> inputBeans, int splitListInTo) throws FlockException, IOException, ExecutionException, InterruptedException {
        String id = Thread.currentThread().getName() + "/"+ DateTime.now().getMillis();
        if (fortress == null) {
            throw new FlockException("No fortress supplied. Unable to process work without a valid fortress");
        }

        schemaRetryService.createDocTypes(fortress, inputBeans.iterator().next());

        createTags(fortress.getCompany(), getTags(inputBeans));
        // Tune to balance against concurrency and batch transaction insert efficiency.
        List<List<EntityInputBean>> splitList = Lists.partition(inputBeans, splitListInTo);
        Collection<TrackResultBean> allResults = new ArrayList<>();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.trace("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        for (List<EntityInputBean> entityInputBeans : splitList) {
            Iterable<TrackResultBean> loopResults = entityRetry.track(fortress, entityInputBeans);
            logger.debug("Tracked requests");
            distributeChanges(fortress, loopResults);

            for (TrackResultBean theResult : loopResults) {
                allResults.add(theResult);
            }
        }
        watch.stop();
        logger.debug("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));

        return allResults;
    }

    private List<TagInputBean> getTags(List<EntityInputBean> entityInputBeans) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (EntityInputBean entityInputBean : entityInputBeans) {
            tags.addAll(entityInputBean.getTags());
        }
        return tags;
    }

    @Override
    public TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        Fortress fortress = fortressService.findByName(company, inputBean.getFortress());
        if (fortress == null)
            fortress = fortressService.registerFortress(company,
                    new FortressInputBean(inputBean.getFortress(), IGNORE_SEARCH_ENGINE)
                            .setTimeZone(inputBean.getTimezone()));
        fortress.setCompany(company);
        return trackEntity(fortress, inputBean);
    }

    @Override
    public TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {
        // Create the basic data within a transaction
        TrackResultBean result = doTrackLog(company, input);
        Collection<TrackResultBean> results = new ArrayList<>();
        results.add(result);
        // Finally distribute the changes
        distributeChanges(result.getEntity().getFortress(), results);
        return result;
    }

    @Transactional
    public TrackResultBean doTrackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {
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
     * @param fortressCode name of the fortress to rebuild
     * @throws org.flockdata.helper.FlockException
     */
    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public Long reindex(Company company, String fortressCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", fortressCode));

        if ( !fortress.isSearchActive())
            throw new FlockException("The fortress does not have search enabled. Nothing to do!");
        Future<Long> result = reindexAsnc(fortress);
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unexpected", e);
        }
        return null;

    }

    @Async("fd-engine")
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
     * Rebuilds all search documents for the supplied fortress of the supplied document label
     *
     * @param fortressName name of the fortress to rebuild
     * @throws org.flockdata.helper.FlockException
     */
    @Override
    @Async("fd-engine")
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
            EntitySearchChange searchDoc = searchService.rebuild(company, entity, lastLog);
            if  (searchDoc!=null)
                searchDocuments.add(searchDoc);
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
    @Secured({"ROLE_AB_ADMIN"})
    public void purge(Company company, String fortressCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressCode + "] does not exist");

        logger.info("Purging fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
        purge(company, fortress);
    }

    @Override
    @Secured("ROLE_AB_ADMIN")
    public void purge(Fortress fortress) throws FlockException {
        purge(fortress.getCompany(), fortress);
    }

    @Async("fd-engine")
    @Transactional
    public Future<Boolean> purge(Company company, Fortress fortress) throws FlockException {

        String indexName = EntitySearchSchema.PREFIX + company.getCode() + "." + fortress.getCode();
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

    public void distributeChanges(final Fortress fortress, final Iterable<TrackResultBean> resultBeans) throws IOException, InterruptedException, ExecutionException, FlockException {

        logger.debug("Distributing changes to sub-services");
        searchService.makeChangesSearchable(fortress, resultBeans);
        // ToDo: how to wait for results when running tests. I hate config properties
        if (engineConfig.isTestMode())
            conceptRetryService.trackConcepts(fortress, resultBeans).get();
        else
            conceptRetryService.trackConcepts(fortress, resultBeans);
    }


}
