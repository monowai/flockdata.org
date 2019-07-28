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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagResultBean;

/**
 * Converts an EntityTag to an EntityTagResult contract
 *
 * @author mike
 * @tag
 * @since 14/01/17
 */
public class EntityTagResult {
  private Long id;
  private Entity entity;
  private TagResultBean tag;
  private String relationship;
  private GeoDataBeans geoData;
  private Map<String, Object> properties;

  EntityTagResult() {
  }

  public EntityTagResult(EntityTag entityTag) {
    this.id = entityTag.getId();
    this.entity = entityTag.getEntity();
    this.tag = new TagResultBean(entityTag.getTag());
    this.properties = entityTag.getProperties();
    relationship = entityTag.getRelationship();
    geoData = entityTag.getGeoData();
  }

  public TagResultBean getTag() {
    return tag;
  }

  public String getRelationship() {
    return relationship;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public GeoDataBeans getGeoData() {
    return geoData;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  @JsonIgnore
  public Integer getWeight() {
    return (Integer) getProperties().get("weight");
  }

  @JsonIgnore
  public Long getId() {
    return id;
  }

  @JsonIgnore
  public Entity getEntity() {
    return entity;
  }
}
