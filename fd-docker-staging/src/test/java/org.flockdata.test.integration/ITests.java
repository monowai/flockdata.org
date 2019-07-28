/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.integration;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.flockdata.client.FdClientIo;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Document;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FdTemplate;
import org.flockdata.integration.FileProcessor;
import org.flockdata.integration.IndexManager;
import org.flockdata.model.ContentModelResult;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

/**
 * Establishes the integration test environment. Descendant classes use @Test functions against
 * this established stack
 *
 * @author mholdsworth
 * @tag Test, Docker
 * @since 3/04/2016
 */
@ContextConfiguration(classes = {
    ClientConfiguration.class,
    IndexManager.class,
    FileProcessor.class,
    FdTemplate.class,
    FdClientIo.class,
    FdRabbitClient.class,
    EnginePing.class,
    StorePing.class,
    SearchPing.class,
    RegistrationPost.class,
    EntityLogsGet.class,
    SearchEsPost.class,
    SearchFdPost.class,
    EntityData.class,
    Health.class,
    EntityGet.class,
    Login.class,
    ModelGet.class,
    ModelPost.class,
    ModelFieldStructure.class,
    TagGet.class,
    TagsGet.class,
    AdminPurgeFortressSegment.class,
    AdminPurgeFortress.class,
    AmqpRabbitConfig.class,
    RestTemplate.class,
    TrackEntityPost.class,
    SearchHelper.class,
    IntegrationHelper.class

})
@RunWith(SpringRunner.class)
@Configuration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles( {"fd-auth-test"})
public class ITests {

  @ClassRule
  public static FdDocker stack = new FdDocker();

  private static Logger logger = LoggerFactory.getLogger(ITests.class);

  /**
   * Contains properties used by rabbitConfig and fdRestWriter
   */
  @Autowired
  private ClientConfiguration clientConfiguration;
  @Autowired
  private FileProcessor fileProcessor;
  @Autowired
  private IntegrationHelper integrationHelper;
  @Autowired
  private RestTemplate restTemplate; // ensure resource is wired
  @Autowired
  private FdTemplate fdTemplate;
  @Autowired
  private EnginePing enginePing;
  @Autowired
  private SearchHelper searchHelper;
  @Autowired
  private SearchEsPost esSearch;
  @Autowired
  private SearchFdPost searchViaFd;
  @Autowired
  private EntityData entityData;
  @Autowired
  private AdminPurgeFortress purge;
  @Autowired
  private EntityGet entityGet;
  @Autowired
  private ModelPost modelPost;
  @Autowired
  private TrackEntityPost trackEntityPost;
  @Autowired
  private EntityLogsGet entityLogsGet;
  @Autowired
  private ModelFieldStructure modelFieldStructure;
  @Autowired
  private TagGet tagGet;
  @Autowired
  private TagsGet tagsGet;
  @Autowired
  private AdminPurgeFortressSegment adminPurgeFortressSegment;

  /**
   * Uses the standard FdClientIo services, which use a RestTemplate and RabbitClient, to talk to FlockData.
   * By default the service URL points to fd-engine but can be reconfigured via clientConfiguration.setServiceUrl(...)
   * to talk to fd-search or fd-store (for ping requests only!).
   * <p>
   * By design, only fd-engine is secured
   */
  @Autowired
  private FdIoInterface fdClientIo;

  @Before
  public void setupServices() {
    assertNotNull(restTemplate);
    assertNotNull(clientConfiguration);
    clientConfiguration.batchSize(1);
    integrationHelper.waitForServices();
  }

  @Test
  public void aaPingFdEngine() {
    logger.info("Ping ;FD Engine");
    CommandResponse<String> response = enginePing.exec();
    assertEquals("Couldn't ping fd-engine", "pong", response.getResult());

  }

  @Test
  public void simpleLogin() {
    logger.info("Testing simpleLogin");
    SystemUserResultBean suResult = fdTemplate.getFdIoInterface().login("integration", "123");
    assertNotNull(suResult);
    assertTrue("User Roles missing", suResult.getUserRoles().length != 0);
  }

  @Test
  public void registration() {
    // An authorised user can create DataAccess users for a given company
    SystemUserResultBean login = integrationHelper.login();
    assertThat(login)
        .isNotNull()
        .hasFieldOrProperty("apiKey");

  }

