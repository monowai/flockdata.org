/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.functional;

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.endpoint.EngineEndPoints;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 14/06/14
 * Time: 10:40 AM
 */
@WebAppConfiguration
public class TestQuery extends EngineBase {

    @Autowired
    WebApplicationContext wac;

    @Test
    public void queryInputsReturned () throws Exception{
        //      Each fortress one Entity (diff docs)
        //          One MH with same tags over both companies
        //          One MH with company unique tags
        setSecurity();

        // Two companies
        //  Each with two fortresses

        SystemUser suA = registerSystemUser("CompanyA", "userA");
        SystemUser suB = registerSystemUser("CompanyB", "userB");

        Fortress coAfA = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("coAfA", true));
        Fortress coAfB = fortressService.registerFortress(suA.getCompany(), new FortressInputBean("coAfB", true));

        Fortress coBfA = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("coBfA", true));
        Fortress coBfB = fortressService.registerFortress(suB.getCompany(), new FortressInputBean("coBfB", true));

        setSecurity();
        //
        //
        EntityInputBean inputBean = new EntityInputBean(coAfA.getName(), "poppy", "SalesDocket", DateTime.now(), "ABC1"); // Sales fortress
        inputBean.addTag(new TagInputBean("c123", "Customer", "purchased")); // This tag tracks over two fortresses
        mediationFacade.trackEntity(suA.getCompany(), inputBean);
        inputBean = new EntityInputBean(coAfB.getName(), "poppy", "SupportSystem", DateTime.now(), "ABC2"); // Support system fortress
        inputBean.addTag(new TagInputBean("c123","Customer","called")); // Customer number - this will be the same tag as for the sales fortress
        inputBean.addTag(new TagInputBean("p111","Product","about"));   // Product code - unique to this fortress
        mediationFacade.trackEntity(suA.getCompany(), inputBean);


        inputBean = new EntityInputBean(coBfA.getName(), "petal", "SalesDocket", DateTime.now(), "ABC1"); // Sales fortress
        inputBean.addTag(new TagInputBean("c123","Customer","purchased")); // This tag tracks over two fortresses
        inputBean.addTag(new TagInputBean("ricky", "SalesRep", "from").setLabel("SalesRep")); // This tag is unique to this company
        mediationFacade.trackEntity(suB.getCompany(), inputBean);
        inputBean = new EntityInputBean(coBfB.getName(), "petal", "SupportSystem", DateTime.now(), "ABC2"); // Support system fortress
        inputBean.addTag(new TagInputBean("c123","Customer","called")); // Customer number - this will be the same tag as for the sales fortress
        inputBean.addTag(new TagInputBean("p111", "Product", "about"));   // Product code - unique to this fortress
        mediationFacade.trackEntity(suB.getCompany(), inputBean);

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
