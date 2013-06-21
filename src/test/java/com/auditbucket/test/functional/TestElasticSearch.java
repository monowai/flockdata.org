package com.auditbucket.test.functional;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.repo.es.model.AuditChange;
import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import com.auditbucket.audit.service.AuditSearchService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestElasticSearch {
    private String uid = "mike@monowai.com";
    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "user1");
    private Log log = LogFactory.getLog(TestElasticSearch.class);

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    AuditSearchService searchService;

    @Autowired
    IAuditChangeDao alRepo;

    @Test
    public void testMappingJson() throws Exception {
        String escWhat = "{\"house\": \"house\"}";
        ObjectMapper om = new ObjectMapper();

        Map<String, Object> indexMe = new HashMap<String, Object>(40);
        indexMe.put("auditKey", "abc");
        Map<String, Object> what = om.readValue(escWhat, Map.class);
        indexMe.put("what", what);
        log.info(indexMe.get("What"));
    }

    @Test
    public void testJson() {
        // Basic JSON/ES tests to figure our what is going on

        IFortress fortress = new Fortress(new FortressInputBean("fortress"), new Company("Monowai"));
        fortress.setIgnoreSearchEngine(false);
        IFortressUser fu = new FortressUser(fortress, uid);
        AuditHeaderInputBean hib = new AuditHeaderInputBean("fortress", "Test", "Test", new DateTime().toDate(), "testRef");
        IAuditHeader auditHeader = new AuditHeader(fu, hib);

        AuditChange auditChange = new AuditChange(auditHeader);

        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
// What changed?
        ObjectMapper om = new ObjectMapper();
        ObjectNode name = om.createObjectNode();
        //name.asToken();
        name.put("first", "Joe");
        name.put("last", "Sixpack");


        auditChange.setWhat(name.toString());

        try {
            log.info(om.writeValueAsString(auditChange));

            // Elasticsearch
            Node node = nodeBuilder().local(true).node();
            Client client = node.client();
            String indexKey = (auditChange.getIndexName());

            // Write the object to Lucene
            IndexResponse ir =
                    client.prepareIndex(indexKey, auditChange.getName())
                            .setSource(om.writeValueAsString(auditChange))
                            .execute()
                            .actionGet();

            assertNotNull(ir);
            log.info(ir.getId());

            // Retrieve from Lucene
            GetResponse response = client.prepareGet(indexKey, auditChange.getName(), ir.getId())
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

    @Test
    public void testViaSpringData() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(auth);
        // As per JSON test, except this time we're doing it all via Spring.

        IFortress fortress = new Fortress(new FortressInputBean("fortress"), new Company("Monowai"));
        fortress.setIgnoreSearchEngine(false);
        IFortressUser fu = new FortressUser(fortress, uid);
        AuditHeaderInputBean hib = new AuditHeaderInputBean("fortress", "Test", "Test", new DateTime().toDate(), "testRef");
        IAuditHeader auditHeader = new AuditHeader(fu, hib);

        IAuditChange auditChange = new AuditChange(auditHeader);

        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
        auditChange.setVersion(System.currentTimeMillis());
        ObjectMapper om = new ObjectMapper();

        Map<String, String> node = new HashMap<String, String>();
        node.put("first", "Joe");
        node.put("last", "Sixpack");

        auditChange.setWhat(om.writeValueAsString(node));

        auditChange = alRepo.save(auditChange);
        assertNotNull(auditChange);
        String searchKey = auditChange.getSearchKey();
        assertNotNull(searchKey);

        // Retrieve parent from Lucene
        byte[] parent = alRepo.findOne(auditHeader, searchKey);

        assertNotNull(parent);
        Map<String, Object> ac = om.readValue(parent, Map.class);
        assertNotNull(ac);
        assertEquals(auditHeader.getAuditKey(), ac.get("auditKey"));
        // Occasionally findOne() fails for unknown reasons. I think it's down to the time between writing the "what"
        //              and reading it back, hence the Thread.sleep
        assertEquals("Joe", ac.get("first"));
        assertEquals("Sixpack", ac.get("last"));

    }


    @Test
    public void testFortressDefaults() {
        FortressInputBean fortress = new FortressInputBean("");
        // Object Defaults
        assertFalse(fortress.getAccumulateChanges());
        assertTrue(fortress.getIgnoreSearchEngine());
        fortress = new FortressInputBean("", true);
        assertTrue(fortress.getAccumulateChanges());
        assertFalse(fortress.getIgnoreSearchEngine());
        fortress = new FortressInputBean("", false);
        assertFalse(fortress.getAccumulateChanges());
        assertTrue(fortress.getIgnoreSearchEngine());

    }


}