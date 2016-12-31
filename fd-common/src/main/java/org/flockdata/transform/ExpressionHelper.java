/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform;

import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.data.ContentModel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Encapsulate methods to evaluate transformational expressions
 * <p>
 * @author mholdsworth
 * @since 30/07/2015
 */
public class ExpressionHelper {

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static final Logger logger = LoggerFactory.getLogger(ExpressionHelper.class);

    public static Object getValue(Object value, ColumnDefinition colDef) {

//        context.setVariable("colDef",colDef);
        if (value == null || value.equals("null"))
            return null;
        else if (NumberUtils.isNumber(value.toString())) {
            if (colDef != null && colDef.getDataType() != null && colDef.getDataType().equalsIgnoreCase("string"))
                return String.valueOf(value);
            else
                return NumberUtils.createNumber(value.toString());
        } else {
            return value.toString().trim();
        }
    }

    public static String getValue(Map<String, Object> row, ColumnDefinition.ExpressionType expCol, ColumnDefinition colDef, Object defaultValue) {
        if (colDef == null)
            return getNullSafeDefault(defaultValue, null);

        String expression = colDef.getExpression(expCol);
        if (expression == null) {
            return getNullSafeDefault(defaultValue, colDef);
        }
        Object result = getValue(row, expression);
        if (result == null)
            return getNullSafeDefault(defaultValue, colDef);
        return result.toString().trim();


    }

    public static String getValue(Map<String, Object> row, String expression, ColumnDefinition colDef, Object defaultValue) {
        return getValue(row, expression, colDef, defaultValue, null);
    }

    /**
     * Returns a value based on the expression. To evaluate a column, use #data['col'] syntax
     *
     * @param row          Existing transformed data
     * @param expression   to evaluate
     * @param colDef       options about the transformation
     * @param defaultValue what to return if expression results in null
     * @return calculated value or defaultValue
     */
    public static String getValue(Map<String, Object> row, String expression, ColumnDefinition colDef, Object defaultValue, ContentModel contentModel) {
        if (colDef == null)
            return getNullSafeDefault(defaultValue, null);

        Object result = (expression==null || expression.length()==0? null:evaluateExpression(contentModel, row, expression));
        if (result == null)
            return getNullSafeDefault(defaultValue, colDef);
        return result.toString().trim();


    }

    public static Object getValue(Map<String, Object> row, String expression) {
        Object result;
        try {
            if (row.containsKey(expression))
                result = row.get(expression);  // Pull value straight from the row
            else
                result = evaluateExpression(row, expression);
        } catch (ExpressionException | StringIndexOutOfBoundsException e) {
            logger.trace("Expression error parsing [" + expression + "]. Returning null");
            result = null;
        }
        return result;
    }

    static Object evaluateExpression(Map<String, Object> row, String expression) {
        return evaluateExpression(null, row, expression);
    }

    private static Object evaluateExpression(ContentModel contentModel, Map<String, Object> row, String expression) {
        if (expression == null)
            return null;

        StandardEvaluationContext context = new StandardEvaluationContext(row);
        setContextVariables(contentModel, row, context);

        try {
            return parser.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            logger.debug(String.format("Error evaluating expression [%s], message was %s ", expression, e.getMessage()));
            throw (e);
        }
    }

    private static void setContextVariables(ContentModel contentModel, Map<String, Object> row, StandardEvaluationContext context) {
        if ( contentModel !=null )
            context.setVariable("model",contentModel);
//        for (String s : row.keySet()) {
//            context.setVariable(s, row.get(s));
//        }
    }


    private static String getNullSafeDefault(Object defaultValue, ColumnDefinition colDef) {
        if (defaultValue == null || defaultValue.equals("")) {
            // May be a literal value to set the property to
            if (colDef == null)
                return null;
            return colDef.getNullOrEmpty();
        }
        return defaultValue.toString().trim();
    }

    public static Long parseDate(ColumnDefinition colDef, String value) {
        if (value == null || value.equals(""))
            return null;
        if (colDef.isDateEpoc()) {
            return Long.parseLong(value) * 1000;
        }

        if (colDef.getDateFormat() == null || colDef.getDateFormat().equalsIgnoreCase("automatic")) {
            try {
                return Date.parse(value);
            } catch (IllegalArgumentException e) {
                // Try other formats
            }
        }
        if (colDef.getDateFormat() != null && colDef.getDateFormat().equalsIgnoreCase("timestamp")) {
            try {
                return Timestamp.valueOf(value).getTime();
            } catch (IllegalArgumentException e) {
                // attempt other conversions
            }
        }

        if (NumberUtils.isDigits(value))  // plain old java millis
            return Long.parseLong(value);

        // Custom Date formats
        String tz = colDef.getTimeZone();
        if (tz == null)
            tz = TimeZone.getDefault().getID();

        try {

            // Try first as DateTime
            return new SimpleDateFormat(colDef.getDateFormat()).parse(value).getTime();
        } catch (DateTimeParseException | IllegalArgumentException | ParseException e) {
            // Just a plain date
            DateTimeFormatter pattern = DateTimeFormatter.ofPattern(colDef.getDateFormat(), Locale.ENGLISH);
            LocalDate date = LocalDate.parse(value, pattern);
            return new DateTime(date.toString(), DateTimeZone.forID(tz)).getMillis();
        }
    }
}
