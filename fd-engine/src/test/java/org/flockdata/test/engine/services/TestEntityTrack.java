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

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import junit.framework.TestCase;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Log;
import org.flockdata.data.Segment;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.engine.track.service.TrackBatchSplitter;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.ObjectHelper;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.store.StoredContent;
import org.flockdata.test.engine.FdNodeHelper;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.LogDetailBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StopWatch;

/**
 * @author mholdsworth
 * @tag Test, Entity, Track
 * @since 15/04/2013
 */
public class TestEntityTrack extends EngineBase {

  @Autowired
  private
  TrackBatchSplitter batchSplitter;
  private Logger logger = LoggerFactory.getLogger(TestEntityTrack.class);

  /**
   * Most basic functionality. An entity is created and can be found
   *
   * @throws Exception error
   */
  @Test
  public void trackByCallerRef_NoContentNoUser() throws Exception {
    SystemUser su = registerSystemUser("trackByCallerRef_NoContentNoUser");
    FortressInputBean fib = new FortressInputBean("trackByCallerRef_NoContentNoUser", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

    EntityInputBean inputBean = new EntityInputBean(fortress, null, "TestTrack", new DateTime(), "ABC123");
    inputBean.setCode("ABC123");
    // This call expects the service layer to create the missing fortress from the entityInput
    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertNotNull(result);
    assertNotNull(result.getEntity().getKey());
    assertNotNull("Find by code failed", entityService.findByCode(su.getCompany(), fortress.getName(), inputBean.getDocumentType().getName(), inputBean.getCode()));
    assertNotNull("Find by key failed", entityService.getEntity(su.getCompany(), result.getKey()));
  }

  /**
   * Next most basic functionality. Create Entity connected to a FortressUser
   *
   * @throws Exception error
   */
  @Test
  public void trackByCallerRef_NoContent_WithFortressUser() throws Exception {
    SystemUser su = registerSystemUser("trackByCallerRef_NoLog");
    FortressInputBean fortressInput = new FortressInputBean("trackByCallerRef_NoLog", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fortressInput);

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");

    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertNotNull(result);
    assertNotNull(result.getEntity().getKey());
    assertNotNull("fortressUser should have been created by the trackEntity request", fortressService.getFortressUser(fortress, inputBean.getFortressUser()));
    Entity e = entityService.findByCode(su.getCompany(), fortressInput.getName(), inputBean.getDocumentType().getName(), inputBean.getCode());
    assertNotNull(e);
    assertNotNull("Locating an entity by code did not set the fortress", e.getSegment());
    assertNotNull("Did not find the Company in the Fortress", e.getSegment().getCompany());
    assertNotNull("Should have found an entity connected to a fortress user", e.getCreatedBy());

  }

  @Test
  public void docTypeFromInput() throws Exception {
    logger.debug("### docTypeFromInput");

    //Transaction t = beginManualTransaction();
    cleanUpGraph();
    SystemUser su = registerSystemUser("docTypeFromInput", mike_admin);
    assertNotNull(su);
    engineConfig.setConceptsEnabled(true);

    FortressInputBean fib = new FortressInputBean("docTypeFromInput", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

    Collection<DocumentResultBean> docs = conceptService.getDocumentsInUse(su.getCompany());
    assertEquals("DB has stray DocumentType objects lying around", 0, docs.size());

    DocumentTypeInputBean docTypeObject = new DocumentTypeInputBean("docTypeFromInput");
    docTypeObject.setTagStructure(EntityTag.TAG_STRUCTURE.TAXONOMY);

    EntityInputBean eib = new EntityInputBean(fortress, docTypeObject, "!123321!")
        .setFortress(fib)
        .setEntityOnly(true);


    TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), eib);
    assertNotNull(trackResult);
    EntityNode entity = entityService.getEntity(su.getCompany(), trackResult.getKey());
    assertNotNull(entity);
    assertEquals(docTypeObject.getName(), entity.getType());
    docs = conceptService.getDocumentsInUse(su.getCompany());
    assertEquals(1, docs.size());
    DocumentNode byName = conceptService.findDocumentType(fortress, docTypeObject.getName());
    assertNotNull(byName);
    assertEquals(EntityTag.TAG_STRUCTURE.TAXONOMY, byName.getTagStructure());
    //DocumentType dType = conceptService.resolveByDocCode(fortress, "ABC123", true);

  }

