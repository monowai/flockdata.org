package org.flockdata.test.engine.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.flockdata.FdEngine;
import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.LoginRequest;
import org.flockdata.authentication.registration.service.CompanyService;
import org.flockdata.authentication.registration.service.RegistrationService;
import org.flockdata.configure.ApiKeyInterceptor;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.*;
import org.flockdata.track.bean.*;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.MediationFacade;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
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
public class WacBase {

    public static final String apiRoot = "/api";
    public static final String LOGIN_PATH = apiRoot + "/login";
    public static final String apiPath = apiRoot+"/v1";

    public static String harry = "harry";
    public static String mike_admin = "mike";
    public static String sally_admin = "sally";

    static Logger logger = LoggerFactory.getLogger(WacBase.class);

    SystemUserResultBean suHarry;

    SystemUserResultBean suMike;
    SystemUserResultBean suSally;
    SystemUserResultBean suIllegal;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
    PlatformConfig engineConfig;

    @Autowired
    CompanyService companyService;

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    MediationFacade mediationFacade;

    @Before
    public void setupMvc() throws Exception{
        suHarry =registerSystemUser("anyco", harry);
        suMike = registerSystemUser("anyco", mike_admin);
        suSally =registerSystemUser("anyco", sally_admin);
        suIllegal = new SystemUserResultBean(new SystemUser("illegal", "noone", null, false).setApiKey("blahh"));
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    public SystemUserResultBean registerSystemUser(String companyName, String accessUser) throws Exception {
        return registerSystemUser(mike(), companyName, accessUser);
    }

    public MockMvc mvc(){
        return mockMvc;
    }

    public static void setSecurityEmpty() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public RequestPostProcessor mike() {
        return user(mike_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    public RequestPostProcessor sally() {
        return user(sally_admin).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    public RequestPostProcessor harry() {
        return user(harry).password("123").roles(FdRoles.FD_ADMIN, FdRoles.FD_USER);
    }

    public static RequestPostProcessor noUser() {
        return user("noone");
    }

    public void setSecurity() throws Exception{
    }

    public Fortress createFortress(SystemUserResultBean su, String fortressName)
            throws Exception {

        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/")
                                .header("api-key", su.getApiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        JsonUtils
                                                .toJson(new FortressInputBean(
                                                        fortressName, true))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        return JsonUtils.toObject(response.getResponse()
                .getContentAsByteArray(), Fortress.class);
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
    public SystemUserResultBean registerSystemUser(RequestPostProcessor user, String company, String accessUser) throws Exception{
        return registerSystemUser(user, company, accessUser, MockMvcResultMatchers.status().isOk());
    }

    public SystemUserResultBean registerSystemUser(RequestPostProcessor user, String company, String accessUser, ResultMatcher status) throws Exception{
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.post(apiPath +"/profiles/")
                        .content(JsonUtils.toJson( new RegistrationBean(company, accessUser)))
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(status).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toObject(json.getBytes(), SystemUserResultBean.class);
    }


    public Entity getEntity(RequestPostProcessor user, String metaKey) throws Exception {
        return getEntity(user, metaKey, MockMvcResultMatchers.status().isOk());
    }
    public Entity getEntity(RequestPostProcessor user, String metaKey, ResultMatcher status) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/{metaKey}", metaKey)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)
        ).andExpect(status).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toObject(json.getBytes(), Entity.class);
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
                .content(JsonUtils.toJsonBytes(eib))
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

    public String adminPing(RequestPostProcessor user) throws Exception {

        ResultActions result = mvc()
                .perform(
                        MockMvcRequestBuilders.get(apiPath +"/admin/ping")
                        .with(user)
                )
                .andDo(print());
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

    public Collection<TagResultBean> getTags(String label, RequestPostProcessor user) throws Exception {
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

    public TagResultBean getTag(RequestPostProcessor user, String label, String code) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        MvcResult response = mvc()
                .perform(MockMvcRequestBuilders.get(apiPath +"/tag/{label}/{code}", label, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user)
                ).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.toObject(json, TagResultBean.class);
    }

    public void getTagNotFound(RequestPostProcessor user, String label, String code) throws Exception {
        mvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label + "/" + code)
                .contentType(MediaType.APPLICATION_JSON)
                .with(user)

        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


    }

    public Collection<DocumentResultBean> getDocuments(String fortress) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/doc/" + fortress)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, DocumentResultBean.class);
    }

    public Collection<ConceptResultBean> getLabelsForDocument(String code, String docResultName) throws Exception {
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/doc/" + code + "/" + docResultName)
                .contentType(MediaType.APPLICATION_JSON)
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

    public Collection<EntityTag> getEntityTags(RequestPostProcessor user, String metaKey) throws Exception{
        MvcResult response = mvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/{metaKey}/tags", metaKey )
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.toCollection(json, EntityTag.class);
    }

    public Collection<DocumentResultBean> makeDocuments(RequestPostProcessor user, Fortress fortress, Collection<DocumentTypeInputBean> docTypes) throws Exception {
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

    public Collection<Map<String, TagResultBean>> getTagPaths(String label, String code, String targetLabel) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        targetLabel = URLEncoder.encode(targetLabel, "UTF-8");

        MvcResult response = mvc()
                .perform(
                        MockMvcRequestBuilders
                                .get(apiPath +"/path/{label}/{code}/{depth}/{lastLabel}", label, code, "4", targetLabel)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return FdJsonObjectMapper.getObjectMapper().readValue(json, new TypeReference<Collection<Map<String, TagResultBean>>>() {
        });
//        return JsonUtils.getAsType(json, type )


    }
}
