/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.model.EntityTagRelationshipInput;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.transform.tags.TagProfile;
import org.flockdata.transform.tags.TagProfileDeserializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 * Date: 9/05/14
 * Time: 7:44 AM
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDefinition implements GeoDefinition {

    private String code;   // Evaluate and setCode()
    private String source; // source property to read from
    private String keyPrefix; // Optional value to prefix a code with
    private String target; // source property to write source to (rename the column)
    private String dateFormat =null; // Java valid date format
    private String timeZone = null; // To use for dates

    private String dataType;

    private Boolean persistent = true;
    private Boolean storeNull = true;


    public enum ExpressionType {CODE, NAME, RELATIONSHIP, KEY_PREFIX, PROP_EXP, LABEL, CALLER_REF}

    // Flags that profile the properties of a column
    private Boolean callerRef=null;
    private Boolean title=null;
    private Boolean description=null;
    private Boolean valueAsProperty=null;
    private Boolean country=null;
    private Boolean createDate=null;
    private Boolean document=null;
    private Boolean tag=null;
    private Boolean mustExist=null;
    private Boolean createUser=null;
    private Boolean updateUser=null;
    private Boolean reverse = false;
    private Boolean updateDate=null;
    private Boolean merge=null;

    private String strategy = null;
    private String fortress = null;
    private String documentType = null;
    private String label;
    private String type; //datatype
    private String name;
    private String value; // User define value

    private String valueOnError;// Value to set to if the format causes an exception

    private String nullOrEmpty;
    private String notFound;

    @JsonDeserialize(using = ColumnDeserializer.class)
    private ArrayList<ColumnDefinition> rlxProperties;

    @JsonDeserialize(using = GeoDeserializer.class)
    private GeoPayload geoData;

    @JsonDeserialize(using = ColumnDeserializer.class)
    private ArrayList<ColumnDefinition> properties; // Properties to add to an object
    private ArrayList<Map<String, String>> entityLinks = new ArrayList<>();
    @JsonDeserialize(using = EntityTagRelationshipDeserializer.class)
    private Collection<EntityTagRelationshipInput> entityTagLinks ;
    private ArrayList<AliasInputBean> aliases;

    private String relationship; // Explicit relationship name
    private String delimiter;

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

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Boolean isCallerRef() {
        return callerRef;
    }

    public Boolean isTitle() {
        return title;
    }

    /**
     * Flags a tag block. Tags are not automatically assigned to the entity, but can be created
     *       while tracking an entity
     */
    public Boolean isTag() {
        return (tag !=null && tag) || isCountry();
    }

    /**
     * if true, then a the tag will never be created. FD will find your tag by Code and Alias
     *
     * @return
     */
    public Boolean isMustExist() {
        return mustExist;
    }

    public Boolean isValueAsProperty() {
        return valueAsProperty;
    }

    public Boolean isCountry() {
        return (country==null?false:country);
    }

    public String getName() {
        return name;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ArrayList<TagProfile> getTargets() {
        return targets;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getStrategy() {
        return strategy;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFortress() {
        return fortress;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDocumentType() {
        return documentType;
    }

    public Boolean isDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getKeyPrefix() {
        return keyPrefix;
    }

    // Overrides the value name of the property
    public String getTarget() {
        if (target == null)
            return source;
        else
            return target;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getRelationship() {
        return relationship;

    }

    public Boolean getReverse() {
        return reverse;
    }

    /**
     * if a delimiter is specified, then the column value will be treated as a delimited string and
     * a Tag.code will be created for each delimited value
     *
     * @return default is a ,
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDelimiter() {
        return delimiter;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getType() {
        return type;
    }

    /**
     * @return is this column carrying the value for the Creating user
     */
    public Boolean isCreateUser() {
        return createUser;
    }

    /**
     * @return is this column carrying the value for the Last Update user
     */
    public Boolean isUpdateUser() {
        return updateUser;
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
    public Boolean isCreateDate() {
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ArrayList<Map<String, String>> getEntityLinks() {
        return entityLinks;
    }

    public Boolean isUpdateDate() {
        return updateDate;
    }

    @JsonIgnore
    public boolean isDateEpoc() {
        return dateFormat != null && dateFormat.equalsIgnoreCase("epoc");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ArrayList<ColumnDefinition> getRlxProperties() {
        return rlxProperties;
    }

    public Boolean isDocument() {
        return document;
    }

    @JsonIgnore
    public boolean hasRelationshipProps() {
        return rlxProperties != null;
    }

    @JsonIgnore
    public boolean isArrayDelimited() {
        return (delimiter != null && delimiter.equalsIgnoreCase("array"));
    }

    /**
     * used to hold an expression for most columns.
     * @return expression
     */
    public String getValue() {
        return value;
    }

    /**
     * evaluates a system column from an expression and set's it as appropriate
     * code :"row#['mycol']"
     * @param expCol pre-defined column
     * @return property to be evaluated
     */
    @JsonIgnore
    @Deprecated // Favour getValue
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
            case KEY_PREFIX:
                return keyPrefix;
            case RELATIONSHIP:
                return relationship;
        }

        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ArrayList<ColumnDefinition> getProperties() {
        return properties;
    }

    @JsonIgnore
    public boolean hasProperites() {
        return this.properties != null && properties.size() > 0;
    }

    public String getSource() {
        return source;
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

    @JsonIgnore
    public boolean isDate() {
        // DAT-523
        return (dataType != null && dataType.equals("date")) || dateFormat!=null;
    }

    public String getValueOnError() {
        return valueOnError;
    }

    @JsonIgnore
    public boolean hasEntityProperties() {
        return !(tag!=null && tag ) && properties!=null && !properties.isEmpty();
    }

    public String getNotFound() {
        return notFound;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getTimeZone() {
        return timeZone;

    }

    public Boolean isPersistent() {
        return persistent;
    }

    @Override
    public GeoPayload getGeoData() {
        return geoData;
    }

    /**
     *
     * @return should properties in this payload be merged if the tag is existing?
     */
    public Boolean isMerge() {
        return merge;
    }

    public void setEntityTagLinks(Collection<EntityTagRelationshipInput> entityTagLinks) {
        this.entityTagLinks = entityTagLinks;
    }

    public Collection<EntityTagRelationshipInput> getEntityTagLinks() {
        return entityTagLinks;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getStoreNull() {
        return storeNull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnDefinition)) return false;

        ColumnDefinition that = (ColumnDefinition) o;

        if (callerRef != that.callerRef) return false;
        if (title != that.title) return false;
        if (description != that.description) return false;
        if (valueAsProperty != that.valueAsProperty) return false;
        if (country != that.country) return false;
        if (createDate != that.createDate) return false;
        if (document != that.document) return false;
        if (tag != that.tag) return false;
        if (mustExist != that.mustExist) return false;
        if (createUser != that.createUser) return false;
        if (updateUser != that.updateUser) return false;
        if (reverse != that.reverse) return false;
        if (updateDate != that.updateDate) return false;
        if (merge != that.merge) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (keyPrefix != null ? !keyPrefix.equals(that.keyPrefix) : that.keyPrefix != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;
        if (dateFormat != null ? !dateFormat.equals(that.dateFormat) : that.dateFormat != null) return false;
        if (timeZone != null ? !timeZone.equals(that.timeZone) : that.timeZone != null) return false;
        if (dataType != null ? !dataType.equals(that.dataType) : that.dataType != null) return false;
        if (persistent != null ? !persistent.equals(that.persistent) : that.persistent != null) return false;
        if (storeNull != null ? !storeNull.equals(that.storeNull) : that.storeNull != null) return false;
        if (strategy != null ? !strategy.equals(that.strategy) : that.strategy != null) return false;
        if (fortress != null ? !fortress.equals(that.fortress) : that.fortress != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (valueOnError != null ? !valueOnError.equals(that.valueOnError) : that.valueOnError != null) return false;
        if (nullOrEmpty != null ? !nullOrEmpty.equals(that.nullOrEmpty) : that.nullOrEmpty != null) return false;
        if (notFound != null ? !notFound.equals(that.notFound) : that.notFound != null) return false;
        if (rlxProperties != null ? !rlxProperties.equals(that.rlxProperties) : that.rlxProperties != null)
            return false;
        if (geoData != null ? !geoData.equals(that.geoData) : that.geoData != null) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (entityLinks != null ? !entityLinks.equals(that.entityLinks) : that.entityLinks != null) return false;
        if (aliases != null ? !aliases.equals(that.aliases) : that.aliases != null) return false;
        if (relationship != null ? !relationship.equals(that.relationship) : that.relationship != null) return false;
        if (delimiter != null ? !delimiter.equals(that.delimiter) : that.delimiter != null) return false;
        return targets != null ? targets.equals(that.targets) : that.targets == null;

    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (keyPrefix != null ? keyPrefix.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (dateFormat != null ? dateFormat.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        result = 31 * result + (persistent != null ? persistent.hashCode() : 0);
        result = 31 * result + (storeNull != null ? storeNull.hashCode() : 0);
        result = 31 * result + (callerRef ? 1 : 0);
        result = 31 * result + (title ? 1 : 0);
        result = 31 * result + (description ? 1 : 0);
        result = 31 * result + (valueAsProperty ? 1 : 0);
        result = 31 * result + (country ? 1 : 0);
        result = 31 * result + (createDate ? 1 : 0);
        result = 31 * result + (document ? 1 : 0);
        result = 31 * result + (tag ? 1 : 0);
        result = 31 * result + (mustExist ? 1 : 0);
        result = 31 * result + (createUser ? 1 : 0);
        result = 31 * result + (updateUser ? 1 : 0);
        result = 31 * result + (reverse ? 1 : 0);
        result = 31 * result + (strategy != null ? strategy.hashCode() : 0);
        result = 31 * result + (fortress != null ? fortress.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (valueOnError != null ? valueOnError.hashCode() : 0);
        result = 31 * result + (nullOrEmpty != null ? nullOrEmpty.hashCode() : 0);
        result = 31 * result + (notFound != null ? notFound.hashCode() : 0);
        result = 31 * result + (rlxProperties != null ? rlxProperties.hashCode() : 0);
        result = 31 * result + (geoData != null ? geoData.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (entityLinks != null ? entityLinks.hashCode() : 0);
        result = 31 * result + (aliases != null ? aliases.hashCode() : 0);
        result = 31 * result + (relationship != null ? relationship.hashCode() : 0);
        result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
        result = 31 * result + (updateDate ? 1 : 0);
        result = 31 * result + (merge ? 1 : 0);
        result = 31 * result + (targets != null ? targets.hashCode() : 0);
        return result;
    }


}
