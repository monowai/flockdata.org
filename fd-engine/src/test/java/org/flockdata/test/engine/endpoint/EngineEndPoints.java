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

package org.flockdata.test.engine.endpoint;

import com.fasterxml.jackson.core.type.TypeReference;
import org.flockdata.authentication.LoginRequest;
import org.flockdata.helper.ApiKeyInterceptor;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.bean.*;
import org.flockdata.track.bean.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Any test using this class will have to be annotated with @WebAppConfiguration
 * in order to inject the WebApplicationContext
 * User: mike
 * Date: 1/09/14
 * Time: 9:57 AM
 */
public class EngineEndPoints {
    MockMvc mockMvc;

    public EngineEndPoints(WebApplicationContext wac) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private MockMvc getMockMvc() {
        return mockMvc;
    }

    public Fortress createFortress(SystemUser su, String fortressName)
            throws Exception {

        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post("/fortress/")
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
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post("/query/documents/")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<DocumentType> getRelationships(SystemUserResultBean su, Collection<String> fortresses) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post("/query/relationships/")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentType.class);
    }

    public MatrixResults getMatrixResult(SystemUser su, MatrixInputBean input) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post("/query/matrix/")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), MatrixResults.class);
    }

    public Map<String, Object> getHealth(SystemUser su) throws Exception {

        //mockMvc = MockMvcBuilders.webAppContextSetup(getMockMvc()).build();
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/admin/health/")
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsMap(json);
    }

    public String ping() throws Exception {
        ResultActions result = getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get("/ping"));
        return result.andReturn().getResponse().getContentAsString();
    }

    public MvcResult login(String user, String pass) throws Exception {
        // As per the entry in test-security.xml
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(user);
        loginReq.setPassword(pass);

        return getMockMvc()
                .perform(
                        MockMvcRequestBuilders.post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(loginReq)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    }

    public TrackRequestResult track(EntityInputBean eib, SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post("/track/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                        .content(JsonUtils.getObjectAsJsonBytes(eib))
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();
        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.getBytesAsObject(json, TrackRequestResult.class);
    }

    public Company getCompany(String name, SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/company/" + name)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getBytesAsObject(json, Company.class);
    }

    public boolean findCompanyIllegal(String name, SystemUser su) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get("/company/" + name)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))

        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
        return true;
    }

    public Collection<Company> findCompanies(SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/company/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();
        return JsonUtils.getAsCollection(json, Company.class);

    }

    public String adminPing() throws Exception {
        ResultActions result = getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get("/admin/ping"));
        return result.andReturn().getResponse().getContentAsString();

    }

    public FortressResultBean postFortress(SystemUser su, FortressInputBean fortressInputBean) throws Exception {
        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post("/fortress/")
                                .header("api-key", su.getApiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(fortressInputBean))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getBytesAsObject(json, FortressResultBean.class);

    }

    public Collection<TagResultBean> getTags(String label) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/tag/" + label)
                        .contentType(MediaType.APPLICATION_JSON)

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getAsCollection(json, TagResultBean.class);

    }

    public TagResultBean getTag(String label, String code) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/tag/" + label + "/" + code)
                        .contentType(MediaType.APPLICATION_JSON)

        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.getBytesAsObject(json, TagResultBean.class);
    }

    public void getTagNotFound(String label, String code) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/tag/" + label + "/" + code)
                        .contentType(MediaType.APPLICATION_JSON)

        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


    }

    public Collection<DocumentResultBean> getDocuments(String fortress) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/doc/" + fortress)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<ConceptResultBean> getLabelsForDocument(String code, String docResultName) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/doc/" + code + "/" + docResultName)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, ConceptResultBean.class);

    }

    public Collection<TagResultBean> createTag(TagInputBean tag) throws Exception {
        ArrayList<TagInputBean> tags = new ArrayList<>();
        tags.add(tag);
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.put("/tag/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(tags))
        ).andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, TagResultBean.class);
    }

    public Map<String, Object> getConnectedTags(String label, String code, String relationship, String targetLabel) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/tag/" + label + "/" + code + "/path/" + relationship + "/" + targetLabel)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsMap(json);

    }

    public Collection<EntityLog> getEntityLogs(SystemUser su, String metaKey) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/entity/" + metaKey + "/log")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, EntityLog.class);
    }

    public void getEntityLogsIllegalEntity(SystemUser su, String metaKey) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get("/entity/" + metaKey + "/log")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
    }

    public Collection<DocumentResultBean> makeDocuments(SystemUser su, Fortress fortress, Collection<DocumentTypeInputBean> docTypes) throws Exception {
        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post("/fortress/" + fortress.getCode() + "/docs")
                                .header("api-key", su.getApiKey())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.getJSON(docTypes))).andReturn();

        byte[] json = response.getResponse().getContentAsByteArray();
        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<FortressSegment> getSegments(String fortressCode) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/fortress/" + fortressCode + "/segments")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, FortressSegment.class);

    }

    public Collection<Map<String, TagResultBean>> getTagPaths(String label, String code, String targetLabel) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.get("/path/" + label + "/" + code + "/4/" + targetLabel)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return FdJsonObjectMapper.getObjectMapper().readValue(json, new TypeReference<Collection<Map<String, TagResultBean>>>() {
        });
//        return JsonUtils.getAsType(json, type )


    }
}
