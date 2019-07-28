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

package org.flockdata.test.unit.importer;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.unit.client.AbstractImport;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * Datatypes
 *
 * @author mholdsworth
 * @since 27/02/2015
 */

public class TestDataTypeConversion extends AbstractImport {

  @Test
  public void preserve_NumberValueAsString() throws Exception {
    String fileName = "/model/data-types.json";
    getTemplate().flush();
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    fileProcessor.processFile(extractProfile, "/data/data-types.csv");
    List<TagInputBean> tagInputBeans = getTemplate().getTags();
    assertEquals(2, tagInputBeans.size());
    for (TagInputBean tagInputBean : tagInputBeans) {
      if (tagInputBean.getLabel().equals("as-string")) {
        assertEquals("00165", tagInputBean.getCode());
      }
    }
    EntityInputBean entity = getTemplate().getEntities().iterator().next();
    assertNotNull(entity.getContent());
    assertEquals("The N/A string should have been set to the default of 0", 0, entity.getContent().getData().get("illegal-num"));
    assertEquals("The Blank string should have been set to the default of 0", 0, entity.getContent().getData().get("blank-num"));
  }

  @Test
  public void double_EntityProperty() throws Exception {
    // Tests that numeric values are converted to explicit data-type
    String fileName = "/model/entity-data-types.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    fileProcessor.processFile(extractProfile, "/data/entity-data-types.csv");
    List<EntityInputBean> entityInputBeans = getTemplate().getEntities();
    assertEquals(2, entityInputBeans.size());
    for (EntityInputBean entityInputBean : entityInputBeans) {
      Object o = entityInputBean.getProperties().get("value");
      TestCase.assertTrue("Should have been cast to a Double but was " + o.getClass().getName(), o instanceof Double);
    }
  }

  @Test
  public void preserve_TagCodeAlwaysString() throws Exception {
    // Even though the source column can be treated as a number, it should be set as a String because
    // it drives a tag code.
    String fileName = "/model/data-types.json";

    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    fileProcessor.processFile(extractProfile, "/data/data-types.csv");
    List<TagInputBean> tagInputBeans = getTemplate().getTags();
    assertEquals(2, tagInputBeans.size());
    for (TagInputBean tagInputBean : tagInputBeans) {
      if (tagInputBean.getLabel().equals("tag-code")) {
        assertEquals("123", tagInputBean.getCode());
      }
    }
    List<EntityInputBean> entities = getTemplate().getEntities();
    for (EntityInputBean entity : entities) {
      Object whatString = entity.getContent().getData().get("tag-code");
      assertEquals("" + whatString.getClass(), true, whatString instanceof String);
    }

  }

  @Test
  public void number_Converts() throws Exception {
    // DAT-454
    String fileName = "/model/data-types.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    String header[] = new String[] {"num"};
    String row[] = new String[] {"0045"};
    Map<String, Object> converted = Transformer.convertToMap(header, row, extractProfile);
    assertEquals("45", converted.get("num").toString());

    row = new String[] {null};
    converted = Transformer.convertToMap(header, row, extractProfile);
    assertEquals(null, converted.get("num"));

  }

  @Test
  public void number_ConvertsWithThousandSeparator() throws Exception {
    // DAT-454

    String fileName = "/model/data-types.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    String header[] = new String[] {"num"};
    String row[] = new String[] {"50,000.99"}; // Not internatioalised
    Map<String, Object> converted = Transformer.convertToMap(header, row, extractProfile);
    assertEquals("50000.99", converted.get("num").toString());


    row = new String[] {""};
    converted = Transformer.convertToMap(header, row, extractProfile);
    assertEquals(null, converted.get("num"));

    row = new String[] {null};
    converted = Transformer.convertToMap(header, row, extractProfile);
    assertEquals(null, converted.get("num"));

  }

  @Test
  public void title_Expression() throws Exception {
    // DAT-457
    String fileName = "/model/data-types.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    fileProcessor.processFile(extractProfile, "/data/data-types.csv");
    List<EntityInputBean> entities = getTemplate().getEntities();
    assertEquals(1, entities.size());
    EntityInputBean entityInputBean = entities.iterator().next();
    assertEquals("Title expression did not evaluate", "00165-test", entityInputBean.getName());
  }

