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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;
import org.flockdata.transform.tags.TagProfile;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionException;
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
                                          Map<String, Object> row,
                                          String column,
                                          Map<String, ColumnDefinition> content,
                                          String value) throws FlockException {
        ColumnDefinition colDef = content.get(column);
        Map<String, Object> properties = new HashMap<>();

        if (colDef.isValueAsProperty()) {
            // ToDo: Eliminate this block. Twas only in place to support the way we handle labels
            tag.setMustExist(colDef.isMustExist()).setLabel(column);
            tag.setReverse(colDef.getReverse());
            tag.setName(getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, column));
            tag.setCode(getValue(row, ColumnDefinition.ExpressionType.CODE, colDef, column));
            if (column != null && value != null) {
                String relationship = getRelationshipName(row, colDef);

                if (Integer.decode(value) != 0) {  // ToDo? Why is this decoding a 0 from a value??
                    properties.put("value", Integer.decode(value));
                    if (relationship != null) {
                        tag.addEntityLink(relationship, properties);
                    } else
                        tag.addEntityLink("undefined", properties);
                } else {
                    return false;
                }
            }
        } else {
            String label;
            if (colDef.getLabel() != null && colDef.getLabel().equals(column))
                label = colDef.getLabel();
            else
                label = resolveValue(colDef.getLabel(), column, colDef, row);
            //
            tag.setMustExist(colDef.isMustExist())
                    .setLabel(colDef.isCountry() ? "Country" : label);

            String codeValue = getValue(row, ColumnDefinition.ExpressionType.CODE, colDef, value);
            tag.setCode(codeValue);

            if (!colDef.isMustExist()) {     // Must exists only resolves the Code, so don't waste time setting the name
                String name = getValue(row, ColumnDefinition.ExpressionType.NAME, colDef, codeValue);
                if (name != null && !name.equals(codeValue))
                    tag.setName(name);
            }

            String relationship = getRelationshipName(row, colDef);

            setAliases(tag, colDef, row);

            if (relationship != null) {
                Map<String, Object> rlxProperties = new HashMap<>();
                if (colDef.getRlxProperties() != null) {
                    for (ColumnDefinition columnDefinition : colDef.getRlxProperties()) {
                        Object propValue = getValue(row.get(columnDefinition.getSource()), columnDefinition);
                        if ( propValue !=null )
                            rlxProperties.put(columnDefinition.getTarget(),
                                    propValue);
                    }
                }

                tag.addEntityLink(relationship, rlxProperties);
            }


            tag.setReverse(colDef.getReverse());
            if (colDef.hasProperites()) {
                for (ColumnDefinition thisCol : colDef.getProperties()) {
                    String sourceCol = thisCol.getSource();
                    value = TransformationHelper.getValue(row, ColumnDefinition.ExpressionType.CODE, thisCol, row.get(sourceCol));
                    if (value !=null )
                        tag.setProperty(thisCol.getTarget() == null ? sourceCol : thisCol.getTarget(), getValue(value, thisCol));
                }
            }
        }

        if (tag.getCode() == null)
            return false;

        setNestedTags(tag, colDef.getTargets(), row

        );
        return true;
    }

    private static String resolveValue(String value, String column, ColumnDefinition colDef, Map<String, Object> row) {
        if (value == null)
            return column; // Default to the column Name
        Object result = getValue(row, ColumnDefinition.ExpressionType.LABEL, colDef, value);
        if (result == null)
            return null;
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

        if (aliases == null)
            return new ArrayList<>();
        Collection<AliasInputBean> results = new ArrayList<>(aliases.size());

        for (AliasInputBean aliasInputBean : aliases) {
            Object colValue = row.get(aliasInputBean.getCode());
            //if (colValue != null) {
                Object code;
                if ( row.containsKey(aliasInputBean.getCode()))
                    code = colValue.toString();
                else
                    code = getValue(row, aliasInputBean.getCode());
                if ( code != null ) {
                    String codeValue = code.toString();
                    AliasInputBean alias = new AliasInputBean(codeValue);
                    String d = aliasInputBean.getDescription();
                    if (StringUtils.trim(d) != null)
                        alias.setDescription(evaluateExpression(row, d).toString());
                    results.add(alias);
                }
            }
        //}
        return results;

    }

    public static String getRelationshipName(Map<String, Object> row, ColumnDefinition colDef) {
        if (colDef.getRelationship() != null)
            return colDef.getRelationship();

        if (colDef.getRlxExp() == null)
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

    public static TagInputBean setNestedTags(TagInputBean setInTo, ArrayList<TagProfile> tagsToAnalyse, Map<String, Object> row) throws FlockException {
        if (tagsToAnalyse == null)
            return null;

        TagInputBean newTag = null;

        for (TagProfile tagProfile : tagsToAnalyse) {
            if (evaluateTag(tagProfile, row)) {
                Object value = row.get(tagProfile.getCode());

                if (value == null || value.equals("")) {
                    value = evaluateExpression(row, tagProfile.getCode());
                    if (value == null || value.equals("")) {
                        logger.debug("No code or code could be found for column {}. A code is required to uniquely identify a tag. Processing continues the but relationship will be ignored", tagProfile.getCode());
                        return setInTo;
                    }
                }

                if (tagProfile.getDelimiter() != null) {
                    // No known entity relationship
                    setInTo.setTargets(tagProfile.getRelationship(), getTagsFromList(tagProfile, row, null));
                } else if (tagProfile.isCountry()) {
                    String iso = value.toString();

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
                        String sourceCol = thisCol.getSource();
                        value = TransformationHelper.getValue(row, ColumnDefinition.ExpressionType.CODE, thisCol, row.get(sourceCol));
                        Object oValue  = getValue(value, thisCol);
                        if (newTag != null && oValue !=null )
                            newTag.setProperty(thisCol.getTarget() == null ? sourceCol : thisCol.getTarget(), oValue);
                    }
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

    public static Collection<TagInputBean> getTagsFromList(TagProfile tagProfile, Map<String, Object> row, String entityRelationship) {
        List<String> tags = Arrays.asList(row.get(tagProfile.getCode()).toString().split(tagProfile.getDelimiter()));
        Collection<TagInputBean> results = new ArrayList<>();

        tags.stream().filter(tag -> tag != null).forEach(tag -> {
            TagInputBean newTag = new TagInputBean(tag, entityRelationship)
                    .setLabel(tagProfile.getLabel());
            newTag.setReverse(tagProfile.getReverse());
            newTag.setMustExist(tagProfile.isMustExist());
            newTag.setLabel(tagProfile.getLabel());
            newTag.setAliases(getTagAliasValues(tagProfile.getAliases(), row));
            results.add(newTag);
        });
        return results;
    }

    public static Map<String, Object> convertToMap(ProfileConfiguration importProfile, String[] headerRow, String[] line) {
        int col = 0;
        Map<String, Object> row = new HashMap<>();
        // ToDo: Absent a header, create a default one - (0,1,2,3,4......)
        if (headerRow == null) {
            // No header row so we will name the columns, starting at 0, by their ordinal
            for (String lineCol : line) {
                Object value = lineCol;
                if (NumberUtils.isNumber(lineCol)) {
                    value = NumberUtils.createNumber(lineCol);
                }

                row.put(Integer.toString(col), value);
                col++;
            }
        } else {
            for (String column : headerRow) {
                try {
                    Object value = line[col];
                    Boolean tryAsNumber = true;
                    String dataType = null;
                    ColumnDefinition colDef = importProfile.getColumnDef(column);
                    if ( colDef != null) {
                        dataType = importProfile.getColumnDef(column).getDataType();

                        // ToDo: Analyze nested tags to see that codes are not be converted
                        // To force a numeric looking tag ("001") to a number you must create a
                        // columnDefinition specifying it as a string.
                        // { "myCol": {"dataType":"string"}}

                        if (dataType == null && colDef.isTag())
                            dataType = "string";
                    }
                    if (dataType != null)
                        if (dataType.equalsIgnoreCase("string"))
                            tryAsNumber = false;
                        else if (dataType.equalsIgnoreCase("number"))
                            tryAsNumber = true;
                    if (tryAsNumber )
                        if (NumberUtils.isNumber(line[col])) {
                            value = NumberUtils.createNumber(line[col]);
                        } else if (dataType!=null && dataType.equalsIgnoreCase("number")) {
                            // Force to a number as it was not detected
                            value = NumberUtils.createNumber(importProfile.getColumnDef(column).getValueOnError());
                        }

                    boolean addValue = true;
                    if ( importProfile.isEmptyIgnored()){
                        if (value == null || value.toString().trim().equals(""))
                            addValue = false;
                    }
                    if ( addValue) {
                        row.put(column, (value instanceof String ? ((String) value).trim():value));
                    }

                    col++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // Column does not exist for this row
                    return row;
                }
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

    private static Object getValue(Map<String, Object> row, String expression) {
        Object result;
        try {
            if (row.containsKey(expression))
                result = row.get(expression);  // Pull value straight from the row
            else
                result = evaluateExpression(row, expression);
        } catch (ExpressionException e) {
            logger.trace("Expression error parsing [" + expression + "]. Returning default value");
            result = null;
        }
        return result;
    }

    static StandardEvaluationContext context = new StandardEvaluationContext();

    private static Object evaluateExpression(Map<String, Object> row, String expression) {
        if (expression == null)
            return null;

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
