/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.integration;

import junit.framework.TestCase;
import org.apache.commons.lang3.time.StopWatch;
import org.flockdata.client.amqp.AmqpServices;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.query.service.MatrixService;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.FdServerWriter;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.*;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.search.IndexManager;
import org.flockdata.search.model.*;
import org.flockdata.store.Store;
import org.flockdata.store.StoreContent;
import org.flockdata.track.bean.*;
import org.flockdata.track.service.*;
import org.flockdata.transform.ClientConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Allows the fd-engine services to be tested against fd-search with actual integration.
 * fd-search is stated by Cargo as a Tomcat server while fd-engine is debuggable in-process.
 * <p/>
 * Note that Logs and Search docs are written asyncronously. For this reason you will see
 * various "waitAWhile" loops giving other threads time to process the payloads before
 * making assertions.
 * <p/>
 * <p/>
 * This approach requires RabbitMQ to be installed to allow integration to occur.
 * <p/>
 * No web interface is launched for fd-engine
 * <p/>
 * Make sure that you create unique User ids for your test.
 * <p/>
 * To run the integration suite:
 * mvn clean install -P integration
 * <p/>
 * If you want to debug engine then you add to your command line
 * -Dfd.debug=true -DforkCount=0
 * <p/>
 * To debug the search service refer to the commented line in pom.xml where the
 * default port is set to 8000
 * <p/>
 * User: nabil, mike
 * Date: 16/07/13
 * Time: 22:51
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@WebIntegrationTest
@ActiveProfiles("integration")
@ContextConfiguration(locations = {"classpath:root-context.xml", "classpath:apiDispatcher-servlet.xml"})
public class TestFdIntegration {

    protected static boolean runMe = true; // pass -Dfd.debug=true to disable all tests
    protected static int fortressMax = 1;

    @Rule
    // Use this to assert exception conditions
    public final ExpectedException exception = ExpectedException.none();

    @Autowired
    EntityService entityService;

    @Autowired
    IndexManager indexHelper;

    @Autowired
    EsIntegrationHelper esHelper;

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    RegistrationService regService;

    @Autowired
    EngineAdminService adminService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SearchServiceFacade searchService;

    @Autowired
    LogService logService;

    @Autowired
    TagService tagService;

    @Autowired
    FortressService fortressService;

    @Qualifier("mediationFacadeNeo")
    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    QueryService queryService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    MatrixService matrixService;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    FdServerWriter serverWriter;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ApplicationContext applicationContext;

    private static Logger logger = LoggerFactory.getLogger(TestFdIntegration.class);
    static MockMvc mockMvc;
    String company = "Monowai";

    static {
        System.setProperty("neo4j.datastore", "./target/data/neo/");
    }


    @AfterClass
    public static void logEndOfClassTests() throws Exception {
        long milliseconds = getSleepSeconds();
        logger.debug("After Class - Sleeping for {}", milliseconds / 1000d);
        Thread.yield();
        Thread.sleep(milliseconds);

    }

    static long getSleepSeconds() {
        String ss = System.getProperty("sleepSeconds");
        if (ss == null || ss.equals(""))
            ss = "3";
        return Long.decode(ss) * 1000;
    }

    public void setDefaultAuth() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(Helper.AUTH_MIKE);