  @Test
  public void date_CreatedDateSets() throws Exception {
    // DAT-457
    String fileName = "/model/data-types.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);
    fileProcessor.processFile(extractProfile, "/data/data-types.csv");
    List<EntityInputBean> entities = getTemplate().getEntities();
    assertEquals(1, entities.size());
    EntityInputBean entityInputBean = entities.iterator().next();

    Calendar calInstance = Calendar.getInstance();
    calInstance.setTime(entityInputBean.getWhen());
    assertEquals(2015, calInstance.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, calInstance.get(Calendar.MONTH));
    assertEquals(14, calInstance.get(Calendar.DATE));

    // DAT-523
    Object randomDate = entityInputBean.getContent().getData().get("randomDate");
    assertNotNull(randomDate);
    TestCase.assertTrue("", randomDate instanceof String);
    new DateTime(randomDate);
    TestCase.assertNull(entityInputBean.getContent().getData().get("nullDate"));


  }

  @Test
  public void date_LastChange() throws Exception {
    // Given 2 dates that could be the last change, check the most recent
    String fileName = "/model/data-types.json";
    ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);
    ExtractProfile extractProfile = new ExtractProfileHandler(contentModel);

    fileProcessor.processFile(extractProfile, "/data/data-types.csv");
    List<EntityInputBean> entities = getTemplate().getEntities();
    assertEquals(1, entities.size());
    EntityInputBean entityInputBean = entities.iterator().next();

    Calendar calInstance = Calendar.getInstance();
    calInstance.setTime(entityInputBean.getLastChange());
    assertEquals(2015, calInstance.get(Calendar.YEAR));
    assertEquals(Calendar.MARCH, calInstance.get(Calendar.MONTH));
    assertEquals(25, calInstance.get(Calendar.DATE));

  }

  @Test
  public void map_NoArgsSuppliedToGetDefaultContentModel() throws Exception {
    assertEquals("Null map should return 0 entries", 0, Transformer.fromMapToModel(null).size());
    Collection<Map<String, Object>> rows = new ArrayList<>();
    assertEquals("Empty row set should return 0 entries", 0, Transformer.fromMapToModel(rows).size());
    rows.add(new HashMap<>());
    assertEquals("Empty row should return 0 entries", 0, Transformer.fromMapToModel(rows).size());
  }

  @Test
  public void map_SimpleDefaultContentModelFromInputData() throws Exception {
    Map<String, Object> firstRow = new HashMap<>();
    firstRow.put("NumCol", 120);
    firstRow.put("StrCol", "Abc");
    firstRow.put("DateCol", "2015-12-12");
    firstRow.put("EpocDate", System.currentTimeMillis());
    firstRow.put("LongNotADate", 123445552);
    firstRow.put("StringThatLooksLikeANumber", 123);
    firstRow.put("NumberThatIsNotADate", 90);


    Map<String, Object> secondRow = new HashMap<>();
    secondRow.put("NumCol", 122);
    secondRow.put("StrCol", "Abc");
    secondRow.put("DateCol", "2015-12-12");
    secondRow.put("EpocDate", System.currentTimeMillis());
    secondRow.put("LongNotADate", 123445550);
    secondRow.put("StringThatLooksLikeANumber", "Turn to a String");
    secondRow.put("NumberThatIsNotADate", 90);

    Collection<Map<String, Object>> rows = new ArrayList<>();
    rows.add(firstRow);
    rows.add(secondRow);
    Map<String, ColumnDefinition> columnDefinitions = Transformer.fromMapToModel(rows);
    assertEquals(firstRow.size(), columnDefinitions.size());
    for (String column : columnDefinitions.keySet()) {
      ColumnDefinition colDef = columnDefinitions.get(column);
      assertNotNull(colDef);
      switch (column) {
        case "NumCol":
        case "LongNotADate":
          assertEquals("number", colDef.getDataType());
          break;
        case "StrCol":
          assertEquals("string", colDef.getDataType());
          break;
        case "DateCol":
        case "EpocDate":
          assertEquals("date", colDef.getDataType());
          break;
        case "StringThatLooksLikeANumber":
          assertEquals("DataType should be String as its the lower common denominator", "string", colDef.getDataType());
          break;
        case "NumberThatIsNotADate":
          assertEquals("DataType should be Number", "number", colDef.getDataType());
          break;
        default:
          fail("unknown column " + colDef);
          break;
      }
    }
  }


}