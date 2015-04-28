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
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.query.service.SearchServiceFacade;
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
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.service.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Non transactional coordinator for mediation services
 * <p>
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
@Service
@Qualifier("mediationFacadeNeo4j")
public class MediationFacadeNeo4j implements MediationFacade {

    // Default behaviour when creating a new fortress
    private static final boolean IGNORE_SEARCH_ENGINE = false;
    @Autowired
    EntityService entityService;

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
    PlatformConfig engineConfig;

    @Autowired
    EntityRetryService entityRetry;

    @Autowired
    ConceptRetryService conceptRetryService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    TagRetryService tagRetryService;

    @Autowired
    EngineAdminService adminService;

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

        if (tagInputs.isEmpty())
            return null;

        return tagRetryService.createTagsFuture(company, tagInputs).get();
    }

    /**
     * Writes the payload to the service. Distributes changes to KV stores and search engine.
     * <p>
     * This is synchronous and blocks until completed. Usually called via integration mechanisms
     *
     * @param inputBeans - input to track
     * @param apiKey     - SystemUser API key
     * @throws FlockException        illegal input
     * @throws FlockServiceException api key is invalid or mandatory tags do not exist
     * @throws IOException           json processing exception
     */

    @ServiceActivator(inputChannel = "doTrackEntity", adviceChain = {"fde.retry"})
    public Collection<TrackResultBean> trackEntities(Collection<EntityInputBean> inputBeans, @Header(value = "apiKey") String apiKey) throws FlockException, IOException, ExecutionException, InterruptedException {
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new AmqpRejectAndDontRequeueException("Unable to resolve the company for your ApiKey");
        Map<Fortress, List<EntityInputBean>> byFortress = TrackBatchSplitter.getEntitiesByFortress(fortressService, c, inputBeans);
        Collection<TrackResultBean>results = new ArrayList<>();
        for (Fortress fortress : byFortress.keySet()) {
            results.addAll(trackEntities(fortress, byFortress.get(fortress), 100));
        }
        return results;
    }

    /**
     * tracks an entity and creates logs. Distributes changes to KV stores and search engine.
     * <p>
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

    @Autowired
    TrackGateway trackGateway;

    @Override
    @Secured({SecurityHelper.ADMIN})
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
        EntityLog log = entityService.getLogForEntity(entity, logId);
        if (log != null)
            return kvService.getContent(entity, log.getLog()).getWhat();

        return new HashMap<>();
    }

//    private Map<Fortress, List<EntityInputBean>> getEntitiesByFortress(Company company, Collection<EntityInputBean> entityInputBeans) throws NotFoundException {
//        Map<Fortress, List<EntityInputBean>> fortressInput = new HashMap<>();
//
//        // Local cache of fortress by name - never very big, often only 1
//        Map<String, Fortress> resolvedFortress = new HashMap<>();
//        for (EntityInputBean entityInputBean : entityInputBeans) {
//            String fortressName = entityInputBean.getFortress();
//            Fortress f = resolvedFortress.get(fortressName);
//            if (f == null) {
//                f = fortressService.findByCode(company, fortressName);
//
//            }
//            if (f != null)
//                resolvedFortress.put(fortressName, f);
//
//            List<EntityInputBean> input = null;
//            if (f != null)
//                input = fortressInput.get(f);// are we caching this already?
//
//            if (input == null) {
//                input = new ArrayList<>();
//
//                FortressInputBean fib = new FortressInputBean(fortressName);
//                fib.setTimeZone(entityInputBean.getTimezone());
//                Fortress fortress = fortressService.registerFortress(company, fib, true);
//                resolvedFortress.put(fortressName, f);
//                fortressInput.put(fortress, input);
//            }
//            input.add(entityInputBean);
//        }
//        return fortressInput;
//    }


    @Override
    public Collection<TrackResultBean> trackEntities(final Fortress fortress, final List<EntityInputBean> inputBeans, int splitListInTo) throws FlockException, IOException, ExecutionException, InterruptedException {
        String id = Thread.currentThread().getName() + "/" + DateTime.now().getMillis();
        if (fortress == null) {
            throw new FlockException("No fortress supplied. Unable to process work without a valid fortress");
        }
        logger.debug("About to create tags");
        //Future<Collection<Tag>> tags = tagRetryService.createTagsFuture(fortress.getCompany(), getTags(inputBeans));
        Future<Collection<Tag>> tags = tagRetryService.createTagsFuture(fortress.getCompany(), getTags(inputBeans));

        logger.debug("About to create docTypes");
        EntityInputBean first = inputBeans.iterator().next();
        Future<DocumentType> docType = schemaRetryService.createDocTypes(fortress, first);

        logger.debug("Dispatched request to create tags");
        // Tune to balance against concurrency and batch transaction insert efficiency.
        Collection<TrackResultBean> allResults = new ArrayList<>();
        // We have to wait for the docType before proceeding to create entities
        try {
            // A long time, but this is to avoid test issues on the low spec build box
            docType.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("Time out looking/creating docType " + first.getDocumentName());
            throw new FlockException("Time out looking/creating docType " + first.getDocumentName());
        }

        List<List<EntityInputBean>>
                splitList = Lists.partition(inputBeans, splitListInTo);

        StopWatch watch = new StopWatch();
        watch.start();
        logger.trace("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        for (List<EntityInputBean> entityInputBeans : splitList) {
            Iterable<TrackResultBean> loopResults = entityRetry.track(fortress, entityInputBeans, (tags!=null?tags.get(): null));
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
            for (TagInputBean tag : entityInputBean.getTags()) {
                if ( !tags.contains(tag))
                    tags.add(tag);
            }
            ///entityInputBean.getTags().stream().filter(tag -> !tag.isMustExist() && !tags.contains(tag)).forEach(tags::add);

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
            entity = entityService.getEntity(company, input.getMetaKey());
        else
            entity = entityService.findByCallerRef(company, input.getFortress(), input.getDocumentType(), input.getCallerRef());
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
    @Secured({SecurityHelper.ADMIN})
    public String reindex(Company company, String fortressCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", fortressCode));

        if (!fortress.isSearchActive())
            throw new FlockException("The fortress does not have search enabled. Nothing to do!");

        String message = null;
        if (fortress.isStoreDisabled()) {
            message = String.format("The store has been disabled for the fortress %s. Only information that has been recorded in a KV store can be re-indexed", fortressCode);
            logger.warn(message);
        }
        if (message != null) {
            message = message + "\n";
        }
        adminService.doReindex(fortress);
        message = message + "Reindex Search request is processing entities for [" + fortressCode + "]";
        logger.info("Reindex Search request is processing entities for [" + fortressCode + "]");
        return message;


    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document label
     *
     * @param fortressName name of the fortress to rebuild
     * @throws org.flockdata.helper.FlockException
     */
    @Override
    @Secured({SecurityHelper.ADMIN})
    public String reindexByDocType(Company company, String fortressName, String docType) throws FlockException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new FlockException("Fortress [" + fortressName + "] could not be found");
        Long skipCount = 0l;
        String message = null;
        if (fortress.isStoreDisabled()) {
            message = String.format("The store has been disabled for the fortress %s. Only information that has been recorded in a KV store can be re-indexed", fortress);
            logger.warn(message);
        }
        adminService.doReindex(fortress, docType);
        if (message != null) {
            message = message + "\n";
        }
        message = message + "Reindex Search request is processing entities for [" + fortressName + "] and document type [" + docType + "]";
        logger.info("Reindex Search request is processing entities for [" + fortressName + "] and document type [" + docType + "]");
        return message;
    }

    @Override
    public EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException {
        return entityService.getEntitySummary(company, metaKey);
    }


    @Override
    @Secured({SecurityHelper.ADMIN})
    public void purge(Company company, String fortressCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressCode + "] does not exist");

        logger.info("Purging fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
        adminService.purge(company, fortress);
    }

    @Override
    @Secured(SecurityHelper.ADMIN)
    public void purge(Fortress fortress) throws FlockException {
        adminService.purge(fortress.getCompany(), fortress);
    }

    @Override
    @Transactional
    public void cancelLastLog(Company company, Entity entity) throws IOException, FlockException {
        EntitySearchChange searchChange;
        // Refresh the entity
        //entity = entityService.getEntity(entity);
        searchChange = entityService.cancelLastLog(company, entity);
        if (searchChange != null) {
            searchService.makeChangeSearchable(searchChange);
        } else {
            logger.info("ToDo: Delete the search document {}", entity);
        }
    }

    public void distributeChanges(final Fortress fortress, final Iterable<TrackResultBean> resultBeans) throws IOException, InterruptedException, ExecutionException, FlockException {

        logger.debug("Distributing changes to sub-services");
        searchService.makeChangesSearchable(fortress, resultBeans);
        // ToDo: how to wait for results when running tests
        if (engineConfig.isTestMode())
            conceptRetryService.trackConcepts(fortress, resultBeans).get();
        else
            conceptRetryService.trackConcepts(fortress, resultBeans);
        logger.debug("Distributed changes");
    }


}
