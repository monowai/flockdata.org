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

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.entity.EntityPayloadTransformer;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * CSV files with no headers
 *
 * @author mholdsworth
 * @since 28/01/2015
 */
public class TestEntityRelationships extends AbstractImport {

  @Test
  public void evaluation_EntityLinkExpressions() throws Exception {
    String file = "/model/entity-links.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
    ExtractProfile params = new ExtractProfileHandler(contentModel);
    long rows = fileProcessor.processFile(params, "/data/entity-relationships.txt");

    assertEquals("Expected to parse 3 rows", 3, rows);
    List<EntityInputBean> entities = getTemplate().getEntities();
    assertEquals("Expected to compress all rows into a single entity", 1, entities.size());
    for (EntityInputBean entity : entities) {
      assertEquals(1, entity.getEntityLinks().size());
      assertNotNull("Relationship name did not evaluate as an expression", entity.getEntityLinks().get("intermediary of"));
    }
    // Due to batch size > 1, entitiesLinks should be merged into a common primary key source
    assertEquals("Expected relationships to 3 entities", 3, entities.iterator().next().getEntityLinks().get("intermediary of").size());
  }

  @Test
  public void string_NoHeaderWithDelimiter() throws Exception {
    String file = "/model/properties-rlx.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(file);
    ExtractProfile params = new ExtractProfileHandler(contentModel);
    params.setHeader(false);

    params.setQuoteCharacter("|");
    assertEquals(Boolean.FALSE, params.hasHeader());
    long rows = fileProcessor.processFile(params, "/data/properties-rlx.txt");
    assertEquals(4, rows);
    List<EntityInputBean> entityBatch = fdTemplate.getEntities();
    assertEquals(4, entityBatch.size());
    for (EntityInputBean entityInputBean : entityBatch) {
      assertFalse("Expression not parsed for code", entityInputBean.getCode().contains("|"));
      assertTrue("Caller ref appears invalid", entityInputBean.getCode().length() > 4);
      assertTrue("Tag not set", entityInputBean.getTags().size() == 3);
      TagInputBean politician = null;
      for (TagInputBean tagInputBean : entityInputBean.getTags()) {
        assertFalse("Expression not parsed for code", tagInputBean.getCode().contains("|"));
        assertNull("Name should be null if it equals the code", tagInputBean.getName());
        if (tagInputBean.getLabel().equals("Politician")) {
          politician = tagInputBean;
        }
        if (tagInputBean.getLabel().equals("InterestGroup")) {
          assertEquals("direct", tagInputBean.getEntityTagLinks().keySet().iterator().next());
        }
      }
      assertNotNull(politician);
      EntityTagRelationshipInput link = politician.getEntityTagLinks().get("receives");
      assertNotNull(link);
      assertNotNull(link.getProperties().get("amount"));
      assertTrue("Amount not calculated as a value", Integer.parseInt(link.getProperties().get("amount").toString()) > 0);

    }
    ObjectMapper om = new ObjectMapper();
    try {
      om.writeValueAsString(entityBatch);
    } catch (Exception e) {
      throw new FlockException("Failed to serialize");
    }

  }

  @Test
  public void entityRowWithEntityLinks() throws Exception {
    ContentModel params = ContentModelDeserializer.getContentModel("/model/csvtest.json");
    EntityPayloadTransformer entity = EntityPayloadTransformer.newInstance(params);
    // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
    String[] headers = new String[] {"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals", "Category", "xRef"};
    // Category column is intentionally null
    String[] data = new String[] {"TitleTests", "TagName", "Gold", "8", "New Zealand", "2008", "12", null, "qwerty"};
    Map<String, Object> json = entity.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)));
    Assert.assertNotNull(json);

    assertTrue("Title Missing", json.containsKey("Title"));
    assertTrue("Tag Missing", json.containsKey("Tag"));
    assertTrue("Tag Value Missing", json.containsKey("TagVal"));
    assertTrue("Tag Value Missing", json.containsKey("ValTag"));
    Map<String, List<EntityKeyBean>> entityLinks = entity.getEntityLinks();

    assertFalse(entityLinks.isEmpty());
    assertEquals(2, entityLinks.size());
    boolean foundExposed = false, foundBlah = false;
    for (String s : entityLinks.keySet()) {
      if (s.equals("exposed")) {
        // Check for 2 values
        assertEquals(2, entityLinks.get("exposed").size());
        foundExposed = true;
      } else if (s.equals("blah")) {
        assertEquals(1, entityLinks.get("blah").size());
        for (String s1 : entityLinks.keySet()) {
          EntityKeyBean ek = entityLinks.get("blah").iterator().next();
          assertEquals("Olympic", ek.getFortressName());
          assertEquals("Other", ek.getDocumentType());
          assertEquals("qwerty", ek.getCode());
        }
        foundBlah = true;
      }
    }
    assertEquals(true, foundBlah & foundExposed);
    org.junit.Assert.assertEquals(data[0], entity.getCode());
    List<TagInputBean> tags = entity.getTags();
    int tagsFound = 0;
    boolean callerRefFoundAsATag = false;
    boolean nullCategoryFound = false;
    for (TagInputBean tag : tags) {

      switch (tag.getCode()) {
        case "Gold Medals":
          EntityTagRelationshipInput o = tag.getEntityTagLinks().get("2008");
          Assert.assertNotNull(o);
          assertEquals(12, o.getProperties().get("value"));
          tagsFound++;
          break;
        case "TagName":
          assertEquals("TagName", tag.getCode());
          tagsFound++;
          break;
        case "Gold":
          assertEquals(true, tag.isMustExist());
          tagsFound++;
          break;
        case "ValTag":
          Assert.assertNotNull(tag.getEntityTagLinks().get("undefined"));
          assertEquals(1, tag.getEntityTagLinks().size());
          assertEquals("ValTag", tag.getName());
          assertEquals("ValTag", tag.getLabel());
          assertEquals(8, (tag.getEntityTagLinks().get("undefined")).getProperties().get("value"));
          tagsFound++;
          break;
        case "New Zealand":
          assertEquals("Country", tag.getLabel());
          tagsFound++;
          break;
        case "TitleTests":
          callerRefFoundAsATag = true;
          assertNull("Name should be null as it is the same as the code", tag.getName());
          tagsFound++;
          break;
        case "Undefined":
          nullCategoryFound = true;
          assertEquals("Undefined", tag.getCode());
          assertEquals("Category", tag.getLabel());
          tagsFound++;
          break;

      }
    }
    assertTrue("The callerRef was flagged as a tag but not found", callerRefFoundAsATag);
    assertTrue("The undefined category column was not found ", nullCategoryFound);
    assertSame(tags.size(), tagsFound);
  }


}
