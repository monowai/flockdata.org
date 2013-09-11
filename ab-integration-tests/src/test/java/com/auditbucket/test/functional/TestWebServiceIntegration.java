/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.test.functional;

import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

/**
 * User: Mike Holdsworth
 * Since: 12/08/13
 */

public class TestWebServiceIntegration {
    @Autowired
    private GraphDatabaseService graphDatabaseService;
    @Autowired
    private Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TestAuditIntegration.class);
    private String email = "test@ab.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");

    //@Test
    public void jsonReadFiles() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authA);
        ObjectMapper mapper = new ObjectMapper();
        String filename = "//tmp/new-trainprofiles.json";
        InputStream is = new FileInputStream(filename);
        JsonNode node = mapper.readTree(is);

        System.out.println("node Count = " + node.size());
        HttpClient httpclient = new DefaultHttpClient();
        String url = "http://localhost:8080/ab-engine/audit/header/new";
        for (JsonNode profile : node.elements().next()) {
            HttpPost auditPost = new HttpPost(url);
            auditPost.addHeader("content-type", "application/json");
            auditPost.addHeader("Authorization", "Basic bWlrZToxMjM=");
            AuditHeaderInputBean inputBean = new AuditHeaderInputBean("capacity", "system", "TrainProfile", DateTime.now(), profile.get("profileID").asText());
            AuditLogInputBean log = new AuditLogInputBean("moira", null, profile.toString());
            inputBean.setAuditLog(log);
            log.setForceReindex(true);
            StringEntity json = new StringEntity(mapper.writeValueAsString(inputBean));
            auditPost.setEntity(json);
            HttpResponse response = httpclient.execute(auditPost);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader((response.getEntity().getContent())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
        }
    }
}
