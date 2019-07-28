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

package org.flockdata.test.engine.mvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.authentication.FdRoles;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Fortress;
import org.flockdata.engine.FdEngine;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressSegmentNode;
import org.flockdata.engine.data.graph.SystemUserNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.ContentModelResult;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.model.ContentValidationResults;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.LoginRequest;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.test.engine.MapBasedStorageProxy;
import org.flockdata.test.engine.Neo4jConfigTest;
import org.flockdata.test.unit.client.FdTemplateMock;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntityResultBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.EntityTagResult;
import org.flockdata.track.bean.MatrixInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.transform.model.ContentModelHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for Web App context driven classes
 *
 * @author mholdsworth
 * @since 12/02/2016
 */
@WebAppConfiguration(value = "src/main/resources")
@SpringBootTest(classes = {
    FdEngine.class,
    Neo4jConfigTest.class,
    FdTemplateMock.class,
    MapBasedStorageProxy.class})
@ActiveProfiles( {"dev", "fd-auth-test"})
@RunWith(SpringRunner.class)
public abstract class MvcBase {

  static final String ANYCO = "anyco";
  private static final String apiRoot = "/api";
  static final String LOGIN_PATH = apiRoot + "/login";
  static final String apiPath = apiRoot + "/v1";
  private static final String OTHERCO = "otherco";
  public static String harry = "harry";
  public static String mike_admin = "mike";
  static String sally_admin = "sally"; // admin in a different company
  static Logger logger = LoggerFactory.getLogger(MvcBase.class);
  @Rule
  public final ExpectedException exception = ExpectedException.none();
  ResultMatcher OK = MockMvcResultMatchers.status().isOk();
  ResultMatcher ACCEPTED = MockMvcResultMatchers.status().isAccepted();
  SystemUserResultBean suMike;

  @Autowired
  PlatformConfig engineConfig;
  @Autowired
  private WebApplicationContext wac;
  @Autowired
  private Neo4jTemplate neo4jTemplate;
  private MockMvc mockMvc;

  static void setSecurityEmpty() {
    SecurityContextHolder.clearContext();
  }

  static RequestPostProcessor noUser() {
    return user("noone");
  }

  public void cleanUpGraph() throws Exception {
    // DAT-348 - override this if you're running a multi-threaded tests where multiple transactions
    //           might be started giving you sporadic failures.
    neo4jTemplate.query("match (n)-[r]-() delete r", null);
    neo4jTemplate.query("match (n) delete n", null);

  }

