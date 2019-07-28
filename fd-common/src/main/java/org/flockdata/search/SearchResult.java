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

import java.util.Map;

/**
 * @author Mike Holdsworth
 * @since 16/09/17
 */
public interface SearchResult {
  String getLastUser();

  String getDescription();

  String getEvent();

  Long getLastUpdate();

  long getWhenCreated();

  String getKey();

  String getFortress();

  String getIndexName();

  String getSearchKey();

  String getDocumentType();

  Long getLogId();

  Long getEntityId();

  String getCode();

  String getCreatedBy();

  Long getFdTimestamp();

  Map<String, Object> getData();

  String getName();
}
