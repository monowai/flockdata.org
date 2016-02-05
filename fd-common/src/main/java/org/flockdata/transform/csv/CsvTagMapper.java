/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform.csv;

import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.Mappable;
import org.flockdata.registration.bean.TagInputBean;
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
