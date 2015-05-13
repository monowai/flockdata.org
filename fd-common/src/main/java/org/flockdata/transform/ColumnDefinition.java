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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.transform.tags.TagProfile;
import org.flockdata.transform.tags.TagProfileDeserializer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 7:44 AM
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDefinition {

    private String sourceCol;
    private String dateFormat =null;
    private String timeZone; // To use for dates
    private Boolean persistent = true;


    public enum ExpressionType {CODE, NAME, RELATIONSHIP, PROP_EXP, LABEL, CALLER_REF}

    // Flags that profile the properties of a column
    private boolean callerRef;
    private boolean title;
    private boolean description;
    private boolean valueAsProperty;
    private boolean country;
    private boolean createDate;
    private boolean document;

    private boolean tag;
    private boolean mustExist;
    private boolean createUser;
    private boolean updateUser;
    private boolean reverse = false;

    private String format;

    private String dataType;

    private String callerRefExp; // Supports the caller ref as an expression
    private String strategy = null;
    private String fortress = null;
    private String documentType = null;
    private String label;
    private String type; //datatype
    private String name;

    private String valueOnError;// Value to set to if the format causes an exception


    private String nullOrEmpty;
    private String appendJoinText = " ";

    private String notFound;

    @JsonDeserialize(using = ColumnDeserializer.class)
    private ArrayList<ColumnDefinition> rlxProperties;

    private ArrayList<AliasInputBean> aliases;

    @JsonDeserialize(using = ColumnDeserializer.class)
    private ArrayList<ColumnDefinition> properties; // Properties to add to an object

    private String[] refColumns;

    private String relationship; // Explicit relationship name
    private String rlxExp; // Relationship expression

    private String delimiter;

    private String code;
    private String source; // property to read from
    private String target; // property to write to (essentially rename the column)

    private ArrayList<Map<String, String>> crossReferences = new ArrayList<>();
    private boolean updateDate;

    public String getLabel() {
        return label;
    }

    /**
     * @param label Noun that describes the tag
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonDeserialize(using = TagProfileDeserializer.class)
    private ArrayList<TagProfile> targets = new ArrayList<>();

    /**
     * @return Columns used to create a callerReference where there is otherwise no identifiable key
     */
    public String[] getRefColumns() {
        return refColumns;
    }

    /**
     * @param tag flags this column as being an identifying tag.
     */
    public void setTag(boolean tag) {
        this.tag = tag;
    }

    public boolean isCallerRef() {
        return callerRef;
    }

    public boolean isTitle() {
        return title;
    }

    public boolean isTag() {
        return tag || isCountry();
    }

    public boolean isMustExist() {
        return mustExist;
    }

    public boolean isValueAsProperty() {
        return valueAsProperty;
    }

    public boolean isCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public ArrayList<TagProfile> getTargets() {
        return targets;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getStrategy() {
        return strategy;
    }

    public String getFortress() {
        return fortress;
    }

    public String getDocumentType() {
        return documentType;
    }

    public boolean isDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    // Overrides the value name of the property
    public String getTarget() {
        if (target == null)
            return source;
        else
            return target;
    }

    public String getRelationship() {
        return relationship;

    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * if a delimiter is specified, then the column value will be treated as a delimited string and
     * a Tag.code will be created for each delimited value
     *
     * @return default is a ,
     */
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setDocument(boolean document) {
        this.document = document;
    }

    public void setRefColumns(String[] refColumns) {
        this.refColumns = refColumns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return is this column carrying the value for the Creating user
     */
    public boolean isCreateUser() {
        return createUser;
    }

    public void setCreateUser(boolean createUser) {
        this.createUser = createUser;
    }

    /**
     * @return is this column carrying the value for the Last Update user
     */
    public boolean isUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(boolean updateUser) {
        this.updateUser = updateUser;
    }

    /**
     * @param mustExist if true, FdServer will throw an error if the tag does not exist
     */
    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
    }

    /**
     * @return is this column carrying the value for the Created Date
     */
    public boolean isCreateDate() {
        return createDate;
    }

    /**
     * Defines the literal to set the Tag.code value to if the value is not present
     * Treats null & "" as equivalent
     *
     * @return literal
     */
    public String getNullOrEmpty() {
        return nullOrEmpty;
    }

    public ArrayList<Map<String, String>> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(ArrayList<Map<String, String>> crossReferences) {
        this.crossReferences = crossReferences;
    }

    public boolean isUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(boolean updateDate) {
        this.updateDate = updateDate;
    }

    public String getFormat() {
        return format;
    }

    @JsonIgnore
    public boolean isDateEpoc() {
        return format != null && format.equalsIgnoreCase("epoc");
    }

    public ArrayList<ColumnDefinition> getRlxProperties() {
        return rlxProperties;
    }

    public boolean isDocument() {
        return document;
    }

    public boolean hasRelationshipProps() {
        return rlxProperties != null;
    }

    @JsonIgnore
    public boolean isArrayDelimited() {
        return (delimiter != null && delimiter.equalsIgnoreCase("array"));
    }

    @JsonIgnore
    public String getExpression(ExpressionType expCol) {
        if (expCol == null)
            return null;
        switch (expCol) {
            case NAME:
                return name;
            case CODE:
                return code;
            case LABEL:
                return label;
            case CALLER_REF:
                return callerRefExp;
            case RELATIONSHIP:
                return rlxExp;
//            case PROP_EXP:
//                return propExp;
        }

        return null;
    }

    public ArrayList<ColumnDefinition> getProperties() {
        return properties;
    }

    public boolean hasProperites() {
        return this.properties != null && properties.size() > 0;
    }


    public String getSource() {
        return source;
    }

    public String getCallerRefExp() {
        return callerRefExp;
    }

    public String getRlxExp() {
        return rlxExp;
    }

    @Override
    public String toString() {
        return "ColumnDefinition{" +
                "label='" + label + '\'' +
                ", source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public ArrayList<AliasInputBean> getAliases() {
        return aliases;
    }

    @JsonIgnore
    public boolean hasAliases() {
        return ( aliases != null && !aliases.isEmpty());
    }

    /**
     * Forces a column to a specifc datatype. By default strings that look like "numbers" will be converted
     * to numbers. To preserve the value as a string set this to "string"
     *
     * @return null if default behavior to be used
     */
    public String getDataType() {
        return dataType;
    }

    public String getValueOnError() {
        return valueOnError;
    }

    @JsonIgnore
    public boolean hasEntityProperies() {
        return !tag && properties!=null && !properties.isEmpty();
    }

    public String getNotFound() {
        return notFound;
    }

    @JsonIgnore
    public void setSourceCol(String sourceCol) {
        this.sourceCol = sourceCol;
    }

    @JsonIgnore
    public String getSourceCol() {
        return sourceCol;
    }

    public String getDateFormat() {
        if ( dateFormat == null )
            return ((SimpleDateFormat)DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getTimeZone() {
        if ( timeZone == null )
            return TimeZone.getDefault().getID();

        return timeZone;

    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

}
