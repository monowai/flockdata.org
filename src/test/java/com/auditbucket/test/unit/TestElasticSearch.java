package com.auditbucket.test.unit;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.repo.es.model.AuditChange;
import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.fasterxml.jackson.databind.JsonNode;
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
    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "user1");
    private Log log = LogFactory.getLog(TestElasticSearch.class);

    @Autowired
    RegistrationService regService;

    @Autowired
    FortressService fortressService;

    @Autowired
    AuditService auditService;

    @Autowired
    IAuditChangeDao alRepo;


    @Test
    public void testJson() {
        // Basic JSON/ES tests to figure our what is going on

        IFortress fortress = new Fortress("fortress", new Company("Monowai"));
        IFortressUser fu = new FortressUser(fortress, uid);
        IAuditHeader auditHeader = new AuditHeader(fu, "Test", new DateTime(), "testrefx");

        AuditChange auditChange = new AuditChange(auditHeader);

        auditChange.setName(uid);
        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
// What changed?
        ObjectMapper om = new ObjectMapper();
        ObjectNode name = om.createObjectNode();
        //name.asToken();
        name.put("first", "Joe");
        name.put("last", "Sixpack");


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
                    client.prepareIndex(indexKey, auditChange.getHeaderKey())
                            .setSource(om.writeValueAsString(auditChange))
                            .execute()
                            .actionGet();

            assertNotNull(ir);
            log.info(ir.getId());

            // Retrieve from Lucene
            GetResponse response = client.prepareGet(indexKey, auditChange.getHeaderKey(), ir.getId())
                    .execute()
                    .actionGet();
            assertNotNull(response);

            AuditChange found = om.readValue(response.getSourceAsBytes(), AuditChange.class);
            assertNotNull(found);
            assertEquals(0, auditChange.getWhen().compareTo(found.getWhen()));

            node.close();


        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    @Test
    public void testViaSpringData() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(auth);
        // As per JSON test, except this time we're doing it all via Spring.

        IFortress fortress = new Fortress("fortress", new Company("Monowai"));
        IFortressUser fu = new FortressUser(fortress, uid);
        IAuditHeader auditHeader = new AuditHeader(fu, "Test", new DateTime(), "testref");


        IAuditChange auditChange = new AuditChange(auditHeader);

        auditChange.setName(uid);
        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
        auditChange.setVersion(System.currentTimeMillis());
        ObjectMapper om = new ObjectMapper();

        Map<String, String> node = new HashMap<String, String>();
        node.put("first", "Joe");
        node.put("last", "Sixpack");


        auditChange.setWhat(om.writeValueAsString(node));

        String indexKey = (auditHeader.getIndexName());

        auditChange = alRepo.save(auditChange);
        assertNotNull(auditChange);
        String childID = auditChange.getChild();
        String parentID = auditChange.getParent();
        assertNotNull(childID);
        assertNotNull(parentID);
        assertNotSame(childID, parentID);

        // Retrieve from Lucene
        byte[] parent = alRepo.findOne(indexKey, auditChange.getRecordType(), parentID);

        IAuditChange ac = om.readValue(parent, AuditChange.class);
        assertNotNull(ac);
        // Occasionally findOne() fails for unknown reasons. I think it's down to the time between writing the "what"
        //              and reading it back, hence the Thread.sleep
        Thread.sleep(2000);
        byte[] child = alRepo.findOne(indexKey, auditChange.getRecordType(), childID);

        assertNotNull(child);

        JsonNode result = om.readTree(child);
        assertNotNull(result);
        assertEquals("Joe", result.get("first").textValue());
        assertEquals("Sixpack", result.get("last").textValue());

    }


}