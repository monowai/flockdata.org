/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityTagRelationshipDefinition;
import org.flockdata.transform.json.ColumnDeserializer;
import org.flockdata.transform.json.EntityTagRelationshipDeserializer;
import org.flockdata.transform.json.GeoDeserializer;
import org.flockdata.transform.json.TagProfileDeserializer;
import org.flockdata.transform.tag.TagProfile;

/**
 * @author mholdsworth
 * @since 9/05/2014
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDefinition implements GeoDefinition {

  private String code;   // Evaluate and setCode()
  private String source; // source property to read from
  private String keyPrefix; // Optional value to prefix a code with
  private String target; // New name for the source - null by default
  private String dateFormat = null; // Java valid date format
  private String timeZone = null; // To use for dates

  private String dataType;

  private Boolean persistent = true;
  private Boolean storeNull = true;
  // Flags that profile the properties of a column
  private Boolean callerRef = null;
  private Boolean title = null;
  private Boolean description = null;
  private Boolean createDate = null;
  private Boolean document = null;
  private Boolean tag = null;
  private Boolean mustExist = null;
  private Boolean createUser = null;
  private Boolean updateUser = null;
  private Boolean reverse = false;
  private Boolean updateDate = null;
  private Boolean merge = null;
  private String fortress = null;
  private String documentType = null;
  private String label;
  private String labelDescription;
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
  private ArrayList<ColumnDefinition> properties; // Properties to add to this object
  private ArrayList<EntityKeyBean> entityLinks = new ArrayList<>();
  @JsonDeserialize(using = EntityTagRelationshipDeserializer.class)
  private Collection<EntityTagRelationshipDefinition> entityTagLinks;
  private ArrayList<AliasInputBean> aliases;
  private String delimiter;    // value delimiter
  @JsonDeserialize(using = TagProfileDeserializer.class)
  private ArrayList<TagProfile> targets = new ArrayList<>();

  public String getLabel() {
    return label;
  }

  /**
   * @param label Noun that describes the tag
   */
  public void setLabel(String label) {
    this.label = label;
  }

  public Boolean isCallerRef() {
    return callerRef;
  }

  public Boolean isTitle() {
    return title;
  }

  /**
   * This block of Json is a Tag.
   *
   * @return true if this column supports tag semantics
   * @see org.flockdata.registration.TagInputBean
   */
  public Boolean isTag() {
    return (tag != null && tag);
  }

  /**
   * if true, then a the tag will not be created during the track request and an exception will be thrown.
   * FD will find your tag by Code and Alias
   *
   * @return boolean
   */
  public Boolean isMustExist() {
    return mustExist;
  }

  public String getName() {
    return name;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public ArrayList<TagProfile> getTargets() {
    return targets;
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

  /**
   * @return rename the source column to this value
   */
  public String getTarget() {
    if (target == null) {
      return source;
    } else {
      return target;
    }
  }

  public void setTarget(String target) {
    this.target = target;
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
   * Treats null and "" as equivalent
   *
   * @return literal
   */
  public String getNullOrEmpty() {
    return nullOrEmpty;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public Collection<EntityKeyBean> getEntityLinks() {
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
   *
   * @return expression
   */
  public String getValue() {
    return value;
  }

  // An expression value to use for a column
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * evaluates a system column from an expression and set's it as appropriate
   * code :"row#['mycol']"
   *
   * @param expCol pre-defined column
   * @return property to be evaluated
   */
  @JsonIgnore
  @Deprecated // Favour getValue
  public String getExpression(ExpressionType expCol) {
    if (expCol == null) {
      return null;
    }
    switch (expCol) {
      case NAME:
        return name;
      case CODE:
        return code;
      case LABEL:
        return label;
      case KEY_PREFIX:
        return keyPrefix;
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

  public void setSource(String source) {
    this.source = source;
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
    return (aliases != null && !aliases.isEmpty());
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

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  @JsonIgnore
  public boolean isDate() {
    // DAT-523
    return (dataType != null && dataType.equals("date"));
  }

  public String getValueOnError() {
    return valueOnError;
  }

  @JsonIgnore
  public boolean hasEntityProperties() {
    return !(tag != null && tag) && properties != null && !properties.isEmpty();
  }

  public String getNotFound() {
    return notFound;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public ColumnDefinition setDateFormat(String format) {
    this.dateFormat = format;
    return this;
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
   * @return should properties in this payload be merged if the tag is existing?
   */
  public Boolean isMerge() {
    return merge;
  }

  public Collection<EntityTagRelationshipDefinition> getEntityTagLinks() {
    return entityTagLinks;
  }

  public void setEntityTagLinks(Collection<EntityTagRelationshipDefinition> entityTagLinks) {
    this.entityTagLinks = entityTagLinks;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean getStoreNull() {
    return storeNull;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getLabelDescription() {
    return labelDescription;
  }

  public ColumnDefinition setTitle(boolean title) {
    this.title = title;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, source, keyPrefix, target, dataType, fortress, documentType, label, type, name, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ColumnDefinition other = (ColumnDefinition) obj;
    return Objects.equals(this.code, other.code)
        && Objects.equals(this.source, other.source)
        && Objects.equals(this.keyPrefix, other.keyPrefix)
        && Objects.equals(this.target, other.target)
        && Objects.equals(this.dataType, other.dataType)
        && Objects.equals(this.fortress, other.fortress)
        && Objects.equals(this.documentType, other.documentType)
        && Objects.equals(this.label, other.label)
        && Objects.equals(this.type, other.type)
        && Objects.equals(this.name, other.name)
        && Objects.equals(this.value, other.value);
  }

  public enum ExpressionType {CODE, NAME, RELATIONSHIP, KEY_PREFIX, PROP_EXP, LABEL, CALLER_REF}
}
