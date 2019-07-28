/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.concept.service.DocTypeRetryService;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressSegmentNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.schema.IndexRetryService;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.engine.tag.service.TagRetryService;
import org.flockdata.engine.track.service.ConceptRetryService;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.EntityRetryService;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.engine.track.service.EntityTagService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.engine.track.service.LogService;
import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.track.bean.TrackResultBean;
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

/**
 * Implementation to track requests into the FD stack
 */
@Service
@Qualifier("mediationFacadeNeo")
public class MediationFacadeNeo implements MediationFacade {

  private static DecimalFormat f = new DecimalFormat();
  private final EntityService entityService;
  private final EntityTagService entityTagService;
  private final FortressService fortressService;
  private final DocTypeRetryService docTypeRetryService;

  private final StorageProxy contentReader;

  private final LogService logService;

  private final PlatformConfig engineConfig;

  private final EntityRetryService entityRetry;

  private final ConceptRetryService conceptRetryService;

  private final IndexRetryService indexRetryService;

  private final SecurityHelper securityHelper;

  private final TagRetryService tagRetryService;

  private final EngineAdminService adminService;

  private final TrackBatchSplitter batchSplitter;

  private final ConceptService conceptService;
  private SearchServiceFacade searchServiceFacade;
  private Logger logger = LoggerFactory.getLogger(MediationFacadeNeo.class);

  @Autowired
  public MediationFacadeNeo(ConceptService conceptService, LogService logService, FortressService fortressService, TrackBatchSplitter batchSplitter, EntityTagService entityTagService, TagRetryService tagRetryService, EntityService entityService, DocTypeRetryService docTypeRetryService, StorageProxy contentReader, EntityRetryService entityRetry, EngineConfig engineConfig, SecurityHelper securityHelper, ConceptRetryService conceptRetryService, EngineAdminService adminService, IndexRetryService indexRetryService) {
    this.conceptService = conceptService;
    this.logService = logService;
    this.fortressService = fortressService;
    this.batchSplitter = batchSplitter;
    this.entityTagService = entityTagService;
    this.tagRetryService = tagRetryService;
    this.entityService = entityService;
    this.docTypeRetryService = docTypeRetryService;
    this.contentReader = contentReader;
    this.entityRetry = entityRetry;
    this.engineConfig = engineConfig;
    this.securityHelper = securityHelper;
    this.conceptRetryService = conceptRetryService;
    this.adminService = adminService;
    this.indexRetryService = indexRetryService;
  }

  @Autowired(required = false)
  void setSearchServiceFacade(SearchServiceFacade searchServiceFacade) {
    this.searchServiceFacade = searchServiceFacade;
  }

  @Override
  public Collection<TrackRequestResult> trackEntities(Collection<EntityInputBean> inputBeans, String apiKey) throws FlockException, InterruptedException, ExecutionException {
    Company c = securityHelper.getCompany(apiKey);
    if (c == null) {
      throw new AmqpRejectAndDontRequeueException("Unable to resolve the company for your ApiKey");
    }
    return trackEntities(c, inputBeans);
  }

  public Collection<TrackRequestResult> trackEntities(Company company, Collection<EntityInputBean> inputBeans) throws FlockException, InterruptedException, ExecutionException {
    Map<Segment, List<EntityInputBean>> byFortress = batchSplitter.getEntitiesBySegment(company, inputBeans);
    Collection<TrackRequestResult> results = new ArrayList<>();
    for (Segment segment : byFortress.keySet()) {
      Collection<TrackResultBean> tr =
          trackEntities(segment, byFortress.get(segment), 1);
      for (TrackResultBean result : tr) {
        results.add(new TrackRequestResult(result));
      }

    }
    return results;

  }

  @Override
  public FdTagResultBean createTag(Company company, TagInputBean tagInput) throws FlockException, ExecutionException, InterruptedException {
    List<TagInputBean> tags = new ArrayList<>();
    tags.add(tagInput);
    return createTags(company, tags).iterator().next();

  }