        if (mockMvc == null)
            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        engineConfig.setStoreEnabled("true"); // Rest to default state for each test
    }

    @BeforeClass
    @Rollback(false)
    public static void pingFdSearch() throws Exception {
        // Always run
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = Helper.getHttpHeaders(null, null, null);
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        logger.info("**** Checking to see if we can ping fd-search @ {}", FD_SEARCH);
        try {
            ResponseEntity<String> response = restTemplate.exchange(FD_SEARCH + "/v1/admin/ping", HttpMethod.GET, requestEntity, String.class);
            assertTrue("didn't get the Pong response", response.getBody().equals("pong"));

        } catch (Exception e) {
            runMe = false; // Everything will fail
            throw new FlockException("Can't connect to FD-Search. No point in continuing");
        }
    }

    @Before
    public void testHealth() throws Exception {
        esHelper.cleanupElasticSearch();
        Map<String, String> health = engineConfig.getHealth();
        assertNotNull(health);
        assertFalse(health.isEmpty());
        assertEquals("Ok", health.get("fd-search"));

    }


    static RestTemplate restTemplate = null;

    private static RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        }
        return restTemplate;
    }

    /**
     * Checks that UTF-8 special chars are correctly transferred across channels
     * This function also touches most of the integration between fd-search and fd-engine
     *
     * @throws Exception
     */
    @Test
    public void utfTextThroughIntegrationChannels() throws Exception {
        //assumeTrue(runMe);
        Map<String, Object> json = Helper.getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("Utf8");

        Store previousStore = engineConfig.store();
        engineConfig.setStore(Store.NONE); // Will resolve to ElasticSearch

        try {
            Fortress fortress = fortressService
                    .registerFortress(su.getCompany(),
                            new FortressInputBean("UTF8-Test")
                                    .setSearchActive(true));

            ContentInputBean log = new ContentInputBean("mikeTest", new DateTime(), json);
            EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "UtfTextCode", new DateTime(), "abzz")
                    .setDescription("This text, Neumannová, might look great in a search result");
            input.setContent(log);
            input.addTag( new TagInputBean("code", "TheLabelz", "anything")); // For tag cloud

            TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
            logger.info("Track request made. About to wait for first search result");

            // Test directly against ElasticSearch
            esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
            esHelper.doEsQuery(result.getEntity(), json.get("Athlete").toString(), 1);

            EntityLog entityLog = entityService.getLastEntityLog(su.getCompany(), result.getMetaKey());

            assertNotNull(entityLog);
            assertNotNull(entityLog.getLog());
            StoreContent content = logService.getContent(result.getEntity(), entityLog.getLog());

            // Now test other ES http integration functions
            // Search
            QueryParams qp = new QueryParams(result.getMetaKey());
            qp.setCompany(su.getCompany().getName());
            qp.setFortress(fortress.getCode());
            EsSearchResult queryResults = queryService.search(su.getCompany(), qp);
            assertNotNull(queryResults);
            assertTrue("Result size should be at least 1 - was {}" + queryResults.getResults().size(), queryResults.getResults().size()>0);

            assertNotNull(content);
            assertNotNull(content.getData());
            assertEquals(json.get("Athlete"), content.getData().get("Athlete"));

            // MetaKey
            MetaKeyResults mkResults = queryService.getMetaKeys(su.getCompany(), qp);
            assertNotNull(mkResults);
            assertTrue("MKResult size should be at least 1 - was {}" + mkResults.getResults().size(), mkResults.getResults().size()>0);

            TagCloudParams tcParams = new TagCloudParams(fortress);
            tcParams.addTag("TheLabelz")
                    .addRelationship("anything");

            TagCloud cloud = queryService.getTagCloud(su.getCompany(), tcParams);
            assertNotNull( cloud);

            // And via FD query
            QueryParams queryParams = new QueryParams("*");
            queryParams.setFortress(fortress.getName().toLowerCase());

            EsSearchResult esSearchResult = queryService.search(su.getCompany(), queryParams);
            assertTrue("Incorrect result count found via queryService ", esSearchResult.getResults().size() == 1);
        } finally {
            engineConfig.setStore(previousStore);
        }
    }

    @Test
    public void search_dataFieldsIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## dataTypes_dataFieldsIndexed");

        SystemUser su = registerSystemUser("dataTypes_dataFieldsIndexed", "dataTypes_dataFieldsIndexed");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("FIB"));
        String docType = "DT";
        String callerRef = "ABC123X";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortress, "wally", docType, new DateTime(), callerRef)
                        .setName("find by name")
                        .setDescription("describe me to be found");

        Map<String, Object> json = Helper.getRandomMap();
        json.put("int", 123);
        json.put("long", 456l);
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), json);
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        esHelper.doEsQuery(entity, entity.getMetaKey());

        esHelper.doEsFieldQuery(entity, EntitySearchSchema.DATA + ".int", "123", 1);
        // scan by name facet?
        esHelper.doFacetQuery(entity, EntitySearchSchema.NAME + ".facet", entityInputBean.getName(), 1);
        // Can we find by description?
        esHelper.doEsQuery(entity, entityInputBean.getDescription());
        assertNull("EntityInput.description is not stored in the entity", entity.getDescription());
        esHelper.deleteEsIndex(indexHelper.parseIndex(entity));
    }

    @Test
    public void track_companyAndFortressWithSpaces() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_companyAndFortressWithSpaces");

        SystemUser su = registerSystemUser("testcompany", "companyAndFortressWithSpaces");
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("Track Test"));
        String docType = "ZZDocCode";
        String callerRef = "ABC123X";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortressA, "wally", docType, new DateTime(), callerRef);

        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), Helper.getRandomMap());
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();

        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        esHelper.doEsQuery(entity, entity.getMetaKey());
        esHelper.deleteEsIndex(indexHelper.parseIndex(entity));
    }

    //    @Test       DAT-521
    public void search_pdfTrackedAndFound() throws Exception {
        assumeTrue(runMe);
        logger.info("## search_pdfTrackedAndFound");

        SystemUser su = registerSystemUser("pdf_TrackedAndFound", "co-fortress");
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("pdf_TrackedAndFound"));
        assertTrue("Search should not be disabled", fortressA.isSearchEnabled());
        String docType = "Contract";
        String callerRef = "PDF-TRACK-123";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortressA, "wally", docType, new DateTime(), callerRef);

        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime());
        contentInputBean.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        entityInputBean.setContent(contentInputBean);

        TrackResultBean trackResultBean = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean);

        assertNotNull(trackResultBean.getCurrentLog().getLog().getFileName());

        Entity entity = trackResultBean.getEntity();

        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        EntityLog lastLog = logService.getLastLog(entity);
        assertNotNull(lastLog);
        assertNotNull(lastLog.getLog().getFileName());

        Helper.waitAWhile("Attachment Mapper can take some time to process the PDF");
        esHelper.doEsQuery(entity, "*", 1);
        esHelper.doEsQuery(entity, "brown fox", 1);
        esHelper.doEsQuery(entity, contentInputBean.getFileName(), 1);
        esHelper.doEsFieldQuery(entity, EntitySearchSchema.META_KEY, entity.getMetaKey(), 1);
        esHelper.doEsFieldQuery(entity, EntitySearchSchema.FILENAME, "test.pdf", 1);
        esHelper.doEsFieldQuery(entity, EntitySearchSchema.ATTACHMENT, "pdf", 1);
        esHelper.deleteEsIndex(indexHelper.parseIndex(entity));
    }


    @Test
    public void track_WithOnlyTagsTracksToSearch() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_WithOnlyTagsTracksToSearch");
        SecurityContextHolder.getContext().setAuthentication(Helper.AUTH_MIKE);
        SystemUser su = registerSystemUser("Mark");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityWithTagsProcess"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "TrackTags", now, "ABCXYZ123");
        inputBean.setEntityOnly(true);
        inputBean.addTag(new TagInputBean("testTagNameZZ", null, "someAuditRLX"));
        inputBean.setEvent("TagTest");
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(summary);
        // Check we can find the Event in ElasticSearch
        esHelper.doEsQuery(summary.getEntity(), inputBean.getEvent(), 1);
        // Can we find the Tag
        esHelper.doEsQuery(summary.getEntity(), "testTagNameZZ", 1);
        esHelper.deleteEsIndex(indexHelper.parseIndex(result.getEntity()));
    }

    @Test
    public void track_UserDefinedProperties() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_UserDefinedProperties");
        SecurityContextHolder.getContext().setAuthentication(Helper.AUTH_MIKE);
        SystemUser su = registerSystemUser("Mittens");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("track_UserDefinedProperties"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "TrackTags", now, "ABCXYZ123");
        inputBean.setEntityOnly(true);

        inputBean.setProperty("myString", "hello world");
        inputBean.setProperty("myNum", 123.45);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(summary);
        esHelper.doEsQuery(summary.getEntity(), "hello world", 1);
        esHelper.doEsQuery(summary.getEntity(), "123.45", 1);
        esHelper.deleteEsIndex(indexHelper.parseIndex(result.getEntity()));
    }

    @Test
    public void search_passThroughQuery() throws Exception {
        assumeTrue(runMe);
        logger.info("## searc_passThroughQuery");
        SecurityContextHolder.getContext().setAuthentication(Helper.AUTH_MIKE);
        SystemUser su = registerSystemUser("searc_passThroughQuery");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("searc_passThroughQuery"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "TrackTags", now);
        inputBean.setEntityOnly(true);

        inputBean.setProperty("myString", "hello world");
        inputBean.setProperty("myNum", 123.45);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(summary);
        QueryParams qp = new QueryParams(fo.getDefaultSegment());
        String queryString = "{\"query_string\": {\n" +
                "      \"query\": \"hello world\"\n" +
                "  }}";
        Map<String, Object> query = JsonUtils.toMap(queryString);

        qp.setQuery(query);
        EsSearchResult searchResult = queryService.search(su.getCompany(), qp);
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getTotalHits());
        Map<String, Object> mapResult = JsonUtils.toMap(searchResult.getJson());
        assertFalse(mapResult.isEmpty());
        esHelper.deleteEsIndex(indexHelper.parseIndex(result.getEntity()));
    }

    @Test
    public void track_immutableEntityWithNoLogsAreIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_immutableEntityWithNoLogsAreIndexed");
        SystemUser su = registerSystemUser("Manfred");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("immutableEntityWithNoLogsAreIndexed"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "immutableLoc", now, "ZZZ123");
        inputBean.setEvent("immutableEntityWithNoLogsAreIndexed");
        inputBean.setEntityOnly(true); // Must be true to make over to search
        TrackResultBean trackResult;
        trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), trackResult.getEntity(), entityService);
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), trackResult.getEntity().getMetaKey());
        esHelper.waitForFirstSearchResult(1, su.getCompany(), trackResult.getEntity(), entityService);
        assertNotNull(summary);
        assertSame("change logs were not expected", 0, summary.getChanges().size());
        assertNotNull("Search record not received", summary.getEntity().getSearchKey());
        // Check we can find the Event in ElasticSearch
        esHelper.doEsQuery(summary.getEntity(), inputBean.getEvent(), 1);

        // Not flagged as meta only so will not appear in the search index until a log is created
        inputBean = new EntityInputBean(fo, "wally", inputBean.getDocumentType().getName(), now, "ZZZ999");
        trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        summary = mediationFacade.getEntitySummary(su.getCompany(), trackResult.getEntity().getMetaKey());
        assertNotNull(summary);
        assertSame("No change logs were expected", 0, summary.getChanges().size());
        assertEquals(null, summary.getEntity().getSearch());
        // Check we can't find the Event in ElasticSearch
        esHelper.doEsQuery(summary.getEntity(), "ZZZ999", 0);
        esHelper.deleteEsIndex(indexHelper.parseIndex(summary.getEntity()));
    }

    @Test
    public void admin_rebuildSearchIndexFromEngine() throws Exception {
        assumeTrue(runMe);
        logger.info("## admin_rebuildSearchIndexFromEngine");
        SystemUser su = registerSystemUser("David");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("rebuildTest"));

        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "RBSearch", new DateTime(), "ABC123");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), Helper.getRandomMap()));
        TrackResultBean auditResult = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Entity entity = entityService.getEntity(su.getCompany(), auditResult.getEntity().getMetaKey());
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        esHelper.doEsQuery(entity, "*");

        // Rebuild....
        SecurityContextHolder.getContext().setAuthentication(Helper.AUTH_MIKE);
        Long lResult = adminService.doReindex(fo).get();
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);
        assertNotNull(lResult);
        assertEquals(1l, lResult.longValue());

        esHelper.doEsQuery(entity, "*");
//        deleteEsIndex(indexHelper.parseIndex(entity));

    }

    @Test
    public void
    load_createEntityAndTimeLogsWithSearchActivated() throws Exception {
        assumeTrue(runMe);
        logger.info("## load_createEntityAndTimeLogsWithSearchActivated");
        int max = 3;
        String metaKey;
        SystemUser su = registerSystemUser("Olivia");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("111"));

        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "LogTiming", new DateTime(), "ABC123");
        TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);

        metaKey = trackResult.getEntity().getMetaKey();

        Entity entity = entityService.getEntity(su.getCompany(), metaKey);
        assertNotNull(entity);
        assertNotNull(entityService.findByCode(fo, inputBean.getDocumentType().getName(), inputBean.getCode()));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;

        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), Helper.getSimpleMap("blah", i))).getEntity();

            i++;
        }
        Helper.waitForLogCount(su.getCompany(), entity, max, entityService);
        esHelper.waitForFirstSearchResult(0, su.getCompany(), entity, entityService);

        watch.stop();
        esHelper.doEsFieldQuery(entity, EntitySearchSchema.DATA + ".blah", "*", 1);
