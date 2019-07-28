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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.data.ContentModel;
import org.flockdata.data.Tag;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityTagRelationshipDefinition;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.tag.TagProfile;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

/**
 * Helper functions for interpreting ColumnDefinitions and setting values
 *
 * @author mholdsworth
 * @since 27/08/2014
 */
public class TransformationHelper {
  private static org.slf4j.Logger logger = LoggerFactory.getLogger(TransformationHelper.class);

  public static boolean setTagInputBean(TagInputBean tag,
                                        Map<String, Object> row,
                                        String column,
                                        ContentModel content,
                                        String value) throws FlockException {

    ColumnDefinition colDef = content.getColumnDef(column);

    String label;
    if (colDef.getLabel() != null && colDef.getLabel().equals(colDef.getTarget() != null ? colDef.getTarget() : column)) {
      label = colDef.getLabel();
    } else {
      label = resolveValue(colDef.getLabel(), column, colDef, row);
    }

    tag.setMustExist(TransformationHelper.evaluate(colDef.isMustExist()))
        .setLabel(label)
        .setNotFoundCode(colDef.getNotFound());

    if (TransformationHelper.evaluate(colDef.isMerge(), true)) {
      tag.setMerge(true);
    }

    String codeValue = ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.CODE, colDef, value);
    tag.setCode(codeValue);

