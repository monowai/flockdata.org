/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.transform.csv;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.Mappable;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.tags.TagProfile;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class EntityMapper extends EntityInputBean implements Mappable {

    private static final Logger logger = LoggerFactory.getLogger(EntityMapper.class);

    public EntityMapper(ContentModel contentModel) {
        setDocumentType(contentModel.getDocumentType());
        setFortress(contentModel.getFortress());
        setFortressUser(contentModel.getFortressUser());
    }

    @Override
    public Map<String, Object> setData(Map<String, Object> row, ContentModel contentModel) throws FlockException {
        if ( !TransformationHelper.processRow(row, contentModel))
            return null;

        setArchiveTags(contentModel.isArchiveTags());
        Map<String, ColumnDefinition> content = contentModel.getContent();
        boolean firstColumn = true;

        for (String sourceColumn : content.keySet()) {
            sourceColumn = sourceColumn.trim();
            ColumnDefinition colDef = content.get(sourceColumn);

            // Import Profile let's you alter the name of the column
            String valueColumn = (colDef != null && colDef.getTarget() == null ? sourceColumn : colDef.getTarget());
            String value = getString(row, valueColumn);

            if (firstColumn) {
                // While the definition is in the profile, the value is in the data.
                // Only do this once.
                if (contentModel.getSegmentExpression() != null && getSegment() == null) {
                    if (row.containsKey(contentModel.getSegmentExpression()))
                        setSegment(getString(row, contentModel.getSegmentExpression()));
                    else {
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
                        if (getLastChange() == null || dValue > getLastChange().getTime())
                            setLastChange(new Date(dValue));
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
                    String relationship = TransformationHelper.getRelationshipName(row, colDef);
                    Collection<TagInputBean> tags = TransformationHelper.getTagsFromList(tagProfile, row, relationship);
                    for (TagInputBean tag : tags) {
                        addTag(tag);
                    }

                }
            } else if (TransformationHelper.evaluate(colDef.isTag())) {
                TagInputBean tag = new TagInputBean();

                if (TransformationHelper.setTagInputBean(tag, row, sourceColumn, contentModel.getContent(), value)) {
                    addTag(tag);
                }
            }
            if (!colDef.getEntityLinks().isEmpty()) {
                for (Map<String, String> key : colDef.getEntityLinks()) {
                    addEntityLink(key.get("relationshipName"), new EntityKeyBean(key.get("documentName"), key.get("fortress"), value));
                }
            }

            if (colDef.getGeoData() != null) {
                TransformationHelper.doGeoTransform(this, row, colDef);
            }

            // Dynamic column DAT-527
            if (colDef.getTarget() != null) {
                Object targetValue = ExpressionHelper.getValue(row, colDef.getValue(), colDef, value);
                Object oValue = TransformationHelper.transformValue(targetValue, sourceColumn, colDef);
                if (oValue != null)
                    row.put(colDef.getTarget(), oValue);
            }
            if (!TransformationHelper.evaluate(colDef.isPersistent(),true)) {
                // DAT-528
                row.remove(sourceColumn);
            } else if (colDef.hasEntityProperties()) {
                for (ColumnDefinition columnDefinition : colDef.getProperties()) {

                    value = ExpressionHelper.getValue(row, columnDefinition.getValue(), columnDefinition, row.get(valueColumn));
                    Object oValue = TransformationHelper.transformValue(value, sourceColumn, colDef);
                    if (columnDefinition.getTarget() != null)
                        valueColumn = columnDefinition.getTarget();
                    if (oValue != null || columnDefinition.getStoreNull()){
                        setProperty(valueColumn, oValue);
                    }

                }
            }

        }

        return row;
    }

    public String getString(Map<String, Object> row, String valueColumn) {
        Object o = row.get(valueColumn);
        String value = null;
        if (o != null)
            value = o.toString().trim();
        return value;
    }

    public static EntityMapper newInstance(ContentModel importProfile) {
        return new EntityMapper(importProfile);
    }

}
