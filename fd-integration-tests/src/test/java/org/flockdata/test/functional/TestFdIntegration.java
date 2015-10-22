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

package org.flockdata.test.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.Suggest;
import io.searchbox.core.SuggestResult;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.GetMapping;
import junit.framework.TestCase;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.time.StopWatch;
import org.flockdata.client.amqp.AmqpHelper;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.admin.EngineAdminService;
import org.flockdata.engine.integration.FdChannels;
import org.flockdata.engine.query.service.MatrixService;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.engine.track.endpoint.FdServerWriter;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.kv.KvContent;
import org.flockdata.kv.service.KvService;
import org.flockdata.model.*;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.search.IndexHelper;
import org.flockdata.search.model.*;
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
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
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
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Allows the fd-engine services to be tested against fd-search with actual integration.
 * fd-search is stated by Cargo as a Tomcat server while fd-engine is debuggable in-process.
 * <p>
 * Note that Logs and Search docs are written asyncronously. For this reason you will see
 * various "waitAWhile" loops giving other threads time to process the payloads before
 * making assertions.
 * <p>
 * <p>
 * This approach requires RabbitMQ to be installed to allow integration to occur.
 * <p>
 * No web interface is launched for fd-engine
 * <p>
 * Make sure that you create unique User ids for your test.
 * <p>
 * To run the integration suite:
 * mvn clean install -P integration
 * <p>
 * If you want to debug engine then you add to your command line
 * -Dfd.debug=true -DforkCount=0
 * <p>
 * To debug the search service refer to the commented line in pom.xml where the
 * default port is set to 8000
 * <p>
 * User: nabil, mike
 * Date: 16/07/13
 * Time: 22:51
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath:root-context.xml", "classpath:apiDispatcher-servlet.xml"})
public class TestFdIntegration {
    private static boolean runMe = true; // pass -Dfd.debug=true to disable all tests
    private static int fortressMax = 1;
    private static JestClient esClient;

    @Rule
    // Use this to assert exception conditions
    public final ExpectedException exception = ExpectedException.none();

    @Autowired
    EntityService entityService;

    @Autowired
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
    KvService kvService;

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

    @Autowired
    FdChannels fdChannels;

    private static Logger logger = LoggerFactory.getLogger(TestFdIntegration.class);
    private static Authentication AUTH_MIKE = new UsernamePasswordAuthenticationToken("mike", "123");
    static MockMvc mockMvc;
    String company = "Monowai";
    static Properties properties = new Properties();
    int esTimeout = 10; // Max attempts to find the result in ES

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

    public static void waitAWhile(String message) throws Exception {
        if (message == null)
            message = "Slept for {} seconds";
        waitAWhile(message, getSleepSeconds());
    }

