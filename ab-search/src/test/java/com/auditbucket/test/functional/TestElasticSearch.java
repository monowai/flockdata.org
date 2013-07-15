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

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditSearchDao;
import com.auditbucket.search.AuditChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestElasticSearch {
    private String uid = "mike@monowai.com";
    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "user1");
    private Logger log = LoggerFactory.getLogger(TestElasticSearch.class);

    @Autowired
    IAuditSearchDao alRepo;

    @Test
    public void testMappingJson() throws Exception {
        String escWhat = "{\"house\": \"house\"}";
        ObjectMapper om = new ObjectMapper();

        Map<String, Object> indexMe = new HashMap<String, Object>(40);
        indexMe.put("auditKey", "abc");
        Map<String, Object> what = om.readValue(escWhat, Map.class);
        indexMe.put("what", what);
        log.info(indexMe.get("what").toString());
    }

    @Test
    public void testJson() throws Exception {
        // Basic JSON/ES tests to figure our what is going on
        // ToDo: the following should be uncommented, but the POM dependancy can't be resolved
        //          in the current POM configuration
//        IFortress fortress = new Fortress(new FortressInputBean("fortress"), new Company("Monowai"));
//        fortress.setIgnoreSearchEngine(false);
//        IFortressUser fu = new FortressUser(fortress, uid);
//        AuditHeaderInputBean hib = new AuditHeaderInputBean("fortress", "Test", "Test", new DateTime().toDate(), "testRef");
//        IAuditHeader auditHeader = new AuditHeader(fu, hib, new DocumentType("TestJson", fu.getFortress().getCompany()));

        AuditChange auditChange = new AuditChange();

        //auditChange.setName("Create");
        auditChange.setWhen(new DateTime());
//// What changed?
        ObjectMapper om = new ObjectMapper();
        Map<String, Object> name = new HashMap<String, Object>();
        name.put("first", "Joe");
        name.put("last", "Sixpack");
        auditChange.setWhat(name);


        try {

            // Elasticsearch
            Node node = nodeBuilder().local(true).node();
            Client client = node.client();
            String indexKey = (auditChange.getIndexName());

            // Write the object to Lucene
            IndexResponse ir =
                    client.prepareIndex(indexKey, auditChange.getWho())
                            .setSource(om.writeValueAsString(auditChange))
                            .setRouting(auditChange.getAuditKey())
                            .execute()
                            .actionGet();

            assertNotNull(ir);
            log.info(ir.getId());

            // Retrieve from Lucene
            GetResponse response = client.prepareGet(indexKey, auditChange.getWho(), ir.getId())
                    .setRouting(auditChange.getAuditKey())
                    .execute()
                    .actionGet();
            assertNotNull(response);

            AuditChange found = om.readValue(response.getSourceAsBytes(), AuditChange.class);
            assertNotNull(found);
            assertEquals(0, auditChange.getWhen().compareTo(found.getWhen()));

            node.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void testViaSpringData() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(auth);
        // As per JSON test, except this time we're doing it all via Spring.
        // ToDo: the following should be uncommented, but the POM dependancy can't be resolved
        //          in the current POM configuration

//        IFortress fortress = new Fortress(new FortressInputBean("fortress"), new Company("Monowai"));
//        fortress.setIgnoreSearchEngine(false);
//        IFortressUser fu = new FortressUser(fortress, uid);
//        AuditHeaderInputBean hib = new AuditHeaderInputBean("fortress", "Test", "Test", new DateTime().toDate(), "testRef");
//        IAuditHeader auditHeader = new AuditHeader(fu, hib, new DocumentType("TestJson", fu.getFortress().getCompany()));

        IAuditChange auditChange = new AuditChange();
        String auditKey = "auditHeader.getAuditKey()";

        auditChange.setWhen(new DateTime());
        ObjectMapper om = new ObjectMapper();

        Map<String, Object> node = new HashMap<String, Object>();
        node.put("first", "Joe");
        node.put("last", "Sixpack");

        auditChange.setWhat(node);

        auditChange = alRepo.save(auditChange);
        assertNotNull(auditChange);
        String searchKey = auditChange.getSearchKey();
        assertNotNull(searchKey);

        // Retrieve parent from Lucene
        //ToDo: uncomment the code below and get this working
//        byte[] parent = alRepo.findOne(auditHeader, searchKey);
//
//        assertNotNull(parent);
//        Map<String, Object> ac = om.readValue(parent, Map.class);
//        assertNotNull(ac);
//        assertEquals(auditKey, ac.get("auditKey"));
//        assertEquals("Joe", ac.get("first"));
//        assertEquals("Sixpack", ac.get("last"));

    }

}