  /**
   * FlockData ships with some basic static data like Countries and Cities.
   * This test checks that they are tracked in to the service. Validates a number of things:
   * Country Content Profile
   * Tag being tracked over an AMQP endpoint
   * Countries can be found via the Tag endpoint by label
   */
  @Test
  public void loadCountries() throws Exception {

    SystemUserResultBean login = integrationHelper.login();

    clientConfiguration.batchSize(5);

    ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/countries.json");
    ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/countries.json", contentModel)
        .setDelimiter(";");

    int countryInputs = fileProcessor.processFile(extractProfile, "/fd-cow.txt");

    assertEquals("Countries not processed", 250, countryInputs); //+1 "Undefined"
    // Tags are processed over a messageQ so will take a wee bit of time to be processed
    integrationHelper.longSleep();
    CommandResponse<TagResultBean[]> countries = tagsGet.exec("Country");
    assertEquals("Error loading countries", null, countries.getError());

    TagResultBean[] countryResults = countries.getResult();
    // By this stage we may or may not have processed all the tags depending on how resource constrained the host machine
    // running the integration stack is. So we'll just check at least two batches of 10 have been processed assuming the
    // rest will pass
    assertTrue("No countries found!", countryResults.length > 10);

    CommandResponse<TagResultBean> foundTag = tagGet.exec("Country", "AU");
    assertEquals("", null, foundTag.getError());
    assertNotNull("Couldn't find Australia", foundTag.getResult());

    foundTag = tagGet.exec("Country", "AUS");
    assertEquals("", null, foundTag.getError());
    assertNotNull("Couldn't find Australia", foundTag.getResult());


    foundTag = tagGet.exec("Country", "Australia");
    assertNull("Country by name - ", foundTag.getError());
    assertNotNull("Couldn't find Australia", foundTag.getResult());

    assertEquals("By Code and By Name they are the same country so should equal", foundTag.getResult(), foundTag.getResult());
    integrationHelper.shortSleep();
    QueryParams qp = searchHelper.getTagQuery(login.getCompanyName(), "country", "australia");

    CommandResponse<Map<String, Object>> response = esSearch.exec(qp);
    assertThat(response)
        .hasFieldOrPropertyWithValue("error", null)
        .hasFieldOrProperty("result");

    searchHelper.assertHitCount("Should have found just 1 hit for Australia", 1, response.getResult());
    response = esSearch.exec(searchHelper
        .getTagMatchQuery(login.getCompanyName(), "country", "aka.bgn_longname", "Commonwealth of Australia"));

    searchHelper.assertHitCount("Didn't find Australia by alias", 1, response.getResult());

  }

  @Test
  public void trackEntityOverHttp() {
    integrationHelper.login();

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("TrackEntity", false))
        .setDocumentType(new DocumentTypeInputBean("someThing"))
        .setContent(new ContentInputBean(Helper.getRandomMap()))
        .addTag(new TagInputBean("someCode", "SomeLabel"));

    CommandResponse<TrackRequestResult> response = trackEntityPost.exec(entityInputBean);
    assertEquals("Track Entity - ", null, response.getError());

    assertNotNull(response.getResult());
    assertNotNull(response.getResult().getKey());
    assertEquals("Should be a new Entity", response.getResult().isNewEntity(), true);
    assertEquals("Problem creating the Content", response.getResult().getLogStatus(), ContentInputBean.LogStatus.OK);

