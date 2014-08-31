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

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.endpoint.EngineEndPoints;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.bean.MetaInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 14/06/14
 * Time: 10:40 AM
 */
@Transactional
@WebAppConfiguration

public class TestQuery extends TestEngineBase {

    @Autowired
    WebApplicationContext wac;


    @Test
    public void queryInputsReturned () throws Exception{
        //      Each fortress one MetaHeader (diff docs)
        //          One MH with same tags over both companies
        //          One MH with company unique tags
        setSecurity();

        // Two companies
        //  Each with two fortresses

        SystemUser suA = regService.registerSystemUser(new RegistrationBean("CompanyA", "userA"));
        SystemUser suB = regService.registerSystemUser(new RegistrationBean("CompanyB", "userB"));

        Fortress coAfA = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("coAfA"));
        Fortress coAfB = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("coAfB"));

        Fortress coBfA = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("coBfA"));
        Fortress coBfB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("coBfB"));

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
        EngineEndPoints engineEndPoints = new EngineEndPoints(wac);
        Collection<DocumentResultBean> foundDocs = engineEndPoints.getDocuments(suA, fortresses);
        assertEquals(1, foundDocs.size());

        fortresses.add(coAfB.getName());
        foundDocs = engineEndPoints.getDocuments(suA, fortresses);//queryEP.getDocumentsInUse (fortresses, suA.getApiKey(), suA.getApiKey());
        assertEquals(2, foundDocs.size());

        // Company B
        fortresses.clear();
        fortresses.add(coBfA.getName());
        assertEquals(1, engineEndPoints.getDocuments(suB, fortresses).size());
        fortresses.add(coBfB.getName());
        assertEquals(2, engineEndPoints.getDocuments(suB, fortresses).size());

    }

}
