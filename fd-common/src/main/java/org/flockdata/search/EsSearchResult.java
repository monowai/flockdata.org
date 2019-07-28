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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Object to tie the keys between fd-engine and fd-search so that fd-engine can keep the document up-to-date
 *
 * @author mholdsworth
 * @tag Search, Entity, Contract
 * @since 13/07/2013
 */
@Data
@Builder
public class EsSearchResult implements SearchResult {


  private String key, fortress, searchKey, documentType;
  private String indexName; // Store Index
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Long logId;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Long entityId;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, Object> data;
  private String code;
  private String createdBy;
  private Long fdTimestamp;
  private String name;
  private String lastUser;
  private String description;
  private String event;
  private Long dateCreated;
  private Long lastUpdate;
  private long whenCreated;

  @Override
  public String toString() {
    return "SearchResult{" +
        "entityId='" + entityId + '\'' +
        ", key='" + key + '\'' +
        ", logId='" + logId + '\'' +
        ", fortress='" + fortress + '\'' +
        ", documentType='" + documentType + '\'' +
        '}';
  }


  public void addFieldValue(String field, Object value) {
    if (this.data == null) {
      data = new HashMap<>();
    }
    if (field.contains(SearchSchema.DATA)) {
      field = field.substring(SearchSchema.DATA.length() + 1);
    }
    this.data.put(field, value);
  }

}
