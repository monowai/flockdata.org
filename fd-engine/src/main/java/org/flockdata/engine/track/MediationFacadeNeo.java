/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track;

import com.google.common.collect.Lists;
import org.flockdata.authentication.FdRoles;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.meta.service.DocTypeRetryService;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.schema.IndexRetryService;
import org.flockdata.engine.tag.service.TagRetryService;
import org.flockdata.engine.track.service.ConceptRetryService;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.EntityRetryService;
import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.*;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.track.bean.*;
import org.flockdata.track.service.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class MediationFacadeNeo implements MediationFacade {

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

    @Autowired (required = false)
    SearchServiceFacade searchService;

    @Autowired
    DocTypeRetryService docTypeRetryService;

    @Autowired
    StorageProxy contentReader;

    @Autowired
    TagService tagService;

    @Autowired
    LogService logService;

    @Autowired
    EngineConfig engineConfig;

    @Autowired
    EntityRetryService entityRetry;

    @Autowired
    ConceptRetryService conceptRetryService;

    @Autowired
    IndexRetryService indexRetryService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    TagRetryService tagRetryService;

    @Autowired
    EngineAdminService adminService;

    @Autowired
    TrackBatchSplitter batchSplitter;

    private Logger logger = LoggerFactory.getLogger(MediationFacadeNeo.class);

    static DecimalFormat f = new DecimalFormat();

    @Override
    public Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, String apiKey) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new AmqpRejectAndDontRequeueException("Unable to resolve the company for your ApiKey");
        return trackEntities(c, inputBeans);
    }

    public Collection<TrackRequestResult> trackEntities(Company company, Collection<EntityInputBean> inputBeans) throws FlockException, InterruptedException, ExecutionException, IOException {
        Map<FortressSegment, List<EntityInputBean>> byFortress = batchSplitter.getEntitiesBySegment(company, inputBeans);
        Collection<TrackRequestResult> results = new ArrayList<>();
        for (FortressSegment segment : byFortress.keySet()) {
            Collection<TrackResultBean> tr =
                    trackEntities(segment, byFortress.get(segment), 2);
            for (TrackResultBean result : tr) {
                results.add(new TrackRequestResult(result));
            }

        }
        return results;

    }

    @Override
    public TagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException {
        List<TagInputBean> tags = new ArrayList<>();
        tags.add(tagInput);
        return createTags(company, tags).iterator().next();

    }

    @Override
    public Collection<TagResultBean> createTags(String apiKey, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {
        Company company = securityHelper.getCompany(apiKey);
        if (company == null )
            throw new RuntimeException( "Illegal company api key");
        return createTags(company, tagInputs);
    }

    @Override
    public Collection<TagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {
        Future<Collection<TagResultBean>> tags = createTagsAsync(company, tagInputs);
        if ( tags == null )
            return null;
        return tags.get();
    }

    private Future<Collection<TagResultBean>> createTagsAsync(Company company, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {

        if (tagInputs.isEmpty())
            return null;
        indexRetryService.ensureUniqueIndexes(tagInputs);
        return tagRetryService.createTags(company, tagInputs);
    }

    @Override
    public TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        Fortress fortress = fortressService.findByName(company, inputBean.getFortress().getName());
        if (fortress == null) {
            logger.debug("Creating new Fortress {}", inputBean.getFortress());
            fortress = fortressService.registerFortress(company, inputBean.getFortress());
        }
        fortress.setCompany(company);
        FortressSegment segment;
        if ( inputBean.getSegment() != null )
            segment = fortressService.addSegment(new FortressSegment(fortress, inputBean.getSegment()));
        else
            segment = fortress.getDefaultSegment();
        return trackEntity(segment, inputBean);
    }

    /**
     * tracks an entity and creates logs. Distributes changes to KV stores and search engine.
     * <p>
     * This is synchronous and blocks until completed
     *
     * @param segment  - system that owns the data
     * @param inputBean - input
     * @return non-null
     * @throws org.flockdata.helper.FlockException illegal input
     * @throws IOException                         json processing exception
     */
    @Override
    public TrackResultBean trackEntity(final FortressSegment segment, final EntityInputBean inputBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        List<EntityInputBean> inputs = new ArrayList<>(1);
        inputs.add(inputBean);
        Collection<TrackResultBean> results = trackEntities(segment, inputs, 1);
        return results.iterator().next();
    }

    @Override
    public TrackResultBean trackEntity(Fortress fortress, EntityInputBean inputBean) throws InterruptedException, FlockException, ExecutionException, IOException {
        return trackEntity(fortress.getDefaultSegment(), inputBean);
    }



    @Override
    public Collection<TrackResultBean> trackEntities(final Fortress fortress, final List<EntityInputBean> inputBeans, int splitListInTo) throws FlockException, IOException, ExecutionException, InterruptedException {
        return trackEntities(fortress.getDefaultSegment(), inputBeans, splitListInTo);
    }

    @Override
    public Collection<TrackResultBean> trackEntities(final FortressSegment segment, final List<EntityInputBean> inputBeans, int splitListInTo) throws FlockException, IOException, ExecutionException, InterruptedException {
        String id = Thread.currentThread().getName() + "/" + DateTime.now().getMillis();
        if (segment == null) {
            throw new FlockException("No fortress supplied. Unable to process work without a valid fortress");
        }

        Future<Collection<DocumentType>> docType = docTypeRetryService.createDocTypes(segment.getFortress(), inputBeans);
        Future<Collection<TagResultBean>> tagResults = createTagsAsync(segment.getCompany(), getTags(inputBeans));
        logger.debug("About to create docTypes");
        EntityInputBean first = inputBeans.iterator().next();

        logger.debug("Dispatched request to create tags");


        try {
            // A long time, but this is to avoid test issues on the low spec build box
            List<List<EntityInputBean>>
                    splitList = Lists.partition(inputBeans, splitListInTo);

            StopWatch watch = new StopWatch();
            watch.start();
            logger.trace("Starting Batch [{}] - size [{}]", id, inputBeans.size());
            Collection<TrackResultBean> allResults = new ArrayList<>();
            // We have to wait for the docType before proceeding to create entities
            Collection<DocumentType> docs = docType.get(10, TimeUnit.SECONDS);
            assert docs.size()!=0;
            for (List<EntityInputBean> entityInputBeans : splitList) {
                Iterable<TrackResultBean> loopResults = entityRetry.track(docs.iterator().next(), segment, entityInputBeans, tagResults);
                logger.debug("Tracked requests");
                distributeChanges(segment.getFortress(), loopResults);

                for (TrackResultBean theResult : loopResults) {
                    allResults.add(theResult);
                }
            }
            watch.stop();
            logger.debug("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));

            return allResults;

        } catch (TimeoutException e) {
            logger.error("Time out looking/creating docType " + first.getDocumentType().getName());
            throw new FlockException("Time out looking/creating docType " + first.getDocumentType().getName());
        }

    }

    private List<TagInputBean> getTags(List<EntityInputBean> entityInputBeans) {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        for (EntityInputBean entityInputBean : entityInputBeans) {
            for (TagInputBean tag : entityInputBean.getTags()) {
                if ( !TagHelper.isSystemLabel(tag.getLabel()) && !tags.contains(tag))
                    tags.add(tag);
            }
            ///entityInputBean.getTags().stream().filter(tag -> !tag.isMustExist() && !tags.contains(tag)).forEach(tags::add);

        }
        return tags;
    }

    @Autowired
    ConceptService conceptService;

    @Override
    public TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {
        // Create the basic data within a transaction
        Entity entity;
        if (input.getKey() != null)
            entity = entityService.getEntity(company, input.getKey());
        else
            entity = entityService.findByCode(company, input.getFortress(), input.getDocumentType(), input.getCode());
        if (entity == null)
            throw new FlockException("Unable to resolve the Entity");

        FortressUser fu = fortressService.createFortressUser(entity.getSegment().getFortress(), input);

        DocumentType documentType = conceptService.findDocumentType(entity.getFortress(), entity.getType()) ;
        TrackResultBean result = logService.writeLog(documentType, entity, input, fu);

        Collection<TrackResultBean> results = new ArrayList<>();
        results.add(result);
        // Finally distribute the changes
        distributeChanges(result.getEntity().getSegment().getFortress(), results);
        return result;
    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressCode name of the fortress to rebuild
     * @throws org.flockdata.helper.FlockException
     */
    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
    public String reindex(Company company, String fortressCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", fortressCode));

        if (!fortress.isSearchEnabled())
            throw new FlockException("The fortress does not have search enabled. Nothing to do!");

        String message = null;
        if (fortress.isStoreDisabled()) {
            message = String.format("Content store has been disabled for the fortress %s. \r\nIf your search document has a Content Body then reprocess from source to create it" +
                    "\r\nYou can elect to enable the KV Store for content storage if wish", fortressCode);
            logger.warn(message);
        }
        if (message != null) {
            message = message + "\n";
        }
        adminService.doReindex(fortress);
        message = message + "Reindex Search request is re-processing Entities and Tags for [" + fortressCode + "]";
        logger.info("Reindex Search request is processing Entities for [" + fortressCode + "]");
        return message;


    }

    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
    public String reindex(Company company, Entity entity) throws FlockException {
        Fortress fortress = entity.getSegment().getFortress();
        if (fortress == null)
            throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", entity.getSegment().getCode()));

        if (!fortress.isSearchEnabled())
            throw new FlockException("The fortress does not have search enabled. Nothing to do!");

        String message = null;
        if (fortress.isStoreDisabled()) {
            message = String.format("Content store has been disabled for this Entity %s. \r\nIf your search document has a Content Body then reprocess from source to create it" +
                    "\r\nYou can elect to enable the KV Store for content storage if wish", entity.getKey());
            logger.warn(message);
        }
        if (message != null) {
            message = message + "\n";
        }
        adminService.doReindex(fortress,entity);
        message = message + "Reindex Search request is re-processing Entity and Tags for [" + entity.getKey() + "]";
        logger.info("Reindex Search request is processing Entities for [" + entity.getKey() + "]");
        return message;


    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document label
     *
     * @param fortressName name of the fortress to rebuild
     * @throws org.flockdata.helper.FlockException
     */
    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
    public String reindexByDocType(Company company, String fortressName, String docType) throws FlockException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new FlockException("Fortress [" + fortressName + "] could not be found");

        String message = null;
        if (fortress.isStoreDisabled()) {
            message = String.format("The store has been disabled for the fortress %s. Only information that has been recorded in a KV store can be re-indexed", fortress);
            logger.warn(message);
        }
        adminService.doReindex(fortress, docType);
        if (message != null) {
            message = message + "\n";
        }
        message = message + "Reindex Search request is processing Entities and Tags for [" + fortressName + "] and document type [" + docType + "]";
        logger.info("Reindex Search request is processing entities for [" + fortressName + "] and document type [" + docType + "]");
        return message;
    }

    @Override
    public EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException {
        return entityService.getEntitySummary(company, key);
    }

    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
    public void mergeTags(Company company, Long source, Long target) {
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
            return contentReader.read(entity, log.getLog()).getData();

        return new HashMap<>();
    }

    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
    public void purge(Company company, String fortressCode) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressCode + "] does not exist");

        logger.info("Processing request to purge fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
        adminService.purge(company, fortress);
    }

    /**
     * Iterates through all search documents and validates that an existing
     * Entity can be found for it by the key returned.
     *
     * @param company
     * @param fortressCode
     * @param docType
     * @return
     */
    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
    public String validateFromSearch(Company company, String fortressCode, String docType) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressCode + "] does not exist");

        logger.info("Validating fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
        adminService.validateFromSearch(company, fortress, docType);
        return null;
    }

    @Override
    @PreAuthorize(FdRoles.EXP_ADMIN)
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
        if (searchChange != null ) {
            if ( searchService !=null ) {
                searchService.setTags(entity, searchChange);
                searchService.makeChangeSearchable(searchChange);
            }
        } else {
            logger.info("ToDo: Delete the search document {}", entity);
        }
    }

    void distributeChanges(final Fortress fortress, final Iterable<TrackResultBean> resultBeans) throws IOException, InterruptedException, ExecutionException, FlockException {

        logger.debug("Distributing changes to sub-services");
        if ( searchService != null )
            searchService.makeChangesSearchable(fortress, resultBeans);
        // ToDo: how to wait for results when running tests
        if (engineConfig.isTestMode())
            conceptRetryService.trackConcepts(fortress, resultBeans).get();
        else
            conceptRetryService.trackConcepts(fortress, resultBeans);
        logger.debug("Distributed changes");
    }


}