    tag.setKeyPrefix(ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.KEY_PREFIX, colDef, null));

    if (colDef.getLabelDescription() != null) {
      Object o = ExpressionHelper.getValue(row, colDef.getLabelDescription(), colDef, colDef.getLabelDescription());
      if (o != null) {
        tag.setDescription(o.toString());
      }
    }

    if (!TransformationHelper.evaluate(colDef.isMustExist())) {     // Must exists only resolves the Code, so don't waste time setting the name
      String name = ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, codeValue);
      if (name != null && !name.equals(codeValue)) {
        tag.setName(name);
      }
    }

    setAliases(tag, colDef, row);
    if (!content.isTagModel()) {
      EntityTagRelationshipInput relationship = getRelationship(row, colDef);
      tag.addEntityTagLink(relationship);
    }


    tag.setReverse(TransformationHelper.evaluate(colDef.getReverse()));
    if (colDef.hasProperites()) { // Properties for the tag
      for (ColumnDefinition propertyColumn : colDef.getProperties()) {
        if (TransformationHelper.evaluate(colDef.isPersistent(), true)) {
          String sourceCol = propertyColumn.getSource();

          Object result = getObject(row, value, propertyColumn, sourceCol);

          if (result != null) {
            tag.setProperty(propertyColumn.getTarget() == null ? sourceCol : propertyColumn.getTarget(), ExpressionHelper.getValue(result, propertyColumn));
          }
        }
      }
    }

    if (tag.getCode() == null || tag.getCode().trim().equals("")) {
      return false;
    }

    setNestedTags(tag, colDef.getTargets(), row);

    return true;
  }

  public static String resolveValue(String value, String column, ColumnDefinition colDef, Map<String, Object> row) {
    if (value == null) {
      return column; // Default to the column Name
    }
    Object result = ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.LABEL, colDef, value);
    if (result == null) {
      return null;
    }
    return result.toString();
  }

  private static void setAliases(TagInputBean tag, ColumnDefinition colDef, Map<String, Object> row) {
    if (colDef.hasAliases()) {
      tag.setAliases(getTagAliasValues(colDef.getAliases(), row));
    }
  }

  private static void setAliases(TagInputBean tag, TagProfile tagDef, Map<String, Object> row) {
    if (tagDef.hasAliases()) {
      tag.setAliases(getTagAliasValues(tag.getAliases(), row));

    }
  }

  private static Collection<AliasInputBean> getTagAliasValues(Collection<AliasInputBean> aliases, Map<String, Object> row) {

    if (aliases == null) {
      return new ArrayList<>();
    }
    Collection<AliasInputBean> results = new ArrayList<>(aliases.size());

    for (AliasInputBean aliasInputBean : aliases) {
      Object colValue = row.get(aliasInputBean.getCode());
      //if (colValue != null) {
      Object code;
      if (row.containsKey(aliasInputBean.getCode())) {
        code = colValue.toString();
      } else {
        code = ExpressionHelper.getValue(row, aliasInputBean.getCode());
      }
      if (code != null && !code.equals("")) {
        String codeValue = code.toString();
        AliasInputBean alias = new AliasInputBean(codeValue);
        String d = aliasInputBean.getDescription();
        if (StringUtils.trim(d) != null) {
          alias.setDescription(d);
        }
        results.add(alias);
      }
    }
    //}
    return results;

  }

  public static EntityTagRelationshipInput getRelationship(Map<String, Object> row, ColumnDefinition colDef) {

    if (colDef.getEntityTagLinks() != null) {
      EntityTagRelationshipDefinition etd = colDef.getEntityTagLinks().iterator().next();
      Object resolvedName = ExpressionHelper.getValue(row, etd.getRelationshipName());
      if (resolvedName == null) {
        resolvedName = etd.getRelationshipName();
      }

      EntityTagRelationshipInput etr = new EntityTagRelationshipInput(resolvedName.toString(), etd.getGeo());
      etr.setReverse(etd.isReverse());
      return resolveETRProperties(row, etd, etr);

    }
    return null;
    //return ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.RELATIONSHIP, colDef, colDef.getRelationship());
  }

  private static EntityTagRelationshipInput resolveETRProperties(Map<String, Object> row, EntityTagRelationshipDefinition etd, EntityTagRelationshipInput etr) {
    if (etd.getProperties() == null) {
      return etr;
    }
    for (ColumnDefinition colDef : etd.getProperties()) {
      Object value = ExpressionHelper.getValue(row, colDef.getValue(), colDef, row.get(colDef.getSource()));
      if (value != null) {
        etr.addProperty(colDef.getTarget(), transformValue(value, colDef.getSource(), colDef));
      }

    }
    return etr;
  }

  private static boolean evaluateTag(TagProfile tagProfile, Map<String, Object> row) {
    String condition = tagProfile.getCondition();
    if (condition == null) {
      return true;
    }
    Object result = ExpressionHelper.evaluateExpression(row, condition);
    return Boolean.parseBoolean(result.toString());
  }

  private static TagInputBean setNestedTags(TagInputBean setInTo, ArrayList<TagProfile> tagsToAnalyse, Map<String, Object> row) throws FlockException {

    if (tagsToAnalyse == null) {
      return null;
    }

    TagInputBean newTag = null;

    for (TagProfile tagProfile : tagsToAnalyse) {
      if (evaluateTag(tagProfile, row)) {
        Object value = row.get(tagProfile.getCode());

        if (value == null || value.equals("")) {
          value = ExpressionHelper.getValue(row, tagProfile.getCode());
          if (value == null || value.equals("")) {
            logger.debug("No code or code could be found for column {}. A code is required to uniquely identify a tag. Processing continues the but relationship will be ignored", tagProfile.getCode());
            return setInTo;
          }
          if (value.toString().equals(tagProfile.getCode())) {
            logger.debug("Unable to identify the code for column {}. A code is required to uniquely identify a tag. Source row {}. Processing continues the but relationship will be ignored", tagProfile.getCode(), row);
            return setInTo;
          }

        }

        if (tagProfile.getDelimiter() != null) {
          // No known entity relationship
          setInTo.setTargets(tagProfile.getRelationship(), getTagsFromList(tagProfile, row, null));
        } else {
          newTag = new TagInputBean(value.toString())
              .setLabel(tagProfile.getLabel());
          Object name = ExpressionHelper.getValue(row, tagProfile.getName());

          if (name != null) {
            newTag.setName(name.toString());
          }

          if (tagProfile.isMerge()) {
            newTag.setMerge(true);
          }

          newTag.setReverse(tagProfile.getReverse());
          newTag.setMustExist(tagProfile.isMustExist());
          newTag.setNotFoundCode(tagProfile.getNotFound());
          if (tagProfile.getLabelDescription() != null) {
            Object o = ExpressionHelper.getValue(row, tagProfile.getLabelDescription());
            if (o != null) {
              newTag.setDescription(o.toString());
            }
          }

          // Todo: Smell - how to return defaults consistently?
          Object keyPrefix = ExpressionHelper.getValue(row, tagProfile.getKeyPrefix());
          if (keyPrefix == null && tagProfile.getKeyPrefix() != null) {
            keyPrefix = tagProfile.getKeyPrefix();
          }
          if (keyPrefix != null) {
            newTag.setKeyPrefix(keyPrefix.toString());
          }

          setInTo.setTargets(tagProfile.getRelationship(), newTag);

        }
        if (tagProfile.hasProperties()) {
          for (ColumnDefinition propertyColumn : tagProfile.getProperties()) {
            if (propertyColumn.isPersistent()) {
              // Code Smell - this code is duplicated from getTagInputBean

              String sourceCol = propertyColumn.getSource();
              value = getObject(row, value, propertyColumn, sourceCol);

              Object oValue = ExpressionHelper.getValue(value, propertyColumn);
              if (newTag != null && oValue != null) {
                newTag.setProperty(propertyColumn.getTarget() == null ? sourceCol : propertyColumn.getTarget(), oValue);
              }
            }
          }
        }
        if (tagProfile.getGeoData() != null) {
          doGeoTransform(newTag, row, tagProfile);
        }

        if (tagProfile.hasAliases()) {
          setAliases(newTag, tagProfile, row);
        }
        if (tagProfile.getTargets() != null) {
          setNestedTags(newTag, tagProfile.getTargets(), row);
        }
      }

    }
    return newTag;
  }

  public static Object getObject(Map<String, Object> row, Object value, ColumnDefinition propertyColumn, String sourceCol) {
    if (sourceCol != null) {
      value = ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.CODE, propertyColumn, row.get(sourceCol));
    } else {
      Object val = ExpressionHelper.getValue(row, propertyColumn.getValue());
      if (val != null) {
        value = val.toString();
      }
    }
    return value;
  }

  public static Collection<TagInputBean> getTagsFromList(TagProfile tagProfile, Map<String, Object> row, EntityTagRelationshipInput entityRelationship) {
    List<String> tags = Arrays.asList(row.get(tagProfile.getCode()).toString().split(tagProfile.getDelimiter()));
    Collection<TagInputBean> results = new ArrayList<>();

    tags.stream().filter(tag -> tag != null).forEach(tag -> {
      TagInputBean newTag = new TagInputBean(tag, tagProfile.getLabel());
      newTag.addEntityTagLink(entityRelationship);
      newTag.setReverse(tagProfile.getReverse());
      newTag.setMustExist(tagProfile.isMustExist());
      newTag.setLabel(tagProfile.getLabel());
      newTag.setNotFoundCode(tagProfile.getNotFound());
      newTag.setAliases(getTagAliasValues(tagProfile.getAliases(), row));
      results.add(newTag);
    });
    return results;
  }

  static String getDataType(Object value, String column) {

    if (value == null) {
      return null;
    }

    Boolean tryAsNumber = true;
    Boolean tryAsDate = true;
    String dataType = null;

    // Code values are always strings
    if (column != null && (column.equals("code") || column.equals("name"))) {
      dataType = "string";
      tryAsNumber = false;
      tryAsDate = false;
    }

    if (tryAsDate) {
      if (isDate(value)) {
        dataType = "date";
        tryAsNumber = false;
      }
    }

    if (value.toString().startsWith("0x"))
    // Treating Hex as String
    {
      return "string";
    }


    if (tryAsNumber) {

      if (NumberUtils.isNumber(value.toString())) {
        value = NumberUtils.createNumber(value.toString());
        if (value != null) {
          dataType = "number";
        }
      } else {
        dataType = "string";
      }
    }

    return dataType;


  }

  /**
   * Guess if the supplied value might be a Date
   *
   * @param value to analyse
   * @return yes/bo
   */
  private static boolean isDate(Object value) {
    // Can we parse the object as a date
    if (value == null) {
      return false;
    }

    try {
      if (value instanceof String && value.toString().length() > 6) {
        DateTime dateVal = DateTime.parse(value.toString());
        if (dateVal.toDate().getTime() > 0) {
          return true;
        }
      }
      // Epoc dates? We're just guessing
      else if (value instanceof Long) {
        new Date((Long) value);
        return true;
      }
    } catch (IllegalArgumentException e) {
      return false;
    }
    return false;

  }

  public static Object transformValue(Object value, String column, ColumnDefinition colDef) {

    if (value == null) {
      return null;
    }

    Boolean tryAsNumber = true;
    String dataType = null;
    if (colDef != null) {
      dataType = colDef.getDataType();

      if (dataType == null && evaluate(colDef.isTag())) {
        dataType = "string";
        tryAsNumber = false;
      }

    }

    // Code values are always strings
    if (column != null && (column.equals("code") || column.equals("name"))) {
      dataType = "string";
      tryAsNumber = false;
    }

    if (value.toString().startsWith("0x"))
    // Treating Hex as String
    {
      return value.toString();
    }

    if (dataType != null) {
      if (dataType.equalsIgnoreCase("string") || dataType.equalsIgnoreCase("date")) {
        tryAsNumber = false;
      } else if (dataType.equalsIgnoreCase("number")) {
        tryAsNumber = true;
        // User wants us to coerce this to a number
        // To do so requires tidying up a few common formatting issues
        value = removeLeadingZeros(value.toString());
        value = removeSeparator(value.toString());// Sets an empty string to null

      }
    }

    if (tryAsNumber) {

      if (value != null && NumberUtils.isNumber(value.toString())) {
        if (dataType != null && dataType.equals("double")) {
          value = value + "d";
        }
        value = NumberUtils.createNumber(value.toString());
      } else if (dataType != null && dataType.equalsIgnoreCase("number")) {
        // Force to a number as it was not detected
        value = NumberUtils.createNumber(colDef == null ? "0" : colDef.getValueOnError());
      }
    }

    return value;


  }

  // Remove the thousands separator using the default locale
  private static Number removeSeparator(String str) {
    if (str == null || str.length() == 0) {
      return null;
    }
    try {
      return NumberFormat.getNumberInstance().parse(str);
    } catch (ParseException e) {
      // Not a number
      //logger.error("Unable to parse value " + str);
    }
    return null;
  }

  private static String removeLeadingZeros(String str) {
    if (!str.startsWith("0")) {
      return str;
    }

    for (int i = 0; i < str.length(); i++) {
      if (str.charAt(i) != '0') {
        return str.substring(i);
      }
    }
    return str;

  }

  public static String[] defaultHeader(String[] line, ContentModel profileConfig) {
    int col = 0;
    Collection<String> header = new ArrayList<>(line.length);

    // No header row so we will name the columns by their ordinal starting with 0
    for (String lineCol : line) {

      ColumnDefinition colDef = profileConfig.getColumnDef(Integer.toString(col));

      if (colDef != null && colDef.getTarget() != null) {
        header.add(colDef.getTarget());
        //colDef.setSourceCol(lineCol);

      } else {
        header.add(Integer.toString(col));
      }

      col++;
    }
    return header.toArray(new String[header.size()]);
  }

  public static void doGeoTransform(UserProperties propertyTarget, Map<String, Object> row, GeoDefinition geoDef) throws FlockException {
    Double x = null, y = null;
    Object o = ExpressionHelper.getValue(row, geoDef.getGeoData().getX());
    if (o != null) {
      x = Double.parseDouble(o.toString());
    }
    o = ExpressionHelper.getValue(row, geoDef.getGeoData().getY());
    if (o != null) {
      y = Double.parseDouble(o.toString());
    }

    if (x != null && y != null) {
      geoDef.getGeoData().setxValue(x);
      geoDef.getGeoData().setyValue(y);
      double[] points = GeoSupport.convert(geoDef.getGeoData());
      if (points != null) {
        propertyTarget.setProperty(Tag.LON, points[0]);
        propertyTarget.setProperty(Tag.LAT, points[1]);
      }
    }
  }

  public static boolean processRow(Map<String, Object> row, ContentModel contentModel) {
    String condition = contentModel.getCondition();
    if (condition != null) {
      Object evaluate = ExpressionHelper.getValue(row, condition);
      if (evaluate != null) {
        return Boolean.parseBoolean(evaluate.toString()); // Don't evaluate this row
      } else {
        return false; // An expression evaluation resulted in null so data is likely to be missing
      }
    }
    return true;

  }

  public static Boolean evaluate(Boolean arg) {
    return evaluate(arg, false);
  }

  public static Boolean evaluate(Boolean arg, boolean valueIfNull) {
    if (arg == null) {
      return valueIfNull;
    }
    return arg;
  }

}
