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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.authentication.SystemUserService;
import org.flockdata.company.endpoint.CompanyEP;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.FdEngine;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.concept.service.TxService;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.query.service.MatrixService;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.engine.tag.service.TagService;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.engine.track.service.EntityTagService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.engine.track.service.LogRetryService;
import org.flockdata.engine.track.service.LogService;
import org.flockdata.engine.track.service.TrackEventService;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.services.CompanyService;
import org.flockdata.services.ContentModelService;
import org.flockdata.services.RegistrationService;
import org.flockdata.services.SchemaService;
import org.flockdata.test.engine.MapBasedStorageProxy;
import org.flockdata.test.engine.Neo4jConfigTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Base test class for Neo4j functional testing
 *
 * @author mholdsworth
 * @tag Test, Neo4j, Engine
 * @since 16/06/2014
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FdEngine.class,
    Neo4jConfigTest.class,
    MapBasedStorageProxy.class})

@ActiveProfiles( {"dev", "fd-auth-test"})
public abstract class EngineBase {

  protected static final String mike_admin = "mike"; // Admin role
  protected static final String harry = "harry";
  // These have to be in test-security.xml in order to create SysUserRegistrations
  static final String sally_admin = "sally";
  static Logger logger = LoggerFactory.getLogger(EngineBase.class);

