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
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.DocumentType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 14/06/14
 * Time: 10:40 AM
 */
@Transactional
public class TestQuery extends TestEngineBase {

    @Test
    public void queryInputsReturned () throws Exception{
        //      Each fortress one MetaHeader (diff docs)
        //          One MH with same tags over both companies
        //          One MH with company unique tags
        setSecurity();

        // Two companies
        //  Each with two fortresses

        SystemUserResultBean suA = registrationEP.registerSystemUser(new RegistrationBean("CompanyA", "userA")).getBody();
        SystemUserResultBean suB = registrationEP.registerSystemUser(new RegistrationBean("CompanyB", "userB")).getBody();

        Fortress coAfA = createFortress(suA, "coAfA");
        Fortress coAfB = createFortress(suA, "coAfB");

        Fortress coBfA = createFortress(suB, "coBfA");
        Fortress coBfB = createFortress(suB, "coBfB");

        setSecurity();
        //
        //
        MetaInputBean inputBean = new MetaInputBean(coAfA.getName(), "poppy", "SalesDocket", DateTime.now(), "ABC1"); // Sales fortress
        inputBean.addTag(new TagInputBean("c123", "purchased").setIndex("Customer")); // This tag tracks over two fortresses
        trackEP.trackHeader(inputBean, suA.getApiKey(), null);
        inputBean = new MetaInputBean(coAfB.getName(), "poppy", "SupportSystem", DateTime.now(), "ABC2"); // Support system fortress
        inputBean.addTag(new TagInputBean("c123","called").setIndex("Customer")); // Customer number - this will be the same tag as for the sales fortress
        inputBean.addTag(new TagInputBean("p111","about").setIndex("Product"));   // Product code - unique to this fortress
        trackEP.trackHeader(inputBean, suA.getApiKey(), null);


        inputBean = new MetaInputBean(coBfA.getName(), "petal", "SalesDocket", DateTime.now(), "ABC1"); // Sales fortress
        inputBean.addTag(new TagInputBean("c123","purchased").setIndex("Customer")); // This tag tracks over two fortresses
        inputBean.addTag(new TagInputBean("ricky", "from").setIndex("SalesRep")); // This tag is unique to this company
        trackEP.trackHeader(inputBean, suB.getApiKey(), null);
        inputBean = new MetaInputBean(coBfB.getName(), "petal", "SupportSystem", DateTime.now(), "ABC2"); // Support system fortress
        inputBean.addTag(new TagInputBean("c123","called").setIndex("Customer")); // Customer number - this will be the same tag as for the sales fortress
        inputBean.addTag(new TagInputBean("p111", "about").setIndex("Product"));   // Product code - unique to this fortress
        trackEP.trackHeader(inputBean, suB.getApiKey(), null);

        Collection<String> fortresses = new ArrayList<>();
        fortresses.add(coAfA.getName());
        Collection<DocumentType> foundDocs = queryEP.getDocumentsInUse (fortresses, suA.getApiKey(), suA.getApiKey());
        assertEquals(1, foundDocs.size());

        fortresses.add(coAfB.getName());
        foundDocs = queryEP.getDocumentsInUse (fortresses, suA.getApiKey(), suA.getApiKey());
        assertEquals(2, foundDocs.size());

        // Company B
        fortresses.clear();
        fortresses.add(coBfA.getName());
        assertEquals(1, queryEP.getDocumentsInUse(fortresses, suB.getApiKey(), suB.getApiKey()).size());
        fortresses.add(coBfB.getName());
        assertEquals(2, queryEP.getDocumentsInUse (fortresses, suB.getApiKey(), suB.getApiKey()).size());

    }
}
