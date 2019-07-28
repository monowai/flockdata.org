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

package org.flockdata.track.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties to create a relationship
 *
 * @author mholdsworth
 * @tag RelationshipTag
 * @since 9/07/2016
 */
public class EntityTagRelationshipInput {
  private boolean geo;
  private boolean reverse; // default is Entity->Tag
  private String relationshipName;
  private Map<String, Object> properties;

  EntityTagRelationshipInput() {
  }

  public EntityTagRelationshipInput(String relationshipName, boolean geo) {
    this(relationshipName);
    this.geo = geo;
  }

  public EntityTagRelationshipInput(String relationshipName, Map<String, Object> properties) {
    this(relationshipName, false);
    this.properties = properties;
  }

  public EntityTagRelationshipInput(String relationshipName) {
    this();
    if (relationshipName != null) {
      relationshipName = relationshipName.trim();
      if (relationshipName.contains(" ")) {
        if (!relationshipName.startsWith("'")) {
          relationshipName = "'" + relationshipName + "'";
        }
      }
    }

    this.relationshipName = relationshipName;
  }

  public boolean isGeo() {
    return geo;
  }

  public void setGeo(Boolean geo) {
    this.geo = geo;
  }

  public String getRelationshipName() {
    return relationshipName;
  }

  public void setRelationshipName(String relationshipName) {
    this.relationshipName = relationshipName;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public EntityTagRelationshipInput setProperties(Map<String, Object> properties) {
    this.properties = properties;
    return this;
  }

  public boolean getReverse() {
    return reverse;
  }

  public EntityTagRelationshipInput setReverse(Boolean reverse) {
    this.reverse = reverse;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityTagRelationshipInput)) {
      return false;
    }

    EntityTagRelationshipInput that = (EntityTagRelationshipInput) o;

    return relationshipName != null ? relationshipName.equals(that.relationshipName) : that.relationshipName == null;

  }

  @Override
  public int hashCode() {
    return relationshipName != null ? relationshipName.hashCode() : 0;
  }

  public void addProperty(String key, Object value) {
    if (properties == null) {
      properties = new HashMap<>();
    }
    properties.put(key, value);
  }
}