    public static void cleanupElasticSearch() throws Exception {
        FileInputStream f = new FileInputStream("./src/test/resources/config.properties");
        properties.load(f);
        String abDebug = System.getProperty("fd.debug");
        if (abDebug != null)
            runMe = !Boolean.parseBoolean(abDebug);

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "suppress"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "testfortress"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "ngram"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "rebuildtest"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "audittest"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "suppress"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "entitywithtagsprocess"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "trackgraph"), "");
        deleteEsIndex(IndexHelper.getIndexRoot("monowai", "111"), "");

        for (int i = 1; i < fortressMax + 1; i++) {
            deleteEsIndex(IndexHelper.PREFIX + "monowai.bulkloada" + i , "");
        }

    }

    public void setDefaultAuth() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);

        if (mockMvc == null)
            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        engineConfig.setStoreEnabled("true"); // Rest to default state for each test
    }

    private static void deleteEsIndex(String indexName, String docType) throws Exception {
        String deleteMe = IndexHelper.parseIndex(indexName, docType);
        logger.info("%% Delete Index {}", deleteMe);
        esClient.execute(new DeleteIndex.Builder(deleteMe).build());
    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        if (esClient != null)
            esClient.shutdownClient();
    }

    @BeforeClass
    @Rollback(false)
    public static void pingFdSearch() throws Exception {
        // Always run
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = getHttpHeaders(null, null, null);
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        logger.info("**** Checking to see if we can ping fd-search @ {}", FD_SEARCH);
        try {
            ResponseEntity<String> response = restTemplate.exchange(FD_SEARCH + "/fd-search/v1/admin/ping", HttpMethod.GET, requestEntity, String.class);
            assertTrue("didn't get the Pong response", response.getBody().equals("pong"));

        } catch (Exception e) {
            runMe = false; // Everything will fail
            throw new FlockException("Can't connect to FD-Search. No point in continuing");
        }
        cleanupElasticSearch();


    }

    @Before
    public void testHealth() throws Exception {
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


    @Test
    public void search_WhatFieldsIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## dataTypes_WhatFieldsIndexed");

        SystemUser su = registerSystemUser("dataTypes_WhatFieldsIndexed", "dataTypes_WhatFieldsIndexed");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("dataTypes_WhatFieldsIndexed"));
        String docType = "DT";
        String callerRef = "ABC123X";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortress.getName(), "wally", docType, new DateTime(), callerRef);

        Map<String, Object> json = getRandomMap();
        json.put("int", 123);
        json.put("long", 456l);
        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), json);
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();
        waitForFirstSearchResult(su.getCompany(), entity.getMetaKey());

        doEsQuery(entity, entity.getMetaKey());
        doEsFieldQuery(entity, EntitySearchSchema.WHAT + ".int", "123", 1);
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
                new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime(), getRandomMap());
        entityInputBean.setContent(contentInputBean);

        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();

        assertEquals(IndexHelper.PREFIX + "testcompany.tracktest", entity.getFortress().getRootIndex());

        waitForFirstSearchResult(su.getCompany(), entity.getMetaKey());

        doEsQuery(entity, entity.getMetaKey());
    }

    @Test
    public void search_pdfTrackedAndFound() throws Exception {
        assumeTrue(runMe);
        logger.info("## search_pdfTrackedAndFound");

        SystemUser su = registerSystemUser("pdf_TrackedAndFound", "co-fortress");
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("pdf_TrackedAndFound"));
        assertTrue("Search should not be disabled", fortressA.isSearchEnabled());
        String docType = "Contract";
        String callerRef = "PDF-TRACK-123";
        EntityInputBean entityInputBean =
                new EntityInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        ContentInputBean contentInputBean = new ContentInputBean("wally", new DateTime());
        contentInputBean.setAttachment(Helper.getPdfDoc(), "pdf", "test.pdf");
        entityInputBean.setContent(contentInputBean);

        TrackResultBean trackResultBean = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean);

        assertNotNull(trackResultBean.getCurrentLog().getLog().getFileName());

        Entity entity = trackResultBean.getEntity();

        waitForFirstSearchResult(su.getCompany(), entity.getMetaKey());

        EntityLog lastLog = logService.getLastLog(entity);
        assertNotNull(lastLog);
        assertNotNull(lastLog.getLog().getFileName());

        waitAWhile("Attachment Mapper can take some time to process the PDF");
        doEsQuery(entity, "*", 1);
        doEsQuery(entity, "brown fox", 1);
        doEsQuery(entity, contentInputBean.getFileName(), 1);
        doEsFieldQuery(entity, EntitySearchSchema.META_KEY, entity.getMetaKey(), 1);
        doEsFieldQuery(entity, EntitySearchSchema.FILENAME, "test.pdf", 1);
        doEsFieldQuery(entity, EntitySearchSchema.ATTACHMENT, "pdf", 1);
    }


    @Test
    public void track_WithOnlyTagsTracksToSearch() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_WithOnlyTagsTracksToSearch");
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        SystemUser su = registerSystemUser("Mark");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityWithTagsProcess"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TrackTags", now, "ABCXYZ123");
        inputBean.setEntityOnly(true);
        inputBean.addTag(new TagInputBean("testTagNameZZ", null, "someAuditRLX"));
        inputBean.setEvent("TagTest");
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(summary);
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getEntity(), inputBean.getEvent(), 1);
        // Can we find the Tag
        doEsQuery(summary.getEntity(), "testTagNameZZ", 1);

    }

    @Test
    public void track_UserDefinedProperties() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_UserDefinedProperties");
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        SystemUser su = registerSystemUser("Mittens");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("track_UserDefinedProperties"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TrackTags", now, "ABCXYZ123");
        inputBean.setEntityOnly(true);

        inputBean.setProperty("myString", "hello world");
        inputBean.setProperty("myNum", 123.45);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(summary);
        doEsQuery(summary.getEntity(), "hello world", 1);
        doEsQuery(summary.getEntity(), "123.45", 1);

    }

    @Test
    public void search_passThroughQuery() throws Exception {
        assumeTrue(runMe);
        logger.info("## searc_passThroughQuery");
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        SystemUser su = registerSystemUser("searc_passThroughQuery");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("searc_passThroughQuery"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TrackTags", now);
        inputBean.setEntityOnly(true);

        inputBean.setProperty("myString", "hello world");
        inputBean.setProperty("myNum", 123.45);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        logger.debug("Created Request ");
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(summary);
        QueryParams qp = new QueryParams(fo.getDefaultSegment());
        String queryString = "{\"query_string\": {\n" +
                "      \"query\": \"hello world\"\n" +
                "  }}";
        Map<String, Object> query = JsonUtils.getAsMap(queryString);

        qp.setQuery(query);
        EsSearchResult searchResult = queryService.search(su.getCompany(), qp);
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getTotalHits());
        Map<String, Object> mapResult = JsonUtils.getAsMap(searchResult.getJson());
        assertFalse(mapResult.isEmpty());
    }

    @Test
    public void track_immutableEntityWithNoLogsAreIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_immutableEntityWithNoLogsAreIndexed");
        SystemUser su = registerSystemUser("Manfred");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("immutableEntityWithNoLogsAreIndexed"));
        DateTime now = new DateTime();
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "immutableLoc", now, "ZZZ123");
        inputBean.setEvent("immutableEntityWithNoLogsAreIndexed");
        inputBean.setEntityOnly(true); // Must be true to make over to search
        TrackResultBean trackResult;
        trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        waitForFirstSearchResult(su.getCompany(), trackResult.getEntity().getMetaKey());
        EntitySummaryBean summary = mediationFacade.getEntitySummary(su.getCompany(), trackResult.getEntity().getMetaKey());
        waitForFirstSearchResult(su.getCompany(), trackResult.getEntity().getMetaKey());
        assertNotNull(summary);
        assertSame("change logs were not expected", 0, summary.getChanges().size());
        assertNotNull("Search record not received", summary.getEntity().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getEntity(), inputBean.getEvent(), 1);

        // Not flagged as meta only so will not appear in the search index until a log is created
        inputBean = new EntityInputBean(fo.getName(), "wally", inputBean.getDocumentName(), now, "ZZZ999");
        trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        summary = mediationFacade.getEntitySummary(su.getCompany(), trackResult.getEntity().getMetaKey());
        assertNotNull(summary);
        assertSame("No change logs were expected", 0, summary.getChanges().size());
        assertEquals(null, summary.getEntity().getSearch());
        // Check we can't find the Event in ElasticSearch
        doEsQuery(summary.getEntity(), "ZZZ999", 0);
    }

    @Test
    public void admin_rebuildSearchIndexFromEngine() throws Exception {
        //assumeTrue(runMe);
        logger.info("## admin_rebuildSearchIndexFromEngine");
        SystemUser su = registerSystemUser("David");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("rebuildTest"));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "RBSearch", new DateTime(), "ABC123");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean auditResult = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Entity entity = entityService.getEntity(su.getCompany(), auditResult.getEntity().getMetaKey());
        waitForFirstSearchResult(su.getCompany(), entity);

        doEsQuery(entity, "*");

        // Rebuild....
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        Long lResult = adminService.doReindex(fo).get();
        waitForFirstSearchResult(su.getCompany(), entity);
        assertNotNull(lResult);
        assertEquals(1l, lResult.longValue());

        doEsQuery(entity, "*");

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

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "LogTiming", new DateTime(), "ABC123");
        TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);

        metaKey = trackResult.getEntity().getMetaKey();

        Entity entity = entityService.getEntity(su.getCompany(), metaKey);
        assertNotNull(entity);
        assertNotNull(entityService.findByCode(fo, inputBean.getDocumentName(), inputBean.getCode()));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;

        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), getSimpleMap("blah", i))).getEntity();

            i++;
        }
        waitForLogCount(su.getCompany(), entity, max);
        waitForFirstSearchResult(su.getCompany(), metaKey);

        watch.stop();
        doEsFieldQuery(entity, EntitySearchSchema.WHAT + ".blah", "*", 1);
    }

    @Test
    public void track_IgnoreGraphAndCheckSearch() throws Exception {
        assumeTrue(runMe);
        logger.info("## track_IgnoreGraphAndCheckSearch started");
        SystemUser su = registerSystemUser("Isabella");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TrackGraph"));

        EntityInputBean entityInput = new EntityInputBean(fortress.getName(), "wally", "ignoreGraph", new DateTime(), "ABC123");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true); // If true, the entity will be indexed
        // Track is suppressed but search is enabled, so the enity will not exist.
        exception.expect(NotFoundException.class);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);

        //assertEquals(IndexHelper.PREFIX + "monowai.trackgraph.", indexName);

        // Putting asserts On elasticsearch
        doEsQuery(result.getEntity(), "*", 1);
        entityInput = new EntityInputBean(fortress.getName(), "wally", entityInput.getDocumentName(), new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        doEsQuery(result.getEntity(), "*", 2);

        entityInput = new EntityInputBean(fortress.getName(), "wally", entityInput.getDocumentName(), new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        Entity entity = mediationFacade.trackEntity(su.getCompany(), entityInput).getEntity();
        assertNull(entity.getMetaKey());
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(result.getEntity(), "*", 2);

        entityInput = new EntityInputBean(fortress.getName(), "wally", entityInput.getDocumentName(), new DateTime(), "ABC124");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(result.getEntity(), "*", 2);

        entityInput = new EntityInputBean(fortress.getName(), "wally", entityInput.getDocumentName(), new DateTime(), "ABC125");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true);
        mediationFacade.trackEntity(su.getCompany(), entityInput);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(result.getEntity(), "*", 3);

    }

    @Test
    public void cancel_searchDocIsRewrittenAfterCancellingLogs() throws Exception {
        assumeTrue(runMe);
        logger.info("## cancel_searchDocIsRewrittenAfterCancellingLogs");
        SystemUser su = registerSystemUser("Felicity");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("cancelLogTag"));
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        String created = "2010-11-20 11:30:00"; // Create
        String fUpdate = "2010-11-20 11:45:00"; // First Update
        String fUpdate2 = "2010-11-21 11:45:00"; // First Update
        DateTime createdDate = new DateTime(Timestamp.valueOf(created));
        DateTime fUpdateDate = new DateTime(Timestamp.valueOf(fUpdate));
        DateTime fUpdateDate2 = new DateTime(Timestamp.valueOf(fUpdate2));

        EntityInputBean entityInput = new EntityInputBean(fo.getName(), "wally", "CancelDoc", createdDate, "ABC123");
        ContentInputBean content = new ContentInputBean("wally", createdDate, getRandomMap());
        entityInput.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        entityInput.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        entityInput.setContent(content);
        // Create the Entity and Log
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertEquals("Fortress Create date did not match", createdDate.getMillis(), result.getEntity().getFortressCreatedTz().getMillis());
        DateTime fdWhen = new DateTime(result.getEntity().getDateCreated());
        assertNotEquals("FlockData's when date should be the current year", createdDate.getYear(), fdWhen.getYear());
        waitForFirstSearchResult(su.getCompany(), result.getEntity());

        // ensure non-analysed tags work
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        // Analyzed tags require exact match...
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Happy Days", 1);
        doEsQuery(result.getEntity(), "happy days", 1);
        // We now have 1 content doc with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        content = new ContentInputBean("wally", fUpdateDate2, getRandomMap());
        entityInput.getTags().clear();
        entityInput.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        entityInput.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        entityInput.setContent(content);
        // !!Second Update!!
        result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), result.getMetaKey());

        assertEquals("Created date changed after an update - wrong", createdDate, entity.getFortressCreatedTz());
        assertEquals("Update dates did not reconcile", fUpdateDate2, entity.getFortressUpdatedTz());
        EntityLog lastLog = logService.getLastLog(entity);
        assertEquals("Second Update not recorded", Long.valueOf(fUpdateDate2.getMillis()), lastLog.getFortressWhen());

        waitAWhile("Waiting for search to affect");

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

        String json = doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Sad Days", 1);
        Map<String, Object> searchDoc = JsonUtils.getAsMap(json);
        Long searchCreated = Long.parseLong(searchDoc.get(EntitySearchSchema.CREATED).toString());
        Long searchUpdated = Long.parseLong(searchDoc.get(EntitySearchSchema.UPDATED).toString());
        assertTrue("Fortress update was not set in to searchDoc", searchUpdated > 0);
        assertEquals("Created date mismatch", createdDate.getMillis(), searchCreated.longValue());
        assertEquals("Last Change date mismatch: expected " + fUpdate2 + " was " + new DateTime(searchUpdated), fUpdateDate2.getMillis(), searchUpdated.longValue());
        doEsTermQuery(entity, EntitySearchSchema.TAG + ".testingc.tag.code.facet", "Days Bay", 1);
        // These were removed in the update
        doEsTermQuery(entity, EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 0);
        doEsTermQuery(entity, EntitySearchSchema.TAG + ".testingb.tag.code.facet", "happy days", 0);

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), result.getEntity());
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        Collection<EntityTag> entityTags = entityTagService.getEntityTags(result.getEntity());
        assertEquals(2, entityTags.size());

        // These should have been added back in due to the cancel operation
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Happy Days", 1);

        // These were removed in the cancel
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.code", "Sad Days", 0);
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingc.code", "Days Bay", 0);


    }

    @Test
    public void tag_UniqueKeySearch() throws Exception {
        // DAT-95
        assumeTrue(runMe);
        logger.info("## tag_UniqueKeySearch");
        SystemUser su = registerSystemUser("Cameron");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("tag_UniqueKeySearch"));
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "UniqueKeySearch", new DateTime(), "tag_UniqueKeySearch");
        ContentInputBean log = new ContentInputBean("wally", new DateTime(), getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        inputBean.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        waitForFirstSearchResult(su.getCompany(), result.getEntity().getMetaKey());
        // ensure that non-analysed tags work
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Happy Days", 1);
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingb.tag.code.facet", "Sad Days", 1);
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingc.tag.code.facet", "Days Bay", 1);
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testingc.tag.code", "days", 1);

    }

    @Test
    public void user_NoFortressUserWorks() throws Exception {
        // DAT-317
        assumeTrue(runMe);
        logger.info("## user_NoFortressUserWorks");
        SystemUser su = registerSystemUser("piper");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("user_NoFortressUserWorks"));

        // FortressUser cannot be resolved from the entity or the log
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), null, "UniqueKey", new DateTime(), "ABC123");
        ContentInputBean log = new ContentInputBean(null, new DateTime(), getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.setContent(log);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        waitForFirstSearchResult(su.getCompany(), result.getEntity().getMetaKey());
        // ensure that non-analysed tags work
        doEsTermQuery(result.getEntity(), EntitySearchSchema.TAG + ".testinga.tag.code", "happy", 1);
        QueryParams queryParams = new QueryParams();
        queryParams.setCompany(su.getCompany().getName());
        queryParams.setFortress(fo.getName());
        queryParams.setSearchText("*");
        EsSearchResult results = queryService.search(su.getCompany(), queryParams);
        assertNotNull(results);
        assertEquals(1, results.getResults().size());

    }

    @Test
    public void search_withNoMetaKeysDoesNotError() throws Exception {
        // DAT-83
        assumeTrue(runMe);
        logger.info("## search_withNoMetaKeysDoesNotError");
        SystemUser su = registerSystemUser("HarryIndex");
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("searchIndexWithNoMetaKeysDoesNotError"));

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setTrackSuppressed(true); // Write a search doc only
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        // First entity and log, but not stored in graph
        mediationFacade.trackEntity(su.getCompany(), inputBean); // Expect a mock result as we're not tracking

        inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());

        waitForFirstSearchResult(su.getCompany(), entity); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery(entity, "*", 2);

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

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestQuery", new DateTime(), "ABC123");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        waitForFirstSearchResult(su.getCompany(), result.getEntity()); // 2nd document in the index

        inputBean = new EntityInputBean(fo.getName(), "wally", inputBean.getDocumentName(), new DateTime(), "ABC124");
        inputBean.setContent(new ContentInputBean("wally", new DateTime(), getRandomMap()));
        result = mediationFacade.trackEntity(su.getCompany(), inputBean);

        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());

        waitForFirstSearchResult(su.getCompany(), entity); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery(entity, "*", 2);

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

        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "TestTrack", fortressDateCreated, "ABC123");
        inputBean.setLastChange(lastUpdated.toDate());
        inputBean.setContent(new ContentInputBean("wally", lastUpdated, getRandomMap()));

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean); // Mock result as we're not tracking
        waitForFirstSearchResult(su.getCompany(), result.getEntity());

        Entity entity = result.getEntity();

        assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(entity.getFortressCreatedTz()));

        EntityLog log = entityService.getLastEntityLog(su.getCompany(), result.getEntity().getMetaKey());
        assertNotNull(log);
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(ftz)));

        // We have one with a metaKey and one without
        doEsQuery(entity, "*", 1);

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
                new EntityInputBean(fortress.getName(), "wally", docType, new DateTime());

        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.code in ES
        entityInputBean.addTag(new TagInputBean("labelA", "ThisLabel", relationshipName));
        entityInputBean.addTag(new TagInputBean("labelB", "ThatLabel", relationshipName));
        entityInputBean.setEntityOnly(true);
        Entity entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();
        waitForFirstSearchResult(su.getCompany(), entity.getMetaKey());

        // Second Document
        entityInputBean = new EntityInputBean(fortress.getName(), "wally", docType, new DateTime());
        entityInputBean.addTag(new TagInputBean("labelA", "ThisLabel", relationshipName));
        entityInputBean.addTag(new TagInputBean("labelB", "ThatLabel", relationshipName));

        entityInputBean.setEntityOnly(true);

        entity = mediationFacade
                .trackEntity(su.getCompany(), entityInputBean)
                .getEntity();

        waitForFirstSearchResult(su.getCompany(), entity.getMetaKey());

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
                        .content(JsonUtils.getJSON(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), EsSearchResult.class);
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
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress"));
        EntityInputBean inputBean = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        //Transaction tx = getTransaction();
        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), indexedResult.getEntity().getMetaKey());

        EntityLog resultBean = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), getSimpleMap("who", "andy"))).getCurrentLog();
        assertNotNull(resultBean);

        waitForFirstSearchResult(su.getCompany(), entity);

        doEsQuery(entity, "andy");

        inputBean = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setSearchSuppressed(true);
        TrackResultBean noIndex = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity noIndexEntity = entityService.getEntity(su.getCompany(), noIndex.getEntity().getMetaKey());

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", noIndexEntity.getMetaKey(), new DateTime(), getSimpleMap("who", "bob")));
        // Bob's not there because we said we didn't want to index that entity
        doEsQuery(entity, "bob", 0);
        doEsQuery(entity, "andy");
    }

    @Test
    public void tag_ReturnsSingleSearchResult() throws Exception {
        assumeTrue(runMe);
        logger.info("## tag_ReturnsSingleSearchResult");

        SystemUser su = registerSystemUser("Peter");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress"));
        EntityInputBean metaInput = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.code in ES
        TagInputBean tag = new TagInputBean("Code Test Works", null, relationshipName);
        metaInput.addTag(tag);

        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), metaInput);
        Entity entity = entityService.getEntity(su.getCompany(), indexedResult.getEntity().getMetaKey());

        Collection<EntityTag> tags = entityTagService.getEntityTags(entity);
        assertNotNull(tags);
        assertEquals(1, tags.size());

        EntityLog resultBean = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), getRandomMap())).getCurrentLog();
        assertNotNull(resultBean);

        waitForFirstSearchResult(su.getCompany(), entity);
        doEsTermQuery(entity, "tag." + relationshipName + ".tag.code.facet", "Code Test Works", 1);
        doEsQuery(entity, "code test works", 1);

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
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        inputBean.setContent(new ContentInputBean("olivia@sunnybell.com", firstDate, getSimpleMap("house", "house1")));
        String metaKey = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity().getMetaKey();

        Entity entity = entityService.getEntity(su.getCompany(), metaKey);
        waitForFirstSearchResult(su.getCompany(), entity.getMetaKey());

        // Initial create
        doEsTermQuery(entity, EntitySearchSchema.WHAT + ".house", "house1", 1); // First log

        // Now make an amendment
        EntityLog secondLog =
                mediationFacade.trackLog(su.getCompany(), new ContentInputBean("isabella@sunnybell.com", entity.getMetaKey(), firstDate.plusDays(1), getSimpleMap("house", "house2"))).getCurrentLog();
        assertNotSame(0l, secondLog.getFortressWhen());

        Set<EntityLog> logs = entityService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(2, logs.size());
        entity = entityService.getEntity(su.getCompany(), metaKey);

        waitAWhile("cancel function step 1");
        Assert.assertEquals("Last Updated dates don't match", secondLog.getFortressWhen().longValue(), entity.getFortressUpdatedTz().getMillis());
        doEsTermQuery(entity, EntitySearchSchema.WHAT + ".house", "house2", 1); // replaced first with second

        // Now cancel the last log
        mediationFacade.cancelLastLog(su.getCompany(), entity);
        waitAWhile("Cancel function step 2");
        logs = entityService.getEntityLogs(fortress.getCompany(), entity.getMetaKey());
        assertEquals(1, logs.size());
        entity = entityService.getEntity(su.getCompany(), metaKey); // Refresh the entity
        waitAWhile("Cancel 2");
        // Should have restored the content back to house1
        doEsTermQuery(entity, EntitySearchSchema.WHAT + ".house", "house1", 1); // Cancelled, so Back to house1

        // Last change cancelled
        // DAT-96
        mediationFacade.cancelLastLog(su.getCompany(), entity);
        logs = entityService.getEntityLogs(entity);
        assertEquals(true, logs.isEmpty());
        waitAWhile("Cancel function step 3");
        doEsQuery(entity, "*", 0);

        entity = entityService.getEntity(su.getCompany(), metaKey); // Refresh the entity
        assertEquals("Search Key set to callerRef", entity.getCode(), entity.getSearchKey());
    }

    @Test
    public void search_nGramDefaults() throws Exception {
        assumeTrue(runMe);
        logger.info("## search_nGramDefaults");
        SystemUser su = registerSystemUser("Romeo");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram"));
        EntityInputBean inputBean = new EntityInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        TagInputBean tagInputBean = new TagInputBean("Description", "testLabel", "linked");
        inputBean.addTag(tagInputBean);

        TrackResultBean indexedResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), indexedResult.getEntity().getMetaKey());

        Map<String, Object> what = getSimpleMap(EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        entity = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", entity.getMetaKey(), new DateTime(), what)).getEntity();
        waitForFirstSearchResult(su.getCompany(), entity);

        String indexName = entity.getFortress().getRootIndex();
        getMapping(indexName);

        // Completion only works as "Starts with"
        doCompletionQuery(IndexHelper.parseIndex(entity), entity.getType(), "des", 1, "didn't find the tag");
        doCompletionQuery(IndexHelper.parseIndex(entity), entity.getType(),"descr", 1, "didn't find the tag");
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
        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");
        inputBean.addTag(tagInputA);
        inputBean.setContent(new ContentInputBean("blah", getRandomMap()));
        Entity entityA = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        waitForFirstSearchResult(su.getCompany(), entityA.getMetaKey());

        TagInputBean tagInputB = new TagInputBean("TagB", "MoveTag", "rlxB");
        inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInputB);
        // Without content, a search doc will not be created
        inputBean.setContent(new ContentInputBean("blah", getRandomMap()));

        Entity entityB = mediationFacade.trackEntity(fortress, inputBean).getEntity();
        waitForFirstSearchResult(su.getCompany(), entityB.getMetaKey());
        Tag tagA = tagService.findTag(su.getCompany(), null, tagInputA.getCode());
        assertNotNull(tagA);
        Tag tagB = tagService.findTag(su.getCompany(), null, tagInputB.getCode());
        assertNotNull(tagB);

        doEsFieldQuery(entityA, "tag.rlxa.movetag.code", "taga", 1);
        doEsFieldQuery(entityA, "tag.rlxb.movetag.code", "tagb", 1);

        mediationFacade.mergeTags(su.getCompany(), tagA.getId(), tagB.getId());
        waitAWhile("Merge Tags", 4000);
        // We should not find anything against tagA",
        doEsFieldQuery(entityA, "tag.rlxa.movetag.code", "taga", 0);
        doEsFieldQuery(entityA, "tag.rlxb.movetag.code", "taga", 0);
        // Both docs will be against TagB
        doEsFieldQuery(entityA, "tag.rlxa.movetag.code", "tagb", 1);
        doEsFieldQuery(entityA, "tag.rlxb.movetag.code", "tagb", 1);

    }

    @Test
    public void amqp_TrackEntity() throws Exception {
        assumeTrue(runMe);
        logger.info("## amqp_TrackEntity");
        SystemUser su = registerSystemUser("amqp_TrackEntity");
        Fortress fortress = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("amqp_TrackEntity", false));

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "olivia@sunnybell.com", "DocType", DateTime.now(), "AAA");

        inputBean.setContent(new ContentInputBean("blah", getRandomMap()));
        //Properties configProperties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("/config.properties"));
        properties.put("apiKey", su.getApiKey());

        ClientConfiguration configuration = new ClientConfiguration(properties);
        configuration.setAmqp(true, false);

        AmqpHelper helper = new AmqpHelper(configuration);

        // ToDo: We're not tracking the response code
        Collection<EntityInputBean> batchBeans = new ArrayList<>();
        batchBeans.add(inputBean);
        helper.publish(batchBeans);
        waitAWhile("AMQP", 8000);
        helper.close();
        Entity entityA = entityService.findByCode(fortress, inputBean.getDocumentName(), inputBean.getCode());
        assertNotNull(entityA);


    }

    @Test
    public void stressWithHighVolume() throws Exception {
        assumeTrue(false);// Suppressing this for the time being
        logger.info("## stressWithHighVolume");
        int runMax = 10, logMax = 10, fortress = 1;

        //for (int i = 1; i < fortressMax + 1; i++) {
            //deleteEsIndex(IndexHelper.PREFIX + "monowai.bulkloada" + i + ".companynode");
            //doEsQuery(IndexHelper.PREFIX + "monowai.bulkloada" + i+".companynode", "*", -1);
        //}

        waitAWhile("Wait {} secs for index to delete ");

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
                EntityInputBean aib = new EntityInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + run);
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
                        waitForFirstSearchResult(su.getCompany(), metaKey);
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

        ContentInputBean log = new ContentInputBean("mikeTest", new DateTime(), getSimpleMap("who", searchFor));
        EntityInputBean input = new EntityInputBean("TestFortress", "mikeTest", "Query", new DateTime(), "abzz");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        waitForFirstSearchResult(su.getCompany(), result.getEntity().getMetaKey());

        QueryParams q = new QueryParams(fortress.getDefaultSegment())
                .setSearchText(searchFor);
        doEsQuery(result.getEntity(), searchFor, 1);

        String qResult = runQuery(q);
        assertNotNull(qResult);
        assertTrue("Couldn't find a hit in the result [" + result + "]", qResult.contains("total\" : 1"));

    }

    @Test
    public void utfText() throws Exception {
        assumeTrue(runMe);
        Map<String, Object> json = getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("Utf8");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("UTF8-Test"));

        ContentInputBean log = new ContentInputBean("mikeTest", new DateTime(), json);
        EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "UtfTextCode", new DateTime(), "abzz");
        input.setContent(log);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        logger.info("Track request made. About to wait for first search result");
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        doEsQuery(result.getEntity(), json.get("Athlete").toString(), 1);
    }

    @Test
    public void geo_TagsWork() throws Exception {
        assumeTrue(runMe);
        logger.info("geo_TagsWork");
        SystemUser su = registerSystemUser("geoTag", "geo_Tag");
        // DAT-339
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("GeoFortress"));

        EntityInputBean entityInput = new EntityInputBean(fortress.getName(), "geoTest", "geoTest", new DateTime(), "abc");
        ContentInputBean content = new ContentInputBean(getSimpleMap("Athlete", "Katerina Neumannová"));
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
        waitForFirstSearchResult(su.getCompany(), resultBean.getEntity());

        doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.stateCode", "ca", 1);
        doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.stateName", "california", 1);

        doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.countryCode", "usa", 1);
        doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.countryName", "united states", 1);

        doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.cityCode", "la", 1);
        doEsFieldQuery(resultBean.getEntity(), "tag.owns.institution.geo.cityName", "los angeles", 1);

    }

    @Test
    public void geo_CachingMultiLocations() throws Exception {
        assumeTrue(runMe);
        logger.info("geo_CachingMultiLocations");
        SystemUser su = registerSystemUser("geo_CachingMultiLocations", "geo_CachingMultiLocations");
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("geo_CachingMultiLocations"));

        EntityInputBean entityInput = new EntityInputBean(fortress.getName(), "geoTest", "geoTest", new DateTime());
        ContentInputBean content = new ContentInputBean(getSimpleMap("Athlete", "Katerina Neumannová"));
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
        entityInput = new EntityInputBean(fortress.getName(), "geoTest", "geoTest", new DateTime());
        content = new ContentInputBean(getSimpleMap("Athlete", "Katerina Neumannová"));
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

        waitForFirstSearchResult(su.getCompany(), resultBeanA.getEntity());
        waitForFirstSearchResult(su.getCompany(), resultBeanB.getEntity());

        doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.stateCode", california.getCode().toLowerCase(), 1);
        doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.cityName", losAngeles.getName().toLowerCase(), 1);
        doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.stateCode", "tx", 1);
        doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.cityCode", "dallas", 1);
        doEsFieldQuery(resultBeanA.getEntity(), "tag.owns.institution.geo.countryCode", unitedStates.getCode().toLowerCase(), 2);
    }

    @Test
    public void store_DisabledByCallerRef() throws Exception {
        // DAT-347 Check content retrieved from KV Store when storage is disabled
        assumeTrue(runMe);
        logger.debug("## store_DisabledByCallerRef");
        Map<String, Object> json = getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("store_DisabledByCallerRef");

        FortressInputBean fib = new FortressInputBean("store_DisabledByCallerRef");
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean content = new ContentInputBean("store_Disabled", new DateTime(), json);
        // Test with a CallerRef
        EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "store_Disabled", new DateTime(), "store_Disabled");
        input.setContent(content);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        waitAWhile("Async log is still processing");
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());

        assertNotNull(entityLog);
        assertEquals(KvService.KV_STORE.NONE.name(), entityLog.getLog().getStorage());
        // @see TestVersioning.log_ValidateValues - this just adds an actual call to fd-search
        logger.info("Track request made. About to wait for first search result");
        waitForFirstSearchResult(su.getCompany(), result.getEntity());

        // Want to get the latest version to obtain the search key for debugging
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        assertEquals(input.getCode(), entity.getSearchKey());
        doEsQuery(result.getEntity(), json.get("Athlete").toString(), 1);
        KvContent kvContent = kvService.getContent(entity, result.getCurrentLog().getLog());
        assertNotNull(kvContent);
        assertNotNull(kvContent.getWhat());
        assertEquals(content.getWhat().get("Athlete"), kvContent.getWhat().get("Athlete"));

        // This will return a mock entity log
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), null);
        assertNotNull(entityLog);
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), 0l);

        kvService.getContent(entity, entityLog.getLog());
        assertNotNull(kvContent);
        assertNotNull(kvContent.getWhat());
        assertEquals(content.getWhat().get("Athlete"), kvContent.getWhat().get("Athlete"));

    }

    @Test
    public void store_DisabledByWithNoCallerRef() throws Exception {
        // DAT-347 Check content retrieved from KV Store when storage is disabled
        assumeTrue(runMe);
        logger.debug("## store_DisabledByWithNoCallerRef");
        Map<String, Object> json = getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("store_DisabledByWithNoCallerRef");

        FortressInputBean fib = new FortressInputBean("store_DisabledByWithNoCallerRef");
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean content = new ContentInputBean("store_Disabled", new DateTime(), json);
        // Test with a CallerRef
        EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "store_Disabled", new DateTime(), null);
        input.setContent(content);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        waitAWhile("Async log is still processing");
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());

        assertNotNull(entityLog);
        assertEquals(KvService.KV_STORE.NONE.name(), entityLog.getLog().getStorage());
        waitForFirstSearchResult(su.getCompany(), result.getEntity());

        // Want to get the latest version to obtain the search key for debugging
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        assertEquals(entity.getMetaKey(), entity.getSearchKey());
        doEsQuery(result.getEntity(), json.get("Athlete").toString(), 1);
        KvContent kvContent = kvService.getContent(entity, result.getCurrentLog().getLog());
        assertNotNull(kvContent);
        assertNotNull(kvContent.getWhat());
        assertEquals(content.getWhat().get("Athlete"), kvContent.getWhat().get("Athlete"));

        // This will return a mock entity log
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), null);
        assertNotNull(entityLog);
        entityLog = entityService.getEntityLog(su.getCompany(), entity.getMetaKey(), 0l);

        kvService.getContent(entity, entityLog.getLog());
        assertNotNull(kvContent);
        assertNotNull(kvContent.getWhat());
        assertEquals(content.getWhat().get("Athlete"), kvContent.getWhat().get("Athlete"));

    }

    @Test
    public void storeDisabled_ReprocessingContentForExistingEntity() throws Exception {
        // DAT-353 Track in an entity. Validate the content. Update the content. Validate the
        //         update is found.
        assumeTrue(runMe);
        Map<String, Object> json = getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("## store_DisabledReprocessContent");

        FortressInputBean fib = new FortressInputBean("store_DisabledReprocessContent");
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        ContentInputBean content = new ContentInputBean("store_DisabledReprocessContent", new DateTime(), json);
        EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "store_Disabled", new DateTime(), "store_Disabled");
        input.setContent(content);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
        waitAWhile("Async log is still processing");
        EntityLog entityLog = entityService.getLastEntityLog(result.getEntity().getId());

        assertNotNull(entityLog);
        logger.info("Track request made. About to wait for first search result");
        waitForFirstSearchResult(su.getCompany(), result.getEntity());
        // Want to get the latest version to obtain the search key for debugging
        Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        // Can we find the changed data in ES?
        doEsQuery(result.getEntity(), content.getWhat().get("Athlete").toString(), 1);
        // And are we returning the same data from the KV Service?
        KvContent kvContent = kvService.getContent(entity, result.getCurrentLog().getLog());
        assertNotNull(kvContent);
        assertNotNull(kvContent.getWhat());
        assertEquals(content.getWhat().get("Athlete"), kvContent.getWhat().get("Athlete"));

        content.setWhat(getSimpleMap("Athlete", "Michael Phelps"));
        input.setContent(content);
        // Update existing entity
        result = mediationFacade.trackEntity(su.getCompany(), input);
        entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
        entityLog = entityService.getLastEntityLog(result.getEntity().getId());
        assertEquals(entity.getFortressCreatedTz().getMillis(), entityLog.getFortressWhen().longValue());
        waitAWhile("Async log is still processing");
        waitAWhile("Waiting for second update to occur");

        kvContent = kvService.getContent(entity, entityLog.getLog());
        assertNotNull(kvContent);
        assertNotNull(kvContent.getWhat());
        assertEquals(content.getWhat().get("Athlete"), kvContent.getWhat().get("Athlete"));


    }

    /**
     * @throws Exception
     */
    @Test
    public void validate_StringsContainingValidNumbers() throws Exception {
        try {
            //assumeTrue(runMe);
            logger.info("## validate_MismatchSubsequentValue");
            SystemUser su = registerSystemUser("validate_MismatchSubsequentValue", "validate_MismatchSubsequentValue");
            assertNotNull(su);
            engineConfig.setStoreEnabled("false");

            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("validate_MismatchSubsequentValue"));
            Map<String, Object> json = getSimpleMap("NumAsString", "1234");

            // Passing in a string "number", we want this to be preserved
            ContentInputBean content = new ContentInputBean("store_Disabled", new DateTime(), json);
            EntityInputBean input = new EntityInputBean(fortress.getName(), "mikeTest", "mismatch", new DateTime(), "store_Disabled");
            input.setContent(content);

            TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), input);
            waitForFirstSearchResult(su.getCompany(), result.getEntity());
            Entity entity = entityService.getEntity(su.getCompany(), result.getEntity().getMetaKey());
            assertNotNull(entity.getSearchKey());
            KvContent kvc = kvService.getContent(entity, result.getCurrentLog().getLog());
            assertNotNull(kvc);
            assertEquals(json.get("NumAsString"), "1234");

            json = getSimpleMap("NumAsString", "NA");
            content = new ContentInputBean("store_Disabled", new DateTime(), json);
            // Create a second entity
            EntityInputBean inputB = new EntityInputBean(fortress.getName(), "mikeTest", "mismatch", new DateTime(), "store_Disabledxx");
            inputB.setContent(content);

            result = mediationFacade.trackEntity(su.getCompany(), inputB);
            entity = waitForFirstSearchResult(su.getCompany(), result.getEntity());

            doEsQuery(result.getEntity(), "*", 2);

            kvc = kvService.getContent(entity, result.getCurrentLog().getLog());
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

        DocumentType docType = new DocumentType(fortress, "DAT-498", EntityService.TAG_STRUCTURE.TAXONOMY);
        EntityInputBean eib = new EntityInputBean(docType, "abc");

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
                        .content(JsonUtils.getJSON(eib))
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        DocumentType foundDoc = conceptService.resolveByDocCode(fortress, docType.getName(), false);
        assertNotNull(foundDoc);
        assertEquals(EntityService.TAG_STRUCTURE.TAXONOMY, foundDoc.getTagStructure());

        TrackRequestResult result = JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), TrackRequestResult.class);
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

        SearchChange searchDoc = searchService.getSearchDocument(foundDoc, entity, null, null);
        assertNotNull(searchDoc);
        assertEquals(EntityService.TAG_STRUCTURE.TAXONOMY, searchDoc.getTagStructure());
        //EntityTag termTag = searchDoc.getTagValues().;
        assertTrue("Couldn't find the term relationship", searchDoc.getTagValues().containsKey("viewed"));
        Map<String, ArrayList<SearchTag>> tagMap = searchDoc.getTagValues().get("viewed");
        assertTrue("Couldn't find the root level term relationship", tagMap.containsKey("term"));
        Collection<SearchTag>searchTags = tagMap.get("term");
        assertEquals(1, searchTags.size());
        SearchTag termSearchTag = searchTags.iterator().next();
        assertEquals("audi a3",termSearchTag.getCode());

        // division, interest and category
        assertEquals(3, termSearchTag.getParent().size());

        waitAWhile("letting search catchup");

        doEsFieldQuery(entity, "metaKey", result.getMetaKey(), 1);

        // Assert that we can find the category as a nested tag in ES
        doEsNestedQuery(entity, "tag.viewed.term", "tag.viewed.term.parent.category.code", "cars", 1);


    }

    private SystemUser registerSystemUser(String companyName, String userName) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
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

    private static String FD_SEARCH = "http://localhost:9081";

    private String runQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHttpHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange(FD_SEARCH + "/fd-search/v1/query/", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Client tracking error {}", e.getMessage());
        }
        return null;
    }

    private String runFdViewQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHttpHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange(FD_SEARCH + "/fd-search/v1/query/fdView", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Client tracking error {}", e.getMessage());
        }
        return null;
    }

    private TrackResultBean createLog(Company company, String metaKey, int log) throws Exception {
        return mediationFacade.trackLog(company, new ContentInputBean("olivia@sunnybell.com", metaKey, new DateTime(), getSimpleMap("who", log)));
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

    private Entity waitForFirstSearchResult(Company company, Entity entity) throws Exception {
        return waitForFirstSearchResult(company, entity.getMetaKey());
    }

    private Entity waitForFirstSearchResult(Company company, String metaKey) throws Exception {
        // Looking for the first searchKey to be logged against the entity
        int i = 1;

        Thread.yield();
        Entity entity = entityService.getEntity(company, metaKey);
        if (entity == null)
            return null;

        int timeout = 10;

        while (entity.getSearch() == null && i <= timeout) {

            entity = entityService.getEntity(company, metaKey);
            //logger.debug("Entity {}, searchKey {}", entity.getId(), entity.getSearchKey());
            if (i > 5) // All this yielding is not letting other threads complete, so we will sleep
                waitAWhile("Sleeping {} secs for entity [" + entity.getId() + "] to update ");
            else if (entity.getSearch() == null)
                Thread.yield(); // Small pause to let things happen

            i++;
        }

        if (entity.getSearch() == null) {
            logger.debug("!!! Search not working after [{}] attempts for entityId [{}]. SearchKey [{}]", i, entity.getId(), entity.getSearchKey());
            fail("Search reply not received from fd-search");
        }
        return entity;
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
                    String result = doEsTermQuery(entity, EntitySearchSchema.META_KEY, entity.getMetaKey(), 1, true);
                    totalSearchRequests++;
                    validateResultFieds(result);

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

    private ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    private void validateResultFieds(String result) throws Exception {
        JsonNode node = objectMapper.readTree(result);

        assertNotNull(node.get(EntitySearchSchema.CREATED));
        assertNotNull(node.get(EntitySearchSchema.WHO));
        assertNotNull(node.get(EntitySearchSchema.UPDATED));
        assertNotNull(node.get(EntitySearchSchema.META_KEY));
        assertNotNull(node.get(EntitySearchSchema.DOC_TYPE));
        assertNotNull(node.get(EntitySearchSchema.FORTRESS));

    }

    private String doEsNestedQuery(Entity entity, String path, String field, String term, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        logger.debug("doEsQuery {}", term);
        JestResult jResult;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for fd-search to catch up");
            String query = "{\n" +
                    "  \"query\": {\n" +
                    "    \"match_all\": {}\n" +
                    "  },\n" +
                    "  \"filter\": {\n" +
                    "    \"nested\": {\n" +
                    "      \"path\": \"" + path + "\",\n" +
                    "      \"filter\": {\n" +
                    "        \"bool\": {\n" +
                    "          \"must\": [\n" +
                    "            {\n" +
                    "              \"term\": {\n" +
                    "                \"" + field + "\": \"" + term + "\"\n" +
                    "              }\n" +
                    "            }\n" +
                    "          ]\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            Search search = new Search.Builder(query)
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(IndexHelper.parseType(entity))
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + entity + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", entity);
                return null;
            }
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else {
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again
            }
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < esTimeout);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        Object json = objectMapper.readValue(jResult.getJsonString(), Object.class);

        assertEquals(entity + "\r\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json),
                expectedHitCount, nbrResult);
        return jResult.getJsonString();
    }

    private String doEsQuery(Entity entity, String queryString) throws Exception {
        return doEsQuery(entity, queryString, 1);
    }

    private String doEsQuery(Entity entity, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        logger.debug("doEsQuery {}", queryString);
        JestResult jResult;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for fd-search to catch up");
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"query\" : \"" + queryString + "\"" +
                    "           }\n" +
                    "      }\n" +
                    "}";

            Search search = new Search.Builder(query)
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(IndexHelper.parseType(entity))
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + entity + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", entity);
                return null;
            }
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else {
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again
            }
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < esTimeout);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        assertNotNull(jResult);
        Object json = objectMapper.readValue(jResult.getJsonString(), Object.class);

        assertEquals(entity + "\r\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json),
                expectedHitCount, nbrResult);
        return jResult.getJsonString();
    }

    private String getMapping(String indexName) throws Exception {
        GetMapping mapping = new GetMapping.Builder()
                .addIndex(indexName)
                .build();

        JestResult jResult = esClient.execute(mapping);
        return jResult.getJsonString();
    }

    private String doEsTermQuery(Entity entity, String metaKey, String metaKey1, int i) throws Exception {
        return doEsTermQuery(entity, metaKey, metaKey1, i, false);
    }

    private String doEsTermQuery(Entity entity, String field, String queryString, int expectedHitCount, boolean suppressLog) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        JestResult jResult;

        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");
            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          term : {\n" +
                    "              \"" + field + "\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(IndexHelper.parseIndex(entity))
                    .addType(entity.getType().toLowerCase())
                    .build();

            jResult = esClient.execute(search);
            String message = entity + " - " + field + " - " + queryString + (jResult == null ? "[noresult]" : "\r\n" + jResult.getJsonString());
            assertNotNull(message, jResult);
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        if (!suppressLog) {
            logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
            logger.trace("searching index [{}] field [{}] for [{}]", entity, field, queryString);
        }

        Object json = objectMapper.readValue(jResult.getJsonString(), Object.class);
        assertEquals(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json), expectedHitCount, nbrResult);

        if (nbrResult != 0) {
            return jResult.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        } else {

            return null;
        }
    }

    private String doEsFieldQuery(Entity entity, String field, String queryString, int expectedHitCount) throws Exception {
        return doEsFieldQuery(IndexHelper.parseIndex(entity), entity.getType(), field, queryString, expectedHitCount);
    }

    /**
     * Use this carefully. Due to ranked search results, you can get more results than you expect. If
     * you are looking for an exact match then consider doEsTermQuery
     *
     * @param index            to search
     * @param field            field containing queryString
     * @param queryString      text to search for
     * @param expectedHitCount result count
     * @return query _source
     * @throws Exception if expectedHitCount != actual hit count
     */
    private String doEsFieldQuery(String index, String type, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given metaKey.
        // Let's assert that
        int runCount = 0, nbrResult;

        JestResult jResult;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "            default_field:   \"" + field + "\", query: \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .addType(IndexHelper.parseType(type))
                    .build();

            jResult = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (jResult == null ? "[noresult]" : "\r\n" + jResult.getJsonString());
            assertNotNull(message, jResult);
            assertNotNull(message, jResult.getJsonObject());
            assertNotNull(message, jResult.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, jResult.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        Object json = objectMapper.readValue(jResult.getJsonString(), Object.class);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        assertEquals("Unexpected hit count searching '" + index + "' for {" + queryString + "} in field {" + field + "}\n\r" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json),
                expectedHitCount, nbrResult);
        if (nbrResult == 0)
            return "";
        else
            return jResult.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
    }

    public static HttpHeaders getHttpHeaders(final String apiKey, final String username, final String password) {

        return new HttpHeaders() {
            {
                if (username != null && password != null) {
                    String auth = username + ":" + password;
                    byte[] encodedAuth = Base64.encodeBase64(
                            auth.getBytes(Charset.forName("US-ASCII")));
                    String authHeader = "Basic " + new String(encodedAuth);
                    set("Authorization", authHeader);
                } else if (apiKey != null)
                    set("api-key", apiKey);
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
            }
        };

    }

    /**
     * Processing delay for threads and integration to complete. If you start getting sporadic
     * Heuristic exceptions, chances are you need to call this routine to give other threads
     * time to commit their work.
     * Likewise, waiting for results from fd-search can take a while. We can't know how long this
     * is so you can experiment on your own environment by passing in -DsleepSeconds=1
     *
     * @param milliseconds to pause for
     * @throws Exception
     */
    public static void waitAWhile(String message, long milliseconds) throws Exception {
        logger.debug(message, milliseconds / 1000d);
        Thread.yield();
        Thread.sleep(milliseconds);

    }

    public static Map<String, Object> getSimpleMap(String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    public static Map<String, Object> getRandomMap() {
        return getSimpleMap("Key", "Test" + System.currentTimeMillis());
    }

    public static Map<String, Object> getBigJsonText(int i) {
        Map<String, Object> map = getSimpleMap("Key", "Random");
        int count = 0;
        do {
            map.put("Key" + count, "Now is the time for all good men to come to the aid of the party");
            count++;
        } while (count < i);
        return map;
    }

    EntityLog waitForLogCount(Company company, Entity entity, int expectedCount) throws Exception {
        // Looking for the first searchKey to be logged against the entity
        int i = 0;
        int timeout = 100;
        int count = 0;

        while (i <= timeout) {
            Entity updatedEntity = entityService.getEntity(company, entity.getMetaKey());
            count = entityService.getLogCount(company, updatedEntity.getMetaKey());

            EntityLog log = entityService.getLastEntityLog(company, updatedEntity.getMetaKey());
            // We have at least one log?
            if (count == expectedCount)
                return log;
            Thread.yield();
            if (i > 20)
                waitAWhile("Waiting {} seconds for the log to update");
            i++;
        }
        if (i > 22)
            logger.info("Wait for log got to [{}] for entityId [{}]", i,
                    entity.getId());
        throw new Exception(String.format("Timeout waiting for the requested log count of %s. Got to %s", expectedCount, count));
    }

    String doCompletionQuery(String index, String type, String queryString, int expectedHitCount, String exceptionMessage) throws Exception {
        // There should only ever be one document for a given Entity.
        // Let's assert that
        int runCount = 0, nbrResult;
        SuggestResult result;
        int esTimeout = 5;

        runCount++;
        String query = "{" +
                "    \"result\" : {\n" +
                "        \"text\" : \"" + queryString + "\",\n" +
                "        \"completion\" : {\n" +
                "            \"field\" : \"" + EntitySearchSchema.ALL_TAGS + "\"\n" +
                "        }\n" +
                "    }" +
                "}";


        Suggest search = new Suggest.Builder(query)
                .addIndex(index)
                .addType(IndexHelper.parseType(type))
                .build();
        result = esClient.execute(search);
        TestCase.assertTrue(result.getErrorMessage(), result.isSucceeded());

        List<SuggestResult.Suggestion> suggestions = result.getSuggestions("result");

        for (SuggestResult.Suggestion suggestion : suggestions) {
            assertEquals(expectedHitCount, suggestion.options.size());
        }


        return result.getJsonString();
    }
}
