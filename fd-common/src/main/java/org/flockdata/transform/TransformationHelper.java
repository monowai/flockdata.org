/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.transform;

import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;
import org.flockdata.transform.tags.TagProfile;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;

/**
 * Helper functions for interpreting ColumnDefinitions and setting values
 * <p>
 * User: mike
 * Date: 27/08/14
 * Time: 7:53 AM
 */
public class TransformationHelper {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(TransformationHelper.class);

    private static final ExpressionParser parser = new SpelExpressionParser();

    public static boolean getTagInputBean(TagInputBean tag,
                                          FdReader staticDataResolver,
                                          Map<String, Object> row,
                                          String column,
                                          Map<String, ColumnDefinition> content,
                                          String value) throws FlockException {
        ColumnDefinition colDef = content.get(column);
        if (colDef.isCountry()) {
            value = staticDataResolver.resolveCountryISOFromName(value);
        }
        Map<String, Object> properties = new HashMap<>();

        if (colDef.isValueAsProperty()) {
            tag.setMustExist(colDef.isMustExist()).setLabel(column);
            tag.setReverse(colDef.getReverse());
            tag.setName(getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, column));
            tag.setCode(getValue(row, ColumnDefinition.ExpressionType.CODE, colDef, column));
            if (column != null && value != null) {
                String relationship = getRelationshipName(row, colDef);

                if (Integer.decode(value) != 0) {  // ToDo? Why is this decoding a 0 from a value??
                    properties.put("value", Integer.decode(value));
                    if (colDef.getName() != null) {
                        tag.addEntityLink(row.get(colDef.getName()).toString(), properties);
                    } else if (relationship != null) {
                        tag.addEntityLink(relationship, properties);
                    } else
                        tag.addEntityLink("undefined", properties);
                } else {
                    return false;
                }
            }
        } else {
            String label = (colDef.getLabel() != null ? colDef.getLabel() : column);
            String codeValue;
            if (colDef.getCode() != null)
                codeValue = getValue(row, ColumnDefinition.ExpressionType.CODE, colDef, row.get(colDef.getCode()).toString());
            else
                codeValue = getValue(row, ColumnDefinition.ExpressionType.CODE, colDef, value);
            tag.setCode(codeValue);

            String defaultVal = codeValue;

            if ( colDef.getNameExp()== null && colDef.getName() == null )
                defaultVal = value; // default to the value passed in
            else if ( colDef.getName() != null )
                defaultVal = row.get(colDef.getName()).toString();

            tag.setName(getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, defaultVal));

            tag.setMustExist(colDef.isMustExist())
               .setLabel(colDef.isCountry() ? "Country" : label);

            String relationship = getRelationshipName(row, colDef);

            if (relationship != null) {
                Map<String, Object> rlxProperties = new HashMap<>();
                if (colDef.getRlxProperties() != null) {
                    for (ColumnDefinition columnDefinition : colDef.getRlxProperties()) {
                        rlxProperties.put(columnDefinition.getTargetProperty(),
                                getValue(row.get(columnDefinition.getSourceProperty()), columnDefinition));
                    }
                }

                tag.addEntityLink(relationship, rlxProperties);
            }


