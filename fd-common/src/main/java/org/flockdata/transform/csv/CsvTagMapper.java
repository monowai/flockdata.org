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
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.DelimitedMappable;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.flockdata.transform.GeoSupport;
import org.flockdata.transform.TransformationHelper;

import java.util.Map;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvTagMapper extends TagInputBean implements DelimitedMappable {


    @Override
    public Map<String, Object> setData(final String[] headerRow, final String[] line, ProfileConfiguration importProfile) throws JsonProcessingException, FlockException {
        Map<String, Object> row = TransformationHelper.convertToMap(importProfile, headerRow, line);
        Map<String, ColumnDefinition> content = importProfile.getContent();

        for (String column : content.keySet()) {
            ColumnDefinition colDef = importProfile.getColumnDef(column);
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
                    setName(TransformationHelper.getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, value));
                    if (colDef.getCode() != null)
                        row.get(colDef.getCode());
                }
                if (colDef.getTarget() != null && colDef.isPersistent()) {
                    value = TransformationHelper.getValue(row, colDef.getValue(), colDef, row.get(column));
                    Object oValue = TransformationHelper.getValue(value, colDef);
                    if (oValue != null)
                        setProperty(colDef.getTarget(), oValue);
                }
                if ( colDef.getGeoData() != null ){
                    Double x = null, y = null;
                    Object o = TransformationHelper.getValue(row, colDef.getGeoData().getX());
                    if ( o !=null )
                        x = Double.parseDouble(o.toString());
                    o = TransformationHelper.getValue(row, colDef.getGeoData().getY());
                    if ( o !=null )
                        y = Double.parseDouble(o.toString());

                    if ( x !=null && y!=null ) {
                        colDef.getGeoData().setxValue(x);
                        colDef.getGeoData().setyValue(y);
                        double[] points = GeoSupport.convert(colDef.getGeoData());
                        if (points != null) {
                            setProperty("lon", points[0]);
                            setProperty("lat", points[1]);
                        }
                    }
                }


            } // ignoreMe
        }
        return row;
    }

}
