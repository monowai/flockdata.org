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

package org.flockdata.search;

import java.util.Objects;

/**
 * A single column that can be reported on
 *
 * @author mholdsworth
 * @since 31/08/2016
 */
public class EsColumn {
  private String name;
  private String displayName;
  private String type;
  private String format;

  public EsColumn() {
  }

  public EsColumn(String name, String type) {
    this();
    this.name = name;
    this.type = type;
    this.displayName = name;
    // Compute a user friendly display name by removing general constants
    if (name.startsWith(SearchSchema.TAG_FIELD)) {
      displayName = name.substring(SearchSchema.TAG_FIELD.length());
    } else if (name.startsWith(SearchSchema.ENTITY_FIELD)) {
      displayName = name.substring(SearchSchema.ENTITY_FIELD.length());
    } else if (name.startsWith(SearchSchema.DATA_FIELD)) {
      displayName = name.substring(SearchSchema.DATA_FIELD.length());
    }

    if (name.endsWith(SearchSchema.FACET_FIELD)) {
      displayName = displayName.substring(0, displayName.length() - SearchSchema.FACET_FIELD.length());
    }

    if (name.equals(SearchSchema.DOC_TYPE)) {
      displayName = "type";
    }

    if (displayName.contains(SearchSchema.TAG_UDP)) {
      displayName = displayName.replace(SearchSchema.TAG_UDP, "");
    }

  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EsColumn)) {
      return false;
    }
    EsColumn esColumn = (EsColumn) o;
    return Objects.equals(name, esColumn.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "EsColumn{" +
        "name='" + name + '\'' +
        ", displayName='" + displayName + '\'' +
        '}';
  }
}
