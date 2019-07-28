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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 13/07/2016
 */
public class TestEntityTagLinks extends AbstractImport {

  @Test
  public void processCsv() throws Exception {
    String file = "/model/entity-tag-links.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
    ExtractProfile params = new ExtractProfileHandler(contentModel);
    long rows = fileProcessor.processFile(params, "/data/test-entity-tag-links.csv");

    assertEquals(1, rows);

    List<EntityInputBean> entities = getTemplate().getEntities();

    for (EntityInputBean entity : entities) {
      assertEquals(4, entity.getTags().size());

      for (TagInputBean tagInputBean : entity.getTags()) {
        if (tagInputBean.getLabel().equals("Jurisdiction")) {
          assertFalse(tagInputBean.getEntityTagLinks().size() == 0);
          EntityTagRelationshipInput jurisdiction = tagInputBean.getEntityTagLinks().get("jurisdiction");
          assertNotNull(jurisdiction);
          assertTrue(jurisdiction.isGeo());
          assertEquals("SAM", tagInputBean.getCode());
        } else if (tagInputBean.getLabel().equals("Country")) {
          assertFalse(tagInputBean.getEntityTagLinks().size() == 0);
          EntityTagRelationshipInput country = tagInputBean.getEntityTagLinks().get("located");
          assertNotNull(country);
          assertTrue(country.isGeo());
          assertEquals("HKG", tagInputBean.getCode());
        } else if (tagInputBean.getLabel().equals("ServiceProvider")) {
          assertTrue(tagInputBean.isReverse());
        } else {
          assertFalse(tagInputBean.isReverse());
        }

      }
    }
  }
}
