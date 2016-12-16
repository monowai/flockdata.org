/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.search.functional;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.search.FdSearch;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchChanges;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.springframework.test.util.AssertionErrors.fail;

/**
 * DAT-359
 *
 * @author mholdsworth
 * @since 27/03/2015
 */
@RunWith(SpringRunner.class)
@SpringApplicationConfiguration(FdSearch.class)
@ActiveProfiles({"dev"})
public class TestDataTypes extends ESBase {

    @Autowired
    private EntityChangeWriter searchRepo;

    @Test (expected =AmqpRejectAndDontRequeueException.class )
    public void validate_MismatchSubsequentValue() throws Exception{
        String fortress = "mismatch";
        String doc = "doc";
        String user = "mike";

        Entity entity = getEntity(fortress, fortress, user, doc);
        deleteEsIndex(entity);

        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity))
                .setDescription("Test Description");

        Map<String,Object> numMap = EntityContentHelper.getSimpleMap("num", 100);
        change.setData(numMap);

        indexMappingService.ensureIndexMapping(change);
        searchRepo.handle(change);
        Thread.sleep(1000);

        doQuery(entity, "*", 1);

        Entity entityB = getEntity(fortress, fortress, user, doc);
        Map<String,Object> strMap = EntityContentHelper.getSimpleMap("num", "NA");
        change = new EntitySearchChange(entityB, indexManager.parseIndex(entityB));
        change.setDescription("Test Description");
        change.setData(strMap);

        searchRepo.handle(change);
        fail("A mapping exception was not thrown");

    }

    @Test
    public void serialize_SearchChanges () throws Exception {
        Company mockCompany = new Company("company");
        mockCompany.setName("company");

        FortressInputBean fib = new FortressInputBean("serialize_SearchChanges", false);
        Fortress fortress = new Fortress(fib, mockCompany);

        DateTime now = new DateTime();
        EntityInputBean eib = new EntityInputBean(fortress,
                "harry",
                "docType",
                now,
                "abc");

        DocumentType doc = new DocumentType(fortress, "docType");
        Entity entity = new Entity("abc", fortress, eib, doc);
        deleteEsIndex(entity);

        EntitySearchChange searchChange = new EntitySearchChange(entity, indexManager.parseIndex(entity));
        SearchChanges changes = new SearchChanges(searchChange);
        String json = JsonUtils.toJson(changes);

        SearchChanges fromJson = JsonUtils.toObject(json.getBytes(), SearchChanges.class);

        TestCase.assertTrue("", fromJson.getChanges().size()==1);

    }
}