  /**
   * Create & Update user-defined Entity properties - no content
   *
   * @throws Exception error
   */
  @Test
  public void modified_UserDefinedProperties_NoContent() throws Exception {
    SystemUser su = registerSystemUser("DAT386", mike_admin);
    FortressInputBean fib = new FortressInputBean("DAT386", true);
    fib.setStoreEnabled(false);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), "12xx09");
    inputBean.setProperty("value", "H8CT04172");
    inputBean.setProperty("avg", ".123");
    // Test with no content
    //inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), Helper.getSimpleMap("name", "a")));
    TrackResultBean result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);
    EntityNode entity = entityService.getEntity(su.getCompany(), result.getKey());
    assertNotNull(entity);
    TestCase.assertEquals(2, entity.getProperties().size());
    assertEquals("Didn't find property key", "H8CT04172", entity.getProperties().get("value"));
    assertEquals(".123", entity.getProperties().get("avg"));

    inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), "12xx09");
    inputBean.setProperty("value", 200d);
    result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);
    entity = entityService.getEntity(su.getCompany(), result.getKey());
    assertNotNull(entity);
    TestCase.assertEquals("The Avg property should have been removed", 1, entity.getProperties().size());
    assertEquals("User-defined property did not change", 200d, entity.getProperties().get("value"));

  }

  /**
   * Start adding Content and locating Logs
   *
   * @throws Exception error
   */
  @Test
  public void track_WithSingleContentLog() throws Exception {
    logger.debug("### fortress_CreateOnTrack");
    engineConfig.setTestMode(true); // Force sync processing of the content and log

    String code = "fortress_CreateOnTrack";
    SystemUser su = registerSystemUser("fortress_CreateOnTrack");
    String fortressName = "fortress_CreateOnTrack";

    FortressInputBean fib = new FortressInputBean(fortressName).setSearchEnabled(false);
    Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), code);

    inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), ContentDataHelper.getSimpleMap("name", "a")));
    List<EntityInputBean> entityInputBeans = new ArrayList<>();
    entityInputBeans.add(inputBean);

    Collection<TrackRequestResult> results = mediationFacade.trackEntities(entityInputBeans, su.getApiKey());
    assertEquals(1, results.size());
    fortress = fortressService.getFortress(su.getCompany(), fortressName);
    assertNotNull(fortress);
    assertFalse(fortress.isSearchEnabled());
    EntityNode entity = entityService.getEntity(su.getCompany(), results.iterator().next().getKey());
    assertNotNull(entity);
    waitForFirstLog(su.getCompany(), entity);

    Collection<EntityLogResult> entityLogs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertNotNull(entityLogs);
    assertEquals("Didn't find the log for the entity", 1, entityLogs.size());
    // Validate that the LastChangeUser is in the Log
    for (EntityLogResult entityLog : entityLogs) {
      assertNotNull(entityLog.getMadeBy());
      assertEquals("poppy", entityLog.getMadeBy());
    }
  }

  /**
   * Track a single Entity with a Collection of contentInputBeans
   *
   * @throws Exception error
   */
  @Test
  public void makeEntity_MultipleIdenticalContentInput() throws Exception {
    logger.debug("### makeEntity_MultipleIdenticalContentInput");
    String code = "dcABC1";
    SystemUser su = registerSystemUser("makeEntity_MultipleIdenticalContentInput");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), code);


    inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), ContentDataHelper.getSimpleMap("name", "a")));
    List<EntityInputBean> entityInputBeans = new ArrayList<>();
    entityInputBeans.add(inputBean);

    inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), code);
    inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), ContentDataHelper.getSimpleMap("name", "a")));
    entityInputBeans.add(inputBean);

    inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), code);
    inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), ContentDataHelper.getSimpleMap("name", "a")));
    entityInputBeans.add(inputBean);

    mediationFacade.trackEntities(fortress, entityInputBeans, 1);
    Entity entity = entityService.findByCode(fortress, "CompanyNode", code);
    assertNotNull(entity);
    waitForFirstLog(su.getCompany(), entity);

    Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertNotNull(logs);
    assertEquals("3 Identical changes should result in a single log", 1, logs.size());
  }

  /**
   * Creates an entity without a log and then independently adds a couple of ContentInputBeans Logs
   * Some basic tests on Previous log checks
   *
   * @throws Exception error
   */
  @Test
  public void createEntity_ThenTrackLogs() throws Exception {
    SystemUser su = registerSystemUser("metaSummaryReturnsLogs", "metaSummaryReturnsLogs");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entitySummary", true));
    DateTime dt = new DateTime().toDateTime();
    DateTime firstDate = dt.minusDays(2);
    EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", firstDate, "ABC1");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();

    EntityNode entity = entityService.getEntity(su.getCompany(), key);
    TrackResultBean resultA = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), firstDate, ContentDataHelper.getSimpleMap("house", "house1")));
    TrackResultBean resultB = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("isabella@sunnybell.com", entity.getKey(), firstDate.plusDays(1), ContentDataHelper.getSimpleMap("house", "house2")));
    waitForLogCount(su.getCompany(), entity, 2);

    // Basic checks on previous logs
    assertEquals("Should be no previous log because it is the first", null, resultA.getCurrentLog().getLog().getPreviousLog());
    assertNotNull("Second Log should point to the first", resultB.getCurrentLog().getLog().getPreviousLog());

    assertEquals(resultA.getCurrentLog().getLog().getId(), resultB.getCurrentLog().getLog().getPreviousLog().getId());

    EntitySummaryBean entitySummary = entityService.getEntitySummary(su.getCompany(), key);
    assertNotNull(entitySummary);
    assertEquals(key, entitySummary.getEntity().getKey());
    assertNotNull(entitySummary.getEntity().getSegment());
    assertNotNull(entitySummary.getEntity().getCreatedBy());
    assertNotNull(entitySummary.getEntity().getLastUser());
    assertEquals(2, entitySummary.getChanges().size());
    for (EntityLogResult entityLog : entitySummary.getChanges()) {
      assertNotNull(entityLog.getLog());
      LogNode log = (LogNode) entityLog.getLog();
      assertNotNull(log.getEvent());
      StoredContent whatResult = entityService.getContent(entity, log);
      assertTrue(whatResult.getData().containsKey("house"));
    }
  }

  @Test
  public void logChangeWithNullKeyButCallerRefExists() throws Exception {
    SystemUser su = registerSystemUser("logChangeWithNullAuditKeyButCallerRefExists");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");
    assertNotNull(mediationFacade.trackEntity(su.getCompany(), inputBean));

    Entity entity = entityService.findByCode(su.getCompany(), fortress.getName(), inputBean.getDocumentType().getName(), inputBean.getCode());
    assertNotNull("Unable to locate entity by code", entity);

    ContentInputBean contentBean = new ContentInputBean("wally", new DateTime(), ContentDataHelper.getSimpleMap("blah", 1));
    contentBean.setFQC(fortress.getName(), "TestTrack", "ABC123");
    TrackResultBean input = mediationFacade.trackLog(su.getCompany(), contentBean);
    assertNotNull(input.getEntity().getKey());
    assertNotNull(entityService.findByCode(fortress, contentBean.getDocumentType(), contentBean.getCode()));
  }

  @Test
  public void nullKey() throws Exception {
    SystemUser su = registerSystemUser("nullKey");
    exception.expect(NotFoundException.class);
    entityService.getEntity(su.getCompany(), null);

  }

  @Test
  public void locatingByCallerRefWillThrowAuthorizationException() throws Exception {
    setSecurity();
    SystemUser su = registerSystemUser("locatingByCallerRefWillThrowAuthorizationException");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    // Check we can't create the same entity twice for a given client ref
    inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");
    String keyB = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    assertEquals(key, keyB);

    setSecurity(sally_admin); // Sally can register users
    SystemUser suB = registerSystemUser("TestTow", harry);
    setSecurity(harry); // Harry can access them
    FortressNode fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("auditTestB", true));
    mediationFacade.trackEntity(suB.getCompany(), new EntityInputBean(fortressB, "wally", "TestTrack", new DateTime(), "123ABC"));

    setSecurity(mike_admin);

    assertNotNull(entityService.findByCode(fortress, "TestTrack", "ABC123"));
    assertNull("Caller refs are case sensitive so this should not be found", entityService.findByCode(fortress, "TestTrack", "abc123"));
    assertNull("Security - shouldn't be able to see this entity", entityService.findByCode(fortress, "TestTrack", "123ABC"));

    setSecurity(harry);
    assertNull("User does not belong to this company.Fortress so should not be able to see it",
        entityService.findByCode(suB.getCompany(), fortress.getCode(), "TestTrack", "ABC123"));

    try {
      assertNull(entityService.getEntity(suB.getCompany(), key));
      fail("Security exception not thrown");

    } catch (SecurityException se) {
      logger.debug("Good stuff!");
    }
  }

  @Test
  public void createEntityTimeLogs() throws Exception {
    SystemUser su = registerSystemUser("createEntityTimeLogs");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("createEntityTimeLogs", true));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");
    Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
    String ahKey = entity.getKey();

    assertNotNull(ahKey);

    assertNotNull(entityService.getEntity(su.getCompany(), ahKey));
    assertNotNull(entityService.findByCode(fortress, "TestTrack", "ABC123"));
    assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
    assertNull(fortressService.getFortressUser(fortress, "wallyz", false));

    int i = 0;
    double max = 10d;
    StopWatch watch = new StopWatch();
    watch.start();
    while (i < max) {
      TrackResultBean subsequent = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", ahKey, new DateTime(), ContentDataHelper.getSimpleMap("blah", i)));

      if (i == 0) {
        waitForFirstLog(su.getCompany(), entity);
      } else {
        waitForLogCount(su.getCompany(), subsequent.getEntity(), i + 1);
      }
      i++;
    }
    watch.stop();
    logger.info(watch.prettyPrint() + " avg = " + (watch.getLastTaskTimeMillis() / 1000d) / max);

    assertEquals(max, (double) entityService.getLogCount(su.getCompany(), ahKey), 0);
  }

  /**
   * Idempotent data
   * Ensure duplicate logs are not created when content data has not changed
   */
  @Test
  public void noDuplicateLogsWithCompression() throws Exception {
    SystemUser su = registerSystemUser("noDuplicateLogsWithCompression");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "ndlwcqw2");
    Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();


    assertNotNull(entity);

    Map<String, Object> jsonA = ContentDataHelper.getSimpleMap("name", "8888");
    jsonA.put("thing", ContentDataHelper.getSimpleMap("m", "happy"));

    Map<String, Object> jsonB = ContentDataHelper.getSimpleMap("thing", ContentDataHelper.getSimpleMap("m", "happy"));
    jsonB.put("name", "8888");
    jsonA.put("thing", ContentDataHelper.getSimpleMap("m", "happy"));


    assertNotNull(entityService.getEntity(su.getCompany(), entity.getKey()));
    assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
    assertNull(fortressService.getFortressUser(fortress, "wallyz", false));
    int i = 0;
    double max = 10d;
    Map<String, Object> json;
    while (i < max) {
      // Same "what" text so should only be one auditLogCount record
      json = (i % 2 == 0 ? jsonA : jsonB);
      mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", entity.getKey(), new DateTime(), json));
      i++;
    }
    assertEquals(1d, (double) entityService.getLogCount(su.getCompany(), entity.getKey()), 0);
    Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), entity.getKey(), true);
    assertNotNull(logs);
    assertFalse(logs.isEmpty());
    assertEquals(1, logs.size());
    for (EntityLogResult entityLog : logs) {
      assertNotNull(entityLog.getData());
      assertFalse(entityLog.getData().isEmpty());
    }
  }

  /**
   * Idempotent data
   * Ensure duplicate logs are not created when content data has not changed
   */
  @Test
  public void orderedLogs() throws Exception {
    SystemUser su = registerSystemUser("orderedLogs");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "ndlwcqw2");
    inputBean.setContent(new ContentInputBean(ContentDataHelper.getSimpleMap("name", "8888")));
    Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

    assertNotNull(entity);

    inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "ndlwcqw2");
    inputBean.setContent(new ContentInputBean(ContentDataHelper.getSimpleMap("name", "0000")));
    mediationFacade.trackEntity(su.getCompany(), inputBean);

    Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), entity.getKey(), true);
    assertNotNull(logs);
    assertEquals(2, logs.size());
    boolean first = true;
    for (EntityLogResult entityLog : logs) {
      assertNotNull(entityLog.getData());
      assertFalse(entityLog.getData().isEmpty());

      if (first) {
        first = false;
        // Newest version is first
        assertEquals("0000", entityLog.getData().get("name"));
      } else {
        assertEquals("8888", entityLog.getData().get("name"));
      }
    }
  }

  @Test
  public void correctLogCountsReturnedForAFortress() throws Exception {
    SystemUser su = registerSystemUser("correctLogCountsReturnedForAFortress");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");
    TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
    String ahKey = resultBean.getEntity().getKey();

    assertNotNull(ahKey);
    assertNotNull(entityService.getEntity(su.getCompany(), ahKey));

    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", ahKey, new DateTime(), ContentDataHelper.getSimpleMap("blah", 0)));
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", ahKey, new DateTime(), ContentDataHelper.getSimpleMap("blah", 1)));
    assertEquals(2, entityService.getLogCount(su.getCompany(), resultBean.getEntity().getKey()));
  }

  @Test
  public void testEntityWithLogChange() throws Exception {
    SystemUser su = registerSystemUser("testEntityWithLogChange");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "uouu87");
    inputBean.setName("MikesNameTest");
    ContentInputBean logBean = new ContentInputBean("wally", null, DateTime.now(), ContentDataHelper.getSimpleMap("blah", 0));
    inputBean.setContent(logBean);
    TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertNotNull(resultBean);
    assertNotNull(resultBean.getEntity().getKey());
    assertEquals("MikesNameTest", resultBean.getEntity().getName());
    assertTrue(resultBean.getEntity().toString().contains(resultBean.getEntity().getKey()));
    assertEquals(1, entityService.getLogCount(su.getCompany(), resultBean.getEntity().getKey()));
  }

  @Test
  public void testEntityWithLogChangeTransactional() throws Exception {
    SystemUser su = registerSystemUser("testEntityWithLogChangeTransactional");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "232146");
    ContentInputBean logBean = new ContentInputBean("wally", null, DateTime.now(), ContentDataHelper.getSimpleMap("blah", 0));
    inputBean.setContent(logBean);
    TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertNotNull(resultBean);
    assertNotNull(resultBean.getEntity().getKey());
    assertEquals(1, entityService.getLogCount(su.getCompany(), resultBean.getEntity().getKey()));
  }

  @Test
  public void updateByCallerRefNoAuditKeyMultipleClients() throws Exception {
    setSecurity(mike_admin);
    // Registering the internal admin as a data access user
    SystemUser su = registerSystemUser("updateByCallerRefNoAuditKeyMultipleClients");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest" + System.currentTimeMillis(), true));
    String docType = "MultiClient";
    String callerRef = "ABC123X";
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", docType, new DateTime(), callerRef);
    String keyA = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    ContentInputBean alb = new ContentInputBean("logTest", new DateTime(), ContentDataHelper.getSimpleMap("blah", 0));
    alb.setFQC(fortress.getName(), docType, callerRef);
    //assertNotNull (alb);
    TrackResultBean arb = mediationFacade.trackLog(su.getCompany(), alb);
    assertNotNull(arb);
    assertEquals(keyA, arb.getEntity().getKey());

    // Scenario - create a new data access user
    setSecurity(sally_admin);
    SystemUser suB = registerSystemUser("TWEE", harry);
    // Switch to the data access user
    setSecurity(harry);
    FortressNode fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("auditTestB" + System.currentTimeMillis(), true));
    inputBean = new EntityInputBean(fortressB, "wally", docType, new DateTime(), callerRef);
    String keyB = mediationFacade.trackEntity(suB.getCompany(), inputBean).getEntity().getKey();

    alb = new ContentInputBean("logTest", new DateTime(), ContentDataHelper.getSimpleMap("blah", 0));
    alb.setFQC(fortressB.getName(), docType, callerRef);
    arb = mediationFacade.trackLog(suB.getCompany(), alb);
    assertNotNull(arb);
    assertEquals("This caller should not see KeyA", keyB, arb.getEntity().getKey());

  }

  @Test
  public void companyAndFortressWithSpaces() throws Exception {
    logger.debug("## companyAndFortressWithSpaces");
    SystemUser su = registerSystemUser("companyAndFortressWithSpaces", mike_admin);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("track Test" + System.currentTimeMillis(), true));
    String docType = "companyAndFortressWithSpaces";
    String callerRef = "ABC123X";
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", docType, new DateTime(), callerRef);
    String keyA = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    assertNotNull(keyA);
  }

  @Test
  public void entitysForDifferentCompaniesAreNotVisible() throws Exception {
    logger.debug("## entitysForDifferentCompaniesAreNotVisible");
    SystemUser su = registerSystemUser("entitysForDifferentCompaniesAreNotVisible");
    String hummingbird = "Hummingbird";

    Authentication authMike = setSecurity(mike_admin);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    EntityInputBean entityInputBean = new EntityInputBean(fortress, "wally", "CompanyNode", new DateTime(), "AHWP");
    String key = mediationFacade.trackEntity(su.getCompany(), entityInputBean).getEntity().getKey();
    assertNotNull(key);
    assertNotNull(entityService.getEntity(su.getCompany(), key));

    //Hummingbird/Gina
    setSecurity(sally_admin);
    SystemUser suB = registerSystemUser(hummingbird, harry);
    Authentication authHarry = setSecurity(harry); // Harry can create data
    FortressNode fortHS = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("honeysuckle", true));
    entityInputBean = new EntityInputBean(fortHS, "harry", "CompanyNode", new DateTime(), "AHHS");
    String ahHS = mediationFacade.trackEntity(suB.getCompany(), entityInputBean).getEntity().getKey();

    assertNotNull(fortressService.getFortressUser(fortress, "wally", true));
    assertNotNull(fortressService.getFortressUser(fortHS, "harry", true));
    assertNull(fortressService.getFortressUser(fortress, "wallyz", false));

    double max = 2000d;
    StopWatch watch = new StopWatch();
    watch.start();

    createLogRecords(authMike, su, key, 20);
    createLogRecords(authHarry, suB, ahHS, 40);
    watch.stop();
    logger.info(watch.prettyPrint() + " avg = " + (watch.getLastTaskTimeMillis() / 1000d) / max);


  }

  @Test
  public void lastChangedWorks() throws Exception {
    logger.debug("## lastChangedWorks");
    SystemUser su = registerSystemUser("lastChangedWorks");
    // Create a second log record in order to workout who last change the EntityNode

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fwportfolio", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "CompanyNode", new DateTime(), "ZZZZ");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    EntityNode trackKey = entityService.getEntity(su.getCompany(), key);
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", trackKey.getKey(), new DateTime(), ContentDataHelper.getSimpleMap("house", "house1"), "Update"));
    trackKey = entityService.getEntity(su.getCompany(), key);
    FortressUserNode fu = fortressService.getUser(trackKey.getLastUser().getId());
    assertEquals("olivia@sunnybell.com", fu.getCode());

    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("harry@sunnybell.com", trackKey.getKey(), new DateTime(), ContentDataHelper.getSimpleMap("house", "house2"), "Update"));
    trackKey = entityService.getEntity(su.getCompany(), key);

    fu = fortressService.getUser(trackKey.getLastUser().getId());
    assertEquals("harry@sunnybell.com", fu.getCode());

    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", trackKey.getKey(), new DateTime(), ContentDataHelper.getSimpleMap("house", "house3"), "Update"));
    trackKey = entityService.getEntity(su.getCompany(), key);

    fu = fortressService.getUser(trackKey.getLastUser().getId());
    assertEquals("olivia@sunnybell.com", fu.getCode());

  }

  @Test
  public void outOfSequenceLogsWorking() throws Exception {
    SystemUser su = registerSystemUser("outOfSequenceLogsWorking");
    DateTime dt = new DateTime().toDateTime();
    DateTime earlyDate = dt.minusDays(2);

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "CompanyNode", new DateTime(), "ZZZZ");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    EntityNode entity = entityService.getEntity(su.getCompany(), key);

    // Create the future one first.
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), new DateTime(), ContentDataHelper.getSimpleMap("house", "house1"), "Update"));
    entity = entityService.getEntity(su.getCompany(), key);
    FortressUserNode fu = fortressService.getUser(entity.getLastUser().getId());
    assertEquals("olivia@sunnybell.com", fu.getCode());
    org.flockdata.data.EntityLog compareLog = logService.getLastLog(entity);

    // Load a historic record. This should not become "last"
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("harry@sunnybell.com", entity.getKey(), earlyDate, ContentDataHelper.getSimpleMap("house", "house2"), "Update"));
    entity = entityService.getEntity(su.getCompany(), key);

    org.flockdata.data.EntityLog lastLog = logService.getLastLog(entity);
    assertNotNull(lastLog);
    assertEquals(compareLog.getId(), lastLog.getId());

    fu = fortressService.getUser(entity.getLastUser().getId());
    assertEquals("olivia@sunnybell.com", fu.getCode()); // The first one we created is the "last one"


  }

  /**
   * test that we find the correct number of changes between a range of dates for a given entity
   */
  @Test
  public void logDateRangesWorking() throws Exception {
    SystemUser su = registerSystemUser("logDateRangesWorking");

    int max = 10;
    FortressNode fortress = fortressService.registerFortress(su.getCompany(),
        new FortressInputBean("wportfolio")
            .setSearchEnabled(false)
            .setStoreEnabled(true));

    DateTime dt = new DateTime().toDateTime();
    DateTime firstDate = dt.minusDays(max);
    DateTime workingDate = firstDate.toDateTime();

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "CompanyNode", firstDate, "123");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    EntityNode entity = entityService.getEntity(su.getCompany(), key);
    int i = 0;
    while (i < max) {
      workingDate = workingDate.plusDays(1);
      TrackResultBean tr = mediationFacade.trackLog(
          su.getCompany(),
          new ContentInputBean("olivia@sunnybell.com", entity.getKey(), workingDate, ContentDataHelper.getSimpleMap("house", "house" + i))
      );

      assertEquals("Loop count " + i, ContentInputBean.LogStatus.OK, tr.getLogStatus());
      i++;
    }

    Collection<EntityLogResult> aLogs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertEquals(max, aLogs.size());

    EntityLog lastLog = entityService.getLastEntityLog(su.getCompany(), entity.getKey());
    Log lastChange = lastLog.getLog();
    assertNotNull(lastChange);
    assertEquals(workingDate, new DateTime(lastLog.getFortressWhen()));
    entity = entityService.getEntity(su.getCompany(), key);
    assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getKey()));

    DateTime then = workingDate.minusDays(4);
    logger.debug("Searching between " + then.toDate() + " and " + workingDate.toDate());
    Collection<EntityLog> logs = entityService.getEntityLogs(su.getCompany(), entity.getKey(), then.toDate(), workingDate.toDate());
    assertEquals(5, logs.size());
    Long logId = logs.iterator().next().getId();
    LogDetailBean change = entityService.getFullDetail(su.getCompany(), entity.getKey(), logId);
    assertNotNull(change);
    assertNotNull(change.getLog());
    assertNotNull(change.getWhat());
    assertEquals(logId, change.getLog().getId());


  }

  @Test
  public void cancel_LastLogBehaves() throws Exception {
    // For use in compensating transaction cases only
    // DAT-53
    SystemUser su = registerSystemUser("cancelLastChangeBehaves");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    DateTime dt = new DateTime().toDateTime();
    DateTime firstDate = dt.minusDays(2);
    EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();

    EntityNode entity = entityService.getEntity(su.getCompany(), key);
    ContentInputBean contentA = new ContentInputBean("olivia@sunnybell.com", entity.getKey(), firstDate, ContentDataHelper.getSimpleMap("house", "house1"));
    org.flockdata.data.EntityLog firstLog = mediationFacade.trackLog(su.getCompany(), contentA).getCurrentLog();
    assertEquals("Incorrect user against the log", contentA.getFortressUser(), firstLog.getLog().getMadeBy().getCode());

    FortressUserNode fu = fortressService.getFortressUser(fortress, firstLog.getLog().getMadeBy().getCode(), false);
    assertNotNull("FortressUser was not created", fu);

    ContentInputBean contentB = new ContentInputBean("isabella@sunnybell.com", entity.getKey(), firstDate.plusDays(1), ContentDataHelper.getSimpleMap("house", "house2"));
    org.flockdata.data.EntityLog secondLog = mediationFacade.trackLog(su.getCompany(), contentB).getCurrentLog();
    assertEquals("Incorrect user against the log", contentB.getFortressUser(), secondLog.getLog().getMadeBy().getCode());

    FortressUserNode fuB = fortressService.getFortressUser(fortress, secondLog.getLog().getMadeBy().getCode(), false);
    assertNotNull("FortressUser was not created", fuB);

    assertNotSame(0L, firstLog.getFortressWhen());
    assertNotSame(0L, secondLog.getFortressWhen());
    Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertEquals("Expected two EntityLogs", 2, logs.size());

    entity = entityService.getEntity(su.getCompany(), key);
    compareUser("Current user not set to that of the second log", entity, secondLog.getLog().getMadeBy().getName());
    assertEquals("fortressWhen does not reconcile between the Entity and the EntityLog", secondLog.getFortressWhen().longValue(), entity.getFortressUpdatedTz().getMillis());


    // ToDo: why an ESC and not a TRB?
    entityService.cancelLastLog(su.getCompany(), entity);
    //assertNotNull(searchChange);
    //assertEquals(secondLog.getLog().getMadeBy().getName(), searchChange.getWho());

    logs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertEquals("Only 1 EntityLog should remain", 1, logs.size());

    entity = entityService.getEntity(su.getCompany(), key, true); // Refresh the entity
    compareUser("Previous user was not set back to Olivia", entity, fu.getName());
    assertEquals(firstLog.getFortressWhen().longValue(), entity.getFortressUpdatedTz().getMillis());

    // Last change cancelled
    entityService.cancelLastLog(su.getCompany(), entity);
    logs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertTrue(logs.isEmpty());
  }

  @Test
  public void lastChangeDatesReconcileWithFortressInput() throws Exception {
    SystemUser su = registerSystemUser("lastChangeDatesReconcileWithFortressInput");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC1");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();

    EntityNode entity = entityService.getEntity(su.getCompany(), key);
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), new DateTime(), ContentDataHelper.getSimpleMap("house", "house1")));
    entity = entityService.getEntity(su.getCompany(), key); // Inflate the entity on the server
    org.flockdata.data.EntityLog lastLog = entityService.getLastEntityLog(su.getCompany(), entity.getKey());
    assertNotNull(lastLog);
    StoredContent content = entityService.getContent(entity, (LogNode) lastLog.getLog());
    assertNotNull(content);
    assertTrue(content.getData().containsKey("house"));
  }

  @Test
  public void dateCreatedAndLastUpdated() throws Exception {
    SystemUser su = registerSystemUser("dateCreatedAndLastUpdated");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    DateTime fortressDateCreated = DateTime.now();
    Thread.sleep(500);
    DateTime logTime;
    EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", fortressDateCreated, "dcalbu");
    ContentInputBean contentInputBean = new ContentInputBean(mike_admin, fortressDateCreated, ContentDataHelper.getSimpleMap("abx", "1"));
    // Time will come from the Log
    inputBean.setContent(contentInputBean);
    TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
    String key = trackResultBean.getEntity().getKey();

    assertEquals(fortressDateCreated.getMillis(), trackResultBean.getEntity().getFortressCreatedTz().getMillis());

    // Creating the 2nd log will advance the last modified time
    logTime = DateTime.now();
    contentInputBean = new ContentInputBean(mike_admin, key, logTime, ContentDataHelper.getSimpleMap("abx", "2"));
    mediationFacade.trackLog(su.getCompany(), contentInputBean);

    EntityLog log = entityService.getLastEntityLog(su.getCompany(), key);
    assertEquals("Fortress modification date&time do not match", log.getFortressWhen().longValue(), logTime.getMillis());
    EntityNode entity = entityService.getEntity(su.getCompany(), key);
    assertEquals(fortressDateCreated.getMillis(), entity.getFortressCreatedTz().getMillis());
    assertEquals("Fortress log time doesn't match", logTime.getMillis(), log.getFortressWhen().longValue());

  }

  @Test
  public void missingLogDateGeneratesSystemDate() throws Exception {
    SystemUser su = registerSystemUser("missingLogDateGeneratesSystemDate");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    DateTime dt = new DateTime().toDateTime();
    EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", dt, "mldgsd99");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();
    EntityNode entity = entityService.getEntity(su.getCompany(), key);

    // Check that TimeZone information is used to correctly establish Now when not passed in a log
    // No Date, so default to NOW in the Fortress Timezone
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), null, ContentDataHelper.getSimpleMap("house", "house1")));

    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), null, ContentDataHelper.getSimpleMap("house", "house2")));

    Collection<org.flockdata.data.EntityLog> logs = entityService.getEntityLogs(entity);
    assertEquals("Logs with missing dates not correctly recorded", 2, logs.size());

    // Can only have one log for an entity at a point in time. Passing in the same date would cause the last log to be rejected
    // so we remove a day from this entry
    DateTime dateMidnight = new DateTime();
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), dateMidnight.toDateTime().minusDays(1), ContentDataHelper.getSimpleMap("house", "house3")));
    org.flockdata.data.EntityLog thirdLog = entityService.getLastEntityLog(su.getCompany(), key);

    // This is being inserted after the last log
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getKey(), dateMidnight.toDateTime(), ContentDataHelper.getSimpleMap("house", "house4")));
    logs = entityService.getEntityLogs(entity);
    assertEquals(4, logs.size());
    if (logger.isDebugEnabled()) {
      for (org.flockdata.data.EntityLog next : logs) {
        logger.debug(next.getId() + " - " + new Date(next.getSysWhen()).toString());
      }
    }
    org.flockdata.data.EntityLog lastLog = entityService.getLastEntityLog(su.getCompany(), key);
    assertNotSame("Last log in should be the last", lastLog.getLog().getId(), thirdLog.getLog().getId());

  }

  @Test
  public void fullEntityDetailsByCallerRef() throws Exception {
    SystemUser su = registerSystemUser("fullEntityDetailsByCallerRef");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "ABC1");
    String key = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getKey();

    Entity entity = entityService.findByCodeFull(fortress, "CompanyNode", "ABC1");
    assertNotNull(entity);
    // DAT-278
    assertNotNull(entity.getType());
    assertEquals(inputBean.getDocumentType().getName(), entity.getType());
    assertEquals(key, entity.getKey());
  }

  @Test
  public void testFortressTimeBoundaries() throws Exception {
    SystemUser su = registerSystemUser("testFortressTimeBoundaries");

    FortressInputBean usFortress = new FortressInputBean("usFortress", true);
    usFortress.setTimeZone(TimeZone.getTimeZone("GMT").getID());
    FortressNode fortressGMT = fortressService.registerFortress(su.getCompany(), usFortress);
    assertEquals(TimeZone.getTimeZone("GMT").getID(), fortressGMT.getTimeZone());

    FortressInputBean astFortress = new FortressInputBean("astFortress", true);
    astFortress.setTimeZone(TimeZone.getTimeZone("AST").getID());
    FortressNode fortressAST = fortressService.registerFortress(su.getCompany(), astFortress);
    assertEquals(TimeZone.getTimeZone("AST").getID(), fortressAST.getTimeZone());

    EntityInputBean astAuditBean = new EntityInputBean(fortressGMT, "olivia@ast.com", "CompanyNode", null, "ABC1");
    EntityInputBean gmtAuditBean = new EntityInputBean(fortressAST, "olivia@gmt.com", "CompanyNode", null, "ABC1");
    String result = mediationFacade.trackEntity(su.getCompany(), astAuditBean).getEntity().getKey();
    EntityNode entity = entityService.getEntity(su.getCompany(), result);
    DateTime astTime = new DateTime(entity.getFortressCreatedTz());

    result = mediationFacade.trackEntity(su.getCompany(), gmtAuditBean).getEntity().getKey();
    entity = entityService.getEntity(su.getCompany(), result);
    DateTime gmtTime = new DateTime(entity.getFortressCreatedTz());

    assertNotSame(astTime.getHourOfDay(), gmtTime.getHourOfDay());

  }

  @Test
  public void entitiesByFortressAndDocType() throws Exception {
    SystemUser su = registerSystemUser("entitiesByFortressAndDocType");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
    assertNotNull(fortress);

    String typeA = "TypeA";
    String typeB = "Type B";

    mediationFacade.trackEntity(su.getCompany(), new EntityInputBean(fortress, "auditTest", typeA, new DateTime(), "abc"));
    mediationFacade.trackEntity(su.getCompany(), new EntityInputBean(fortress, "auditTest", typeA, new DateTime(), "abd"));
    mediationFacade.trackEntity(su.getCompany(), new EntityInputBean(fortress, "auditTest", typeB, new DateTime(), "abc"));

    assertEquals("3 Entities should have been created for the fortress", 3, entityService.getEntities(fortress, 0L).size());
    assertEquals("Should be 2 for the DocType " + typeA, 2, entityService.getEntities(fortress, typeA, 0L).size());
    assertEquals("Case sensitivity failed", 2, entityService.getEntities(fortress, "typea", 0L).size());
    assertEquals(1, entityService.getEntities(fortress, typeB, 0L).size());
    assertEquals("Case sensitivity failed", 1, entityService.getEntities(fortress, "type b", 0L).size());
  }

  @Test
  public void findEntitiesForCollectionOfKeys() throws Exception {
    SystemUser su = registerSystemUser("findEntitiesForCollectionOfKeys", "findEntitiesForCollectionOfKeys");

    FortressNode fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
    assertNotNull(fortressA);

    String typeA = "TypeA";
    String typeB = "Type B";

    TrackResultBean ra = mediationFacade.trackEntity(su.getCompany(), new EntityInputBean(fortressA, "auditTest", typeA, new DateTime(), "aba"));
    TrackResultBean rb = mediationFacade.trackEntity(su.getCompany(), new EntityInputBean(fortressA, "auditTest", typeA, new DateTime(), "abb"));
    TrackResultBean rc = mediationFacade.trackEntity(su.getCompany(), new EntityInputBean(fortressA, "auditTest", typeB, new DateTime(), "abc"));

    setSecurity(sally_admin);
    SystemUser suB = registerSystemUser("other company", harry);
    setSecurity(harry); // Harry can create data
    FortressNode fortressB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("XYZ", true));
    TrackResultBean validButNotForCallerA = mediationFacade.trackEntity(suB.getCompany(), new EntityInputBean(fortressB, "auditTest", typeB, new DateTime(), "abc"));
    Collection<String> toFind = new ArrayList<>();
    setSecurity(mike_admin);
    toFind.add(ra.getEntity().getKey());
    toFind.add(rb.getEntity().getKey());
    toFind.add(rc.getEntity().getKey());
    toFind.add(validButNotForCallerA.getEntity().getKey());

    Collection<EntityNode> foundEntitys = entityService.getEntities(su.getCompany(), toFind).values();
    assertEquals("Caller was authorised to find 3 entities", 3, foundEntitys.size());

    foundEntitys = entityService.getEntities(suB.getCompany(), toFind).values();
    assertEquals("Caller was only authorised to find 1 entity", 1, foundEntitys.size());

  }

  @Test
  public void utf8Strings() throws Exception {
    Map<String, Object> json = ContentDataHelper.getSimpleMap("Athlete", "Katerina Neumannová");
    SystemUser su = registerSystemUser("utf8Strings", "utf8Strings");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("wportfolio", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), "ABC1");
    inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), json));

    // Validate that the bytes serialize via plain old Json mapper
    byte[] bytes = JsonUtils.getMapper().writeValueAsBytes(inputBean);
    EntityInputBean temp = JsonUtils.getMapper().readValue(bytes, EntityInputBean.class);
    assertEquals(inputBean.getContent().getData().get("Athlete").toString(), temp.getContent().getData().get("Athlete").toString());


    ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(
        new Jackson2JsonObjectMapper(JsonUtils.getMapper())
    );

    Message<EntityInputBean> msg = new GenericMessage<>(temp);
    Message<?> transformed = transformer.transform(msg);
    assertNotNull("Msg couldn't transform via spring mechanism", transformed);

    TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
    waitForFirstLog(su.getCompany(), trackResultBean.getEntity());
    org.flockdata.data.EntityLog lastLog = logService.getLastLog(trackResultBean.getEntity());

    StoredContent content = storageService.read(trackResultBean.getEntity(), lastLog.getLog());
    assertEquals(json.get("Athlete"), content.getData().get("Athlete"));

    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertNotNull(result);
    assertEquals("Second call determine that nothing has changed", ContentInputBean.LogStatus.IGNORE, result.getLogStatus());
  }

  @Test
  public void lastLog_CorrectlySequencesInSeparateCallsViaBatchLoad() throws Exception {
    SystemUser su = registerSystemUser("lastLog_CorrectlySequencesInSeparateCallsViaBatchLoad");
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityEntityDiff", true));
    assertFalse(fortress.isSearchEnabled());
    String callerRef = UUID.randomUUID().toString();
    List<EntityInputBean> inputBeans = new ArrayList<>();

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), callerRef);
    ContentInputBean contentInputBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getSimpleMap("col", 123));
    inputBean.setContent(contentInputBean);
    inputBeans.add(inputBean);
    logger.debug("** First Track Event");
    Collection<TrackResultBean> results = mediationFacade.trackEntities(fortress, inputBeans, 10);
    Entity entity = results.iterator().next().getEntity();
    waitForFirstLog(su.getCompany(), entity);

    inputBeans.clear();

    entity = entityService.findByCode(fortress, "TestTrack", callerRef);
    assertNotNull(entity);

    // Now we record a change
    contentInputBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getSimpleMap("col", 321));
    inputBean.setContent(contentInputBean);
    inputBeans = new ArrayList<>();
    inputBeans.add(inputBean);
    logger.debug("** Second Track Event - creating {} entities. Current count = {}", inputBeans.size(), entityService.getLogCount(su.getCompany(), entity.getKey()));

    mediationFacade.trackEntities(fortress, inputBeans, 1);
    logger.debug("Current count now at {}", entityService.getLogCount(su.getCompany(), entity.getKey()));

    waitForLogCount(su.getCompany(), entity, 2);
    entity = entityService.findByCode(fortress, "TestTrack", callerRef);
    org.flockdata.data.EntityLog lastLog = entityService.getLastEntityLog(su.getCompany(), entity.getKey());
    assertNotNull(lastLog);
    StoredContent what = storageService.read(entity, lastLog.getLog());

    assertNotNull(what);
    Object value = what.getData().get("col");
    assertNotNull(value);
    assertEquals("321", value.toString());
  }

  @Test
  public void datesInEntitysAndLogs() throws Exception {
    SystemUser su = registerSystemUser("datesInEntitysAndLogs", "datesInEntitysAndLogs");
    FortressInputBean f = new FortressInputBean("dateFun", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), f);

    DateTime past = new DateTime(2010, 10, 1, 11, 35);

    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", past, "ABC1");
    inputBean.setContent(new ContentInputBean("poppy", past, ContentDataHelper.getSimpleMap("name", "value")));
    TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
    Entity entityBean = trackResultBean.getEntity();
    waitForFirstLog(su.getCompany(), entityBean);
    EntityLog lastLog = logService.getLastLog(entityBean);
    assertEquals(past.getMillis(), lastLog.getFortressWhen().longValue());
    assertEquals(past.getMillis(), entityBean.getFortressCreatedTz().getMillis());
    assertEquals("Created " + entityBean.getFortressCreatedTz(),
        past.getMillis(), entityBean.getFortressCreatedTz().toDate().getTime());

  }

  @Test
  public void date_FortressDateFields() throws Exception {
    // DAT-196
    logger.debug("## utcDateFields");
    SystemUser su = registerSystemUser("abFortressDateFields", "userDatesFields");
    FortressInputBean fib = new FortressInputBean("utcDateFields", true);
    String timeZone = "Europe/Copenhagen"; // Arbitrary TZ
    fib.setTimeZone(timeZone);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
    DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone));

    DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4, 30, tz);
    DateTime lastUpdated = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone)));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", fortressDateCreated, "ABC123");
    assertEquals("MetaInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
    inputBean.setContent(new ContentInputBean("wally", lastUpdated, ContentDataHelper.getRandomMap()));

    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking

    Entity entity = result.getEntity();
    assertEquals(indexManager.getPrefix() + su.getCompany().getCode() + "." + fortress.getCode(), fortress.getRootIndex());
    assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(entity.getFortressCreatedTz()));

    EntityLog log = entityService.getLastEntityLog(su.getCompany(), result.getEntity().getKey());
    assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
  }

  @Test
  public void clientInDifferentTZ() throws Exception {
    // DAT-196
    logger.debug("## clientInDifferentTZ");
    SystemUser su = registerSystemUser("clientInDifferentTZ", "clienttz");
    FortressInputBean fib = new FortressInputBean("clientInDifferentTZ", true);
    String fortressTz = "Europe/Copenhagen"; // Arbitrary TZ
    fib.setTimeZone(fortressTz);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
    logger.debug("Fortress obtained - SU Company {}, Fortress Company {}", su.getCompany(), fortress.getCompany());

    DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortressTz));

    // No timezone is specifically created and the client is in a different country
    //  and sending data to the server.
    DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4, 30);// This should get converted to the fortress TZ, not left in the clients
    DateTime expectedCreateDate = new DateTime(fortressDateCreated, tz); // The date we are expecting
    DateTime lastUpdated = new DateTime();// In the clients TZ. Needs to be treated as if in the fortress TZ

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TZTest", fortressDateCreated, "ABC123");
    assertEquals("EntityInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
    inputBean.setContent(new ContentInputBean("wally", lastUpdated, ContentDataHelper.getRandomMap()));

    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);

    EntityNode entity = entityService.getEntity(su.getCompany(), result.getEntity().getKey());
    logger.debug("***  problem {}", entity.toString());
    logger.debug("**** Fortress {}, Company {}, Entity Fortress {}", entity.getSegment(), entity.getSegment().getCompany(), result.getEntity().getSegment());
    assertEquals("Why is this failing", indexManager.getPrefix() + su.getCompany().getCode() + "." + fortress.getCode(), fortress.getRootIndex());
    assertEquals("DateCreated not in Fortress TZ", 0, expectedCreateDate.compareTo(entity.getFortressCreatedTz()));

    EntityLog log = entityService.getLastEntityLog(su.getCompany(), result.getEntity().getKey());
    assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
  }

  @Test
  public void defaultFortressTZWhenNoneExists() throws Exception {
    // DAT-196
    logger.debug("## defaultFortressTZWhenNoneExists");
    SystemUser su = registerSystemUser("defaultFortressTZWhenNoneExists", "defaultFortressTZWhenNoneExists");

    String fortressTz = "Europe/Copenhagen"; // Arbitrary TZ

    DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortressTz));

    // No timezone is specifically created and the client is in a different country
    //  and sending data to the server.
    DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4, 30);// This should get converted to the fortress TZ, not left in the clients
    DateTime expectedCreateDate = new DateTime(fortressDateCreated, tz); // The date we are expecting
    DateTime lastUpdated = new DateTime();// In the clients TZ. Needs to be treated as if in the fortress TZ

    FortressInputBean fortressBean = new FortressInputBean("clientInDifferentTZ", true);
    fortressBean.setTimeZone(fortressTz);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fortressBean);

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", fortressDateCreated);

    assertEquals("MetaInputBean mutated the date", 0, fortressDateCreated.toDate().compareTo(inputBean.getWhen()));
    inputBean.setContent(new ContentInputBean("wally", lastUpdated, ContentDataHelper.getRandomMap()));

    TrackResultBean result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean); // Mock result as we're not tracking

    fortress = fortressService.findByName(su.getCompany(), fortressBean.getName());
    assertNotNull(fortress);
    assertEquals(fortressTz, fortress.getTimeZone());

    EntityNode entity = entityService.getEntity(su.getCompany(), result.getEntity().getKey());
    assertEquals("DateCreated not in Fortress TZ", 0, expectedCreateDate.compareTo(entity.getFortressCreatedTz()));

    EntityLog log = entityService.getLastEntityLog(su.getCompany(), result.getEntity().getKey());
    assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(tz)));
  }

  @Test
  public void event_NullWhenMetaOnlyIsFalse() throws Exception {
    // DAT-276
    logger.debug("## event_NullWhenMetaOnlyIsFalse");
    SystemUser su = registerSystemUser("event_NullWhenMetaOnlyIsFalse", "defUser");

    FortressInputBean fortressBean = new FortressInputBean("event_MetaOnlyRecordsOtherwiseNull", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fortressBean);

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime());

    inputBean.setContent(new ContentInputBean("wally", new DateTime(), ContentDataHelper.getRandomMap()));

    TrackResultBean result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean); // Mock result as we're not tracking
    assertNull(result.getEntity().getEvent());

  }

  @Test
  public void event_NotNullWhenMetaOnlyIsTrue() throws Exception {
    // DAT-276
    logger.debug("## event_NotNullWhenMetaOnlyIsTrue");
    SystemUser su = registerSystemUser("event_NotNullWhenMetaOnlyIsTrue", "defUser");

    FortressInputBean fortressBean = new FortressInputBean("event_NotNullWhenMetaOnlyIsTrue", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fortressBean);

    EntityInputBean inputBean =
        new EntityInputBean(fortress, "wally", "TestTrack", new DateTime())
            .setEvent("Create");

    inputBean.setEntityOnly(true);
    TrackResultBean result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean); // Mock result as we're not tracking
    assertNotNull("Event should not be null for metaOnly==true", result.getEntity().getEvent());

  }

  @Test
  public void event_Serializable() throws Exception {
    // DAT-276
    Entity entity = FdNodeHelper.getEntity("lba", "abc", "asdf", "asdf");
    TrackResultBean trackResultBean = new TrackResultBean(entity, new DocumentNode(entity.getType()));
    trackResultBean.addServiceMessage("Blah");
    byte[] bytes = ObjectHelper.serialize(new TrackRequestResult(trackResultBean));
    assertNotNull(bytes);
  }

  @Test
  public void dates_SameDateTwoChanges() throws Exception {
    logger.debug("## dates_SameDateTwoChanges");
    SystemUser su = registerSystemUser("dates_SameDateTwoChanges", "user");

    FortressInputBean fortressBean = new FortressInputBean("dates_SameDateTwoChanges", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fortressBean);

    String created = "2010-11-20 11:30:00"; // Create
    String fUpdate = "2010-11-21 11:45:00"; // First Update

    DateTime createDate = new DateTime(Timestamp.valueOf(created));
    DateTime updateDate = new DateTime(Timestamp.valueOf(fUpdate));

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", createDate);
    ContentInputBean cib = new ContentInputBean(ContentDataHelper.getSimpleMap("key", 1));
    cib.setWhen(updateDate.toDate());
    inputBean.setContent(cib);

    TrackResultBean result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);
    assertEquals(1, entityService.getLogCount(su.getCompany(), result.getKey()));

    // Same date, but different content - should create a new log
    cib = new ContentInputBean(ContentDataHelper.getSimpleMap("key", 2));
    cib.setWhen(updateDate.toDate());
    inputBean.setContent(cib);

    result = mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);
    assertEquals(1, entityService.getLogCount(su.getCompany(), result.getKey()));
    EntityLog log = logService.getLastLog(result.getEntity());
    assertEquals(Long.valueOf(updateDate.getMillis()), log.getFortressWhen());
    StoredContent storedContent = storageService.read(result.getEntity(), log.getLog());
    assertNotNull(storedContent);
    Object value = storedContent.getData().get("key");
    assertNotNull(value);
    assertEquals(2, Integer.parseInt(value.toString()));

  }

  @Test
  public void trackByCallerCode_FortressUserInEntityButNotLog() throws Exception {
    SystemUser su = registerSystemUser("trackByCallerRef_FortressUserInEntityButNotLog");
    //Fortress fortress = fortressService.registerFortress("auditTest");
    FortressInputBean fib = new FortressInputBean("FortressUserInEntityButNotLog", true);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "TestTrack", new DateTime(), "ABC123");
    ContentInputBean aib = new ContentInputBean("wally", new DateTime(), ContentDataHelper.getSimpleMap("blah", 1));
    aib.setFortressUser(null); // We want AB to extract this from the entity
    aib.setFQC(fortress.getName(), "TestTrack", "ABC123");
    inputBean.setContent(aib);
    // This call expects the service layer to create the missing fortress from the entityInput
    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertNotNull(result);
    assertNotNull(result.getEntity().getKey());
    assertNotNull(entityService.findByCode(su.getCompany(), fortress.getName(), aib.getDocumentType(), aib.getCode()));
  }

  @Test
  public void split_BatchByFortressNoSegment() throws Exception {
    SystemUser su = registerSystemUser("split_BatchByFortressNoSegment");

    //Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("split_BatchByFortress", true));
    String name = "Space Fortress";

    Collection<EntityInputBean> entities = new ArrayList<>();
    EntityInputBean entity = new EntityInputBean(new FortressInputBean(name), new DocumentTypeInputBean("Census"));
    entity.setDescription("Mt. Albert 2013 Maori");
    entity.setArchiveTags(false);
    entities.add(entity);

    entity = new EntityInputBean(new FortressInputBean(name), new DocumentTypeInputBean("Census"));
    entity.setDescription("Mt. Albert 2013 Asian");
    entity.setArchiveTags(false);
    entities.add(entity);

    Map<Segment, List<EntityInputBean>> results = batchSplitter.getEntitiesBySegment(su.getCompany(), entities);
    Segment segment = results.keySet().iterator().next();
    assertNotNull(segment);
    assertEquals(2, results.get(segment).size());
  }

  @Test
  public void split_BatchByFortressSegment() throws Exception {
    SystemUser su = registerSystemUser("split_BatchByFortressSegment");

    //Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("split_BatchByFortress", true));
    String name = "Space Fortress";

    Collection<EntityInputBean> entities = new ArrayList<>();

    EntityInputBean entity = new EntityInputBean(new FortressInputBean(name), new DocumentTypeInputBean("Census"))
        .setDescription("Mt. Albert 2013 Maori")
        .setArchiveTags(false)
        .setSegment("Segment One");

    entities.add(entity);

    entity = new EntityInputBean(new FortressInputBean(name), new DocumentTypeInputBean("Census"))
        .setDescription("Mt. Albert 2013 Asian")
        .setArchiveTags(false)
        .setSegment("Segment Two");

    entities.add(entity);

    Map<Segment, List<EntityInputBean>> results = batchSplitter.getEntitiesBySegment(su.getCompany(), entities);
    assertEquals("Each entity should have been assigned to it's own segment", 2, results.size());
    for (Segment segment : results.keySet()) {
      assertEquals(1, results.get(segment).size());
    }
  }

  /**
   * Create fortress and doctype via in the EntityInputBean
   * <p>
   * Useful for checking the user experience of working with the Result view
   *
   * @throws Exception error
   */
  @Test
  public void contentInputWithDocumentAndFortress() throws Exception {
    SystemUser su = registerSystemUser("contentInputWithDocumentAndFortress");
    EntityInputBean entityInputBean = new EntityInputBean()
        .setFortress(new FortressInputBean("contentInputWithDocumentAndFortress", true))
        .setCode("findme")
        .setContent(new ContentInputBean(ContentDataHelper.getRandomMap()))
        .setDocumentType(new DocumentTypeInputBean("contentInputWithDocumentAndFortress"))
        .addTag(new TagInputBean("someCode", "SomeLabel"));

    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInputBean);
    assertNotNull(result);
    assertNotNull(result.getEntity().getKey());
    // Wrap the entity to the outside worlds view of things.
    EntityResultBean eb = new EntityResultBean(entityService.getEntity(su.getCompany(), result.getKey()));
    assertNotEquals(0, eb.getDateCreated());
    assertNotNull(eb.getDateUpdated());

    FortressNode fortress = fortressService.findByName(su.getCompany(), entityInputBean.getFortress().getName());
    assertNotNull(fortress);
    assertEquals(entityInputBean.getFortress().getName(), fortress.getName());
    assertNotNull(conceptService.findDocumentType(fortress, entityInputBean.getDocumentType().getName()));

    EntitySearchChange searchChange = searchService.getEntityChange(result);
    assertNotNull(searchChange);
    assertNotNull(searchChange.getData());
    assertFalse(searchChange.getData().isEmpty());

  }

  /**
   * Track an Entity to a disabled store. We still return a Mock log as the content _can_ be retrived
   * from the search engine. The ID's of a mockedLog will be that of the Entity
   *
   * @throws Exception error
   */
  @Test
  public void mockLogForEntity() throws Exception {
    logger.debug("### mockLogForEntity");
    engineConfig.setTestMode(true); // Force sync processing of the content and log

    String code = "mockLogForEntity";
    SystemUser su = registerSystemUser("mockLogForEntity");
    String fortressName = "mockLogForEntity";

    FortressInputBean fib = new FortressInputBean(fortressName).setSearchEnabled(false).setStoreEnabled(false);
    Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), code);

    inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), ContentDataHelper.getSimpleMap("name", "a")));

    TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
    assertEquals(ContentInputBean.LogStatus.OK, result.getLogStatus());

    Collection<EntityLogResult> entityLogs = entityService.getEntityLogs(su.getCompany(), result.getKey());
    assertNotNull(entityLogs);
    assertEquals("Didn't find the log for the entity", 1, entityLogs.size());
    // Validate that the LastChangeUser is in the Log
    for (EntityLogResult entityLog : entityLogs) {
      assertEquals(true, entityLog.getLog().isMocked());
      assertEquals(result.getEntity().getId(), entityLog.getId());
      assertEquals(result.getEntity().getId(), entityLog.getLog().getId());
    }
  }


  private void compareUser(String exceptionMessage, EntityNode entity, String userName) {
    FortressUserNode fu = fortressService.getUser(entity.getLastUser().getId());
    assertEquals(exceptionMessage, userName, fu.getCode());

  }

  private void createLogRecords(Authentication auth, SystemUser su, String key, double recordsToCreate) throws Exception {
    int i = 0;
    SecurityContextHolder.getContext().setAuthentication(auth);
    while (i < recordsToCreate) {
      mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", key, new DateTime(), ContentDataHelper.getSimpleMap("house", "house" + i), (String) null));
      i++;
    }
    assertEquals(recordsToCreate, (double) entityService.getLogCount(su.getCompany(), key), 0);
  }


}
