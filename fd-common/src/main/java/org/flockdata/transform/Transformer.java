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

package org.flockdata.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.entity.EntityPayloadTransformer;
import org.flockdata.transform.entity.JsonEntityTransformer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.flockdata.transform.model.PayloadTransformer;
import org.flockdata.transform.tag.TagPayloadTransformer;
import org.flockdata.transform.xml.XmlMappable;
import org.joda.time.DateTime;

/**
 * @author mholdsworth
 * @since 18/12/2015
 */
public class Transformer {

  public static EntityInputBean toEntity(Map<String, Object> row, ContentModel contentModel) throws FlockException {
    PayloadTransformer payloadTransformer = EntityPayloadTransformer.newInstance(contentModel);
    Map<String, Object> jsonData = payloadTransformer.transform(row);
    if (jsonData == null) {
      return null;// No entity did not get created
    }

    EntityInputBean entityInputBean = (EntityInputBean) payloadTransformer;

    if (TransformationHelper.evaluate(contentModel.isEntityOnly()) || jsonData.isEmpty()) {
      entityInputBean.setEntityOnly(true);
      // It's all Meta baby - no log information
    } else {
      String updatingUser = entityInputBean.getUpdateUser();
      if (updatingUser == null) {
        updatingUser = (entityInputBean.getFortressUser() == null ?
            contentModel.getFortressUser() : entityInputBean.getFortressUser());
      }

      ContentInputBean contentInputBean = new ContentInputBean(updatingUser,
          (entityInputBean.getWhen() != null ? new DateTime(entityInputBean.getWhen()) : null),
          jsonData);
      contentInputBean.setEvent(contentModel.getEvent());
      entityInputBean.setContent(contentInputBean);
    }
    return entityInputBean;

  }

  public static Collection<TagInputBean> toTags(Map<String, Object> row, ContentModel contentModel) throws FlockException {
    TagPayloadTransformer mappable = TagPayloadTransformer.newInstance(contentModel);
    mappable.transform(row);
    return mappable.getTags();

  }

  public static EntityInputBean toEntity(JsonNode node, ContentModel importProfile) throws FlockException {
    JsonEntityTransformer entityInputBean = new JsonEntityTransformer();
    entityInputBean.setData(node, importProfile);
    if (entityInputBean.getFortress() == null) {
      entityInputBean.setFortress(importProfile.getFortress());
    }
    ContentInputBean contentInputBean = new ContentInputBean();
    if (contentInputBean.getFortressUser() == null) {
      contentInputBean.setFortressUser(importProfile.getFortressUser());
    }
    entityInputBean.setContent(contentInputBean);
    contentInputBean.setData(FdJsonObjectMapper.getObjectMapper().convertValue(node, Map.class));

    return entityInputBean;

  }

  public static EntityInputBean toEntity(XmlMappable mappable, XMLStreamReader xsr, ContentModel importProfile)
      throws FlockException, JAXBException, JsonProcessingException {

    XmlMappable row = mappable.newInstance(importProfile);
    ContentInputBean contentInputBean = row.setXMLData(xsr, importProfile);
    EntityInputBean entityInputBean = (EntityInputBean) row;

    if (entityInputBean.getFortress() == null) {
      entityInputBean.setFortress(importProfile.getFortress());
    }

    if (entityInputBean.getFortressUser() == null) {
      entityInputBean.setFortressUser(importProfile.getFortressUser());
    }


    if (contentInputBean != null) {
      if (contentInputBean.getFortressUser() == null) {
        contentInputBean.setFortressUser(importProfile.getFortressUser());
      }
      entityInputBean.setContent(contentInputBean);
    }
    return entityInputBean;
  }

  /**
   * Constructs a default content profile from the data that the caller want's to import. Pretty simple functionality
   * that the caller should further enrich.
   * <p>
   * Does things like find lowest common denominator data type from the sample content supplied, i.e. Number {@literal ->} String
   *
   * @param content data to analyse
   * @return basic ContentProfile that describes the Contents
   */
  public static Map<String, ColumnDefinition> fromMapToModel(Collection<Map<String, Object>> content) {
    Map<String, ColumnDefinition> result = new TreeMap<>();
    if (content == null) {
      return result;
    }

    for (Map<String, Object> row : content) {
      for (String column : row.keySet()) {
        Object value = TransformationHelper.transformValue(row.get(column), column, null);
        String thisDataType = TransformationHelper.getDataType(value, column);
        if (value != null) {
          ColumnDefinition existingColumn = result.get(column);
          if (existingColumn != null && !existingColumn.getDataType().equals(thisDataType) &&
              !existingColumn.getDataType().equals("string")) {
            existingColumn.setDataType("string");// lowest common denominator
          } else if (existingColumn == null) {
            ColumnDefinition columnDefinition = new ColumnDefinition();
            columnDefinition.setDataType(thisDataType);
            columnDefinition.setTarget(getValidTarget(column));
            result.put(column, columnDefinition);
          }
        }

      }
    }

    return result;
  }

  public static boolean isValidForEs(String colName) {
    return !colName.contains(".");
  }

  private static String getValidTarget(String column) {
    if (column == null || column.length() == 0) {
      return null;
    }
    if (!isValidForEs(column)) // ElasticSearch does not accept columns with a period
    {
      return column.replace(".", "");
    }
    return null; // Null nothing to rename
  }

  public static Map<String, Object> convertToMap(String[] headerRow, String[] line) {
    if (headerRow == null || line == null) {
      throw new IllegalArgumentException("Header row or data row was null");
    }
    Map<String, Object> result = new HashMap<>();
    int col = 0;
    for (String column : headerRow) {
      Object value = line[col];
      result.put(column, TransformationHelper.transformValue(value, column, null));
      col++;
    }
    return result;
  }

  public static Collection<Map<String, Object>> convertToMap(DataConversionRequest request) {
    ExtractProfile extractProfile = new ExtractProfileHandler(request.getContentModel());
    Collection<Map<String, Object>> results = new ArrayList<>();
    Collection<String[]> lines = request.getData();
    for (String[] line : lines) {
      results.add(convertToMap(request.getHeader(), line, extractProfile));
    }
    return results;
  }

  public static Map<String, Object> convertToMap(String[] headerRow, String[] line, ExtractProfile extractProfile) {
    int col = 0;
    Map<String, Object> row = new HashMap<>();
    ContentModel contentModel = extractProfile.getContentModel();
    try {
      for (String column : headerRow) {
        column = column.trim();
        // Find first by the name (if we're using a raw header
        ColumnDefinition colDef = contentModel.getColumnDef(column);
        if (colDef == null)
        // Might be indexed by column number if there was no csv
        {
          colDef = contentModel.getColumnDef(Integer.toString(col));
        }

        Object value = line[col];
        value = TransformationHelper.transformValue(value, column, colDef);
        boolean addValue = true;
        if (TransformationHelper.evaluate(contentModel.isEmptyIgnored())) {
          if (value == null || value.toString().trim().equals("")) {
            addValue = false;
          }
        }
        if (addValue) {
          row.put(column, (value instanceof String ? ((String) value).trim() : value));
        }

        col++;
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      // Column does not exist for this row

    }

    return row;
  }
}
