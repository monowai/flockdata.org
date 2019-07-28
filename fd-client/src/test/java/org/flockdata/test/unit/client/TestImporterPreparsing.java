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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import junit.framework.TestCase;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * Preparsing datasets before import
 *
 * @author mholdsworth
 * @since 28/01/2015
 */
public class TestImporterPreparsing extends AbstractImport {
  @Test
  public void string_PreParseRow() throws Exception {

    ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/pre-parse.json");
    ExtractProfile params = new ExtractProfileHandler(contentModel, false);
    assertEquals(',', params.getDelimiter());
    params.setQuoteCharacter("|");
    assertEquals(false, params.hasHeader());
    long rows = fileProcessor.processFile(params, "/data/properties-rlx.txt");
    assertEquals(4, rows);

    List<EntityInputBean> entityBatch = fdTemplate.getEntities();

    for (EntityInputBean entityInputBean : entityBatch) {
      assertFalse("Expression not parsed for code", entityInputBean.getCode().contains("|"));
      assertTrue("Tag not set", entityInputBean.getTags().size() == 3);
      TagInputBean politician = null;
      for (TagInputBean tagInputBean : entityInputBean.getTags()) {
        assertFalse("Expression not parsed for code", tagInputBean.getCode().contains("|"));
        if (tagInputBean.getLabel().equals("Politician")) {
          politician = tagInputBean;
        }
        if (tagInputBean.getLabel().equals("InterestGroup")) {
          assertEquals("direct", tagInputBean.getEntityTagLinks().keySet().iterator().next());
          for (String key : tagInputBean.getEntityTagLinks().keySet()) {
            EntityTagRelationshipInput etib = tagInputBean.getEntityTagLinks().get(key);
            TestCase.assertEquals(2, etib.getProperties().size());
            TestCase.assertNotNull(etib.getProperties().get("amount"));
            TestCase.assertEquals("ABC123", etib.getProperties().get("calculatedColumn"));

          }
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

}
