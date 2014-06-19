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

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: mike
 * Date: 19/06/14
 * Time: 8:47 AM
 */
public class TestTagConcepts extends TestEngineBase {
    @Test
    public void conceptsInUse() throws Exception {
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Assert.assertNotNull(su);

        Fortress fortA = fortressService.registerFortress("fortA");

        DocumentType dType = schemaService.resolveDocType(fortA, "ABC123", true);
        commitManualTransaction(t);// Should only be only one concept

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
        Collection<Concept> concepts = queryEP.getConcepts(docs, su.getApiKey(), su.getApiKey());
        org.junit.Assert.assertNotNull(concepts);
        assertEquals(1, concepts.size());

        // add a second concept
        input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "sold").setIndex("Rep"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey());
        waitAWhile("Concepts creating...");

        concepts = queryEP.getRelationships(docs, su.getApiKey(), su.getApiKey());
        assertEquals("Second concept wasn't added", 2, concepts.size());

        Boolean foundCustomer= false, foundRep= false;
        for (Concept concept : concepts) {
            if (concept.getName().equals("Customer")){
                foundCustomer = true;
                assertEquals(1, concept.getRelationships().size());
            }
            if (concept.getName().equals("Rep")) {
                foundRep = true;
                assertEquals(1, concept.getRelationships().size());
            }

        }
        assertTrue("Didn't find Customer concept", foundCustomer);
        assertTrue("Didn't find Rep concept", foundRep);

    }
}
