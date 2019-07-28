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

package org.flockdata.transform.entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.model.PayloadTransformer;
import org.flockdata.transform.tag.TagProfile;
import org.joda.time.DateTime;
import org.springframework.expression.spel.SpelEvaluationException;

/**
 * @author mholdsworth
 * @since 27/04/2014
 */
public class EntityPayloadTransformer extends EntityInputBean implements PayloadTransformer {

  private static final ColumnDefinition EMPTY_COLDEF = new ColumnDefinition();
  //    private static final Logger logger = LoggerFactory.getLogger(EntityMapper.class);
  private final ContentModel contentModel;

  private EntityPayloadTransformer(ContentModel contentModel) {
    this.contentModel = contentModel;
    setDocumentType(contentModel.getDocumentType());
    setFortress(contentModel.getFortress());
    setFortressUser(contentModel.getFortressUser());

  }

  public static EntityPayloadTransformer newInstance(ContentModel importProfile) {
    return new EntityPayloadTransformer(importProfile);
  }

  public Map<String, Object> transform(Map<String, Object> row) throws FlockException {
    return transform(row, contentModel);
  }

  protected Map<String, Object> transform(Map<String, Object> row, ContentModel contentModel) throws FlockException {
    if (!TransformationHelper.processRow(row, contentModel)) {
      return null;
    }

    setArchiveTags(contentModel.isArchiveTags());
    Map<String, ColumnDefinition> content = contentModel.getContent();
    boolean firstColumn = true;

    for (String sourceColumn : content.keySet()) {
      sourceColumn = sourceColumn.trim();
      ColumnDefinition colDef = content.get(sourceColumn);

      // Import Profile let's you alter the name of the column
      if (colDef == null) {
        colDef = EMPTY_COLDEF;
      }

      String valueColumn = (colDef.getTarget() == null ? sourceColumn : colDef.getTarget());
      String value = getString(row, valueColumn);

      if (TransformationHelper.evaluate(contentModel.isTrackSuppressed())) {
        setTrackSuppressed(true);
      }

      if (TransformationHelper.evaluate(contentModel.isSearchSuppressed())) {
        setSearchSuppressed(true);
      }

      if (firstColumn) {
        // While the definition is in the profile, the value is in the data.
        // Only do this once.
        if (contentModel.getSegmentExpression() != null && getSegment() == null) {
          if (row.containsKey(contentModel.getSegmentExpression())) {
            setSegment(getString(row, contentModel.getSegmentExpression()));
          } else {
            try {
              setSegment(ExpressionHelper.getValue(row, contentModel.getSegmentExpression(), colDef, null, contentModel));
            } catch (SpelEvaluationException e) {

              throw new FlockException("Unable to evaluate the segment expression for " + Arrays.toString(row.values().toArray()) + ".\r\n " + e.getMessage());
            }
          }
        }
        firstColumn = false;
      }
      // Process the column definition by evaluating expression and handling
      //  the boolean functional flags in the Contents ColumnDefinition
      if (TransformationHelper.evaluate(colDef.isDescription())) {

        setDescription(ExpressionHelper.getValue(row, colDef.getValue(), colDef, value));
      }
      if (TransformationHelper.evaluate(colDef.isTitle())) {
        String title = ExpressionHelper.getValue(row, colDef.getValue(), colDef, value);
        setName(title);
      }
      if (TransformationHelper.evaluate(colDef.isCreateUser())) { // The user in the calling system
        setFortressUser(value);
      }
      if (TransformationHelper.evaluate(colDef.isUpdateUser())) {
        setUpdateUser(value);
      }
      if (TransformationHelper.evaluate(colDef.isDate())) {
        // DAT-523
        if (value == null || value.equals("")) {
          row.put(sourceColumn, null);
        } else {
          Long dValue = ExpressionHelper.parseDate(colDef, value);
          row.put(sourceColumn, new DateTime(dValue).toString());

          if (TransformationHelper.evaluate(colDef.isCreateDate())) {
            setWhen(new Date(dValue));
          }
          if (TransformationHelper.evaluate(colDef.isUpdateDate())) {
            if (getLastChange() == null || dValue > getLastChange().getTime()) {
              setLastChange(new Date(dValue));
            }
          }
        }
      }


      if (TransformationHelper.evaluate(colDef.isCallerRef())) {
        String callerRef = ExpressionHelper.getValue(row, colDef.getValue(), colDef, value);
        setCode(callerRef);
      }

      if (colDef.getDelimiter() != null) {
        // Implies a tag because it is a comma delimited list of values
        // Only simple mapping is achieved here
        if (value != null && !value.equals("")) {
          TagProfile tagProfile = new TagProfile();
          tagProfile.setLabel(colDef.getLabel());
          tagProfile.setReverse(TransformationHelper.evaluate(colDef.getReverse()));
          tagProfile.setMustExist(TransformationHelper.evaluate(colDef.isMustExist()));
          tagProfile.setCode(sourceColumn);
          tagProfile.setDelimiter(colDef.getDelimiter());
          EntityTagRelationshipInput relationship = TransformationHelper.getRelationship(row, colDef);
          Collection<TagInputBean> tags = TransformationHelper.getTagsFromList(tagProfile, row, relationship);
          for (TagInputBean tag : tags) {
            addTag(tag);
          }

        }
      } else if (TransformationHelper.evaluate(colDef.isTag())) {
        TagInputBean tag = new TagInputBean();

        if (TransformationHelper.setTagInputBean(tag, row, sourceColumn, contentModel, value)) {
          addTag(tag);
        }
      }
      if (!colDef.getEntityLinks().isEmpty()) {
        for (EntityKeyBean key : colDef.getEntityLinks()) {
          //

          Object relationship = ExpressionHelper.getValue(row, key.getRelationshipName());

          if (relationship == null) {
            relationship = key.getRelationshipName();
          }

          if (relationship != null) {
            addEntityLink(key.setCode(value).setRelationshipName(relationship.toString()));
          }
        }
      }

      if (colDef.getGeoData() != null) {
        TransformationHelper.doGeoTransform(this, row, colDef);
      }

      // Dynamic column DAT-527
      if (colDef.getTarget() != null && colDef.getTarget().length() > 0) {
        Object targetValue = ExpressionHelper.getValue(row, colDef.getValue(), colDef, value);
        Object oValue = TransformationHelper.transformValue(targetValue, sourceColumn, colDef);
        if (oValue != null) {
          row.put(colDef.getTarget(), oValue);
        }
      }
      if (!TransformationHelper.evaluate(colDef.isPersistent(), true)) {
        // DAT-528
        row.remove(sourceColumn);
      } else if (colDef.hasEntityProperties()) {
        for (ColumnDefinition columnDefinition : colDef.getProperties()) {
          // Expression can be set on a by property value otherwise default to that of the parent
          String expression = (columnDefinition.getValue() != null ? columnDefinition.getValue() : colDef.getValue());

          value = ExpressionHelper.getValue(row, expression, columnDefinition, value);
          Object oValue = TransformationHelper.transformValue(value, sourceColumn, colDef);
          if (columnDefinition.getTarget() != null) {
            valueColumn = columnDefinition.getTarget();
          }
          if (oValue != null || columnDefinition.getStoreNull()) {
            setProperty(valueColumn, oValue);
          }

        }
      }

    }

    return row;
  }

  private String getString(Map<String, Object> row, String valueColumn) {
    Object o = row.get(valueColumn);
    String value = null;
    if (o != null) {
      value = o.toString().trim();
    }
    return value;
  }

}
