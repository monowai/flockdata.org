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

package org.flockdata.test.unit.client;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.flockdata.data.ContentModel;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 8/07/2016
 */
public class TestBlankTag extends AbstractImport {

  @Test
  public void blankTagCodeDoesNotSucceed() throws Exception {
    ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/blank-tags.json");
    //ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/blank-tags.json", contentModel);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    fileProcessor.processFile(extractProfile, "/data/blank-tags.txt");

    List<EntityInputBean> entities = getTemplate().getEntities();
    assertEquals(3, entities.size());

    // Tags must have a non-null non-blank code value to be valid. Data row 1 is such a scenario

    int count = 0;
    for (EntityInputBean entity : entities) {
      if (count == 0) {
        assertEquals(0, entity.getTags().size());
      } else if (count == 1) {
        assertEquals(1, entity.getTags().size());
      } else if (count == 3) {
        assertEquals("Delimited country tags did not parse", 7, entity.getTags().size());
      }
      count++;
    }

  }
}
