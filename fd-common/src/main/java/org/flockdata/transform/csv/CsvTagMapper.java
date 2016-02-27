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
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.ExpressionHelper;
import org.flockdata.transform.TransformationHelper;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvTagMapper extends TagInputBean implements Mappable {

//    @Override
//    public Map<String, Object> setData(final String[] headerRow, final String[] line, ProfileConfiguration importProfile) throws JsonProcessingException, FlockException {
//        return setData(TransformationHelper.convertToMap(importProfile, headerRow, line), importProfile);
//    }

    @Override
    public Map<String, Object> setData(Map<String,Object> row, ContentProfile importProfile) throws FlockException {

        if ( !TransformationHelper.processRow(row, importProfile))
            return null;

        Map<String, ColumnDefinition> content = importProfile.getContent();

        for (String column : content.keySet()) {
            ColumnDefinition colDef = content.get(column);
            String value;
            Object colValue = row.get(column);
            // colValue may yet be an expression
            value = (colValue != null ? colValue.toString() : null);
            if (value != null)
                value = value.trim();

            if (colDef != null) {

                if (colDef.isTag()) {
                    TransformationHelper.setTagInputBean(this, row, column, content, value);
                }
                if (colDef.isTitle()) {
                    setName(ExpressionHelper.getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, value));
                    if (colDef.getCode() != null)
                        row.get(colDef.getCode());
                }
                if (colDef.getTarget() != null && colDef.isPersistent()) {
                    value = ExpressionHelper.getValue(row, colDef.getValue(), colDef, row.get(column));
                    Object oValue = ExpressionHelper.getValue(value, colDef);
                    if (oValue != null)
                        setProperty(colDef.getTarget(), oValue);
                }
                if ( colDef.getGeoData() != null ){
                    TransformationHelper.doGeoTransform(this, row, colDef);
                }


            } // ignoreMe
        }
        return row;
    }

}