//        deleteEsIndex(indexHelper.parseIndex(entity));
    }

    @Test
    public void track_IgnoreGraphAndCheckSearch() throws Exception {
        assumeTrue(runMe);
        pingFdSearch();
        logger.info("## track_IgnoreGraphAndCheckSearch started");
        SystemUser su = registerSystemUser("Isabella");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TrackGraph"));

        EntityInputBean entityInput = new EntityInputBean(fortress, "wally", "ignoreGraph", new DateTime(), "ABC123");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true); // If true, the entity will be sent to fd-search (but with no content)
        // Entity is suppressed in the graph.
        exception.expect(NotFoundException.class);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Helper.waitAWhile("track_IgnoreGraphAndCheckSearch", 6000);

        // Entity IS indexed in fd-search
        esHelper.doEsQuery(result.getEntity(), "*", 1);

        entityInput = new EntityInputBean(fortress, "wally", entityInput.getDocumentType().getName(), new DateTime(), "ABC124")
                .setTrackSuppressed(true)
                .setEntityOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        Helper.waitAWhile("2nd Entity sent to fd-search");
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        esHelper.doEsQuery(result.getEntity(), "*", 2);

        entityInput = new EntityInputBean(fortress, "wally", entityInput.getDocumentType().getName(), new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        Entity entity = mediationFacade.trackEntity(su.getCompany(), entityInput).getEntity();
        assertNull(entity.getMetaKey());
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        // Updating the same caller ref should not create a 3rd record
        esHelper.doEsQuery(result.getEntity(), "*", 2);

        entityInput = new EntityInputBean(fortress, "wally", entityInput.getDocumentType().getName(), new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        // Updating the same caller ref should not create a 3rd record
        esHelper.doEsQuery(result.getEntity(), "*", 2);

        entityInput = new EntityInputBean(fortress, "wally", entityInput.getDocumentType().getName(), new DateTime(), "ABC125");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        // Updating the same caller ref should not create a 3rd record
        esHelper.doEsQuery(result.getEntity(), "*", 3);

    }

    @Test
    public void cancel_searchDocIsRewrittenAfterCancellingLogs() throws Exception {
        assumeTrue(runMe);
        logger.info("## cancel_searchDocIsRewrittenAfterCancellingLogs");
        SystemUser su = registerSystemUser("Felicity");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("cancelLogTag"));
        String created = "2010-11-20 11:30:00"; // Create
        String fUpdate2 = "2010-11-21 11:45:00"; // First Update
        DateTime createdDate = new DateTime(Timestamp.valueOf(created));
        DateTime updatedDate = new DateTime(Timestamp.valueOf(fUpdate2));

        EntityInputBean entityInput = new EntityInputBean(fo, "wally", "CancelDoc", createdDate, "ABC123");
        ContentInputBean content = new ContentInputBean("wally", createdDate, Helper.getRandomMap());
        entityInput.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        entityInput.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        entityInput.setContent(content);
        // Create the Entity and Log
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertEquals("Fortress Create date did not match", createdDate.getMillis(), result.getEntity().getFortressCreatedTz().getMillis());
        DateTime fdWhen = new DateTime(result.getEntity().getDateCreated());
        assertNotEquals("FlockData's when date should be the current year", createdDate.getYear(), fdWhen.getYear());
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);

        // ensure non-analysed tags work
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        // Analyzed tags require exact match...
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Happy Days", 1);
        esHelper.doEsQuery(result.getEntity(), "happy days", 1);
        // We now have 1 content doc with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        content = new ContentInputBean("wally", updatedDate, Helper.getRandomMap());
        entityInput.getTags().clear();
        entityInput.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        entityInput.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        entityInput.setContent(content);
        // !!Second Update!!
        result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), result.getMetaKey());

        assertEquals("Created date changed after an update - wrong", createdDate, entity.getFortressCreatedTz());
        assertEquals("Update dates did not reconcile", updatedDate, entity.getFortressUpdatedTz());
        EntityLog lastLog = logService.getLastLog(entity);
        assertEquals("Second Update not recorded", Long.valueOf(updatedDate.getMillis()), lastLog.getFortressWhen());

        Helper.waitAWhile("Waiting for search to affect");

        Collection<EntityTag> tags = entityTagService.getEntityTags(entity);
        assertEquals(2, tags.size());
        boolean sadFound = false, daysFound = false;

        for (EntityTag tag : tags) {
            if (tag.getTag().getCode().equalsIgnoreCase("sad days"))
                sadFound = true;
            else if (tag.getTag().getCode().equalsIgnoreCase("days bay"))
                daysFound = true;
        }
        assertTrue("Did not find the days tag", daysFound);
        assertTrue("Did not find the sad tag", sadFound);
        // We now have 2 logs, sad tags and no happy tags

        String json = esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Sad Days", 1);
        Map<String, Object> searchDoc = JsonUtils.toMap(json);
        Long searchCreated = Long.parseLong(searchDoc.get(EntitySearchSchema.CREATED).toString());
        Long searchUpdated = Long.parseLong(searchDoc.get(EntitySearchSchema.UPDATED).toString());
        assertTrue("Fortress update was not set in to searchDoc", searchUpdated > 0);
        assertEquals("Created date mismatch", createdDate.getMillis(), searchCreated.longValue());
        assertEquals("Last Change date mismatch: expected " + fUpdate2 + " was " + new DateTime(searchUpdated), updatedDate.getMillis(), searchUpdated.longValue());
        esHelper.doEsTermQuery(entity, EntitySearchSchema.TAG + ".testingc.tag.code.facet", "Days Bay", 1);
        // These were removed in the update
        esHelper.doEsTermQuery(entity, EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 0);
        esHelper.doEsTermQuery(entity, EntitySearchSchema.TAG + ".testingb.tag.code.facet", "happy days", 0);

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), result.getEntity());
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        Collection<EntityTag> entityTags = entityTagService.getEntityTags(result.getEntity());
        assertEquals(2, entityTags.size());

        // These should have been added back in due to the cancel operation
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Happy Days", 1);

        // These were removed in the cancel
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.code", "Sad Days", 0);
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingc.code", "Days Bay", 0);

