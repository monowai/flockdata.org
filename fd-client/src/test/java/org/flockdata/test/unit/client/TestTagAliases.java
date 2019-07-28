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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collection;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * Alias tags
 *
 * @author mholdsworth
 * @since 27/01/2015
 */
public class TestTagAliases extends AbstractImport {
  @Test
  public void string_csvTagAliases() throws Exception {
    String paramFile = "/model/csv-tag-alias.json";

    ContentModel params = ContentModelDeserializer.getContentModel(paramFile);
    fileProcessor.processFile(new ExtractProfileHandler(params), "/data/csv-tag-alias.txt");

    Collection<TagInputBean> tagInputBeans = getTemplate().getTags();
    assertEquals(3, tagInputBeans.size());
    for (TagInputBean tagInputBean : tagInputBeans) {
      switch (tagInputBean.getCode()) {
        case "AL":
          assertTrue(tagInputBean.hasAliases());
          assertNotNull(tagInputBean.getNotFoundCode());
          assertEquals(1, tagInputBean.getAliases().size());
          assertEquals("1", tagInputBean.getAliases().iterator().next().getCode());
          assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
          break;
        case "AK":
          assertTrue(tagInputBean.hasAliases());
          assertNotNull(tagInputBean.getNotFoundCode());
          assertEquals(1, tagInputBean.getAliases().size());
          assertEquals("2", tagInputBean.getAliases().iterator().next().getCode());
          assertEquals("USCensus", tagInputBean.getAliases().iterator().next().getDescription());
          break;
        case "AB":
          assertFalse(tagInputBean.hasAliases());
          break;
      }
    }
  }


}