    CommandResponse<EntityResultBean> foundEntity = entityGet.exec(null, response.getResult().getKey());
    assertEquals("Find Entity - ", null, foundEntity.getError());
    assertNotNull(foundEntity.getResult().getKey());

  }

  @Test
  public void trackEntityOverAmqpThenFindInSearch() throws Exception {
    integrationHelper.login();
    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("TrackEntityAmqp")
            .setSearchEnabled(true))
        .setCode("findme")
        .setDocumentType(new DocumentTypeInputBean("entityamqp"))
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")))
        .addTag(new TagInputBean("someCode", "SomeLabel"));

    fdTemplate.writeEntity(entityInputBean, true);

    CommandResponse<EntityResultBean> response = integrationHelper.waitForEntityKey(logger, "trackEntityOverAmqpThenFindInSearch", entityGet, entityInputBean, null);

    EntityResultBean entityResult = response.getResult();
    assertNotNull(entityResult);
    assertNotNull(entityResult.getKey());
    response = integrationHelper.waitForSearch(logger, "trackEntityOverAmqpThenFindInSearch", entityGet, entityInputBean, null);
    entityResult = response.getResult();
    assertFalse("Search was incorrectly suppressed", entityResult.isSearchSuppressed());
    assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, entityResult.getSearch());
    assertEquals("Search Key was not set to the code of the entityInput", entityInputBean.getCode(), entityResult.getSearchKey());

    integrationHelper.shortSleep();
    QueryParams qp = new QueryParams(entityResult.getCode())
        .setEntityOnly(true)
        .setCode(entityResult.getCode())
        .setFortress(entityInputBean.getFortress().getName());

    CommandResponse<EsSearchRequestResult> esResponse = searchViaFd.exec(qp);
    assertThat(esResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("error", null)
        .hasFieldOrProperty("result");

    EsSearchRequestResult searchResults = esResponse.getResult();
    assertEquals("Didn't get a search hit on the Entity", 1, searchResults.getResults().size());
    assertEquals("Keys do not match", entityResult.getKey(), searchResults.getResults().iterator().next().getKey());

    // Test we can find via UTF-8
    qp.setSearchText(entityInputBean.getContent().getData().get("key").toString());
    searchViaFd.exec(qp);
    assertEquals("Couldn't find via UTF-8 Text search", entityResult.getKey(), searchResults.getResults().iterator().next().getKey());

  }

  @Test
  public void validateEntityLogs() throws Exception {
    integrationHelper.login();

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("validateEntityLogs", false))
        .setCode("findme")
        .setDocumentType(new DocumentTypeInputBean("validateEntityLogs"))
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "value")))
        .addTag(new TagInputBean("someCode", "SomeLabel"));

    fdTemplate.writeEntity(entityInputBean, true);
    integrationHelper.longSleep();
    CommandResponse<EntityResultBean> response = integrationHelper.waitForEntityKey(logger, "validateEntityLogs", entityGet, entityInputBean, null);

    EntityResultBean entityResult = response.getResult();
    assertThat(entityResult)
        .isNotNull()
        .hasFieldOrProperty("key");

    CommandResponse<EntityLogResult[]> elResponse = integrationHelper.waitForEntityLog(logger, entityLogsGet, 1, entityResult.getKey());
    assertNotNull(elResponse.getResult());
    assertEquals("Didn't find a log", 1, elResponse.getResult().length);
    assertNotNull("No data was returned", elResponse.getResult()[0].getData());
    assertEquals("Content property not found", "value", elResponse.getResult()[0].getData().get("key"));

    entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("validateEntityLogs", false))
        .setCode("findme")
        .setDocumentType(new DocumentTypeInputBean("validateEntityLogs"))
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "updated")));


    // Updating an existing entity
    fdTemplate.writeEntity(entityInputBean, true);
    elResponse = integrationHelper.waitForEntityLog(logger, entityLogsGet, 2, response.getResult().getKey());
    assertEquals("Didn't find the second log", 2, elResponse.getResult().length);
    assertEquals("Didn't find the updated field as the first result", "updated", elResponse.getResult()[0].getData().get("key"));
    assertEquals("Didn't find the original field as the second result", "value", elResponse.getResult()[1].getData().get("key"));
  }

  @Test
  public void findByESPassThroughWithUTF8() throws Exception {
    integrationHelper.login();

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("findByESPassThroughWithUTF8")
            .setSearchEnabled(true))
        .setCode("Katerina Neumannová")
        .setDescription("Katerina Neumannová")
        .setDocumentType(new DocumentTypeInputBean("entityamqp"))
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")));

    fdTemplate.writeEntity(entityInputBean, true);

    CommandResponse<EntityResultBean> response = integrationHelper.waitForEntityKey(logger, "findByESPassThroughWithUTF8", entityGet, entityInputBean, null);
    assertEquals("Error waiting by entity Key", null, response.getError());

    assertThat(response.getResult())
        .isNotNull()
        .hasFieldOrProperty("key");

    response = integrationHelper.waitForSearch(logger, "findByESPassThroughWithUTF8", entityGet, entityInputBean, null);
    assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, response.getResult().getSearch());

    integrationHelper.shortSleep();
    QueryParams qp = new QueryParams()
        .setFortress(entityInputBean.getFortress().getName())
        .setTypes(entityInputBean.getDocumentType().getName())
        .setMatchAll(true);

    CommandResponse<Map<String, Object>> esResponse = esSearch.exec(qp);
    assertEquals("Search Reply ", null, esResponse.getError());

    assertFalse("errors were found " + esResponse.getResult().get("__errors__"), esResponse.getResult().containsKey("__errors__"));

    searchHelper.assertHitCount("Expected 1 hit", 1, esResponse.getResult());
    assertTrue("UTF-8 failure. Couldn't find " + entityInputBean.getCode(), searchHelper.getHits(esResponse.getResult()).contains(entityInputBean.getCode()));
  }

  @Test
  public void simpleTags() throws Exception {
    SystemUserResultBean login = integrationHelper.login();

    Collection<TagInputBean> tags = new ArrayList<>();
    String tagLabel = "simpleTag";
    TagInputBean tagInputBean = new TagInputBean("ACode", tagLabel).setDescription("Description of the Tag");
    tags.add(tagInputBean);
    tagInputBean = new TagInputBean("BCode", tagLabel);
    tags.add(tagInputBean);

    fdClientIo.writeTags(tags);
    integrationHelper.longSleep();  // Async delivery, so lets wait a bit....

    CommandResponse<TagResultBean[]> tagResponse = integrationHelper.waitForTagCount(logger, tagsGet, tagLabel);

    assertNull(tagResponse.getError());
    TagResultBean[] foundTags = tagResponse.getResult();
    assertNotNull(foundTags);
    assertEquals("Missing tags", 2, foundTags.length);

    QueryParams qp = searchHelper.getTagQuery(login.getCompanyName(), tagInputBean.getLabel(), "*");


    CommandResponse<Map<String, Object>> response = esSearch.exec(qp);
    integrationHelper.shortSleep(); // Extra time for ES to commit
    assertEquals("Search Reply ", null, response.getError());

    Map<String, Object> esResult = response.getResult();
    assertFalse("errors were found " + esResult.get("__errors__"), esResult.containsKey("__errors__"));

    searchHelper.assertHitCount("Expected 2 hits for two tags when searching for *", 2, response.getResult());

    // Assert that updates work
    tags.clear();
    tagInputBean = new TagInputBean("ACode", tagLabel)
        .setName("acode wonder")
        .setMerge(true);

    tagInputBean.setProperty("aprop", 123);

    tags.add(tagInputBean);
    fdClientIo.writeTags(tags);
    integrationHelper.longSleep();  // Async delivery, so lets wait a bit....

    qp = searchHelper.getTagQuery(login.getCompanyName(), tagInputBean.getLabel(), "wonder");

    response = esSearch.exec(qp);
    integrationHelper.shortSleep();

    assertNull("Search Reply ", response.getError());
    searchHelper.assertHitCount("Expected single hit for an updated tag", 1, response.getResult());

    String json = searchHelper.getHits(response.getResult());
    assertNotNull(json);
    assertTrue("ACode tag should have been in the result", json.contains("ACode"));
    assertTrue("Didn't find correct search text", json.contains("acode wonder"));

  }

  @Test
  public void bulkTagsDontBlock() throws Exception {
    SystemUserResultBean login = integrationHelper.login();

    Collection<TagInputBean> setA = getRandomTags();
    Collection<TagInputBean> setB = getRandomTags();
    Collection<TagInputBean> setC = getRandomTags();
    Collection<TagInputBean> setD = getRandomTags();
    fdTemplate.writeTags(setA);
    fdTemplate.writeTags(setB);
    fdTemplate.writeTags(setC);
    fdTemplate.writeTags(setD);
    integrationHelper.longSleep();
    QueryParams qp = searchHelper.getTagQuery(login.getCompanyName(), "Set*", "code*");

    CommandResponse<Map<String, Object>> response = esSearch.exec(qp);
    assertNull("Not finding any tags", response.getError());


  }

  @Test
  public void purgeFortressRemovesEsIndex() throws Exception {
    integrationHelper.login();

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("purgeFortressRemovesEsIndex")
            .setSearchEnabled(true))
        .setCode("SearchDoc")
        .setDocumentType(new DocumentTypeInputBean("DeleteSearchDoc"))
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

    fdTemplate.writeEntity(entityInputBean, true);


    integrationHelper.waitForEntityKey(logger, "purgeFortressRemovesEsIndex", entityGet, entityInputBean, null);
    integrationHelper.waitForSearch(logger, "purgeFortressRemovesEsIndex", entityGet, entityInputBean, null);
    integrationHelper.longSleep(); // Give ES time to commit

    QueryParams qp = new QueryParams("*");
    qp.setFortress("purgeFortressRemovesEsIndex");
    qp.setTypes("DeleteSearchDoc".toLowerCase());
    CommandResponse<EsSearchRequestResult> esResponse = searchViaFd.exec(qp);

    assertNotNull(esResponse.getResult());
    assertEquals("expected 1 hit", 1L, esResponse.getResult().getTotalHits());

    CommandResponse<String> response = purge.exec("purgeFortressRemovesEsIndex");
    integrationHelper.longSleep(); // Give ES time to commit
    assertEquals("Purge fortress failed", null, response.getError());

    assertNull("The entity should not exist because the fortress was purged", entityGet.exec(entityInputBean, null).getResult());
    assertEquals("The entity search doc was not removed", 0L, searchViaFd.exec(qp).getResult().getTotalHits());


  }

  @Test
  public void purgeSegmentRemovesOnlyTheSpecifiedOne() throws Exception {
    integrationHelper.login();

    DocumentTypeInputBean docType = new DocumentTypeInputBean("DeleteSearchDoc");

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("purgeSegment")
            .setSearchEnabled(true))
        .setCode("MySearchA")
        .setSegment("2015")
        .setDocumentType(docType)
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));


    integrationHelper.waitForEntityKey(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, entityInputBean, null);
    integrationHelper.waitForSearch(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, entityInputBean, null);

    entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("purgeSegment")
            .setSearchEnabled(true))
        .setCode("MySearchB")
        .setSegment("2016")
        .setDocumentType(docType)
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Quick brown fox")));

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));

    integrationHelper.waitForEntityKey(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, entityInputBean, null);
    integrationHelper.waitForSearch(logger, "purgeSegmentRemovesOnlyTheSpecifiedOne", entityGet, entityInputBean, null);

    integrationHelper.shortSleep(); // Give ES time to commit

    QueryParams qp = new QueryParams("*")
        .setFortress("purgeSegment")
        .setEntityOnly(true)
        .setTypes(docType.getCode().toLowerCase());

    CommandResponse<EsSearchRequestResult> esResponse = searchViaFd.exec(qp);

    assertThat(esResponse)
        .isNotNull()
        .hasFieldOrProperty("result")
        .hasFieldOrPropertyWithValue("error", null);

    assertThat(esResponse.getResult())
        .hasFieldOrPropertyWithValue("totalHits", 2L)
    ;

    qp.setSegment("2015");
    esResponse = searchViaFd.exec(qp);

    assertNotNull(esResponse.getResult());
    assertEquals("expected 1 hit on segment 2015", 1L, esResponse.getResult().getTotalHits());

    qp.setSegment("2016");
    esResponse = searchViaFd.exec(qp);

    assertNotNull(esResponse.getResult());
    assertEquals("expected 1 hit on segment 2016", 1L, esResponse.getResult().getTotalHits());

    // Now to purge 2015 so we are left only with 2016
    adminPurgeFortressSegment.exec("purgeSegment", docType.getName(), "2015");
    integrationHelper.longSleep();

    esResponse = searchViaFd.exec(qp);
    assertEquals("", null, esResponse.getError());
    assertEquals("expected 1 hit on segment 2016", 1L, esResponse.getResult().getTotalHits());

    qp.setSegment("2015");
    esResponse = searchViaFd.exec(qp);
    assertEquals("Command failed to execute", null, esResponse.getError());

    // Check we can track back into previously purged fortress
    entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("purgeSegment")
            .setSearchEnabled(true))
        .setCode("MySearchA")
        .setSegment("2015")
        .setDocumentType(docType)
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Find Me")));

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
    integrationHelper.longSleep();
    qp.setSegment("2015");
    esResponse = searchViaFd.exec(qp);

    assertThat(esResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("error", null);

    assertThat(esResponse.getResult())
        .hasFieldOrPropertyWithValue("totalHits", 1L);
  }

  /**
   * Much the same as purgeSegmentRemovesOnlyTheSpecifiedOne but ensures the functions
   * work for entities that have no logs
   *
   * @throws Exception anything
   */
  @Test
  public void purgeSegmentEntitiesWithNoLogs() throws Exception {
    integrationHelper.login();
    final String FORTRESS = "purgeSegmentEntitiesWithNoLogs";
    DocumentTypeInputBean docType = new DocumentTypeInputBean(FORTRESS);

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean(FORTRESS)
            .setSearchEnabled(true))
        .setCode("MySearchA")
        .setSegment("2015")
        .setDocumentType(docType)
        .setEntityOnly(true);

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));

    integrationHelper.waitForEntityKey(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, entityInputBean, null);
    integrationHelper.waitForSearch(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, entityInputBean, null);

    entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean(FORTRESS)
            .setSearchEnabled(true))
        .setCode("MySearchB")
        .setSegment("2016")
        .setDocumentType(docType)
        .setEntityOnly(true);

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));

    integrationHelper.waitForEntityKey(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, entityInputBean, null);
    integrationHelper.waitForSearch(logger, "purgeSegmentEntitiesWithNoLogs", entityGet, entityInputBean, null);

    integrationHelper.shortSleep(); // Give ES some extra time to commit

    QueryParams qp = new QueryParams("*")
        .setFortress(FORTRESS)
        .setEntityOnly(true)
        .setTypes(docType.getCode().toLowerCase());

    CommandResponse<EsSearchRequestResult> esResponse = searchViaFd.exec(qp);

    assertNotNull(esResponse.getResult());
    assertEquals("Searching across both segments returns 2", 2L, esResponse.getResult().getTotalHits());

    qp.setSegment("2015");
    esResponse = searchViaFd.exec(qp);
    assertThat(esResponse)
        .isNotNull()
        .hasFieldOrProperty("result");

    assertEquals("expected 1 hit on segment 2015", 1L, esResponse.getResult().getTotalHits());

    qp.setSegment("2016");
    esResponse = searchViaFd.exec(qp);

    assertNotNull(esResponse.getResult());
    assertEquals("expected 1 hit on segment 2016", 1L, esResponse.getResult().getTotalHits());

    // Now to purge 2015 so we are left only with 2016
    adminPurgeFortressSegment.exec(FORTRESS, docType.getName(), "2015");
    integrationHelper.longSleep();

    esResponse = searchViaFd.exec(qp);
    assertEquals("expected 1 hit on segment 2016", 1L, esResponse.getResult().getTotalHits());

    // Check we can track back into previously purged fortress
    entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean(FORTRESS)
            .setSearchEnabled(true))
        .setCode("MySearchA")
        .setSegment("2015")
        .setDocumentType(docType)
        .setEntityOnly(true);

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
    integrationHelper.longSleep();
    qp.setSegment("2015");
    esResponse = searchViaFd.exec(qp);

    assertNotNull(esResponse.getResult());
    assertEquals("Didn't expect an error", null, esResponse.getError());
    assertEquals("expected 1 hit on segment 2015", 1L, esResponse.getResult().getTotalHits());


  }

  @Test
  public void getEntityFieldStructure() throws Exception {
    integrationHelper.login();

    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("getEntityFieldStructure")
            .setSearchEnabled(true))
        .setCode("Katerina Neumannová")
        .setDescription("Katerina Neumannová")
        .setDocumentType(new DocumentTypeInputBean("entityamqp"))
        .addTag(new TagInputBean("anyCode", "anyLabel").addEntityTagLink("anyrlx"))
        .setContent(new ContentInputBean(Helper.getSimpleMap("key", "Katerina Neumannová")));

    fdClientIo.writeEntities(integrationHelper.toCollection(entityInputBean));
    CommandResponse<EntityResultBean> response = integrationHelper.waitForEntityKey(logger, "getEntityFieldStructure", entityGet, entityInputBean, null);

    assertEquals("", null, response.getError());
    EntityResultBean entityResult = response.getResult();
    assertNotNull(entityResult);
    assertNotNull(entityResult.getKey());
    response = integrationHelper.waitForSearch(logger, "getEntityFieldStructure", entityGet, entityInputBean, response.getResult().getKey());
    assertEquals("Reply from fd-search was not received. Search key should have been set to 1", 1, response.getResult().getSearch());

    // Searching with no parameters
    CommandResponse<ContentStructure> modelResponse = modelFieldStructure.exec(entityInputBean.getFortress().getName(), entityInputBean.getDocumentType().getName());

    assertEquals("", null, modelResponse.getError());
    ContentStructure structure = modelResponse.getResult();

    assertTrue("All data columns were un-faceted strings so nothing should be returned", structure.getData().isEmpty());
    assertFalse(structure.getLinks().isEmpty());
    assertFalse(structure.getSystem().isEmpty());
  }

  @Test
  public void persistEntityRelationshipModel() throws Exception {
    integrationHelper.login();
    ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/test-entity-relationships.json");
    assertNotNull(contentModel);
    Collection<ContentModel> models = new ArrayList<>();
    models.add(contentModel);
    CommandResponse<Collection<ContentModelResult>> response = modelPost.exec(models);
    assertEquals("Error - ", null, response.getError());
    assertEquals("Expected a response equal to the number of inputs", 1, response.getResult().size());
    ContentModel found = fdClientIo.getContentModel(contentModel.getFortress(), contentModel.getDocumentType());
    assertNotNull(found);
    assertFalse(found.getContent().isEmpty());
  }

  @Test
  public void versionableEntity() throws Exception {
    integrationHelper.login();

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put("value", "alpha");
    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("TrackEntity")
            .setSearchEnabled(true)
            .setStoreEnabled(true))
        .setDocumentType(new DocumentTypeInputBean("someThing"))
        .setContent(new ContentInputBean(dataMap));

    CommandResponse<TrackRequestResult> response = trackEntityPost.exec(entityInputBean);
    assertEquals("Track Entity - ", null, response.getError());

    assertNotNull(response.getResult());
    assertNotNull(response.getResult().getKey());
    assertEquals("Should be a new Entity", response.getResult().isNewEntity(), true);
    assertEquals("Problem creating the Content", response.getResult().getLogStatus(), ContentInputBean.LogStatus.OK);

    CommandResponse<EntityResultBean> foundEntity = integrationHelper.waitForEntityKey(logger, "versionableEntity", entityGet, null, response.getResult().getKey());
    assertNull("Find Entity - ", foundEntity.getError());
    assertNotNull(foundEntity.getResult().getKey());

    CommandResponse<EntityLogResult[]> elResponse = entityLogsGet.exec(foundEntity.getResult().getKey());
    assertEquals("Expected one log", 1, elResponse.getResult().length);
    EntityLogResult foundLog = elResponse.getResult()[0];
    assertEquals("Data value mismatch", dataMap.get("value").toString(), foundLog.getData().get("value").toString());

    // Now test that the value updates
    dataMap.put("value", "beta");
    entityInputBean.setContent(new ContentInputBean(dataMap));
    entityInputBean.setKey(foundEntity.getResult().getKey());

    response = trackEntityPost.exec(entityInputBean);
    assertEquals("Track Entity - ", null, response.getError());
    integrationHelper.shortSleep();
    elResponse = entityLogsGet.exec(response.getResult().getKey());
    assertEquals("Expected two logs", 2, elResponse.getResult().length);

  }

  @Test
  public void suppressVersionsOnByDocBasis() throws Exception {
    integrationHelper.login();

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put("value", "alpha");
    String key = new Date().toString();
    EntityInputBean entityInputBean = new EntityInputBean()
        .setCode(key)
        .setFortress(new FortressInputBean("suppressVersionsOnByDocBasis")
            .setSearchEnabled(true)
            .setStoreEnabled(true)) // Enable the store
        .setDocumentType(new DocumentTypeInputBean("someThing")
            .setVersionStrategy(Document.VERSION.DISABLE)) // But suppress version history for this class of Entity
        .setContent(new ContentInputBean(dataMap));

    CommandResponse<EntityResultBean> response = entityGet.exec(entityInputBean, null);
    assertTrue("Expected an error. entity should not exist", response.getError() != null);

    CommandResponse<TrackRequestResult> trackResponse = trackEntityPost.exec(entityInputBean);
    assertEquals("Track Entity - ", null, trackResponse.getError());
    TrackRequestResult trackResult = trackResponse.getResult();
    assertThat(trackResult)
        .isNotNull()
        .hasFieldOrPropertyWithValue("newEntity", true)
        .hasFieldOrPropertyWithValue("logStatus", ContentInputBean.LogStatus.OK)
        .hasFieldOrProperty("key");

    integrationHelper.longSleep();// Wait for log to write to ES
    CommandResponse<EntityLogResult[]> logResponse = entityLogsGet.exec(trackResult.getKey());
    assertThat(logResponse)
        .hasFieldOrPropertyWithValue("error", null);
    assertThat(logResponse.getResult())
        .isNotNull()
        .hasSize(1);

    EntityLogResult mockedLog = logResponse.getResult()[0];
    assertTrue("Log was not flagged as mocked", mockedLog.isMocked());
    CommandResponse<EntityResultBean> entityResponse = entityGet.exec(null, trackResult.getKey());
    assertNotNull(entityResponse.getResult());
    integrationHelper.shortSleep(); // Waiting for the data to be stored in ES

    CommandResponse<Map<String, Object>> edResponse = entityData.exec(trackResult.getKey());
    if (edResponse.getResult() == null) {
      integrationHelper.shortSleep(); // Waiting for the data to be stored in ES
      edResponse = entityData.exec(trackResult.getKey());
    }
    assertNotNull(edResponse.getResult());
    assertFalse(edResponse.getResult().isEmpty());
    TestCase.assertEquals(dataMap.get("value"), edResponse.getResult().get("value"));


    edResponse = entityData.exec(entityInputBean);
    assertNull("Get Data by key = ", edResponse.getError());

    assertNotNull(edResponse.getResult());
    assertFalse(edResponse.getResult().isEmpty());
    TestCase.assertEquals(dataMap.get("value"), edResponse.getResult().get("value"));

    // Now test that the value updates
    dataMap.put("value", "beta");
    entityInputBean.setContent(new ContentInputBean(dataMap));

    //response = entityGet.exec(fdClientIo, null, trackResult.getKey());
    response = integrationHelper.waitForEntityKey(logger, "suppressVersionsOnByDocBasis", entityGet, null, trackResult.getKey());
    assertEquals("Find Entity by key - ", null, response.getError());

    CommandResponse<EntityResultBean> entityByCode = integrationHelper.waitForEntityKey(logger, "suppressVersionsOnByDocBasis", entityGet, entityInputBean, null);
    assertEquals("Find Entity by code - ", null, entityByCode.getError());

    entityInputBean.setKey(entityByCode.getResult().getKey());

    trackResponse = trackEntityPost.exec(entityInputBean);
    assertEquals("Track Entity - ", null, trackResponse.getError());
    integrationHelper.shortSleep();
    CommandResponse<Map<String, Object>> entityDataResponse = entityData.exec(entityByCode.getResult().getKey());
    assertEquals("Get Data", null, entityDataResponse.getError());

    assertNotNull(entityDataResponse.getResult());
    assertFalse(entityDataResponse.getResult().isEmpty());
    TestCase.assertEquals(dataMap.get("value"), entityDataResponse.getResult().get("value"));
  }

  private Collection<TagInputBean> getRandomTags() {
    int i = 0;
    int max = 20;

    Collection<TagInputBean> tags = new ArrayList<>();
    while (i < max) {
      TagInputBean tagInputBean = new TagInputBean("codea" + i, "Set" + i);
      tags.add(tagInputBean);

      i++;
    }
    return tags;
  }


//    private static JLineShellComponent shell;

  // Test the shell
//    @BeforeClass
//    public static void startUp() throws InterruptedException {
//        Bootstrap bootstrap = new Bootstrap();
//        shell = bootstrap.getJLineShellComponent();
//    }
//
//    @AfterClass
//    public static void shutdown() {
//        shell.stop();
//    }
//
//    public static JLineShellComponent getShell() {
//        return shell;
//    }
//
//    @Test
//    public void testShellPing () throws Exception {
//        CommandResult commandResult = getShell().executeCommand("ping");
//        assertNotNull(commandResult);
//        assertEquals("Didn't get a pong","pong", commandResult.getResult().toString());
//    }
}
