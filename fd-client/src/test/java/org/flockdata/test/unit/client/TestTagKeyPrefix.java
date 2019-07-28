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

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;

import java.util.Collection;
import java.util.List;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 22/07/2015
 */
public class TestTagKeyPrefix extends AbstractImport {
  private Logger logger = LoggerFactory.getLogger(TestTagKeyPrefix.class);

  @Test
  public void prefix_TagKeyWorks() throws Exception {

    ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/tag-key-prefix.json");
    ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/header-ignore-empty.json", contentModel);

    fileProcessor.processFile(extractProfile, "/data/tag-key-prefix.csv");

    List<TagInputBean> tagInputBeans = getTemplate().getTags();
    // The profile defines a nested tag but the value is missing in the source

    assertEquals(2, tagInputBeans.size());
    for (TagInputBean tagInputBean : tagInputBeans) {
      switch (tagInputBean.getKeyPrefix()) {
        case "UK":
          validateCountryAndLiteralKeyPrefix(tagInputBean.getTargets().get("region"));
          break;
        case "NZ":
          validateCountryAndLiteralKeyPrefix(tagInputBean.getTargets().get("region"));
          break;
        default:
          throw new RuntimeException("Unexpected tag " + tagInputBean.toString());
      }
      logger.info(tagInputBean.toString());
    }
  }

  private void validateCountryAndLiteralKeyPrefix(Collection<TagInputBean> countries) {
    assertFalse(countries.isEmpty());
    TagInputBean country = countries.iterator().next();
    assertEquals("literal", country.getKeyPrefix());
  }
}