  static {
    System.setProperty("neo4j.datastore", "./target/data/neo/");
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();
  @Autowired

  public PlatformConfig engineConfig;
  @Autowired
  public CompanyService companyService;
  @Autowired
  protected
  FortressService fortressService;
  @Autowired
  protected
  EntityService entityService;
  @Autowired
  protected
  EntityTagService entityTagService;
  @Autowired
  protected MediationFacade mediationFacade;
  @Autowired
  protected LogService logService;
  @Autowired
  protected LogRetryService logRetryService;

  @Autowired
  RegistrationService regService;
  @Autowired
  SchemaService schemaService;
  @Autowired
  ConceptService conceptService;
  @Autowired
  GeographyService geoService;
  @Autowired
  IndexManager indexManager;
  @Autowired
  TxService txService;
  @Autowired
  TrackEventService trackEventService;
  @Autowired
  SystemUserService systemUserService;
  @Autowired
  TagService tagService;
  @Autowired
  QueryService queryService;
  @Autowired
  MatrixService matrixService;
  @Autowired
  @Deprecated // Use companyService instead
      CompanyEP companyEP;
  @Autowired
  SearchServiceFacade searchService;
  @Autowired
  StorageProxy storageService;
  @Autowired
  EngineAdminService adminService;
  @Autowired
  Neo4jTemplate neo4jTemplate;
  @Autowired
  SecurityHelper securityHelper;
  @Autowired
  ContentModelService contentModelService;
  Authentication authDefault = new UsernamePasswordAuthenticationToken(
      mike_admin, "123");

  public static void setSecurity(Authentication auth) {
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  public static Authentication setSecurity(String userName) {
    Authentication auth = new UsernamePasswordAuthenticationToken(userName, "123");
    setSecurity(auth);
    return auth;
  }

  public static void setUnauthorized() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  public static void waitAWhile() throws Exception {
    waitAWhile(null, 1500);
  }

  public static void waitAWhile(int millis) throws Exception {
    waitAWhile(null, millis);
  }

  public static void waitAWhile(String message) throws Exception {
    String ss = System.getProperty("sleepSeconds");
    if (ss == null || ss.equals("")) {
      ss = "1";
    }
    if (message == null) {
      message = "Slept for {} seconds";
    }
    waitAWhile(message, Long.decode(ss) * 1000);
  }

  /**
   * Processing delay for threads and integration to complete. If you start
   * getting sporadic Heuristic exceptions, chances are you need to call this
   * routine to give other threads time to commit their work. Likewise,
   * waiting for results from fd-search can take a while. We can't know how
   * long this is so you can experiment on your own environment by passing in
   * -DsleepSeconds=1
   *
   * @param milliseconds to pause for
   * @throws Exception problems
   */
  public static void waitAWhile(String message, long milliseconds)
      throws Exception {
    Thread.sleep(milliseconds);
    logger.trace(message, milliseconds / 1000d);
  }

  public FortressNode createFortress(SystemUser su) throws Exception {
    return fortressService.registerFortress(su.getCompany(), new FortressInputBean("" + System.currentTimeMillis(), true));
  }

  @Rollback(false)
  @BeforeTransaction
  public void cleanUpGraph() {
    // DAT-348 - override this if you're running a multi-threaded tests where multiple transactions
    //           might be started giving you sporadic failures.
    // DAT-493 Function removed
    // https://github.com/spring-projects/spring-data-neo4j/issues/308
    //Neo4jHelper.cleanDb(neo4jTemplate);
    neo4jTemplate.query("match (n)-[r]-() delete r,n", null);
    neo4jTemplate.query("match (n) delete n", null);
  }

  @Before
  public void setSecurity() throws Exception {
    engineConfig.setMultiTenanted(false);
    engineConfig.setTestMode(true); // prevents Async log processing from occurring
    engineConfig.setStoreEnabled(true);
    engineConfig.setConceptsEnabled(false);
    SecurityContextHolder.getContext().setAuthentication(authDefault);
  }

  Transaction beginManualTransaction() {
    return neo4jTemplate.getGraphDatabase().beginTx();
  }

  void commitManualTransaction(Transaction t) {
    t.success();
    t.close();
  }

  public SystemUser registerSystemUser() throws Exception {
    return registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);

  }

  public SystemUser registerSystemUser(String companyName) throws Exception {
    return registerSystemUser(companyName, Long.toHexString(System.currentTimeMillis()));
  }

  public SystemUser registerSystemUser(String companyName, String accessUser) throws Exception {
    logger.debug("Creating company {}", companyName);
    Company company = companyService.create(companyName);
    SystemUser su = regService.registerSystemUser(company, RegistrationBean.builder()
        .login(accessUser)
        .name(accessUser)
        .company(company)
        .unique(false)
        .build());
    logger.debug("Returning SU {}", su);
    return su;
  }

  EntityLog waitForLogCount(Company company, Entity entity, int expectedCount) throws Exception {
    // Looking for the first searchKey to be logged against the entity
    int i = 0;
    int timeout = 100;
    int count = 0;
    //int sleepCount = 90;
    //logger.debug("Sleep Count {}", sleepCount);
    //Thread.sleep(sleepCount); // Avoiding RELATIONSHIP[{id}] has no property with propertyKey="__type__" NotFoundException
    while (i <= timeout) {
      EntityNode updateEntity = entityService.getEntity(company, entity.getKey());
      count = entityService.getLogCount(company, updateEntity.getKey());

      EntityLog log = entityService.getLastEntityLog(company, updateEntity.getKey());
      // We have at least one log?
      if (count == expectedCount) {
        return log;
      }
      Thread.yield();
      if (i > 20) {
        waitAWhile("Waiting for the log to update {}");
      }
      i++;
    }
    if (i > 22) {
      logger.info("Wait for log got to [{}] for metaId [{}]", i,
          entity.getId());
    }
    throw new Exception(String.format("Timeout waiting for the defined log count of %s. We found %s", expectedCount, count));
  }

  long waitForFirstLog(Company company, Entity source) throws Exception {
    // Looking for the first searchKey to be logged against the entity
    long thenTime = System.currentTimeMillis();
    int i = 0;

    EntityNode entity = entityService.getEntity(company, source.getKey());

    int timeout = 100;
    while (i <= timeout) {
      EntityLog log = entityService.getLastEntityLog(company, entity.getKey());
      if (log != null) {
        return i;
      }
      Thread.yield();
      if (i > 20) {
        waitAWhile("Waiting for the log to arrive {}");
      }
      i++;
    }
    if (i > 22) {
      logger.info("Wait for log got to [{}] for metaId [{}]", i,
          entity.getId());
    }
    return System.currentTimeMillis() - thenTime;
  }


  public void testJson() throws Exception {
    FortressNode fortressNode = new FortressNode(new FortressInputBean(
        "testing"), CompanyNode.builder().name("testCompany").build());
    byte[] bytes = JsonUtils.toJsonBytes(fortressNode);
    FortressNode f = JsonUtils.toObject(bytes, FortressNode.class);
    assertNotNull(f);
    assertNull(f.getCompany());// JsonIgnored - Discuss!
    assertEquals("testing", f.getName());
  }

  public void assertNodeDoesNotExist(Long nodeId) {
    assertNodeDoesNotExist("Node should not have existed", nodeId);
  }

  public void assertNodeDoesNotExist(String message, Long nodeId) {
    Result<Map<String, Object>> results = neo4jTemplate.query("match (n) where id(n)= " + nodeId + " return n", null);
    assertFalse(message, results.iterator().hasNext());

  }


}