            tag.setReverse(colDef.getReverse());
            if (colDef.hasProperites()) {
                for (ColumnDefinition thisCol : colDef.getProperties()) {
                    String sourceCol = thisCol.getSourceProperty();
                    value = TransformationHelper.getValue(row, ColumnDefinition.ExpressionType.CODE, thisCol, row.get(sourceCol));
                    tag.setProperty(thisCol.getTargetProperty() == null ? sourceCol : thisCol.getTargetProperty(), getValue(value, thisCol));
                }
            }
        }

        if (tag.getCode() == null)
            return false;

        setNestedTags(tag, colDef.getTargets(), row, staticDataResolver

        );
        return true;
    }

    public static String getRelationshipName(Map<String, Object> row, ColumnDefinition colDef) {
        if (colDef.getRelationship() != null )
            return colDef.getRelationship();

        if ( colDef.getRlxExp() == null)
            return null;

        return getValue(row, ColumnDefinition.ExpressionType.RELATIONSHIP, colDef, Tag.UNDEFINED);
    }

    private static boolean evaluateTag(TagProfile tagProfile, Map<String, Object> row) {
        String condition = tagProfile.getCondition();
        if (condition == null)
            return true;
        Object result = evaluateExpression(row, condition);
        return Boolean.parseBoolean(result.toString());
    }

    public static TagInputBean setNestedTags(TagInputBean setInTo, ArrayList<TagProfile> tagsToAnalyse, Map<String, Object> row, FdReader reader) throws FlockException {
        if (tagsToAnalyse == null)
            return null;

        TagInputBean newTag = null;

        for (TagProfile tagProfile : tagsToAnalyse) {
            if (evaluateTag(tagProfile, row)) {
                Object value = row.get(tagProfile.getColumn());

                if (value == null || value.equals("")) {
                    String codeExp = tagProfile.getCodeExp();
                    if (codeExp != null) {
                        value = evaluateExpression(row, codeExp);
                    }
                    if (value == null || value.equals("")) {
                        logger.error("No code or codeExp could be found for column {}. A code is required to uniquely identify a tag. Processing continues the but relationship will be ignored", tagProfile.getColumn());
                        value = "";
                        //throw new FlockException(String.format("Undefined row value for %s", tagProfile.getColumn()));
                    }
                }

                if (tagProfile.getDelimiter() != null) {
                    // No known entity relationship
                    setInTo.setTargets(tagProfile.getRelationship(), getTagsFromList(tagProfile, row, null));
                } else if (tagProfile.isCountry()) {
                    String iso = reader.resolveCountryISOFromName(value.toString());
                    if (iso == null) // Regression tests
                        iso = value.toString();
                    newTag = new TagInputBean(iso)
                            .setLabel(tagProfile.getLabel());
                    setInTo.setTargets(tagProfile.getRelationship(), newTag);

                } else {
                    newTag = new TagInputBean(value.toString())
                            .setLabel(tagProfile.getLabel());
                    newTag.setReverse(tagProfile.getReverse());
                    newTag.setMustExist(tagProfile.isMustExist());
                    setInTo.setTargets(tagProfile.getRelationship(), newTag);
                }
                if (tagProfile.hasProperites()) {
                    for (ColumnDefinition thisCol : tagProfile.getProperties()) {
                        String sourceCol = thisCol.getSourceProperty();
                        value = TransformationHelper.getValue(row, ColumnDefinition.ExpressionType.CODE, thisCol, row.get(sourceCol));
                        if (newTag != null)
                            newTag.setProperty(thisCol.getTargetProperty() == null ? sourceCol : thisCol.getTargetProperty(), getValue(value, thisCol));
                    }
                }

                if (tagProfile.getTargets() != null) {
                    setNestedTags(newTag, tagProfile.getTargets(), row, reader);
                }
            }

        }
        return newTag;
    }

    public static Collection<TagInputBean> getTagsFromList(TagProfile tagProfile, Map<String, Object> row, String entityRelationship) {
        List<String> tags = Arrays.asList(row.get(tagProfile.getColumn()).toString().split(tagProfile.getDelimiter()));
        Collection<TagInputBean> results = new ArrayList<>();

        tags.stream().filter(tag -> tag != null).forEach(tag -> {
            TagInputBean newTag = new TagInputBean(tag, entityRelationship)
                    .setLabel(tagProfile.getLabel());
            newTag.setReverse(tagProfile.getReverse());
            newTag.setMustExist(tagProfile.isMustExist());
            newTag.setLabel(tagProfile.getLabel());
            results.add(newTag);
        });
        return results;
    }

    public static Map<String, Object> convertToMap(String[] headerRow, String[] line) {
        int col = 0;
        Map<String, Object> row = new HashMap<>();
        if (headerRow == null) {
            // No header row so we will name the columns, starting at 0, by their ordinal
            for (String lineCol : line) {
                row.put(Integer.toString(col), lineCol);
                col++;
            }
        } else {
            for (String column : headerRow) {
                row.put(column, line[col]);
                col++;
            }
        }
        return row;
    }

    public static Object getValue(Object value, ColumnDefinition colDef) {
        if (value == null || value.equals("null"))
            return null;
        else if (NumberUtils.isNumber(value.toString())) {
            if (colDef != null && colDef.getType() != null && colDef.getType().equalsIgnoreCase("string"))
                return String.valueOf(value);
            else
                return NumberUtils.createNumber(value.toString());
        } else {
//                Date date = null;
//                try {
//                    if ( colDef!=null && colDef.getDateFormat()!=null ) {
//                        date = DateUtils.parseDate(line[col], colDef.getDateFormat());
//                        row.put(column, date.getTime());
//                    }
//                } catch (ParseException e) {
//                    //
//                }
            //if ( date == null ) // Stash it as a string
            return value.toString().trim();
        }
    }

    public static String getValue(Map<String, Object> row, ColumnDefinition.ExpressionType expCol, ColumnDefinition colDef, Object defaultValue) {
        if (colDef == null)
            return getNullSafeDefault(defaultValue, colDef);
        String expression = colDef.getExpression(expCol);
        if (expression == null) {
            return getNullSafeDefault(defaultValue, colDef);
        }
        Object result = evaluateExpression(row, expression);
        if (result == null)
            return getNullSafeDefault(defaultValue, colDef);
        return result.toString().trim();
    }

    private static Object evaluateExpression(Map<String, Object> row, String expression) {
        StandardEvaluationContext context = new StandardEvaluationContext();
//        try {
//            context.registerFunction("replace", StringParser.class.getDeclaredMethod("replace", String.class, Character.class));
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
        context.setVariable("row", row);
        return parser.parseExpression(expression).getValue(context);
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
}
