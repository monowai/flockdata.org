package com.auditbucket.test.unit;

import com.auditbucket.audit.dao.IAuditChangeDao;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
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

        auditChange.getName(uid);
        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
// What changed?
        ObjectNode name = new ObjectNode(JsonNodeFactory.instance);

        name.put("first", "Joe");
        name.put("last", "Sixpack");
        auditChange.setWhat(name.toString());

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
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


        AuditChange auditChange = new AuditChange(auditHeader);

        auditChange.getName(uid);
        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
        auditChange.setVersion(System.currentTimeMillis());

        ObjectNode name = new ObjectNode(JsonNodeFactory.instance);

        name.put("first", "Joe");
        name.put("last", "Sixpack");
        auditChange.setWhat(name.toString());

        String indexKey = (auditHeader.getIndexName());

        String childID = alRepo.save(auditChange);
        assertNotNull(childID);
        // Retrieve from Lucene
        byte[] found = alRepo.findOne(indexKey, auditChange.getDataType(), childID);
        assertNotNull(found);
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

        JsonNode node = om.readTree(found);
        log.info(node.toString());

        /*assertEquals(0, auditChange.getWhen().compareTo(found.getWhen()));
        assertNotNull(found.getId());
        assertNotSame("", found.getId());
        assertNotNull(found.getVersion());
        assertNotSame(0, found.getVersion());*/


    }


}