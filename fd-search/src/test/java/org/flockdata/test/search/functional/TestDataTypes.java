/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.springframework.test.util.AssertionErrors.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.base.EntityChangeWriter;
import org.flockdata.test.helper.ContentDataHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * DAT-359
 *
 * @author mholdsworth
 * @since 27/03/2015
 */
@RunWith(SpringRunner.class)
public class TestDataTypes extends ESBase {

  @Autowired
  private EntityChangeWriter searchRepo;

  @Test(expected = AmqpRejectAndDontRequeueException.class)
  public void validate_MismatchSubsequentValue() throws Exception {
    String fortress = "mismatch";
    String doc = "doc";
    String user = "mike";

    Entity entity = getEntity(fortress, fortress, user, doc);
    deleteEsIndex(entity);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity))
        .setDescription("Test Description");

    Map<String, Object> numMap = ContentDataHelper.getSimpleMap("num", 100);
    change.setData(numMap);

    indexMappingService.ensureIndexMapping(change);
    searchRepo.handle(change);
    Thread.sleep(1000);

    doQuery(entity, "*");

    Entity entityB = getEntity(fortress, fortress, user, doc);
    Map<String, Object> strMap = ContentDataHelper.getSimpleMap("num", "NA");
    change = new EntitySearchChange(entityB, searchConfig.getIndexManager().toIndex(entityB));
    change.setDescription("Test Description");
    change.setData(strMap);

    searchRepo.handle(change);
    fail("A mapping exception was not thrown");

  }

  @Test
  public void dateStrings() throws Exception {
    String fortress = "dateStrings";
    String doc = "dates";
    String user = "mike";

    Entity entity = getEntity(fortress, fortress, user, doc);
    deleteEsIndex(entity);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity))
        .setDescription("Test Description");

    Date date = new Date();
    DateFormat outputFormatter = new SimpleDateFormat("yyyy/MM/dd");
    String output = outputFormatter.format(date);

    Map<String, Object> dateMap = new HashMap<>();
    dateMap.put("someDate", "2017-12-31");
    dateMap.put("rawDate", outputFormatter.parse(output));

    change.setData(dateMap);

    indexMappingService.ensureIndexMapping(change);
    searchRepo.handle(change);
    Thread.sleep(1000);

    String result = doQuery(entity, "*");
    assertNotNull(result);
    Collection<Map<String, Object>> hits = getHits(result);
    assertEquals(1, hits.size());
    Map<String, Object> source = (Map<String, Object>) hits.iterator().next().get("_source");
    assertNotNull(source);
    assertEquals(entity.getKey(), source.get("code"));
    Map<String, Object> theData = (Map<String, Object>) source.get("data");
    assertNotNull(theData.get("someDate"));
    assertEquals(dateMap.get("someDate"), theData.get("someDate"));
    Date foundDate = (Date) dateMap.get("rawDate");
    assertEquals("Dates did not match", dateMap.get("rawDate"), foundDate);

  }

}
