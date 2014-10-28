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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Map;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 7:44 AM
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDefinition {

    // Flags that profile the properties of a column
    private boolean callerRef;
    private boolean title;
    private boolean description;
    private boolean valueAsProperty;
    private boolean country;
    private boolean createdDate;
    private boolean tag;
    private boolean mustExist;
    private boolean createdUser;
    private boolean updateUser;
    private boolean reverse = false;

    private String   dateFormat;
    private String   strategy = null;
    private String   fortress = null;
    private String   documentType = null;
    private String   label;
    private String   type; //datatype
    private String   nameColumn;
    private String   nullOrEmpty;
    private String   appendJoinText = " ";
    private String   relationshipName;

    private String[] refColumns;

    private String relationship;

    private String delimiter;

    private String code;
    private String customPropertyName;

    private ArrayList<Map<String,String>>crossReferences = new ArrayList<>();

    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label Noun that describes the tag
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonDeserialize(using = TagProfileDeserializer.class)
    private ArrayList<TagProfile> targets = new ArrayList<>();

    /**
     *
     * @return Columns used to create a callerReference where there is otherwise no identifiable key
     */
    public String[] getRefColumns() {
        return refColumns;
    }

    /**
     *
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

    public String getNameColumn() {
        return nameColumn;
    }

    public String getRelationshipName() {
        if (relationshipName == null)
            return (isCountry() ? null : "undefined");
        return relationshipName;
    }

    public ArrayList<TagProfile> getTargets() {
        return targets;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getStrategy() {
        return strategy;
    }

//    public String[] getColumns() {
//        return columns;
//    }

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

    public String getCustomPropertyName() {
        return customPropertyName;
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
     * @return default is a ,
     */
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

//    public void setMetaValues(String[] metaValues) {
//        this.metaValues = metaValues;
//    }

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
     *
     * @return is this column carrying the value for the Creating user
     */
    public boolean isCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(boolean createdUser) {
        this.createdUser = createdUser;
    }

    /**
     *
     * @return is this column carrying the value for the Last Update user
     */
    public boolean isUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(boolean updateUser) {
        this.updateUser = updateUser;
    }

    /**
     *
     * @param mustExist if true, FdServer will throw an error if the tag does not exist
     */
    public void setMustExist(boolean mustExist) {
        this.mustExist = mustExist;
    }

    /**
     *
     * @return is this column carrying the value for the Created Date
     */
    public boolean isCreatedDate() {
        return createdDate;
    }

    /**
     * Defines the literal to set the Tag.code value to if the value is not present
     * Treats null & "" as equivalent
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
}
