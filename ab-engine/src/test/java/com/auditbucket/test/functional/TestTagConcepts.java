/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Relationship;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.Concept;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.MetaHeader;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.node.Neo4jHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Non transactional tests - these are slower due to cleaning the DB down for each test run
 * User: mike
 * Date: 19/06/14
 * Time: 8:47 AM
 */
public class TestTagConcepts extends TestEngineBase {
    private Logger logger = LoggerFactory.getLogger(TestTagConcepts.class);

    @Override
    public void cleanUpGraph() {
        Neo4jHelper.cleanDb(template);
    }
    @Test
    public void multipleDocsSameFortress() throws Exception {
        logger.debug("### multipleDocsSameFortress");
        Neo4jHelper.cleanDb(template);
        setSecurity();
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();
        //registrationEP.registerSystemUser(new RegistrationBean("CompanyA", "userA")).getBody();
        SystemUser su = regService.registerSystemUser(new RegistrationBean("multipleDocsSameFortress", mike_admin));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("multipleDocsSameFortress");

        DocumentType dType = schemaService.resolveDocType(fortress, "ABC123", true);
        commitManualTransaction(t);// Should only be only one docTypes

        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocType(fortress, "ABC123", false);
        Assert.assertEquals(id, dType.getId());

        MetaInputBean input = new MetaInputBean(fortress.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "purchased").setIndex("Customer"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile("Concepts creating...");
        validateConcepts("DocA", su, 1);

        // Different docs, same concepts
        input = new MetaInputBean(fortress.getName(), "jinks", "DocB", new DateTime());
        input.addTag(new TagInputBean("cust123", "purchased").setIndex("Customer"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile("Concepts creating...");

        validateConcepts((Collection<String>) null, su, 3); // 3 Doc types.
        Assert.assertEquals("Docs In Use not supporting 'null args' for fortress'", 3, queryService.getDocumentsInUse(su.getCompany(), null).size());

        // DAT-112
        Set<DocumentType> found = validateConcepts("DocA", su, 1);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals(1, found.iterator().next().getConcepts().size());
        found = validateConcepts("DocB", su, 1);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals(1, found.iterator().next().getConcepts().size());


    }

    @Test
    public void fortressConcepts() throws Exception {
        logger.debug("### fortressConcepts");
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();
        setSecurity();
        SystemUser su = regService.registerSystemUser(new RegistrationBean("fortressConcepts", mike_admin));
        Assert.assertNotNull(su);

        Fortress fortA = fortressService.registerFortress("fortressConcepts");

        DocumentType dType = schemaService.resolveDocType(fortA, "ABC123", true);
        commitManualTransaction(t);// Should only be only one docTypes

        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocType(fortA, "ABC123", false);
        Assert.assertEquals(id, dType.getId());

        MetaInputBean input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "purchased").setIndex("Customer"));
        MetaHeader meta = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();

        assertNotNull(trackEP.getMetaHeader(meta.getMetaKey(), su.getApiKey(), su.getApiKey()));

        input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust124", "purchased").setIndex("Customer"));

        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile("Concepts creating...");

        Collection<String> docs = new ArrayList<>();
        docs.add("DocA");
        Collection<DocumentType> documentTypes = queryService.getConcepts(su.getCompany(), docs);
        org.junit.Assert.assertNotNull(documentTypes);
        assertEquals(1, documentTypes.size());

        // add a second docTypes
        input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "sold").setIndex("Rep"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey());
        waitAWhile("Concepts creating...");

        documentTypes = queryService.getConceptsWithRelationships(su.getCompany(), docs);
        assertEquals("Only one doc type should exist", 1, documentTypes.size());

        Boolean foundCustomer= false, foundRep= false;

        for (DocumentType docTypes : documentTypes) {
            for ( Concept concept : docTypes.getConcepts()) {
                if (concept.getName().equals("Customer")){
                    foundCustomer = true;
                    assertEquals(1, concept.getRelationships().size());
                    assertEquals("purchased", concept.getRelationships().iterator().next().getName());
                    assertEquals(true, concept.toString().contains(concept.getName()));
                }
                if (concept.getName().equals("Rep")) {
                    foundRep = true;
                    assertEquals(1, concept.getRelationships().size());
                    assertEquals("sold", concept.getRelationships().iterator().next().getName());
                    assertEquals(true, concept.toString().contains(concept.getName()));
                }
            }

        }
        assertTrue("Didn't find Customer concept", foundCustomer);
        assertTrue("Didn't find Rep concept", foundRep);
    }

