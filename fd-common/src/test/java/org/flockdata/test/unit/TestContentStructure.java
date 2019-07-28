/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.unit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import org.flockdata.helper.JsonUtils;
import org.flockdata.search.ContentStructure;
import org.flockdata.search.EsColumn;
import org.flockdata.search.SearchSchema;
import org.junit.Test;

/**
 * Sanity tests for ContentStructure
 *
 * @author mholdsworth
 * @since 31/08/2016
 */
public class TestContentStructure {

  @Test
  public void serializeEntityTagRelationship() throws Exception {
    ContentStructure contentStructure = new ContentStructure();
    contentStructure.addData(new EsColumn(SearchSchema.DATA_FIELD + "twee.facet", "string"));
    contentStructure.addLink(new EsColumn(SearchSchema.TAG_FIELD + "twee.facet", "string"));
    contentStructure.addLink(new EsColumn(SearchSchema.ENTITY + ".twee.facet", "string"));
    contentStructure.addFd(new EsColumn("whenCreated", "date"));
    String json = JsonUtils.toJson(contentStructure);
    assertNotNull(json);
    ContentStructure deserialzied = JsonUtils.toObject(json, ContentStructure.class);
    assertNotNull(deserialzied);

    assertEquals(2, deserialzied.getLinks().size());
    assertEquals(1, deserialzied.getSystem().size());
    assertEquals(1, deserialzied.getData().size());

    assertEquals("data. prefix and .facet suffix should have been removed", "twee", deserialzied.getData().iterator().next().getDisplayName());
    assertEquals("Display name didn't default to name", "whenCreated", deserialzied.getSystem().iterator().next().getDisplayName());
    for (EsColumn esColumn : deserialzied.getLinks()) {
      assertEquals("tag. & e. prefix and .facet suffix should have been removed", "twee", esColumn.getDisplayName());
    }


  }
}
