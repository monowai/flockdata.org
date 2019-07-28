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

import org.flockdata.data.Tag;


/**
 * Associates a Tag with an Entity
 *
 * @author mholdsworth
 * @tag Contract, Tag, Entity, Geo
 * @since 28/06/2013
 */
public class EntityTagInputBean {
  private String tagCode;
  private String key;
  private String type;
  private String index;
  private boolean since;
  private boolean geoRlx;
  private String tagKeyPrefix;

  private EntityTagInputBean() {
  }

  /**
   * @param key              key for an existing entity
   * @param tagCode          name of an existing tag
   * @param relationshipName relationship name to create
   */
  public EntityTagInputBean(String key, String tagCode, String relationshipName) {
    this();
    this.key = key;
    this.tagCode = tagCode;
    if (relationshipName == null) {
      this.type = "general";
    } else {
      this.type = relationshipName;
    }
  }

  public String getKey() {
    return key;
  }

  public String getTagCode() {
    return tagCode;
  }

  /**
   * Type of relationship
   *
   * @return relationship name
   */
  public String getType() {
    return type;
  }

  public String getIndex() {
    if (index == null || "".equals(index)) {
      return Tag.DEFAULT;
    } else {
      return index;
    }
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public boolean isSince() {
    return since;
  }

  /**
   * When connecting the tag to the entity, tell FD to record a timestamp as a user defined property
   *
   * @param since yes/no
   * @return this
   */
  public EntityTagInputBean setSince(boolean since) {
    this.since = since;
    return this;
  }

  public String getTagKeyPrefix() {
    return tagKeyPrefix;
  }

  public void setTagKeyPrefix(String tagKeyPrefix) {
    this.tagKeyPrefix = tagKeyPrefix;
  }

  public boolean isGeoRlx() {
    return geoRlx;
  }
}
