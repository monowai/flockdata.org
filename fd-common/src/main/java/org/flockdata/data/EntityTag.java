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

package org.flockdata.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.flockdata.track.bean.GeoDataBeans;

/**
 * @author mholdsworth
 * @since 13/07/2016
 */
public interface EntityTag {
  // Key value indicating when, in the fortress, this relationship was established
  String SINCE = "since";
  // Property that refers to when this relationship was introduced to FD
  String FD_WHEN = "fdWhen";

  Boolean isGeoRelationship();

  Long getId();

  Entity getEntity();

  Tag getTag();

  String getRelationship();

  void setRelationship(String name);

  void setGeo(Boolean geo);

  Integer getWeight();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  GeoDataBeans getGeoData();

  EntityTag setGeoData(GeoDataBeans geoBeans);

  Boolean isReversed();

  Map<String, Object> getProperties();

  enum TAG_STRUCTURE {TAXONOMY, DEFAULT}
}
