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
import org.codehaus.jackson.map.ObjectMapper;
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

import static junit.framework.Assert.*;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestElasticSearch {
    private String company = "Monowai";
    private String uid = "mike@monowai.com";
    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "user1");

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

        auditChange.setWho(uid);
        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
// What changed?
        ObjectNode name = new ObjectNode(JsonNodeFactory.instance);

        name.put("first", "Joe");
        name.put("last", "Sixpack");
        auditChange.setWhat(name.toString());

        ObjectMapper om = new ObjectMapper();
        try {
            System.out.println(om.writeValueAsString(auditChange));

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
            System.out.println(ir.getId());

            // Retrieve from Lucene
            GetResponse response = client.prepareGet(indexKey, auditChange.getHeaderKey(), ir.id())
                    .execute()
                    .actionGet();
            assertNotNull(response);
            System.out.println(response.type());

            AuditChange found = om.readValue(response.getSourceAsBytes(), AuditChange.class);
            assertNotNull(found);
            assertEquals(0, auditChange.getWhen().compareTo(found.getWhen()));

            node.close();


        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    @Test
    public void testViaSpringData() {

        SecurityContextHolder.getContext().setAuthentication(auth);
        // As per JSON test, except this time we're doing it all via Spring.

        IFortress fortress = new Fortress("fortress", new Company("Monowai"));
        IFortressUser fu = new FortressUser(fortress, uid);
        IAuditHeader auditHeader = new AuditHeader(fu, "Test", new DateTime(), "testref");


        AuditChange auditChange = new AuditChange(auditHeader);

        auditChange.setWho(uid);
        auditChange.setEvent("Create");
        auditChange.setWhen(new Date());
        auditChange.setVersion(System.currentTimeMillis());

        ObjectNode name = new ObjectNode(JsonNodeFactory.instance);

        name.put("first", "Joe");
        name.put("last", "Sixpack");
        auditChange.setWhat(name.toString());

        String indexKey = (auditHeader.getIndexName());

        String id = alRepo.save(auditChange);
        assertNotNull(id);
        // Retrieve from Lucene
        IAuditChange found = alRepo.findOne(indexKey, auditChange.getDataType(), id);
        assertNotNull(found);

        assertEquals(0, auditChange.getWhen().compareTo(found.getWhen()));
        assertNotNull(found.getId());
        assertNotSame("", found.getId());
        assertNotNull(found.getVersion());
        assertNotSame(0, found.getVersion());


    }


}