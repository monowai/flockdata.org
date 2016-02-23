package org.flockdata.test.engine.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.flockdata.FdEngine;
import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.LoginRequest;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.*;
import org.flockdata.track.bean.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Base class for Web App context driven classes
 * Created by mike on 12/02/16.
 */
@WebAppConfiguration
@ActiveProfiles({"dev", "web-dev", "fd-auth-test"})
@SpringApplicationConfiguration(FdEngine.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class MvcBase {

    static final String apiRoot = "/api";
    static final String LOGIN_PATH = apiRoot + "/login";
    static final String apiPath = apiRoot+"/v1";
    public static final String ANYCO = "anyco";

    public static String harry = "harry";
    public static String mike_admin = "mike";
    public static String sally_admin = "sally";
    //public static String gina_admin = "gina";

    static Logger logger = LoggerFactory.getLogger(MvcBase.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    SystemUserResultBean suHarry;
    SystemUserResultBean suMike;
    SystemUserResultBean suSally;
    SystemUserResultBean suIllegal;

    @Autowired
    public WebApplicationContext wac;

    @Autowired
    Neo4jTemplate neo4jTemplate;

    private MockMvc mockMvc;

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    public void cleanUpGraph() throws Exception {
        // DAT-348 - override this if you're running a multi-threaded tests where multiple transactions
        //           might be started giving you sporadic failures.
        neo4jTemplate.query("match (n)-[r]-() delete r,n", null);
        neo4jTemplate.query("match (n) delete n", null);

    }


    @Before
    public void setupMvc() throws Exception{
        engineConfig.setMultiTenanted(false);
        if (mockMvc == null ) {
            mockMvc = MockMvcBuilders
                    .webAppContextSetup(wac)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }
        cleanUpGraph();
        suMike = makeProfile(ANYCO, mike_admin);
        suHarry = makeProfile(mike(), ANYCO, harry);// Harry works at Anyco where Mike is the administrator
        suSally = makeProfile(ANYCO, sally_admin);
        suIllegal = new SystemUserResultBean(new SystemUser("illegal", "noone", null, false).setApiKey("blahh"));


    }



    public SystemUserResultBean makeProfile(String companyName, String accessUser) throws Exception {
        return makeProfile(mike(), companyName, accessUser);
    }

    public MockMvc mvc() throws Exception{
        if ( mockMvc == null )
            setupMvc();
        return mockMvc;
    }

    static void setSecurityEmpty() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public RequestPostProcessor mike() {
        return user(mike_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    public RequestPostProcessor sally() {
        return user(sally_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    public RequestPostProcessor harry() {
        return user(harry).password("123").roles( FdRoles.FD_USER);
    }

    static RequestPostProcessor noUser() {
        return user("noone");
    }

    public void setSecurity() throws Exception{
    }

    FortressResultBean createFortress(RequestPostProcessor user, String fortressName)
            throws Exception {

        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user)
                                .content(
                                        JsonUtils
                                                .toJson(new FortressInputBean(
                                                        fortressName, true))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        return JsonUtils.toObject(response.getResponse()
                .getContentAsByteArray(), FortressResultBean.class);
    }

    public Collection<DocumentResultBean> getDocuments(SystemUser su, Collection<String> fortresses) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders
                .post(apiPath +"/query/documents/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentResultBean.class);
    }

    public Collection<DocumentType> getRelationships(SystemUserResultBean su, Collection<String> fortresses) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath +"/query/relationships/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentType.class);
    }

    public MatrixResults getMatrixResult(SystemUser su, MatrixInputBean input) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath +"/query/matrix/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.toObject(response.getResponse().getContentAsByteArray(), MatrixResults.class);
    }
    public SystemUserResultBean makeProfile(RequestPostProcessor user, String company, String accessUser) throws Exception{
        return makeProfile(user, company, accessUser, MockMvcResultMatchers.status().isCreated());
    }

    public SystemUserResultBean makeProfile(RequestPostProcessor user, String company, String accessUser, ResultMatcher status) throws Exception{
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath +"/profiles/")
                        .content(JsonUtils.toJson( new RegistrationBean(company, accessUser)))
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(status).andReturn();

        if ( response.getResolvedException() ==null ) {
            String json = response.getResponse().getContentAsString();

            return JsonUtils.toObject(json.getBytes(), SystemUserResultBean.class);
        }
        throw response.getResolvedException();
    }


    public EntityBean getEntity(RequestPostProcessor user, String metaKey) throws Exception {
        return getEntity(user, metaKey, MockMvcResultMatchers.status().isOk());
    }
    public EntityBean getEntity(RequestPostProcessor user, String metaKey, ResultMatcher status) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/{metaKey}", metaKey)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(status).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toObject(json.getBytes(), EntityBean.class);
    }

    public Map<String, Object> getHealth(RequestPostProcessor user) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/admin/health/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toMap(json);
    }


    public Map<String, Object> getHealth(SystemUserResultBean su) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/admin/health/")
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
                        MockMvcRequestBuilders.get(apiRoot +"/ping"));
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
                                .content(JsonUtils.toJson( new LoginRequest(user, pass))))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    }

    public MvcResult login(RequestPostProcessor user) throws Exception {

        return mvc()
                .perform(
                        MockMvcRequestBuilders.post(LOGIN_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.toJson( user)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    }

    public TrackRequestResult track(RequestPostProcessor user, EntityInputBean eib) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.post(apiPath +"/track/")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
                .content(JsonUtils.toJson(eib))
        )   .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TrackRequestResult.class);
    }

    public Company getCompany(String name, SystemUserResultBean su) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders
                .get(apiPath +"/company/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toObject(json, Company.class);
    }

    public boolean findCompanyIllegal(String name, SystemUserResultBean su) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath +"/company/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))

        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
        return true;
    }

    public Collection<Company> findCompanies(SystemUserResultBean su) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/company/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();
        return JsonUtils.toCollection(json, Company.class);

    }

    public String authPing(RequestPostProcessor user, ResultMatcher expectedResult) throws Exception {

        ResultActions result = mvc()
                .perform(
                        MockMvcRequestBuilders.get(apiPath +"/admin/ping")
                        .with(user)
                )
                .andExpect(expectedResult)  ;
       return result.andReturn().getResponse().getContentAsString();

    }

    public FortressResultBean makeFortress(RequestPostProcessor user, FortressInputBean fortressInputBean) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/")
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.toJson(fortressInputBean))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toObject(json, FortressResultBean.class);

    }

    public FortressResultBean getFortress(RequestPostProcessor user, String code) throws Exception{
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath +"/fortress/{code}",code)
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                                ).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toObject(json, FortressResultBean.class);

    }

    public Collection<FortressResultBean> getFortresses(RequestPostProcessor user) throws Exception{
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath +"/fortress/")
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                               ).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toCollection(json, FortressResultBean.class);

    }

    public Collection<TagResultBean> getTags(RequestPostProcessor user, String label) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toCollection(json, TagResultBean.class);

    }

    public TagResultBean getTagWithPrefix(RequestPostProcessor user, String label, String keyPrefix, String code) throws Exception{
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");

        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath +"/tag/{label}/{prefix}/{code}", label, keyPrefix, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TagResultBean.class);

    }

    public TagResultBean getTag(RequestPostProcessor user, String label, String code, ResultMatcher resultMatch) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath +"/tag/{label}/{code}", label, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)

                )
                .andExpect(resultMatch).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TagResultBean.class);
    }

    public void getTagNotFound(RequestPostProcessor user, String label, String code) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label + "/" + code)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)

        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


    }

    public Collection<DocumentResultBean> getDocuments(RequestPostProcessor user, String fortress) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/doc/" + fortress)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentResultBean.class);
    }

    public Collection<ConceptResultBean> getLabelsForDocument(RequestPostProcessor user, String code, String docResultName) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/doc/" + code + "/" + docResultName)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, ConceptResultBean.class);

    }

    public Collection<TagResultBean> createTag(RequestPostProcessor user, TagInputBean tag) throws Exception {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        tags.add(tag);
        MvcResult response = mvc().perform(MockMvcRequestBuilders.put(apiPath +"/tag/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(tags))
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, TagResultBean.class);
    }

    public Map<String, Object> getConnectedTags(RequestPostProcessor user, String label, String code, String relationship, String targetLabel) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label + "/" + code + "/path/" + relationship + "/" + targetLabel)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toMap(json);

    }

    public Collection<EntityLog> getEntityLogs(RequestPostProcessor user, String metaKey) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/" + metaKey + "/log")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, EntityLog.class);
    }

    public void getEntityLogsIllegalEntity(RequestPostProcessor user, String metaKey) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/" + metaKey + "/log")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
    }

    public Collection<EntityTagResult> getEntityTags(RequestPostProcessor user, String metaKey) throws Exception{
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/{metaKey}/tags", metaKey )
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, EntityTagResult.class);
    }

    public Collection<DocumentResultBean> makeDocuments(RequestPostProcessor user, MetaFortress fortress, Collection<DocumentTypeInputBean> docTypes) throws Exception {
        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/{code}/docs",fortress.getCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user)
                                .content(JsonUtils.toJson(docTypes))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.toCollection(json, DocumentResultBean.class);
    }

    public Collection<FortressSegment> getSegments(RequestPostProcessor user, String fortressCode) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/fortress/" + fortressCode + "/segments")
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, FortressSegment.class);

    }

    public Collection<Map<String, TagResultBean>> getTagPaths(RequestPostProcessor user, String label, String code, String targetLabel) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        targetLabel = URLEncoder.encode(targetLabel, "UTF-8");

        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath +"/path/{label}/{code}/{depth}/{lastLabel}", label, code, "4", targetLabel)
                                .with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return FdJsonObjectMapper.getObjectMapper().readValue(json, new TypeReference<Collection<Map<String, TagResultBean>>>() {
        });
//        return JsonUtils.getAsType(json, type )


    }
}