  @Before
  public void setupMvc() throws Exception {
    engineConfig.setMultiTenanted(false);
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders
          .webAppContextSetup(wac)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
    }
    cleanUpGraph();
    suMike = makeDataAccessProfile(ANYCO, mike_admin);
    makeDataAccessProfile(mike(), ANYCO, harry);
    makeDataAccessProfile(OTHERCO, sally_admin);
//        new SystemUserResultBean(new SystemUser("illegal", "noone", null, false).setApiKey("blahh"));


  }

  public SystemUserResultBean makeDataAccessProfile(String companyName, String accessUser) throws Exception {
    return makeDataAccessProfile(mike(), companyName, accessUser);
  }

  public MockMvc mvc() throws Exception {
    if (mockMvc == null) {
      setupMvc();
    }
    return mockMvc;
  }

  /**
   * @return mike - works for AnyCo
   */
  public RequestPostProcessor mike() {
    return user(mike_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
  }

  /**
   * @return sally - works for OtherCo
   */
  public RequestPostProcessor sally() {
    return user(sally_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
  }

  public RequestPostProcessor harry() {
    return user(harry)
        .password("123")
        .roles(FdRoles.FD_USER);
  }

  FortressResultBean updateFortress(RequestPostProcessor user, String code, FortressInputBean update, ResultMatcher resultMatch) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .post(apiPath + "/fortress/{code}", code)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(
                    JsonUtils
                        .toJson(update)))
        .andExpect(resultMatch)
        .andReturn();
    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), FortressResultBean.class);
    }

    throw response.getResolvedException();

  }

  FortressResultBean updateFortress(RequestPostProcessor user, String code, FortressInputBean update) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .post(apiPath + "/fortress/{code}", code)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(
                    JsonUtils
                        .toJson(update)))
        .andReturn();
    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), FortressResultBean.class);
    }

    throw response.getResolvedException();

  }

  FortressResultBean makeFortress(RequestPostProcessor user, String fortressName)
      throws Exception {
    return this.makeFortress(user, new FortressInputBean(fortressName, true));
  }

  public FortressResultBean makeFortress(RequestPostProcessor user, FortressInputBean fortressInputBean) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .post(apiPath + "/fortress/")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(fortressInputBean))).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();
    return JsonUtils.toObject(json, FortressResultBean.class);

  }

  public Collection<DocumentResultBean> getDocuments(SystemUserNode su, Collection<String> fortresses) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders
        .post(apiPath + "/doc/")
        .header("api-key", su.getApiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.toJson(fortresses))
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, DocumentResultBean.class);
  }

  public DocumentResultBean getDocument(RequestPostProcessor user, String fortress, String docName) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders
        .get(apiPath + "/doc/{fortress}/{docName}", fortress, docName)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
        .content(JsonUtils.toJson(fortress))
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toObject(json, DocumentResultBean.class);
  }

  public Collection<DocumentNode> getRelationships(SystemUserResultBean su, Collection<String> fortresses) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath + "/query/relationships/")
        .header("api-key", su.getApiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.toJson(fortresses))
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, DocumentNode.class);
  }

  public MatrixResults getMatrixResult(SystemUserNode su, MatrixInputBean input) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath + "/query/matrix/")
        .header("api-key", su.getApiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.toJson(input))
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), MatrixResults.class);
  }

  public SystemUserResultBean makeDataAccessProfile(RequestPostProcessor user, String company, String accessUser) throws Exception {
    return makeDataAccessProfile(user, company, accessUser, MockMvcResultMatchers.status().isCreated());
  }

  public SystemUserResultBean makeDataAccessProfile(RequestPostProcessor user, String company, String accessUser, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.post(apiPath + "/profiles/")
            .content(JsonUtils.toJson(RegistrationBean.builder().companyName(company).login(accessUser).build()))
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), SystemUserResultBean.class);
    }
    throw response.getResolvedException();
  }

  public EntityResultBean getEntity(RequestPostProcessor user, String key) throws Exception {
    return getEntity(user, key, MockMvcResultMatchers.status().isOk());
  }

  public EntityResultBean getEntity(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/{key}", key)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(status).andReturn();

    if (response.getResolvedException() != null) {
      throw response.getResolvedException();
    }
    String json = response.getResponse().getContentAsString();
    return JsonUtils.toObject(json.getBytes(), EntityResultBean.class);
  }

  public EntitySummaryBean getEntitySummary(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/{key}/summary", key)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(status).andReturn();

    if (response.getResolvedException() != null) {
      throw response.getResolvedException();
    }
    String json = response.getResponse().getContentAsString();
    return JsonUtils.toObject(json.getBytes(), EntitySummaryBean.class);
  }

  public Map<String, Object> getHealth(RequestPostProcessor user) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/admin/health/")
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toMap(json);
  }

  public Map<String, Object> getHealth(SystemUserResultBean su) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/admin/health/")
        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
        .contentType(MediaType.APPLICATION_JSON)
        .with(noUser())
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toMap(json);
  }

  public String ping() throws Exception {
    ResultActions result = mvc()
        .perform(
            MockMvcRequestBuilders.get(apiRoot + "/ping"));
    return result.andReturn().getResponse().getContentAsString();
  }

  /**
   * Tests logging in to the API over the REST endpoint.
   *
   * @param user who are you?
   * @param pass password
   * @return MvcResult
   * @throws Exception anything goes wrong
   */
  public MvcResult login(String user, String pass) throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, pass)
    );

    return mvc()
        .perform(
            MockMvcRequestBuilders.post(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new LoginRequest(user, pass))))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

  }

  public MvcResult login(RequestPostProcessor user) throws Exception {

    return mvc()
        .perform(
            MockMvcRequestBuilders.post(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(user)))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

  }

  public TrackRequestResult track(RequestPostProcessor user, EntityInputBean eib) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath + "/track/")
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
        .content(JsonUtils.toJson(eib))
    ).andDo(log())
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andReturn();
    byte[] json = response.getResponse().getContentAsByteArray();

    return JsonUtils.toObject(json, TrackRequestResult.class);
  }

  public CompanyNode getCompany(String name, RequestPostProcessor user) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders
        .get(apiPath + "/company/" + name)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();
    return JsonUtils.toObject(json, CompanyNode.class);
  }

  public boolean findCompanyIllegal(String name, RequestPostProcessor user) throws Exception {
    mvc().perform(MockMvcRequestBuilders.get(apiPath + "/company/" + name)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)

    ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
    return true;
  }

  public Collection<CompanyNode> findCompanies(RequestPostProcessor user) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/company/")
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)

    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();
    return JsonUtils.toCollection(json, CompanyNode.class);

  }

  public String authPing(RequestPostProcessor user, ResultMatcher expectedResult) throws Exception {

    ResultActions result = mvc()
        .perform(
            MockMvcRequestBuilders.get(apiPath + "/admin/ping")
                .with(user)
        )
        .andExpect(expectedResult);
    return result.andReturn().getResponse().getContentAsString();

  }

  public void deleteFortress(RequestPostProcessor user, String fortressName, ResultMatcher expectedResult) throws Exception {

    mvc()
        .perform(
            MockMvcRequestBuilders.delete(apiPath + "/admin/{fortressName}", fortressName)
                .with(user)
        ).andExpect(expectedResult);

  }

  public Collection<DocumentResultBean> getFortressDocs(RequestPostProcessor user, String code) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .get(apiPath + "/fortress/{code}/docs", code)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toCollection(json.getBytes(), DocumentResultBean.class);
    }
    throw response.getResolvedException();


  }

  public FortressResultBean getFortress(RequestPostProcessor user, String code) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .get(apiPath + "/fortress/{code}", code)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), FortressResultBean.class);
    }
    throw response.getResolvedException();


  }

  public FortressInputBean getDefaultFortress(RequestPostProcessor user) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .get(apiPath + "/fortress/defaults")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), FortressInputBean.class);
    }
    throw response.getResolvedException();


  }

  public ContentModel getDefaultContentModel(RequestPostProcessor user, ContentValidationRequest content) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .post(apiPath + "/model/default")
                .content(JsonUtils.toJson(content))
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentModelHandler.class);
    }
    throw response.getResolvedException();


  }

  public Collection<FortressResultBean> getFortresses(RequestPostProcessor user) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .get(apiPath + "/fortress/")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();
    return JsonUtils.toCollection(json, FortressResultBean.class);

  }

  public Collection<TagResultBean> getTags(RequestPostProcessor user, String label) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/" + label)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();
    return JsonUtils.toCollection(json, TagResultBean.class);

  }

  public TagResultBean getTagWithPrefix(RequestPostProcessor user, String label, String keyPrefix, String code) throws Exception {
    label = URLEncoder.encode(label, "UTF-8");
    code = URLEncoder.encode(code, "UTF-8");

    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/tag/{label}/{prefix}/{code}", label, keyPrefix, code)
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andDo(log())
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();

    return JsonUtils.toObject(json, TagResultBean.class);

  }

  public TagResultBean getTag(RequestPostProcessor user, String label, String code, ResultMatcher resultMatch) throws Exception {
    label = URLEncoder.encode(label, "UTF-8");
    code = URLEncoder.encode(code, "UTF-8");
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/tag/{label}/{code}", label, code)
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)

        )
        .andExpect(resultMatch).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();

    return JsonUtils.toObject(json, TagResultBean.class);
  }

  public void getTagNotFound(RequestPostProcessor user, String label, String code) throws Exception {
    mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/" + label + "/" + code)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)

    ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


  }

  public Collection<DocumentResultBean> getDocuments(RequestPostProcessor user, String fortress) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/doc/" + fortress)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, DocumentResultBean.class);
  }

  public Collection<ConceptResultBean> getLabelsForDocument(RequestPostProcessor user, String docResultName) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/concept/{doc}/values", docResultName)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, ConceptResultBean.class);

  }

  public Collection<TagResultBean> createTag(RequestPostProcessor user, TagInputBean tag) throws Exception {
    ArrayList<TagInputBean> tags = new ArrayList<>();
    tags.add(tag);
    MvcResult response = mvc().perform(MockMvcRequestBuilders.put(apiPath + "/tag/")
        .contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtils.toJson(tags))
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, TagResultBean.class);
  }

  public Collection<TagResultBean> getCountries(RequestPostProcessor user) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/geo/")
        //.contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, TagResultBean.class);
  }

  public Collection<TagResultBean> getTags(RequestPostProcessor user) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/")
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, TagResultBean.class);
  }

  ContentValidationRequest batchRequest(RequestPostProcessor user, ContentValidationRequest validationRequest) throws Exception {
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .post(apiPath + "/batch/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(JsonUtils.toJson(validationRequest))).andReturn();

    if (response.getResolvedException() == null) {
      byte[] json = response.getResponse().getContentAsByteArray();
      return JsonUtils.toObject(json, ContentValidationRequest.class);

    }

    throw (response.getResolvedException());

  }

  public Map<String, Object> getConnectedTags(RequestPostProcessor user, String label, String code, String relationship, String targetLabel) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/tag/" + label + "/" + code + "/path/" + relationship + "/" + targetLabel)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toMap(json);

  }

  public Collection<EntityLogResult> getEntityLogs(RequestPostProcessor user, String key) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/" + key + "/log")
        .with(user)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, EntityLogResult.class);
  }

  public Map<String, Object> getEntityData(RequestPostProcessor user, String key) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/{key}/log/last/data", key)
        .with(user)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toMap(json);
  }

  public Map<String, Object> getEntityData(RequestPostProcessor user, EntityInputBean eib) throws Exception {
    assert eib != null;
    assert eib.getCode() != null;

    MvcResult response = mvc().perform(
        MockMvcRequestBuilders.get(apiPath + "/entity/{fortress}/{docType}/{code}/log/last/data", eib.getFortress().getName(), eib.getDocumentType().getName(), eib.getCode())
            .with(user)
            .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toMap(json);
  }

  public void getEntityLogsIllegalEntity(RequestPostProcessor user, String key) throws Exception {
    mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/" + key + "/log")
        .with(user)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
  }

  public Collection<EntityTagResult> getEntityTags(RequestPostProcessor user, String key) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/entity/{key}/tags", key)
        .with(user)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, EntityTagResult.class);
  }

  public DocumentResultBean makeDocuments(RequestPostProcessor user, Fortress fortress, DocumentTypeInputBean docTypes) throws Exception {
    assert docTypes != null;
    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .post(apiPath + "/fortress/{code}/doc", fortress.getCode())
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(JsonUtils.toJson(docTypes))).andReturn();

    byte[] json = response.getResponse().getContentAsByteArray();
    return JsonUtils.toObject(json, DocumentResultBean.class);
  }

  public Collection<DocumentResultBean> getDocumentWithSegments(RequestPostProcessor user, String fortressCode, String docType) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/fortress/{fortress}/{doc}/segments", fortressCode, docType)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json.getBytes(), DocumentResultBean.class);

  }

  public Collection<FortressSegmentNode> getDocumentWithSegments(RequestPostProcessor user, String fortressCode) throws Exception {
    MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath + "/fortress/{fortressCode}/segments", fortressCode)
        .contentType(MediaType.APPLICATION_JSON)
        .with(user)
    ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return JsonUtils.toCollection(json, FortressSegmentNode.class);

  }

  public Collection<Map<String, TagResultBean>> getTagPaths(RequestPostProcessor user, String label, String code, String targetLabel) throws Exception {
    label = URLEncoder.encode(label, "UTF-8");
    code = URLEncoder.encode(code, "UTF-8");
    targetLabel = URLEncoder.encode(targetLabel, "UTF-8");

    MvcResult response = mvc()
        .perform(
            MockMvcRequestBuilders
                .get(apiPath + "/path/{label}/{code}/{depth}/{lastLabel}", label, code, "4", targetLabel)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(log())
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    String json = response.getResponse().getContentAsString();

    return FdJsonObjectMapper.getObjectMapper().readValue(json, new TypeReference<Collection<Map<String, TagResultBean>>>() {
    });
//        return JsonUtils.getAsType(json, type )


  }

  public ContentModelResult makeTagModel(RequestPostProcessor user, String code, ContentModel contentModel, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.post(apiPath + "/model/tag/{code}", code)
            .content(JsonUtils.toJson(contentModel))
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentModelResult.class);
    }
    throw response.getResolvedException();
  }

  public ContentModelResult makeContentModel(RequestPostProcessor user, String fortress, String documentType, ContentModel contentModel, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.post(apiPath + "/model/{fortress}/{documentType}", fortress, documentType)
            .content(JsonUtils.toJson(contentModel))
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentModelResult.class);
    }
    throw response.getResolvedException();
  }

  public Collection<ContentModelResult> makeContentModels(RequestPostProcessor user, Collection<ContentModel> contentModel, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.post(apiPath + "/model/")
            .content(JsonUtils.toJson(contentModel))
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toCollection(json.getBytes(), ContentModelResult.class);
    }
    throw response.getResolvedException();
  }

  public Collection<ContentModel> findContentModels(RequestPostProcessor user, Collection<String> contentModelKeys, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.post(apiPath + "/model/download")
            .content(JsonUtils.toJson(contentModelKeys))
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toCollection(json.getBytes(), ContentModel.class);
    }
    throw response.getResolvedException();
  }

  public Collection<ContentModelResult> findContentModels(RequestPostProcessor user, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/model/")
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toCollection(json.getBytes(), ContentModelResult.class);
    }
    throw response.getResolvedException();
  }

  public ContentModelResult findContentModelByKey(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/model/{key}", key)
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentModelResult.class);
    }
    throw response.getResolvedException();

  }

  public void deleteContentModel(RequestPostProcessor user, String key, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.delete(apiPath + "/model/{key}", key)
            .with(user)
        ).andExpect(status).andReturn();
    if (response.getResolvedException() != null) {
      throw response.getResolvedException();
    }
  }

  public ContentModel getContentModel(RequestPostProcessor user, String fortress, String documentType, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/model/{fortress}/{documentType}", fortress, documentType)
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentModelHandler.class);
    }
    throw response.getResolvedException();
  }

  public ContentModel getContentModel(RequestPostProcessor user, String code, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/model/tag/{code}", code)
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentModelHandler.class);
    }
    throw response.getResolvedException();
  }

  ContentValidationResults validateContentModel(RequestPostProcessor user, ContentValidationRequest contentProfile, ResultMatcher result) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.post(apiPath + "/model/validate")
            .content(JsonUtils.toJson(contentProfile))
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(result).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), ContentValidationResults.class);
    }
    throw response.getResolvedException();
  }

  public MatrixResults getContentStructure(RequestPostProcessor user, String fortressCode, ResultMatcher status) throws Exception {
    MvcResult response = mvc()
        .perform(MockMvcRequestBuilders.get(apiPath + "/concept/{code}/structure", fortressCode)
            .contentType(MediaType.APPLICATION_JSON)
            .with(user)
        ).andExpect(status).andReturn();

    if (response.getResolvedException() == null) {
      String json = response.getResponse().getContentAsString();

      return JsonUtils.toObject(json.getBytes(), MatrixResults.class);
    }
    throw response.getResolvedException();
  }


}