    @Test
    public void multipleRelationships() throws Exception {
        logger.debug("### multipleRelationships");
        Neo4jHelper.cleanDb(template);
        setSecurity();
        engineAdmin.setConceptsEnabled(true);
        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean("multipleRelationships", mike_admin));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("multipleRelationships");

        DocumentType dType = schemaService.resolveDocType(fortress, "ABC123", true);
        commitManualTransaction(t);// Should only be only one docTypes

        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocType(fortress, "ABC123", false);
        Assert.assertEquals(id, dType.getId());

        MetaInputBean input = new MetaInputBean(fortress.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "purchased").setIndex("Customer"));
        input.addTag(new TagInputBean("harry", "soldto").setIndex("Customer"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        input = new MetaInputBean(fortress.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust121", "purchased").setIndex("Customer"));
        input.addTag(new TagInputBean("harry", "soldto").setIndex("Customer"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile("Concepts creating...");
        waitAWhile("Concepts creating...");
        validateConcepts("DocA", su, 1);

        Collection<String>docs = new ArrayList<>();
        docs.add("DocA");
        Set<DocumentType> docTypes = queryService.getConceptsWithRelationships(su.getCompany(), docs);
        for (DocumentType docType : docTypes) {
            Collection<Concept>concepts = docType.getConcepts();
            for (Concept concept : concepts) {
                Collection<Relationship> relationships  =concept.getRelationships();
                for (Relationship relationship : relationships) {
                    logger.debug(relationship.getName());
                }
                Assert.assertEquals(2, relationships.size());

            }
        }
        Assert.assertEquals("Docs In Use not supporting 'null args'", 2,queryService.getConceptsWithRelationships(su.getCompany(), null).size());
    }
    @Test
    public void relationshipWorkForMultipleDocuments() throws Exception {
        logger.debug("### relationshipWorkForMultipleDocuments");
        Neo4jHelper.cleanDb(template);
        setSecurity();
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean("relationshipWorkForMultipleDocuments", mike_admin));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("relationshipWorkForMultipleDocuments");

        DocumentType docA = schemaService.resolveDocType(fortress, "DOCA", true);
        DocumentType docB = schemaService.resolveDocType(fortress, "DOCB", true);
        commitManualTransaction(t);// Should only be only one docTypes

        Assert.assertNotNull(docA);
        Long idA = docA.getId();
        docA = schemaService.resolveDocType(fortress, docA.getName(), false);
        Assert.assertEquals(idA, docA.getId());

        MetaInputBean input = new MetaInputBean(fortress.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "purchased").setIndex("Customer"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        input = new MetaInputBean(fortress.getName(), "jinks", docB.getName(), new DateTime());
        input.addTag(new TagInputBean("cust121", "purchased").setIndex("Customer"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile("Concepts creating...");

        Collection<String>docs = new ArrayList<>();
        docs.add(docA.getName());
        docs.add(docB.getName());
        boolean docAFound = false;
        boolean docBFound = false;
        Set<DocumentType> docTypes = queryService.getConceptsWithRelationships(su.getCompany(), docs);
        for (DocumentType docType : docTypes) {
            Collection<Concept>concepts = docType.getConcepts();
            for (Concept concept : concepts) {
                Collection<Relationship> relationships  =concept.getRelationships();
                for (Relationship relationship : relationships) {
                    Assert.assertEquals(1, relationship.getDocumentTypes().size());
                    if ( docType.getName().equals(docA.getName()))
                        docAFound = true;
                    else if (docType.getName().equals(docB.getName()) )
                        docBFound = true;
                }
            }
        }
        // ToDo: it is unclear if we should track in this manner
        assertTrue("DocA Not Found in the concept", docAFound);
        assertTrue("DocB Not Found in the concept", docBFound);
        Assert.assertEquals("Docs In Use not supporting 'null args'", 2, queryService.getConceptsWithRelationships(su.getCompany(), null).size());
    }

    /**
     * Assert that we only get back relationships for a the selected document type. Checks that
     * Relationships, created via an association to a tag (Linux:Tag), can be filtered by doc type.
     * e.g. Sales and Promo both have a differently named relationship to the Device tag. When retrieving
     * Sales, we should only get the "purchased" relationship. Likewise with Promo, we should only get the "offer"
     *
     * @throws Exception
     */
    @Test
    public void uniqueRelationshipByDocType() throws Exception{
        logger.debug("### uniqueRelationshipByDocType");
        Neo4jHelper.cleanDb(template);
        setSecurity();
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("fortA");

        DocumentType sale = schemaService.resolveDocType(fortress, "Sale", true);
        commitManualTransaction(t);
        waitAWhile();
        t = beginManualTransaction();
        DocumentType promo = schemaService.resolveDocType(fortress, "Promotion", true);
        commitManualTransaction(t);

        MetaInputBean promoInput = new MetaInputBean(fortress.getName(), "jinks", promo.getName(), new DateTime());
        promoInput.addTag(new TagInputBean("Linux", "offer").setIndex("Device"));
        //promoInput.addTag(new TagInputBean("Mike", "sold").setIndex("Person"));
        trackEP.trackHeader(promoInput, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();

        MetaInputBean salesInput = new MetaInputBean(fortress.getName(), "jinks", sale.getName(), new DateTime());
        salesInput.addTag(new TagInputBean("Linux", "purchased").setIndex("Device"));
        //promoInput.addTag(new TagInputBean("Gary", "authorised").setIndex("Person"));
        trackEP.trackHeader(salesInput, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile();
        Collection<String>docs = new ArrayList<>();
        docs.add(promo.getName());
        docs.add(sale.getName());
        validateConcepts(docs, su, 2);
        docs.clear();
        docs.add(promo.getName());
        Set<DocumentType>foundDocs = validateConcepts(docs, su, 1);
        for (DocumentType foundDoc : foundDocs) {
            Assert.assertEquals("Promotion", foundDoc.getName());
            Collection<Concept> concepts = foundDoc.getConcepts();
            Assert.assertEquals(1, concepts.size());
            Concept concept = concepts.iterator().next();
            Assert.assertEquals("Device", concept.getName());
            Assert.assertEquals(1, concept.getRelationships().size());
            logger.info(foundDoc.toString());
        }
        //Set<DocumentType> concepts = queryEP.getRelationships(docs, su.getApiKey(), su.getApiKey());

    }

    @Test
    public void purgeFortressRemovesConcepts() throws Exception {
        logger.debug("### uniqueRelationshipByDocType");
        Neo4jHelper.cleanDb(template);
        setSecurity();

        engineAdmin.setConceptsEnabled(true);

        Transaction t ;

        SystemUser su = regService.registerSystemUser(new RegistrationBean("relationshipWorkForMultipleDocuments", mike_admin));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("relationshipWorkForMultipleDocuments");

        waitAWhile();
        t = beginManualTransaction();
        DocumentType claim = schemaService.resolveDocType(fortress, "Claim", true);
        commitManualTransaction(t);

        MetaInputBean promoInput = new MetaInputBean(fortress.getName(),
                "jinks",
                claim.getName(),
                new DateTime());
        promoInput.addTag(
                new TagInputBean("a1065", "identifier").setIndex("Claim"));

        trackEP.trackHeader(promoInput, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();

        waitAWhile();
        Collection<String>docs = new ArrayList<>();
        docs.add(claim.getName());
        validateConcepts(docs, su, 1);
        docs.clear();
        docs.add(claim.getName());
        Set<DocumentType>foundDocs = validateConcepts(docs, su, 1);
        for (DocumentType foundDoc : foundDocs) {
            Assert.assertEquals("Claim", foundDoc.getName());
            Collection<Concept> concepts = foundDoc.getConcepts();
            Assert.assertEquals(1, concepts.size());
            Concept concept = concepts.iterator().next();
            Assert.assertEquals("Claim", concept.getName());
            Assert.assertEquals(1, concept.getRelationships().size());
            logger.info(foundDoc.toString());
        }
        adminEP.purgeFortress(fortress.getName(), su.getApiKey(), su.getApiKey());
        Assert.assertEquals(0, schemaService.getCompanyDocumentsInUse(fortress.getCompany()).size());
    }

    private Set<DocumentType> validateConcepts(String document, SystemUser su, int expected) throws Exception{
        Collection<String>docs = new ArrayList<>();

        docs.add(document);
        return validateConcepts(docs, su, expected);
    }

    private Set<DocumentType> validateConcepts(Collection<String> docs, SystemUser su, int expected) throws Exception{
        Set<DocumentType> concepts = queryService.getConcepts(su.getCompany(), docs, true);
        String message = "Collection";
        if ( docs!=null && docs.size()==1 )
            message = docs.iterator().next();
        assertEquals( message+ " concepts", expected, concepts.size()); // Purchased docTypes
        return concepts;

    }

}
