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
import org.springframework.data.neo4j.support.node.Neo4jHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: mike
 * Date: 19/06/14
 * Time: 8:47 AM
 */
public class TestTagConcepts extends TestEngineBase {
    @Override
    public void cleanUpGraph() {
        // Nothing
    }
    @Test
    public void multipleDocsSameFortress() throws Exception {
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("fortA");

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

        validateConcepts("DocB", su, 1);
        validateConcepts("DocA", su, 1);

    }

    private Collection<String> validateConcepts(String document, SystemUser su, int expected) throws Exception{
        Collection<String>docs = new ArrayList<>();

        docs.add(document);
        return validateConcepts(docs, su, expected);
    }
    private Collection<String>validateConcepts(Collection<String> docs, SystemUser su, int expected) throws Exception{
        Set<DocumentType> concepts = queryEP.getRelationships(docs, su.getApiKey(), su.getApiKey());
        String message = "Collection";
        if ( docs.size()==1 )
            message = docs.iterator().next();
        assertEquals( message+ " concepts", expected, concepts.size()); // Purchased docTypes
        return docs;

    }
    @Test
    public void fortressConcepts() throws Exception {
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Assert.assertNotNull(su);

        Fortress fortA = fortressService.registerFortress("fortA");

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
        Collection<DocumentType> documentTypes = queryEP.getConcepts(docs, su.getApiKey(), su.getApiKey());
        org.junit.Assert.assertNotNull(documentTypes);
        assertEquals(1, documentTypes.size());

        // add a second docTypes
        input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "sold").setIndex("Rep"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey());
        waitAWhile("Concepts creating...");

        documentTypes = queryEP.getRelationships(docs, su.getApiKey(), su.getApiKey());
        assertEquals("Only one doc type should exist", 1, documentTypes.size());

        Boolean foundCustomer= false, foundRep= false;

        for (DocumentType docTypes : documentTypes) {
            for ( Concept concept : docTypes.getConcepts()) {
                if (concept.getName().equals("Customer")){
                    foundCustomer = true;
                    assertEquals(1, concept.getRelationships().size());
                    assertEquals("purchased", concept.getRelationships().iterator().next().getName());
                }
                if (concept.getName().equals("Rep")) {
                    foundRep = true;
                    assertEquals(1, concept.getRelationships().size());
                    assertEquals("sold", concept.getRelationships().iterator().next().getName());
                }
            }

        }
        assertTrue("Didn't find Customer concept", foundCustomer);
        assertTrue("Didn't find Rep concept", foundRep);
    }

    @Test
    public void multipleRelationships() throws Exception {
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Assert.assertNotNull(su);

        Fortress fortress = fortressService.registerFortress("fortA");

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
        validateConcepts("DocA", su, 1);

        Collection<String>docs = new ArrayList<>();
        docs.add("DocA");
        Set<DocumentType> docTypes = queryEP.getRelationships(docs, su.getApiKey(), su.getApiKey());
        for (DocumentType docType : docTypes) {
            Set<Concept>concepts = docType.getConcepts();
            for (Concept concept : concepts) {
                Collection<Relationship> relationships  =concept.getRelationships();
                Assert.assertEquals(2, relationships.size());

            }
        }
    }

}
