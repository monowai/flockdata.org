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

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.ClientConfig;
import io.searchbox.indices.DeleteIndex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpRequestExecutor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
    public void mkh() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authA);
//        Neo4jHelper.cleanDb(graphDatabaseService, true);

        HttpClient httpclient = new DefaultHttpClient();
        String url = "http://localhost:8080/ab-engine/profiles/register";
        HttpPut register = new HttpPut(url);
        register.addHeader("content-type", "application/json");
        StringEntity json = new StringEntity("{\n" +
                "  \"name\":\"mike\",\n" +
                "  \"companyName\":\"Monowai\",\n" +
                "  \"password\":\"aa\"\n" +
                "}");

        register.setEntity(json);

        HttpResponse response = httpclient.execute(register);

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));

        String output;
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            System.out.println(output);
        }
    }

}
