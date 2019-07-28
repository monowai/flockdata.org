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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.flockdata.test.helper.MockDataFactory.DEFAULT_ET_NAME;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.EsColumn;
import org.flockdata.search.QueryParams;
import org.flockdata.search.SearchChanges;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 31/08/2016
 */
@RunWith(SpringRunner.class)
public class TestContentStructure extends ESBase {

  @Test
  public void contentFieldsReturned() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(2);

    String fortress = "contentFieldsReturned";
    String company = "company";
    String doc = "doc";
    String user = "mike";

    json.put("numeric", 100);

    Entity entity = getEntity(company, fortress, user, doc);
    deleteEsIndex(entity);
    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));

    Collection<EntityTag> tags = new ArrayList<>();
    TagInputBean tagInputBean = new TagInputBean("SomeCode", "SomeLabel",
        new EntityTagRelationshipInput("blah"))
        .setProperty("mynum", 100);

    EntityTag entityTag = MockDataFactory.getEntityTag(entity, tagInputBean);

    assertTrue(entityTag.getTag().hasProperties());
    assertEquals(1, entityTag.getTag().getProperties().size());
    assertEquals(100, entityTag.getTag().getProperties().get("mynum"));

    tags.add(entityTag);
    change.setStructuredTags(tags);
    change.setDescription("Test Description");
    change.setData(json);

    esSearchWriter.createSearchableChange(new SearchChanges(change));
    Thread.sleep(600);
    QueryParams queryParams = new QueryParams();
    queryParams.setCompany(company);
    queryParams.setTypes(doc);
    queryParams.setFortress(fortress);

    ContentStructure dataStructure = contentService.getStructure(queryParams);
    assertNotNull(dataStructure);
    Collection<EsColumn> dataFields = dataStructure.getData();
    assertEquals("Keyword string fields should not be returned", 1, dataFields.size());
    assertEquals("Expected a tag and its numeric user defined property", 2, dataStructure.getLinks().size());
    Collection<EsColumn> linkFields = dataStructure.getLinks();
    for (EsColumn linkField : linkFields) {
      if (linkField.getDisplayName().startsWith(DEFAULT_ET_NAME + ".somelabel.code")) {
        assertEquals(DEFAULT_ET_NAME + ".somelabel.code", linkField.getDisplayName());
      } else {
        assertEquals(DEFAULT_ET_NAME + ".somelabel.mynum", linkField.getDisplayName());
      }
    }

    Collection<EsColumn> fdFields = dataStructure.getSystem();
    assertEquals(4, fdFields.size());
  }


}
