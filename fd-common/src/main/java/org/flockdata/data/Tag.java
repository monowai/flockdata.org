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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author mike
 * @tag
 * @since 2/01/17
 */
public interface Tag {
  String DEFAULT_TAG = "Tag";
  String DEFAULT = ":" + Tag.DEFAULT_TAG;
  String UNDEFINED = "undefined";
  String PROPS_PREFIX = "props-";
  String LAT = "latitude";
  String LON = "longitude";
  String NODE_LAT = PROPS_PREFIX + LAT;
  String NODE_LON = PROPS_PREFIX + LON;

  String getName();

  Long getId();

  String getKey();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  Map<String, Object> getProperties();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  String getCode();

  String getLabel();

  Set<Alias> getAliases();

  @JsonIgnore
  Boolean isNew();

  void addProperty(String key, Object property);

  @JsonIgnore
  Map<String, Collection<Tag>> getSubTags();

  @JsonIgnore
  Collection<Tag> getSubTags(String key);

  boolean hasSubTags();

  boolean hasProperties();

  void addAlias(Alias alias);
}
