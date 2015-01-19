/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.endpoint;

import org.flockdata.authentication.LoginRequest;
import org.flockdata.helper.ApiKeyInterceptor;
import org.flockdata.engine.schema.model.DocumentTypeNode;
import org.flockdata.company.model.FortressNode;
import org.flockdata.helper.JsonUtils;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.registration.bean.SystemUserResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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

    private MockMvc getMockMvc(){
        return  mockMvc;
    }

    public Fortress createFortress(SystemUser su, String fortressName)
            throws Exception {

        MvcResult response = getMockMvc()
                .perform(
                        MockMvcRequestBuilders
                                .post("/fortress/")
                                .header("api-key", su.getApiKey())
                                        // .("company", su.getCompany())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        JsonUtils
                                                .getJSON(new FortressInputBean(
                                                        fortressName, true))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        Fortress fortress = JsonUtils.getBytesAsObject(response.getResponse()
                .getContentAsByteArray(), FortressNode.class);
        fortress.setCompany(su.getCompany());
        return fortress;
    }

    public Collection<DocumentResultBean> getDocuments(SystemUser su, Collection<String> fortresses) throws Exception {
        MvcResult response =   getMockMvc().perform(MockMvcRequestBuilders.post("/query/documents/")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentResultBean.class);
    }

    public Collection<DocumentTypeNode> getRelationships(SystemUserResultBean su, Collection<String> fortresses) throws Exception {
        MvcResult response =   getMockMvc().perform(MockMvcRequestBuilders.post("/query/relationships/")
                        .header("api-key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(fortresses))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsCollection(json, DocumentTypeNode.class);
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

    public String ping () throws Exception {
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

    public TrackResultBean track(EntityInputBean eib, SystemUser su) throws Exception {
        MvcResult response = getMockMvc().perform(MockMvcRequestBuilders.post("/track/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                        .content(JsonUtils.getObjectAsJsonBytes(eib))
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();
        byte[] json = response.getResponse().getContentAsByteArray();

        return JsonUtils.getBytesAsObject(json, TrackResultBean.class);
    }

    public String adminPing()throws Exception {
        ResultActions result = getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get("/admin/ping"));
        return result.andReturn().getResponse().getContentAsString();

    }
}
