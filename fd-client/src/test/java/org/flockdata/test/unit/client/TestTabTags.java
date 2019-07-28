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
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.flockdata.transform.tag.TagPayloadTransformer;
import org.junit.Test;

/**
 * Tag targets are handled
 *
 * @author mholdsworth
 * @since 27/01/2015
 */
public class TestTabTags {
  @Test
  public void string_NestedTags() throws Exception {
    ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/sectors.json");
    TagPayloadTransformer tagTransformer = TagPayloadTransformer.newInstance(contentModel);
    String[] headers = new String[] {"Catcode", "Catname", "Catorder", "Industry", "Sector", "Sector Long"};
    String[] data = new String[] {"F2600", "Private Equity & Investment Firms", "F07", "Securities & Investment", "Finance/Insur/RealEst", "Finance", "Insurance & Real Estate"};

    Map<String, Object> json = tagTransformer.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(contentModel)));
    assertEquals(1, tagTransformer.getTags().size());
    TagInputBean tag = tagTransformer.getTags().iterator().next();

    assertNotNull(json);
    assertNotNull(tagTransformer);
    assertEquals("Code does not match", "F2600", tag.getCode());
    assertEquals("Name does not match", "Private Equity & Investment Firms", tag.getName());
    assertNotNull(tag.getProperties().get("order"));
    assertEquals(1, tag.getTargets().size());
    Collection<TagInputBean> targets = tag.getTargets().get("comprises");
    assertEquals(1, targets.size());
    tag = targets.iterator().next();
    assertNotNull(tag.getDescription());
  }

}
