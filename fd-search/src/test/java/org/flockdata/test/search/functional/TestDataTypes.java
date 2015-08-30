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

package org.flockdata.test.search.functional;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.search.service.TrackSearchDao;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.SearchChange;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.springframework.test.util.AssertionErrors.fail;

/**
 * DAT-359
 *
 * Created by mike on 27/03/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestDataTypes extends ESBase {

    @Autowired
    TrackSearchDao searchRepo;

    @Test (expected =AmqpRejectAndDontRequeueException.class )
    public void validate_MismatchSubsequentValue() throws Exception{
        String fortress = "mismatch";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entityA = Helper.getEntity(company, fortress, user, doc);

        SearchChange change = new EntitySearchChange(entityA);
        change.setDescription("Test Description");
        Map<String,Object> numMap = Helper.getSimpleMap("num", 100);
        change.setWhat(numMap );


        deleteEsIndex(entityA);
        searchRepo.ensureIndex(change);
        searchRepo.handle(change);
        Thread.sleep(1000);

        doQuery(entityA, "*", 1);

        Entity entityB = Helper.getEntity(company, fortress, user, doc);
        Map<String,Object> strMap = Helper.getSimpleMap("num", "NA");
        change = new EntitySearchChange(entityB);
        change.setDescription("Test Description");
        change.setWhat(strMap);

        searchRepo.handle(change);
        fail("A mapping exception was not thrown");

    }

    @Test
    public void serialize_SearchChanges () throws Exception {
        Company mockCompany = new Company("company");
        mockCompany.setName("company");

        FortressInputBean fib = new FortressInputBean("fortress", false);
        Fortress fortress = new Fortress(fib, mockCompany);

        DateTime now = new DateTime();
        EntityInputBean eib = new EntityInputBean("fortress",
                "harry",
                "docType",
                now,
                "abc");

        DocumentType doc = new DocumentType(fortress, "docType");
        Entity entity = new Entity("abc", fortress, eib, doc);


        EntitySearchChange searchChange = new EntitySearchChange(entity);
        EntitySearchChanges changes = new EntitySearchChanges(searchChange);
        String json = JsonUtils.getJSON(changes);

        EntitySearchChanges fromJson = JsonUtils.getBytesAsObject(json.getBytes(), EntitySearchChanges.class);

        TestCase.assertTrue("", fromJson.getChanges().size()==1);

    }
}