//        deleteEsIndex(indexHelper.parseIndex(entity));
    }

    @Test
    public void tag_UniqueKeySearch() throws Exception {
        // DAT-95
        assumeTrue(runMe);
        logger.info("## tag_UniqueKeySearch");
        SystemUser su = registerSystemUser("Cameron");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tag_UniqueKeySearch"));
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "UniqueKeySearch", new DateTime(), "tag_UniqueKeySearch");
        ContentInputBean log = new ContentInputBean("wally", new DateTime(), Helper.getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        inputBean.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        // ensure that non-analysed tags work
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Happy Days", 1);
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Sad Days", 1);
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingc.tag.code.facet", "Days Bay", 1);
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingc.tag.code", "days", 1);
//        deleteEsIndex(indexHelper.parseIndex(result.getEntity()));
    }

    @Test
    public void user_NoFortressUserWorks() throws Exception {
        // DAT-317
        assumeTrue(runMe);
        logger.info("## user_NoFortressUserWorks");
        SystemUser su = registerSystemUser("piper");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("user_NoFortressUserWorks"));

        // FortressUser cannot be resolved from the entity or the log
        EntityInputBean inputBean = new EntityInputBean(fo, null, "UniqueKey", new DateTime(), "ABC123");
        ContentInputBean log = new ContentInputBean(null, new DateTime(), Helper.getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        // ensure that non-analysed tags work
        esHelper.doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        QueryParams queryParams = new QueryParams();
        queryParams.setCompany(su.getCompany().getName());
        queryParams.setFortress(fo.getName());
        queryParams.setSearchText("*");
        EsSearchResult results = queryService.search(su.getCompany(), queryParams);
        assertNotNull(results);
        assertEquals(1, results.getResults().size());
//        deleteEsIndex(indexHelper.parseIndex(result.getEntity()));

    }

    @Test
    public void search_withNoMetaKeysDoesNotError() throws Exception {
        // DAT-83
        assumeTrue(runMe);
        logger.info("## search_withNoMetaKeysDoesNotError");
        SystemUser su = registerSystemUser("HarryIndex");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("searchIndexWithNoMetaKeysDoesNotError"));

        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "TestTrack", new DateTime(), "ABC123")
                .setTrackSuppressed(true)
                .setContent(new ContentInputBean("wally", new DateTime(), Helper.getRandomMap()));
        // First entity and log, but not stored in graph
        mediationFacade.trackEntity(su.getCompany(), inputBean); // Expect a mock result as we're not tracking

        inputBean = new EntityInputBean(fo, "wally", "TestTrack", new DateTime(), "ABC124")
                .setContent(new ContentInputBean("wally", new DateTime(), Helper.getRandomMap()));
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());

        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService); // 2nd document in the index
        // We have one with a metaKey and one without
        esHelper.doEsQuery(entity, "*", 2);

        QueryParams qp = new QueryParams(fo.getDefaultSegment());
        qp.setSearchText("*");
        String queryResult = runFdViewQuery(qp);
        assertNotNull(queryResult);
        assertTrue("Should be 2 query results - one with a metaKey and one without", queryResult.contains("\"totalHits\":2,"));

    }

    @Test
    public void query_engineResultsReturn() throws Exception {
        assumeTrue(runMe);
        logger.info("## query_engineResultsReturn");
        SystemUser su = registerSystemUser("Kiwi");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("QueryTest"));

        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "TestQuery", new DateTime(), "ABC123");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), Helper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService); // 2nd document in the index

        inputBean = new EntityInputBean(fo, "wally", inputBean.getDocumentType().getName(), new DateTime(), "ABC124");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), Helper.getRandomMap()));
        result = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());

        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService); // 2nd document in the index
        // We have one with a metaKey and one without
        esHelper.doEsQuery(entity, "*", 2);

        QueryParams qp = new QueryParams(fo.getDefaultSegment());
        qp.setSearchText("*");
        runFdViewQuery(qp);
        EsSearchResult queryResults = runSearchQuery(su, qp);
        assertNotNull(queryResults);
        assertEquals(2, queryResults.getResults().size());

    }

    @Test
    public void date_utcDatesThruToSearch() throws Exception {
        // DAT-196
        assumeTrue(runMe);
        logger.info("## date_utcDatesThruToSearch");
        SystemUser su = registerSystemUser("Kiwi-UTC");
        FortressInputBean fib = new FortressInputBean("utcDateFieldsThruToSearch", false);
        fib.setTimeZone("Europe/Copenhagen"); // Arbitrary TZ
        Fortress fo = fortressService.registerFortress(su.getCompany(), fib);

        DateTimeZone ftz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fib.getTimeZone()));
        DateTimeZone utz = DateTimeZone.UTC;
        DateTimeZone ltz = DateTimeZone.getDefault();

        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4, 30, DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Copenhagen")));
        DateTime lastUpdated = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Copenhagen")));

        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "TestTrack", fortressDateCreated, "ABC123");
        inputBean.setLastChange(lastUpdated.toDate());
        inputBean.setContent(new ContentInputBean("wally", lastUpdated, Helper.getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);

        Entity entity = result.getEntity();

        assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(entity.getFortressCreatedTz()));

        EntityLog log = entityService.getLastEntityLog(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(log);
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(ftz)));

        // We have one with a metaKey and one without
        esHelper.doEsQuery(entity, "*", 1);

        QueryParams qp = new QueryParams(fo.getDefaultSegment());
        qp.setSearchText("*");
        runFdViewQuery(qp);
        EsSearchResult queryResults = runSearchQuery(su, qp);
        assertNotNull(queryResults);
        assertEquals(1, queryResults.getResults().size());
        for (SearchResult searchResult : queryResults.getResults()) {
            logger.info("whenCreated utc-{}", new DateTime(searchResult.getWhenCreated(), utz));
            assertEquals(fortressDateCreated, new DateTime(searchResult.getWhenCreated(), ftz));
            logger.info("whenCreated ftz-{}", new DateTime(searchResult.getWhenCreated(), ftz));
            assertEquals(new DateTime(fortressDateCreated, utz), new DateTime(searchResult.getWhenCreated(), utz));
            logger.info("lastUpdate  utc-{}", new DateTime(searchResult.getLastUpdate(), utz));
            assertNotNull(searchResult.getLastUpdate());
            assertEquals(lastUpdated, new DateTime(searchResult.getLastUpdate(), ftz));
            logger.info("lastUpdate  ftz-{}", new DateTime(searchResult.getLastUpdate(), ftz));
            assertEquals(new DateTime(lastUpdated, utz), new DateTime(searchResult.getLastUpdate(), utz));
            assertNotNull(searchResult.getFdTimestamp());
            logger.info("timestamp   ltz-{}", new DateTime(searchResult.getFdTimestamp(), ltz));

        }

    }

    /**
     * Integrated co-occurrence query works
     *
     * @throws Exception
     */
    @Test
    public void query_MatrixResults() throws Exception {
        assumeTrue(runMe);
        logger.info("## query_MatrixResults");

        SystemUser su = registerSystemUser("query_MatrixResults", "query_MatrixResults");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("query_MatrixResults"));
        String docType = "DT";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortress, "wally", docType, new DateTime());

        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.code in ES
        entityInputBean.addTag(new TagInputBean("labelA", "ThisLabel", relationshipName));
        entityInputBean.addTag(new TagInputBean("labelB", "ThatLabel", relationshipName));
        entityInputBean.setEntityOnly(true);
        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        // Second Document
        entityInputBean = new EntityInputBean(fortress, "wally", docType, new DateTime());
        entityInputBean.addTag(new TagInputBean("labelA", "ThisLabel", relationshipName));
        entityInputBean.addTag(new TagInputBean("labelB", "ThatLabel", relationshipName));

        entityInputBean.setEntityOnly(true);

        entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();

        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        MatrixInputBean matrixInputBean = new MatrixInputBean();
        matrixInputBean.setQueryString("*");

        ArrayList<String> tags = new ArrayList<>();
        tags.add("ThisLabel");
        tags.add("ThatLabel");

        ArrayList<String> rlx = new ArrayList<>();
        rlx.add(relationshipName.toLowerCase());
        matrixInputBean.setFromRlxs(rlx);
        matrixInputBean.setToRlxs(rlx);
        matrixInputBean.setConcepts(tags);
        ArrayList<String> fortresses = new ArrayList<>();
        fortresses.add(fortress.getName().toLowerCase());
        matrixInputBean.setFortresses(fortresses);
        matrixInputBean.setByKey(false);

        MatrixResults matrixResults = matrixService.getMatrix(su.getCompany(), matrixInputBean);
        assertEquals(null, matrixResults.getNodes());
        assertEquals(2, matrixResults.getEdges().size());

        matrixInputBean.setByKey(true);
        matrixResults = matrixService.getMatrix(su.getCompany(), matrixInputBean);

        assertEquals(2, matrixResults.getEdges().size());
        assertEquals(2, matrixResults.getNodes().size());


    }

    private EsSearchResult runSearchQuery(SystemUser su, QueryParams input) throws Exception {
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/query/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), EsSearchResult.class);
    }

    /**
     * Suppresses the indexing of a log record even if the fortress is set to index everything
     *
     * @throws Exception
     */
    @Test
    public void search_suppressOnDemand() throws Exception {
        assumeTrue(runMe);
        logger.info("## search_suppressOnDemand");

        SystemUser su = registerSystemUser("Barbara");
        Fortress fortress =
                fortressService.registerFortress(su.getCompany(),
                        new FortressInputBean("search_suppressOnDemand"));

        EntityInputBean entityInput = new
                EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
        entityInput.setContent(new ContentInputBean("olivia@sunnybell.com", new DateTime(), Helper.getSimpleMap("who", "andy")));
        //Transaction tx = getTransaction();
        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), entityInput);

        Entity entity = entityService.getEntity(su.getCompany(), indexedResult.getEntity().getMetaKey());
        assertNotNull(indexedResult);

        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        esHelper.doEsQuery(entity, "andy");

        entityInput = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
        entityInput.setSearchSuppressed(true);
        entityInput.setContent(new ContentInputBean("olivia@sunnybell.com", new DateTime(), Helper.getSimpleMap("who", "bob")));

        // Bob's not there because we said we didn't want to index that entity
        esHelper.doEsQuery(entity, "bob", 0);
        esHelper.doEsQuery(entity, "andy");
    }

    @Test
    public void tag_ReturnsSingleSearchResult() throws Exception {
        assumeTrue(runMe);
        logger.info("## tag_ReturnsSingleSearchResult");

        SystemUser su = registerSystemUser("Peter");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress"));
        EntityInputBean metaInput = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.code in ES
        TagInputBean tag = new TagInputBean("Code Test Works", null, relationshipName);
        metaInput.addTag(tag);

        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), metaInput);
        Entity entity = entityService.getEntity(su.getCompany(), indexedResult.getEntity().getMetaKey());

        Collection<EntityTag> tags = entityTagService.getEntityTags(entity);
        assertNotNull(tags);
        assertEquals(1, tags.size());

        EntityLog resultBean = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), Helper.getRandomMap())).getCurrentLog();
        assertNotNull(resultBean);

        esHelper.waitForFirstSearchResult(0, su.getCompany(), entity, entityService);
        esHelper.doEsTermQuery(entity, "tag." + relationshipName + ".tag.code.facet", "Code Test Works", 1);
        esHelper.doEsQuery(entity, "code test works", 1);

    }

    @Test
    public void cancel_UpdatesSearchCorrectly() throws Exception {
        assumeTrue(runMe);
        // DAT-53
        logger.info("## cancel_UpdatesSearchCorrectly");

        SystemUser su = registerSystemUser("Rocky");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testCancelUpdatesSearchCorrectly"));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        inputBean.setContent(new ContentInputBean("olivia@sunnybell.com", firstDate, Helper.getSimpleMap("house", "house1")));
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getMetaKey();

        Entity entity = entityService.getEntity(su.getCompany(), metaKey);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entity, entityService);

        // Initial create
        esHelper.doEsTermQuery(entity, EntitySearchSchema.DATA + ".house", "house1", 1); // First log

        // Now make an amendment
        EntityLog secondLog =
                mediationFacade.trackLog(su.getCompany(), new ContentInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), Helper.getSimpleMap("house", "house2"))).getCurrentLog();
        assertNotSame(0l, secondLog.getFortressWhen());

        Set<EntityLog> logs = entityService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(2, logs.size());
        entity = entityService.getEntity(su.getCompany(), metaKey);

        Helper.waitAWhile("cancel function step 1");
        Assert.assertEquals("Last Updated dates don't match", secondLog.getFortressWhen().longValue(), entity.getFortressUpdatedTz().getMillis());
        esHelper.doEsTermQuery(entity, EntitySearchSchema.DATA + ".house", "house2", 1); // replaced first with second

        // Now cancel the last log
        mediationFacade.cancelLastLog(su.getCompany(), entity);
        Helper.waitAWhile("Cancel function step 2");
        logs = entityService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(1, logs.size());
        entity = entityService.getEntity(su.getCompany(), metaKey); // Refresh the entity
        Helper.waitAWhile("Cancel 2");
        // Should have restored the content back to house1
        esHelper.doEsTermQuery(entity, EntitySearchSchema.DATA + ".house", "house1", 1); // Cancelled, so Back to house1

        // Last change cancelled
        // DAT-96
        mediationFacade.cancelLastLog(su.getCompany(), entity);
        logs = entityService.getEntityLogs(entity);
        assertEquals(true, logs.isEmpty());
        Helper.waitAWhile("Cancel function step 3");
        esHelper.doEsQuery(entity, "*", 0);

        entity = entityService.getEntity(su.getCompany(), metaKey); // Refresh the entity
        assertEquals("Search Key set to callerRef", entity.getCode(), entity.getSearchKey());
    }

    @Test
    public void search_nGramDefaults() throws Exception {
        assumeTrue(runMe);
        logger.info("## search_nGramDefaults");
        SystemUser su = registerSystemUser("Romeo");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram"));
        EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
        TagInputBean tagInputBean = new TagInputBean("Description", "testLabel", "linked");
        inputBean.addTag(tagInputBean);

        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), indexedResult.getEntity().getMetaKey());

        Map<String, Object> what = Helper.getSimpleMap(EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        entity = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), what)).getEntity();
        esHelper.waitForFirstSearchResult(0, su.getCompany(), entity, entityService);

        String indexName = entity.getFortress().getRootIndex();
        esHelper.getMapping(indexName);

        // Completion only works as "Starts with"
        esHelper.doCompletionQuery(indexHelper.parseIndex(entity), entity.getType(), "des", 1, "didn't find the tag");
        esHelper.doCompletionQuery(indexHelper.parseIndex(entity), entity.getType(), "descr", 1, "didn't find the tag");
        // This is a description
        // 123456789012345678901

    }

    @Test
    public void merge_SearchDocIsReWrittenAfterTagMerge() throws Exception {
        assumeTrue(runMe);
        //DAT-279
        logger.info("## merge_SearchDocIsReWrittenAfterTagMerge");
        SystemUser su = registerSystemUser("merge_SimpleSearch");
        Fortress fortress = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("mergeSimpleSearch", false));

        TagInputBean tagInputA = new TagInputBean("TagA", "MoveTag", "rlxA");
        EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");
        inputBean.addTag(tagInputA);
        inputBean.setContent(new ContentInputBean("blah", Helper.getRandomMap()));
        Entity entityA = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entityA, entityService);

        TagInputBean tagInputB = new TagInputBean("TagB", "MoveTag", "rlxB");
        inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInputB);
        // Without content, a search doc will not be created
        inputBean.setContent(new ContentInputBean("blah", Helper.getRandomMap()));

        Entity entityB = mediationFacade.trackEntity(fortress, inputBean).getEntity();
        esHelper.waitForFirstSearchResult(1, su.getCompany(), entityB, entityService);
        Tag tagA = tagService.findTag(su.getCompany(), null, tagInputA.getCode());
        assertNotNull(tagA);
        Tag tagB = tagService.findTag(su.getCompany(), null, tagInputB.getCode());
        assertNotNull(tagB);

        esHelper.doEsFieldQuery(entityA, "tag.rlxa.movetag.code", "taga", 1);
        esHelper.doEsFieldQuery(entityA, "tag.rlxb.movetag.code", "tagb", 1);

        mediationFacade.mergeTags(su.getCompany(), tagA.getId(), tagB.getId());
        Helper.waitAWhile("Merge Tags", 4000);
        // We should not find anything against tagA",
        esHelper.doEsFieldQuery(entityA, "tag.rlxa.movetag.code", "taga", 0);
        esHelper.doEsFieldQuery(entityA, "tag.rlxb.movetag.code", "taga", 0);
        // Both docs will be against TagB
        esHelper.doEsFieldQuery(entityA, "tag.rlxa.movetag.code", "tagb", 1);
        esHelper.doEsFieldQuery(entityA, "tag.rlxb.movetag.code", "tagb", 1);

    }

    @Test
    public void linkedEntity_KeyValues() throws Exception {
        assumeTrue(runMe);
        SystemUser su = registerSystemUser("linkedEntity_KeyValues");
        Fortress timeRecordingFortress = fortressService.registerFortress(
                su.getCompany(), new FortressInputBean("timesheet"));

        EntityInputBean staffInput
                = new EntityInputBean(timeRecordingFortress, "wally", "Staff", new DateTime(), "ABC123")
                .addTag(new TagInputBean("Cleaner", "Position", "role"));
        TrackResultBean staffResult = mediationFacade.trackEntity(su.getCompany(), staffInput);

        esHelper.waitForFirstSearchResult(staffResult, entityService);

        DocumentType docTypeWork = new DocumentType(timeRecordingFortress, "Work");
        docTypeWork = conceptService.findOrCreate(timeRecordingFortress, docTypeWork);

        assertEquals("Version strategy did not default to fortress",
                DocumentType.VERSION.FORTRESS, docTypeWork.getVersionStrategy());

        EntityInputBean workInput =
                new EntityInputBean(timeRecordingFortress, new DocumentTypeInputBean(docTypeWork.getName()), "ABC321");
        TestCase.assertEquals(docTypeWork.getName(), workInput.getDocumentType().getName());

        workInput.addEntityLink("position", new EntityKeyBean(staffInput)
                .setMissingAction(EntityKeyBean.ACTION.ERROR));

        // This track call should link Work to Staff
        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workInput);
        esHelper.waitForFirstSearchResult(workResult, entityService);

        Collection<EntityKeyBean> entities = entityService.getInboundEntities(workResult.getEntity(), true);
        TestCase.assertTrue("Work and Staff were not linked", entities.size() == 1);

        esHelper.doEsFieldQuery(workResult.getEntity(), "e.staff.tag.role.position.code", "cleaner", 1);

    }

    @Test
    public void linkedEntity_AddingTo() throws Exception {
        assumeTrue(runMe);
        SystemUser su = registerSystemUser("linkedEntity_AddingTo", "linkedEntity_AddingTo");
        Fortress timeRecordingFortress = fortressService.registerFortress(
                su.getCompany(), new FortressInputBean("linkedEntity_AddingTo"));

        EntityInputBean staffInput
                = new EntityInputBean(timeRecordingFortress, "wally", "Staff", new DateTime(), "ABC123")
                .addTag(new TagInputBean("Cleaner", "Position", "role"));
        TrackResultBean staffResult = mediationFacade.trackEntity(su.getCompany(), staffInput);

        esHelper.waitForFirstSearchResult(staffResult, entityService);

        DocumentType docTypeWork = new DocumentType(timeRecordingFortress, "Work");
        docTypeWork = conceptService.findOrCreate(timeRecordingFortress, docTypeWork);

        assertEquals("Version strategy did not default to fortress",
                DocumentType.VERSION.FORTRESS, docTypeWork.getVersionStrategy());

        EntityInputBean workInput =
                new EntityInputBean(timeRecordingFortress, new DocumentTypeInputBean(docTypeWork.getName()), "ABC321")
                        .setDescription("a description to search on");
        TestCase.assertEquals(docTypeWork.getName(), workInput.getDocumentType().getName());


        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workInput);
        esHelper.waitForFirstSearchResult(workResult, entityService);

        EntityKeyBean workKey = new EntityKeyBean(workInput)
                .setMissingAction(EntityKeyBean.ACTION.ERROR);

        Collection<EntityKeyBean> parents = new ArrayList<>();
        parents.add(new EntityKeyBean(staffInput)
                .setMissingAction(EntityKeyBean.ACTION.ERROR));

        // Do the link
        TestCase.assertEquals("No requests to link entities should have been ignored", 0,
                entityService.linkEntities(su.getCompany(), workKey, parents, "worked").size());

        // Verify the link worked
        TestCase.assertTrue("Work and Staff were not linked",
                entityService.getInboundEntities(workResult.getEntity(), false).size() == 1);

        // ToDo: This reindex should happen in the service if a link is being added. Currently it's manual
        adminService.doReindex(workResult.getEntity());
        // This simply ensures that the bumpSearch is working when fd-engine.search.update=true
        esHelper.waitForSearchCount(2, workResult, entityService);
        //ToDo: the SearchDoc routines need to be consolidated. REINDEX does not add linked entities
        //esHelper.doEsFieldQuery(workResult.getEntity(), "e.staff.tag.role.position.code", "cleaner",1 );

    }

    @Test
    public void amqp_TrackEntityBatch() throws Exception {
        assumeTrue(runMe);
        logger.info("## amqp_TrackEntity");
        SystemUser su = registerSystemUser("amqp_TrackEntity");
        Fortress fortress = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("amqp_TrackEntity")
                        .setSearchActive(false)
                        .setStoreActive(true));

        //KvService.KV_STORE previousStore = engineConfig.setKvStore(KvService.KV_STORE.RIAK);

        int required = 5;
        int count = 0;
        Collection<EntityInputBean> entityBatch = new ArrayList<>();

        while (count < required) {
            EntityInputBean beanA = Helper
                    .getEntity(fortress, "olivia@sunnybell.com", "DocType", "AAA" + count,
                            new ContentInputBean("blah", Helper.getRandomMap()));
            entityBatch.add(beanA);

            count++;
        }

        Properties properties = getProperties(su);
        AmqpServices amqpServices = null;

        try {
            ClientConfiguration configuration = new ClientConfiguration(properties);
            configuration.setAmqp(true, false);

            amqpServices = new AmqpServices(configuration);

            // ToDo: Figure out response codes

            if (true) {
                // each entity as a separate batch
                for (EntityInputBean entityInputBean : entityBatch) {
                    Collection<EntityInputBean> singleEntity = new ArrayList<>();
                    singleEntity.add(entityInputBean);
                    amqpServices.publish(singleEntity);
                }
            } else {
                // Altogether
                amqpServices.publish(entityBatch);
                // Improving the chance of a deadlock :)
                //        amqpServices.publish(entityBatch);
            }

            Helper.waitAWhile("AMQP", required * 1500);

            // Rerun the same batch
            amqpServices.publish(entityBatch);

            // Changes were not made so checking should be quick
            Helper.waitAWhile("AMQP", required * 750);

            for (EntityInputBean entityInputBean : entityBatch) {
                assertNotNull("" + entityInputBean.toString()
                        , entityService.findByCode(fortress, entityInputBean.getDocumentType().getName(), entityInputBean.getCode()));
            }

        } finally {
            if (amqpServices != null)
                amqpServices.close();
        }

    }

    static Properties getProperties(SystemUser su) throws Exception {
        Properties properties = new Properties();
        FileInputStream f = new FileInputStream("./src/test/resources/config.properties");
        properties.load(f);

        if (su != null)
            properties.put("apiKey", su.getApiKey());

        String fdDebug = System.getProperty("fd.debug");
        if (fdDebug != null)
            runMe = !Boolean.parseBoolean(fdDebug);
        return properties;
    }

    @Test
    public void stressWithHighVolume() throws Exception {
        assumeTrue(false);// Suppressing this for the time being
        logger.info("## stressWithHighVolume");
        int runMax = 10, logMax = 10, fortress = 1;

        Helper.waitAWhile("Wait {} secs for index to delete ");

        SystemUser su = registerSystemUser("Gina");

        ArrayList<Long> list = new ArrayList<>();

        logger.info("FortressCount: " + fortressMax + " RunCount: " + runMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (runMax * logMax * fortressMax) + " messages to be handled");

        StopWatch watch = new StopWatch();
        long totalRows = 0;

        DecimalFormat f = new DecimalFormat("##.000");

        watch.start();
        while (fortress <= fortressMax) {

            String fortressName = "bulkloada" + fortress;
            StopWatch fortressWatch = new StopWatch();
            fortressWatch.start();
            int run = 1;
            long requests = 0;

            Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean(fortressName));
            requests++;
            logger.info("Starting run for " + fortressName);
            while (run <= runMax) {
                boolean searchChecked = false;
                EntityInputBean aib = new EntityInputBean(iFortress, fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + run);
                TrackResultBean arb = mediationFacade.trackEntity(su.getCompany(), aib);
                String metaKey = arb.getEntity().getMetaKey();
                requests++;
                int log = 1;
                while (log <= logMax) {
                    Thread.yield();
                    createLog(su.getCompany(), metaKey, log);
                    Thread.yield();
                    requests++;
                    if (!searchChecked) {
                        searchChecked = true;
                        requests++;
                        watch.suspend();
                        fortressWatch.suspend();
                        esHelper.waitForFirstSearchResult(1, su.getCompany(), arb.getEntity(), entityService);
                        watch.resume();
                        fortressWatch.resume();
                    } // searchCheck done
                    log++;
                } // Logs created
                run++;
            } // Entities finished with
            fortressWatch.stop();
            double fortressRunTime = (fortressWatch.getTime()) / 1000d;
            logger.info("*** {} took {}  [{}] Avg processing time= {}. Requests per second {}",
                    iFortress.getName(),
                    fortressRunTime,
                    requests,
                    f.format(fortressRunTime / requests),
                    f.format(requests / fortressRunTime));
            watch.split();
            //splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + requests;
            list.add(iFortress.getId());
            fortress++;
        }
        watch.stop();
        double totalTime = watch.getTime() / 1000d;
        logger.info("*** Processed {} requests. Data sets created in {} secs. Fortress avg = {} avg requests per second {}",
                totalRows,
                f.format(totalTime),
                f.format(totalTime / fortressMax),
                f.format(totalRows / totalTime));

        validateLogsIndexed(list, runMax, logMax);
        doSearchTests(runMax, list);
    }

    @Test
    public void simpleQueryEPWorksForImportedRecord() throws Exception {
        assumeTrue(runMe);
        String searchFor = "testing";

        SystemUser su = registerSystemUser("Nik");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestFortress"));

        ContentInputBean log = new ContentInputBean("mikeTest", new DateTime(), Helper.getSimpleMap("who", searchFor));
        EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "Query", new DateTime(), "abzz");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);

        QueryParams q = new QueryParams(fortress.getDefaultSegment())
                .setSearchText(searchFor);
        esHelper.doEsQuery(result.getEntity(), searchFor, 1);

        String qResult = esHelper.runQuery(q);
        assertNotNull(qResult);
        assertTrue("Couldn't find a hit in the result [" + result + "]", qResult.contains("total\" : 1"));

    }

    @Test
    public void geo_TagsWork() throws Exception {
        assumeTrue(runMe);
        logger.info("geo_TagsWork");
        SystemUser su = registerSystemUser("geoTag", "geo_Tag");
        // DAT-339
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("GeoFortress"));

        EntityInputBean entityInput = new EntityInputBean(fortress, "geoTest", "geoTest", new DateTime(), "abc");
        ContentInputBean content = new ContentInputBean(Helper.getSimpleMap("Athlete", "Katerina Neumannová"));
        entityInput.setContent(content);
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country", "").setName("United States");
        TagInputBean cityInputTag = new TagInputBean("LA", "City", "").setName(city);
        TagInputBean stateInputTag = new TagInputBean("CA", "State", "").setName("California");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "Institution", "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        entityInput.addTag(institutionTag);

        // Institution<-city<-state<-country
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(resultBean);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), resultBean.getEntity(), entityService);

        esHelper.doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.stateCode", "ca", 1);
        esHelper.doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.stateName", "california", 1);

        esHelper.doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.countryCode", "usa", 1);
        esHelper.doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.countryName", "united states", 1);

        esHelper.doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.cityCode", "la", 1);
        esHelper.doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.cityName", "los angeles", 1);

    }

    @Test
    public void geo_CachingMultiLocations() throws Exception {
        assumeTrue(runMe);
        logger.info("geo_CachingMultiLocations");
        SystemUser su = registerSystemUser("geo_CachingMultiLocations", "geo_CachingMultiLocations");
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("geo_CachingMultiLocations"));

        EntityInputBean entityInput = new EntityInputBean(fortress, "geoTest", "geoTest", new DateTime());
        ContentInputBean content = new ContentInputBean(Helper.getSimpleMap("Athlete", "Katerina Neumannová"));
        entityInput.setContent(content);

        String la = "Los Angeles";

        TagInputBean institutionTag = new TagInputBean("mikecorp", "Institution", "owns");

        TagInputBean unitedStates = new TagInputBean("USA", "Country", "").setName("United States");
        TagInputBean losAngeles = new TagInputBean("LA", ":City", "").setName(la);
        TagInputBean california = new TagInputBean("CA", "State", "").setName("California");

        // Institution is in a city
        institutionTag.setTargets("located", losAngeles);
        losAngeles.setTargets("state", california);
        california.setTargets("country", unitedStates);
        entityInput.addTag(institutionTag);

        // Institution<-city<-state<-country
        TrackResultBean resultBeanA = mediationFacade.trackEntity(su.getCompany(), entityInput);

        // Create second one with different geo data
        entityInput = new EntityInputBean(fortress, "geoTest", "geoTest", new DateTime());
        content = new ContentInputBean(Helper.getSimpleMap("Athlete", "Katerina Neumannová"));
        entityInput.setContent(content);
        institutionTag = new TagInputBean("mikecorpb", "Institution", "owns");
        // Institution is in a city
        TagInputBean portland = new TagInputBean("Dallas", "City", "").setName("Dallas");
        TagInputBean oregon = new TagInputBean("TX", "State", "").setName("Texas");

        institutionTag.setTargets("located", portland);
        portland.setTargets("state", oregon);
        oregon.setTargets("country", unitedStates);
        entityInput.addTag(institutionTag);

        TrackResultBean resultBeanB = mediationFacade.trackEntity(su.getCompany(), entityInput);

        esHelper.waitForFirstSearchResult(1, su.getCompany(), resultBeanA.getEntity(), entityService);
        esHelper.waitForFirstSearchResult(1, su.getCompany(), resultBeanB.getEntity(), entityService);

        esHelper.doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.stateCode", california.getCode().toLowerCase(), 1);
        esHelper.doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.cityName", losAngeles.getName().toLowerCase(), 1);
        esHelper.doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.stateCode", "tx", 1);
        esHelper.doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.cityCode", "dallas", 1);
        esHelper.doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.countryCode", unitedStates.getCode().toLowerCase(), 2);
    }

    @Test
    public void store_DisabledByCallerRef() throws Exception {
        // DAT-347 Check content retrieved from KV Store when storage is disabled
        assumeTrue(runMe);
        logger.debug("## store_DisabledByCallerRef");
        Map<String, Object> json = Helper.getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("store_DisabledByCallerRef");

        FortressInputBean fib = new FortressInputBean("store_DisabledByCallerRef");
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean content = new ContentInputBean("store_Disabled", new DateTime(), json);
        // Test with a CallerRef
        EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "store_Disabled", new DateTime(), "store_Disabled");
        input.setContent(content);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        Helper.waitAWhile("Async log is still processing");
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());

        assertNotNull(entityLog);
        assertEquals(Store.NONE.name(), entityLog.getLog().getStorage());
        // @see TestVersioning.log_ValidateValues - this just adds an actual call to fd-search
        logger.info("Track request made. About to wait for first search result");
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);

        // Want to get the latest version to obtain the search key for debugging
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        assertEquals(input.getCode(), entity.getSearchKey());
        esHelper.doEsQuery(result.getEntity(), json.get("Athlete").toString(), 1);
        StoreContent storeContent = logService.getContent(entity, result.getCurrentLog().getLog());
        assertNotNull(storeContent);
        assertNotNull(storeContent.getData());
        assertEquals(content.getData().get("Athlete"), storeContent.getData().get("Athlete"));

        // This will return a mock entity log
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), null);
        assertNotNull(entityLog);
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), 0l);

        logService.getContent(entity, entityLog.getLog());
        assertNotNull(storeContent);
        assertNotNull(storeContent.getData());
        assertEquals(content.getData().get("Athlete"), storeContent.getData().get("Athlete"));

    }

    @Test
    public void store_DisabledByWithNoCallerRef() throws Exception {
        // DAT-347 Check content retrieved from KV Store when storage is disabled
        assumeTrue(runMe);
        logger.debug("## store_DisabledByWithNoCallerRef");
        Map<String, Object> json = Helper.getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("store_DisabledByWithNoCallerRef");

        FortressInputBean fib = new FortressInputBean("store_DisabledByWithNoCallerRef");
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean content = new ContentInputBean("store_Disabled", new DateTime(), json);
        // Test with a CallerRef
        EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "store_Disabled", new DateTime(), null);
        input.setContent(content);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        Helper.waitAWhile("Async log is still processing");
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());

        assertNotNull(entityLog);
        assertEquals(Store.NONE.name(), entityLog.getLog().getStorage());
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);

        // Want to get the latest version to obtain the search key for debugging
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        assertEquals(entity.getMetaKey(), entity.getSearchKey());
        esHelper.doEsQuery(result.getEntity(), json.get("Athlete").toString(), 1);
        StoreContent storeContent = logService.getContent(entity, result.getCurrentLog().getLog());
        assertNotNull(storeContent);
        assertNotNull(storeContent.getData());
        assertEquals(content.getData().get("Athlete"), storeContent.getData().get("Athlete"));

        // This will return a mock entity log
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), null);
        assertNotNull(entityLog);
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), 0l);

        logService.getContent(entity, entityLog.getLog());
        assertNotNull(storeContent);
        assertNotNull(storeContent.getData());
        assertEquals(content.getData().get("Athlete"), storeContent.getData().get("Athlete"));

    }

    @Test
    public void storeDisabled_ReprocessingContentForExistingEntity() throws Exception {
        // DAT-353 Track in an entity. Validate the content. Update the content. Validate the
        //         update is found.
        assumeTrue(runMe);
        Map<String, Object> json = Helper.getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("## store_DisabledReprocessContent");

        FortressInputBean fib = new FortressInputBean("store_DisabledReprocessContent");
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean content = new ContentInputBean("store_DisabledReprocessContent", new DateTime(), json);
        EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "store_Disabled", new DateTime(), "store_Disabled");
        input.setContent(content);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        Helper.waitAWhile("Async log is still processing");
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());

        assertNotNull(entityLog);
        logger.info("Track request made. About to wait for first search result");
        esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
        // Want to get the latest version to obtain the search key for debugging
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        // Can we find the changed data in ES?
        esHelper.doEsQuery(result.getEntity(), content.getData().get("Athlete").toString(), 1);
        // And are we returning the same data from the KV Service?
        StoreContent storeContent = logService.getContent(entity, result.getCurrentLog().getLog());
        assertNotNull(storeContent);
        assertNotNull(storeContent.getData());
        assertEquals(content.getData().get("Athlete"), storeContent.getData().get("Athlete"));

        content.setData(Helper.getSimpleMap("Athlete", "Michael Phelps"));
        input.setContent(content);
        // Update existing entity
        result = mediationFacade.trackEntity(su.getCompany(), input);
        entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        entityLog = entityService.getLastEntityLog(result.getEntity().getId());
        assertEquals(entity.getFortressCreatedTz().getMillis(), entityLog.getFortressWhen().longValue());
        Helper.waitAWhile("Async log is still processing");
        Helper.waitAWhile("Waiting for second update to occur");

        storeContent = logService.getContent(entity, entityLog.getLog());
        assertNotNull(storeContent);
        assertNotNull(storeContent.getData());
        assertEquals(content.getData().get("Athlete"), storeContent.getData().get("Athlete"));


    }

    /**
     * @throws Exception
     */
    @Test
    public void validate_StringsContainingValidNumbers() throws Exception {
        try {
            assumeTrue(runMe);
            logger.info("## validate_MismatchSubsequentValue");
            SystemUser su = registerSystemUser("validate_MismatchSubsequentValue", "validate_MismatchSubsequentValue");
            assertNotNull(su);
            engineConfig.setStoreEnabled("false");

            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("validate_MismatchSubsequentValue"));
            Map<String, Object> json = Helper.getSimpleMap("NumAsString", "1234");

            // Passing in a string "number", we want this to be preserved
            ContentInputBean content = new ContentInputBean("store_Disabled", new DateTime(), json);
            EntityInputBean input = new EntityInputBean(fortress, "mikeTest", "mismatch", new DateTime(), "store_Disabled");
            input.setContent(content);

            TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
            esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);
            Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
            assertNotNull(entity.getSearchKey());
            StoreContent kvc = logService.getContent(entity, result.getCurrentLog().getLog());
            assertNotNull(kvc);
            assertEquals(json.get("NumAsString"), "1234");

            json = Helper.getSimpleMap("NumAsString", "NA");
            content = new ContentInputBean("store_Disabled", new DateTime(), json);
            // Create a second entity
            EntityInputBean inputB = new EntityInputBean(fortress, "mikeTest", "mismatch", new DateTime(), "store_Disabledxx");
            inputB.setContent(content);

            result = mediationFacade.trackEntity(su.getCompany(), inputB);
            entity = esHelper.waitForFirstSearchResult(1, su.getCompany(), result.getEntity(), entityService);

            esHelper.doEsQuery(result.getEntity(), "*", 2);

            kvc = logService.getContent(entity, result.getCurrentLog().getLog());
            assertNotNull(kvc);
            assertEquals(json.get("NumAsString"), "NA");
        } finally {
            engineConfig.setStoreEnabled("true");
        }

    }

    @Test
    public void tags_TaxonomyStructure() throws Exception {

        assumeTrue(runMe);

        logger.info("## tags_TaxonomyStructure");

        setDefaultAuth();
        SystemUser su = registerSystemUser("tags_TaxonomyStructure", "tags_TaxonomyStructure");
        assertNotNull(su);
        engineConfig.setStoreEnabled("false");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tags_TaxonomyStructure"));
        assertTrue("Search not enabled- this test will fail", fortress.isSearchEnabled());

        DocumentTypeInputBean docType = new DocumentTypeInputBean("DAT-498")
                .setTagStructure(EntityService.TAG_STRUCTURE.TAXONOMY);
        EntityInputBean eib = new EntityInputBean(fortress, docType, "abc");

        TestCase.assertEquals(EntityService.TAG_STRUCTURE.TAXONOMY, eib.getDocumentType().getTagStructure());

        // Establish a multi path hierarchy
        TagInputBean interest = new TagInputBean("Motor", "Interest");
        TagInputBean category = new TagInputBean("cars", "Category");
        TagInputBean luxury = new TagInputBean("luxury cars", "Division");
        TagInputBean brands = new TagInputBean("brands", "Division");
        TagInputBean audi = new TagInputBean("audi", "Division");

        // Working from bottom up - (term)-[..]-(Interest)
        TagInputBean term = new TagInputBean("audi a3", "Term").setEntityLink("viewed");

        term.setTargets("classifying", luxury);

        luxury.setTargets("typed", category);
        category.setTargets("is", interest);

        brands.setTargets("typed", category);
        audi.setTargets("sub", brands);
        term.setTargets("classifying", audi);

        Collection<TagInputBean> tags = new ArrayList<>();
        tags.add(term);
        eib.setTags(tags);
        eib.setEntityOnly(true);

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/track/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(eib))
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        DocumentType foundDoc = conceptService.resolveByDocCode(fortress, docType.getName(), false);
        assertNotNull(foundDoc);
        assertEquals(EntityService.TAG_STRUCTURE.TAXONOMY, foundDoc.getTagStructure());

        TrackRequestResult result = JsonUtils.toObject(response.getResponse().getContentAsByteArray(), TrackRequestResult.class);
        assertNotNull(result);
        assertNotNull(result.getMetaKey());

        // Nested query
        Entity entity = entityService.getEntity(su.getCompany(), result.getMetaKey());
        assertNotNull(entity);
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(su.getCompany(), entity);
        assertEquals(1, entityTags.size());
        EntityTag termTag = entityTags.iterator().next();
        assertEquals("Term", termTag.getTag().getLabel());
        // Validate the structure

        TrackResultBean trackResultBean = new TrackResultBean(entity, foundDoc);
        SearchChange searchDoc = searchService.getSearchDocument(trackResultBean, null);
        assertNotNull(searchDoc);
        assertEquals(EntityService.TAG_STRUCTURE.TAXONOMY, searchDoc.getTagStructure());
        //EntityTag termTag = searchDoc.getTagValues().;
        assertTrue("Couldn't find the term relationship", searchDoc.getTagValues().containsKey("viewed"));
        Map<String, ArrayList<SearchTag>> tagMap = searchDoc.getTagValues().get("viewed");
        assertTrue("Couldn't find the root level term relationship", tagMap.containsKey("term"));
        Collection<SearchTag> searchTags = tagMap.get("term");
        assertEquals(1, searchTags.size());
        SearchTag termSearchTag = searchTags.iterator().next();
        assertEquals("audi a3", termSearchTag.getCode());

        // division, interest and category
        assertEquals(3, termSearchTag.getParent().size());

        Helper.waitAWhile("letting search catchup");

        esHelper.doEsFieldQuery(entity, "metaKey", result.getMetaKey(), 1);

        // Assert that we can find the category as a nested tag in ES
        esHelper.doEsNestedQuery(entity, "tag.viewed.term", "tag.viewed.term.parent.category.code", "cars", 1);


    }

    @Test
    public void segments_ExistInElasticSearch() throws Exception {
        assumeTrue(runMe); // Assets that an entity is created in it's exact segement and can be found across segments

        logger.info("## segments_ExistInElasticSearch");

        setDefaultAuth();
        SystemUser su = registerSystemUser("segments_ExistInElasticSearch", "segments_ExistInElasticSearch");
        assertNotNull(su);
        engineConfig.setStoreEnabled("false");
        Store previousKv = engineConfig.setStore(Store.NONE);
        try {

            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("segmenttest"));
            FortressSegment segment2014 = new FortressSegment(fortress, "2014");
            FortressSegment segment2015 = new FortressSegment(fortress, "2015");
            segment2014 = fortressService.addSegment(segment2014);
            segment2015 = fortressService.addSegment(segment2015);

            // Includes the default segment. Do we want this behaviour??
            assertEquals(3, fortressService.getSegments(fortress).size());

            assertTrue("Search not enabled- this test will fail", fortress.isSearchEnabled());

            DocumentTypeInputBean docType = new DocumentTypeInputBean("DAT-506");
            EntityInputBean entityInputBean =
                    new EntityInputBean(fortress, docType, "abc")
                            .setSegment(segment2014.getCode())
                            .setContent(new ContentInputBean(Helper.getRandomMap()));

            Entity entity2014 = mediationFacade
                    .trackEntity(segment2014, entityInputBean)
                    .getEntity();

            esHelper.waitForFirstSearchResult(1, su.getCompany(), entity2014, entityService);
            assertEquals(segment2014.getCode(), entity2014.getSegment().getCode());

            entityInputBean =
                    new EntityInputBean(fortress, docType, "cba")
                            .setSegment(segment2015.getCode())
                            .setContent(new ContentInputBean(Helper.getRandomMap()));

            Entity entity2015 = mediationFacade
                    .trackEntity(segment2015, entityInputBean)
                    .getEntity();

            assertEquals(segment2015.getCode(), entity2015.getSegment().getCode());
            esHelper.waitForFirstSearchResult(1, su.getCompany(), entity2015, entityService);

            esHelper.doEsQuery(entity2014, "*", 1);
            esHelper.doEsQuery(entity2015, "*", 1);

            // Find both docs across segmented indexes
            esHelper.doEsQuery(fortress.getRootIndex() + ".*", entity2014.getType(), "*", 2);

            // Ensure we can find the entity content when it is stored in a segmented ES index
            EntityLog lastLog = entityService.getLastEntityLog(entity2014.getId());
            StoreContent contentFromEs = logService.getContent(entity2014, lastLog.getLog());
            assertNotNull("fd-store, unable to locate the content from ES", contentFromEs);
        } finally {
            engineConfig.setStore(previousKv);
        }
    }

    private SystemUser registerSystemUser(String companyName, String userName) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(Helper.AUTH_MIKE);
        Company c = companyService.create(companyName);
        SystemUser su = regService.registerSystemUser(c, new RegistrationBean(companyName, userName));
        // creating company alters the schema that sometimes throws a heuristic exception.
        engineConfig.setStoreEnabled("true");
        Thread.yield();
        return su;

    }

    private SystemUser registerSystemUser(String loginToCreate) throws Exception {
        setDefaultAuth();
        return registerSystemUser(company, loginToCreate);
    }

    static String FD_SEARCH = "http://localhost:9081";

    private String runFdViewQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = Helper.getHttpHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange(FD_SEARCH + "v1/query/fdView", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Client tracking error {}", e.getMessage());
        }
        return null;
    }

    private TrackResultBean createLog(Company company, String metaKey, int log) throws Exception {
        return mediationFacade.trackLog(company, new ContentInputBean("olivia@sunnybell.com", metaKey, new DateTime(), Helper.getSimpleMap("who", log)));
    }

    private void validateLogsIndexed(ArrayList<Long> list, int countMax, int expectedLogCount) throws Exception {
        logger.info("Validating logs are indexed");
        int fortress = 0;
        int count = 1;
        //DecimalFormat f = new DecimalFormat("##.000");
        while (fortress < fortressMax) {
            while (count <= countMax) {
                Entity entity = entityService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + count);
                Set<EntityLog> logs = entityService.getEntityLogs(entity);
                assertNotNull(logs);
                assertEquals("Wrong number of logs returned", expectedLogCount, logs.size());
                for (EntityLog log : logs) {
                    assertEquals("logId [" + log.getId() + "] changeId[" + log.getLog().getId() + "], event[ " + log.getLog().getEvent() + "]", true, log.isIndexed());
                }

                count++;
            }
            fortress++;
        }

    }

    private void doSearchTests(int auditCount, ArrayList<Long> list) throws Exception {
        int fortress;
        int searchLoops = 200;
        int search = 0;
        int totalSearchRequests = 0;
        logger.info("Performing Search Tests for {} fortresses", fortressMax);
        StopWatch watch = new StopWatch();
        watch.start();

        do {
            fortress = 0;
            do {
                int x = 1;
                do {
                    int random = (int) (Math.random() * ((auditCount) + 1));
                    if (random == 0)
                        random = 1;

                    Entity entity = entityService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + random);
                    assertNotNull("ABC" + random, entity);
                    assertNotNull("Looks like fd-search is not sending back results", entity.getSearchKey());
                    EntityLog entityLog = logService.getLastLog(entity);
                    assertNotNull(entityLog);

                    assertTrue("fortress " + fortress + " run " + x + " entity " + entity.getMetaKey() + " - " + entityLog.getId(), entityLog.isIndexed());
                    String result = esHelper.doEsTermQuery(entity, EntitySearchSchema.META_KEY, entity.getMetaKey(), 1, true);
                    totalSearchRequests++;
                    esHelper.validateResultFieds(result);

                    x++;
                } while (x < auditCount);
                fortress++;
            } while (fortress < fortressMax);
            search++;
        } while (search < searchLoops);

        watch.stop();
        double end = watch.getTime() / 1000d;
        logger.info("Total Search Requests = " + totalSearchRequests + ". Total time for searches " + end + " avg requests per second = " + totalSearchRequests / end);
    }

}
