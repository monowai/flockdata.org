package org.flockdata.test.engine.functional;

import com.fasterxml.jackson.core.type.TypeReference;
import org.flockdata.authentication.LoginRequest;
import org.flockdata.configure.ApiKeyInterceptor;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.*;
import org.flockdata.track.bean.*;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Base class for Web App context driven classes
 * Created by mike on 12/02/16.
 */
@WebAppConfiguration
@ActiveProfiles({"dev", "web-dev", "fd-auth-test"})
public class WacBase extends EngineBase {

    public static final String apiRoot = "/api";
    public static final String LOGIN_PATH = apiRoot + "/login";
    public static final String apiPath = apiRoot+"/v1";

    @Autowired
    public WebApplicationContext wac;

    public MockMvc mockMvc;

    @Before
    public void setupMvc(){
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    public MockMvc getMockMvc(){
        return mockMvc;
    }

    public Fortress createFortress(SystemUser su, String fortressName)
            throws Exception {

        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/")
                                .header("api-key", su.getApiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        JsonUtils
                                                .getJSON(new FortressInputBean(
                                                        fortressName, true))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        Fortress fortress = JsonUtils.getBytesAsObject(response.getResponse()
                .getContentAsByteArray(), Fortress.class);
        fortress.setCompany(su.getCompany());
        return fortress;
    }

    public Collection<DocumentResultBean> getDocuments(SystemUser su, Collection<String> fortresses) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders
                .post(apiPath +"/query/documents/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.getJSON(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<DocumentType> getRelationships(SystemUserResultBean su, Collection<String> fortresses) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post(apiPath +"/query/relationships/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.getJSON(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentType.class);
    }

    public MatrixResults getMatrixResult(SystemUser su, MatrixInputBean input) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post(apiPath +"/query/matrix/")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.getJSON(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), MatrixResults.class);
    }

    public Map<String, Object> getHealth(SystemUser su) throws Exception {

        //mockMvc = MockMvcBuilders.webAppContextSetup(getMockMvc()).build();
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/admin/health/")
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsMap(json);
    }

    public String ping() throws Exception {
        ResultActions result = getMockMvc()
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

        return getMockMvc()
                .perform(
                        MockMvcRequestBuilders.post(LOGIN_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON( new LoginRequest(user, pass))))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    }

    public TrackRequestResult track(EntityInputBean eib, SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post(apiPath +"/track/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                .content(JsonUtils.getObjectAsJsonBytes(eib))
        )   .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.getBytesAsObject(json, TrackRequestResult.class);
    }

    public Company getCompany(String name, SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders
                .get(apiPath +"/company/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getBytesAsObject(json, Company.class);
    }

    public boolean findCompanyIllegal(String name, SystemUser su) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/company/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))

        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
        return true;
    }

    public Collection<Company> findCompanies(SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/company/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();
        return JsonUtils.getAsCollection(json, Company.class);

    }

    public String adminPing() throws Exception {
        ResultActions result = getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(apiPath +"/admin/ping"));
        return result.andReturn().getResponse().getContentAsString();

    }

    public FortressResultBean postFortress(SystemUser su, FortressInputBean fortressInputBean) throws Exception {
        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/")
                                .header("api-key", su.getApiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(fortressInputBean))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getBytesAsObject(json, FortressResultBean.class);

    }

    public Collection<TagResultBean> getTags(String label) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label)
                .contentType(MediaType.APPLICATION_JSON)

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getAsCollection(json, TagResultBean.class);

    }

    public TagResultBean getTagWithPrefix(String label, String keyPrefix, String code) throws Exception{
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");

        MvcResult response = getMockMvc()
                .perform(MockMvcRequestBuilders.get(apiPath +"/tag/{label}/{prefix}/{code}", label, keyPrefix, code)
                        .contentType(MediaType.APPLICATION_JSON)

                ).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.getBytesAsObject(json, TagResultBean.class);

    }

    public TagResultBean getTag(String label, String code) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        MvcResult response = getMockMvc()
                .perform(MockMvcRequestBuilders.get(apiPath +"/tag/{label}/{code}", label, code)
                        .contentType(MediaType.APPLICATION_JSON)

                ).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.getBytesAsObject(json, TagResultBean.class);
    }

    public void getTagNotFound(String label, String code) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label + "/" + code)
                .contentType(MediaType.APPLICATION_JSON)

        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


    }

    public Collection<DocumentResultBean> getDocuments(String fortress) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/doc/" + fortress)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<ConceptResultBean> getLabelsForDocument(String code, String docResultName) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/doc/" + code + "/" + docResultName)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, ConceptResultBean.class);

    }

    public Collection<TagResultBean> createTag(TagInputBean tag) throws Exception {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        tags.add(tag);
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.put(apiPath +"/tag/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.getJSON(tags))
        ).andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, TagResultBean.class);
    }

    public Map<String, Object> getConnectedTags(String label, String code, String relationship, String targetLabel) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/tag/" + label + "/" + code + "/path/" + relationship + "/" + targetLabel)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsMap(json);

    }

    public Collection<EntityLog> getEntityLogs(SystemUser su, String metaKey) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/" + metaKey + "/log")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, EntityLog.class);
    }

    public void getEntityLogsIllegalEntity(SystemUser su, String metaKey) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/entity/" + metaKey + "/log")
                .header("api-key", su.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
    }

    public Collection<DocumentResultBean> makeDocuments(SystemUser su, Fortress fortress, Collection<DocumentTypeInputBean> docTypes) throws Exception {
        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post(apiPath +"/fortress/" + fortress.getCode() + "/docs")
                                .header("api-key", su.getApiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(docTypes))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<FortressSegment> getSegments(String fortressCode) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get(apiPath +"/fortress/" + fortressCode + "/segments")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, FortressSegment.class);

    }

    public Collection<Map<String, TagResultBean>> getTagPaths(String label, String code, String targetLabel) throws Exception {
        label = URLEncoder.encode(label, "UTF-8");
        code = URLEncoder.encode(code, "UTF-8");
        targetLabel = URLEncoder.encode(targetLabel, "UTF-8");

        MvcResult response = getMockMvc()
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