  @Override
  public Collection<FdTagResultBean> createTags(String apiKey, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {
    Company company = securityHelper.getCompany(apiKey);
    if (company == null) {
      throw new RuntimeException("Illegal company api key");
    }
    return createTags(company, tagInputs);
  }

  @Override
  public Collection<FdTagResultBean> createTags(Company company, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {
    Future<Collection<FdTagResultBean>> tags = createTagsAsync(company, tagInputs);
    if (tags == null) {
      return null;
    }
    return tags.get();
  }

  private Future<Collection<FdTagResultBean>> createTagsAsync(Company company, Collection<TagInputBean> tagInputs) throws FlockException, ExecutionException, InterruptedException {

    if (tagInputs.isEmpty()) {
      return null;
    }
    indexRetryService.ensureUniqueIndexes(tagInputs);
    return tagRetryService.createTags(company, tagInputs);
  }

  @Override
  public TrackResultBean trackEntity(Company company, EntityInputBean inputBean) throws FlockException, ExecutionException, InterruptedException {
    FortressNode fortress = fortressService.findByName(company, inputBean.getFortress().getName());
    if (fortress == null) {
      logger.debug("Creating new Fortress {}", inputBean.getFortress());
      fortress = fortressService.registerFortress(company, inputBean.getFortress());
    }
    fortress.setCompany(company);
    Segment segment;
    if (inputBean.getSegment() != null) {
      segment = fortressService.addSegment(new FortressSegmentNode(fortress, inputBean.getSegment()));
    } else {
      segment = fortress.getDefaultSegment();
    }
    return trackEntity(segment, inputBean);
  }

  /**
   * tracks an entity and creates logs. Distributes changes to KV stores and search engine.
   * <p>
   * This is synchronous and blocks until completed
   *
   * @param segment   - system that owns the data
   * @param inputBean - input
   * @return non-null information about the trackRequest success
   * @throws org.flockdata.helper.FlockException illegal input
   */
  @Override
  public TrackResultBean trackEntity(final Segment segment, final EntityInputBean inputBean) throws FlockException, ExecutionException, InterruptedException {
    List<EntityInputBean> inputs = new ArrayList<>(1);
    inputs.add(inputBean);
    Collection<TrackResultBean> results = trackEntities(segment, inputs, 1);
    return results.iterator().next();
  }

  @Override
  public TrackResultBean trackEntity(FortressNode fortress, EntityInputBean inputBean) throws InterruptedException, FlockException, ExecutionException, IOException {
    return trackEntity(fortress.getDefaultSegment(), inputBean);
  }

  @Override
  public Collection<TrackResultBean> trackEntities(final FortressNode fortress, final List<EntityInputBean> inputBeans, int splitListInTo) throws FlockException, IOException, ExecutionException, InterruptedException {
    return trackEntities(fortress.getDefaultSegment(), inputBeans, splitListInTo);
  }

  @Override
  public Collection<TrackResultBean> trackEntities(final Segment segment, final List<EntityInputBean> inputBeans, int splitListInTo) throws FlockException, ExecutionException, InterruptedException {
    String id = Thread.currentThread().getName() + "/" + DateTime.now().getMillis();
    if (segment == null) {
      throw new FlockException("No fortress supplied. Unable to process work without a valid fortress");
    }

    Future<Collection<DocumentNode>> docType = docTypeRetryService.createDocTypes(segment, inputBeans);
    Future<Collection<FdTagResultBean>> tagResults = createTagsAsync(segment.getCompany(), getTags(inputBeans));
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
      Collection<DocumentNode> docs = docType.get(10, TimeUnit.SECONDS);
      for (List<EntityInputBean> entityInputBeans : splitList) {
        DocumentNode documentType = null;
        if (docs.iterator().hasNext()) {
          documentType = docs.iterator().next();
        }

        Iterable<TrackResultBean> loopResults = entityRetry.track(documentType, segment, entityInputBeans, tagResults);
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
        if (!TagHelper.isSystemLabel(tag.getLabel()) && !tags.contains(tag)) {
          tags.add(tag);
        }
      }
      ///entityInputBean.getTags().stream().filter(tag -> !tag.isMustExist() && !tags.contains(tag)).forEach(tags::add);

    }
    return tags;
  }

  @Override
  public TrackResultBean trackLog(Company company, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {
    // Create the basic data within a transaction
    EntityNode entity;
    if (input.getKey() != null) {
      entity = entityService.getEntity(company, input.getKey());
    } else {
      entity = (EntityNode) entityService.findByCode(company, input.getFortress().getCode(), input.getDocumentType(), input.getCode());
    }
    if (entity == null) {
      throw new FlockException("Unable to resolve the Entity");
    }

    FortressUserNode fu = fortressService.createFortressUser(entity.getSegment().getFortress(), input);

    DocumentNode documentType = conceptService.findDocumentType(entity.getFortress(), entity.getType());
    TrackResultBean result = logService.writeLog(documentType, entity, input, fu);

    Collection<TrackResultBean> results = new ArrayList<>();
    results.add(result);
    // Finally distribute the changes
    distributeChanges(result.getEntity().getSegment().getFortress(), results);
    return result;
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public String reindex(CompanyNode company, String fortressCode) throws FlockException {
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    if (fortress == null) {
      throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", fortressCode));
    }

    if (!fortress.isSearchEnabled()) {
      throw new FlockException("The fortress does not have search enabled. Nothing to do!");
    }

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
  public String reindex(CompanyNode company, EntityNode entity) throws FlockException {
    Fortress fortress = entity.getSegment().getFortress();
    if (fortress == null) {
      throw new NotFoundException(String.format("No fortress to reindex with the name %s could be found", entity.getSegment().getCode()));
    }

    if (!fortress.isSearchEnabled()) {
      throw new FlockException("The fortress does not have search enabled. Nothing to do!");
    }

    String message = null;
    if (!fortress.isStoreEnabled()) {
      message = String.format("Content store has been disabled for this Entity %s. \r\nIf your search document has a Content Body then reprocess from source to create it" +
          "\r\nYou can elect to enable the KV Store for content storage if wish", entity.getKey());
      logger.warn(message);
    }
    if (message != null) {
      message = message + "\n";
    }
    adminService.doReindex(fortress, entity);
    message = message + "Reindex Search request is re-processing Entity and Tags for [" + entity.getKey() + "]";
    logger.info("Reindex Search request is processing Entities for [" + entity.getKey() + "]");
    return message;


  }

  /**
   * Rebuilds all search documents for the supplied fortress of the supplied document label
   *
   * @param fortressName name of the fortress to rebuild
   * @throws org.flockdata.helper.FlockException business data exception
   */
  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public String reindexByDocType(CompanyNode company, String fortressName, String docType) throws FlockException {
    FortressNode fortress = fortressService.findByCode(company, fortressName);
    if (fortress == null) {
      throw new FlockException("Fortress [" + fortressName + "] could not be found");
    }

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
  public EntitySummaryBean getEntitySummary(CompanyNode company, String key) throws FlockException {
    return entityService.getEntitySummary(company, key);
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public void mergeTags(Company company, Long source, Long target) {
    // ToDo: Transactional?
    // Update the search docs for the affected entities
    Collection<Long> entities = entityTagService.mergeTags(source, target);
    searchServiceFacade.reIndex(entities);

  }

  @Override
  public Map<String, Object> getLogContent(EntityNode entity, Long logId) {
    EntityLog log = entityService.getLogForEntity(entity, logId);
    if (log != null && contentReader != null) {
      return contentReader.read(entity, log.getLog()).getData();
    }

    return new HashMap<>();
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public void purge(Company company, String fortressCode) throws FlockException {
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    if (fortress == null) {
      throw new NotFoundException("Fortress [" + fortressCode + "] does not exist");
    }

    logger.info("Processing request to purge fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
    adminService.purge(fortress);
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public void purge(Company company, String fortressCode, String docType) {
    purge(company, fortressCode, docType, null);
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public void purge(Company company, String fortressCode, String docType, String segment) {
    FortressNode fortress = fortressService.findByCode(company, fortressCode);

    if (fortress == null) {
      throw new NotFoundException("Not Found " + fortressCode);
    }

    DocumentNode documentType = conceptService.findDocumentType(fortress, docType, false);
    if (documentType == null) {
      throw new NotFoundException("Not Found " + docType);
    }

    logger.info("Purging fortress {} {}", fortress, documentType);
    adminService.purge(company, fortress, conceptService.findDocumentTypeWithSegments(documentType), segment);
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public String validateFromSearch(CompanyNode company, String fortressCode, String docType) throws FlockException {
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    if (fortress == null) {
      throw new NotFoundException("Fortress [" + fortressCode + "] does not exist");
    }

    logger.info("Validating fortress [{}] on behalf of [{}]", fortress, securityHelper.getLoggedInUser());
    adminService.validateFromSearch(company, fortress, docType);
    return null;
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public void purge(Fortress fortress) throws FlockException {
    adminService.purge(fortress);
  }

  @Override
  @Transactional
  public void cancelLastLog(Company company, Entity entity) throws IOException, FlockException {
    EntitySearchChange searchChange;
    // Refresh the entity
    //entity = entityService.getEntity(entity);
    searchChange = entityService.cancelLastLog(company, (EntityNode) entity);
    if (searchChange != null) {
      if (searchServiceFacade != null) {
        searchServiceFacade.setTags(entity, searchChange);
        searchServiceFacade.makeChangeSearchable(searchChange);
      }
    } else {
      logger.info("ToDo: Delete the search document {}", entity);
    }
  }

  private void distributeChanges(final Fortress fortress, final Iterable<TrackResultBean> resultBeans) throws InterruptedException, ExecutionException, FlockException {

    logger.debug("Distributing changes to sub-services");
    if (searchServiceFacade != null) {
      searchServiceFacade.makeChangesSearchable(fortress, resultBeans);
    }
    // ToDo: how to wait for results when running tests
    if (engineConfig.isTestMode()) {
      conceptRetryService.trackConcepts(resultBeans).get();
    } else {
      conceptRetryService.trackConcepts(resultBeans);
    }
    logger.debug("Distributed changes");
  }


}
