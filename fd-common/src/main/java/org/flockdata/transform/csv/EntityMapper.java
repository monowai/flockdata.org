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
import org.flockdata.profile.model.ContentProfile;
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

    public EntityMapper(ContentProfile contentProfile) {
        setDocumentType(contentProfile.getDocumentType());
        setFortressName(contentProfile.getFortressName());
        setFortressUser(contentProfile.getFortressUser());
    }

    @Override
    public Map<String, Object> setData(Map<String, Object> row, ContentProfile importProfile) throws FlockException {
        if (!TransformationHelper.processRow(row, importProfile))
            return null;

        setArchiveTags(importProfile.isArchiveTags());
        Map<String, ColumnDefinition> content = importProfile.getContent();
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
                if (importProfile.getSegmentExpression() != null && getSegment() == null) {
                    if (row.containsKey(importProfile.getSegmentExpression()))
                        setSegment(getString(row, importProfile.getSegmentExpression()));
                    else {
                        try {
                            setSegment(ExpressionHelper.getValue(row, importProfile.getSegmentExpression(), colDef, null));
                        } catch (SpelEvaluationException e) {

                            throw new FlockException("Unable to evaluate the segment expression for " + Arrays.toString(row.values().toArray()) + ".\r\n " + e.getMessage());
                        }
                    }
                }
                firstColumn = false;
            }
            // Process the column definition by evaluating expression and handling
            //  the boolean functional flags in the Contents ColumnDefinition
            if (colDef.isDescription()) {

                setDescription(ExpressionHelper.getValue(row, colDef.getValue(), colDef, value));
            }
            if (colDef.isTitle()) {
                String title = ExpressionHelper.getValue(row, colDef.getValue(), colDef, value);
                setName(title);
            }
            if (colDef.isCreateUser()) { // The user in the calling system
                setFortressUser(value);
            }
            if (colDef.isUpdateUser()) {
                setUpdateUser(value);
            }
            if (colDef.isDate()) {
                // DAT-523
                if (value == null || value.equals("")) {
                    row.put(sourceColumn, null);
                } else {
                    Long dValue = ExpressionHelper.parseDate(colDef, value);
                    row.put(sourceColumn, new DateTime(dValue).toString());

                    if (colDef.isCreateDate()) {
                        setWhen(new Date(dValue));
                    }
                    if (colDef.isUpdateDate()) {
                        if (getLastChange() == null || dValue > getLastChange().getTime())
                            setLastChange(new Date(dValue));
                    }
                }
            }


            if (colDef.isCallerRef()) {
                String callerRef = ExpressionHelper.getValue(row, colDef.getValue(), colDef, value);
                setCode(callerRef);
            }

            if (colDef.getDelimiter() != null) {
                // Implies a tag because it is a comma delimited list of values
                // Only simple mapping is achieved here
                if (value != null && !value.equals("")) {
                    TagProfile tagProfile = new TagProfile();
                    tagProfile.setLabel(colDef.getLabel());
                    tagProfile.setReverse(colDef.getReverse());
                    tagProfile.setMustExist(colDef.isMustExist());
                    tagProfile.setCode(sourceColumn);
                    tagProfile.setDelimiter(colDef.getDelimiter());
                    String relationship = TransformationHelper.getRelationshipName(row, colDef);
                    Collection<TagInputBean> tags = TransformationHelper.getTagsFromList(tagProfile, row, relationship);
                    for (TagInputBean tag : tags) {
                        addTag(tag);
                    }

                }
            } else if (colDef.isTag()) {
                TagInputBean tag = new TagInputBean();

                if (TransformationHelper.setTagInputBean(tag, row, sourceColumn, importProfile.getContent(), value)) {
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
            if (!colDef.isPersistent()) {
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

    public static EntityMapper newInstance(ContentProfile importProfile) {
        return new EntityMapper(importProfile);
    }

}
