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

package org.flockdata.transform.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.GeoDefinition;
import org.flockdata.transform.GeoPayload;
import org.flockdata.transform.json.GeoDeserializer;

/**
 * @author mholdsworth
 * @tag Tag, ContentModel, Geo
 * @since 27/05/2014
 */
public class TagProfile implements GeoDefinition {
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String name;
  private String code;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String keyPrefix;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Boolean reverse = false;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String notFound;

  private String relationship;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String delimiter = null;
  //    private boolean country = false;
  private String label;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String labelDescription;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String condition;// boolean expression that determines if this tag will be created
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private ArrayList<TagProfile> targets;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private ArrayList<ColumnDefinition> properties;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private ArrayList<ColumnDefinition> rlxProperties;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private ArrayList<AliasInputBean> aliases;

  @JsonDeserialize(using = GeoDeserializer.class)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private GeoPayload geoData;

  private boolean mustExist;
  private boolean merge;

  public Boolean getReverse() {
    return reverse;
  }

  public void setReverse(Boolean reverse) {
    this.reverse = reverse;
  }

  public String getRelationship() {
    return relationship;
  }

  public void setRelationship(String relationship) {
    this.relationship = relationship;
  }

  public String getName() {
    return name;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  /**
   * @return The label node that will be created. Reverts to the column name if not defined
   */
  public String getLabel() {
    if (label == null) {
      return code;
    }
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ArrayList<TagProfile> getTargets() {
    return targets;
  }

  public void setTargets(ArrayList<TagProfile> targets) {
    this.targets = targets;
  }

  @Override
  public String toString() {
    return "CsvTag{" +
        "code='" + code + '\'' +
        ", relationship='" + relationship + '\'' +
        ", label='" + label + '\'' +
        '}';
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public String getCondition() {
    return condition;
  }

  public ArrayList<ColumnDefinition> getProperties() {
    return properties;
  }

  public ArrayList<ColumnDefinition> getRlxProperties() {
    return rlxProperties;
  }

  public String getLabelDescription() {
    return labelDescription;
  }

  public boolean isMustExist() {
    return mustExist;
  }

  public void setMustExist(boolean mustExist) {
    this.mustExist = mustExist;
  }

  public boolean hasProperties() {
    return properties != null && properties.size() > 0;
  }

  public ArrayList<AliasInputBean> getAliases() {
    return aliases;
  }

  public void setAliases(ArrayList<AliasInputBean> aliases) {
    this.aliases = aliases;
  }

  public boolean hasAliases() {
    return aliases != null && aliases.size() > 0;
  }

  public String getNotFound() {
    return notFound;
  }

  @Override
  public GeoPayload getGeoData() {
    return geoData;
  }

  public boolean isMerge() {
    return merge;
  }

  public void setMerge(boolean merge) {
    this.merge = merge;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TagProfile)) {
      return false;
    }

    TagProfile that = (TagProfile) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (code != null ? !code.equals(that.code) : that.code != null) {
      return false;
    }
    if (keyPrefix != null ? !keyPrefix.equals(that.keyPrefix) : that.keyPrefix != null) {
      return false;
    }
    if (reverse != null ? !reverse.equals(that.reverse) : that.reverse != null) {
      return false;
    }
    if (relationship != null ? !relationship.equals(that.relationship) : that.relationship != null) {
      return false;
    }
    if (delimiter != null ? !delimiter.equals(that.delimiter) : that.delimiter != null) {
      return false;
    }
    if (label != null ? !label.equals(that.label) : that.label != null) {
      return false;
    }
    if (condition != null ? !condition.equals(that.condition) : that.condition != null) {
      return false;
    }
    return geoData != null ? geoData.equals(that.geoData) : that.geoData == null;

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (keyPrefix != null ? keyPrefix.hashCode() : 0);
    result = 31 * result + (reverse != null ? reverse.hashCode() : 0);
    result = 31 * result + (relationship != null ? relationship.hashCode() : 0);
    result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
    result = 31 * result + (label != null ? label.hashCode() : 0);
    result = 31 * result + (condition != null ? condition.hashCode() : 0);
    result = 31 * result + (geoData != null ? geoData.hashCode() : 0);
    return result;
  }
}
