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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.helper.JsonUtils;

public class EsSearchRequestResult {

  @JsonDeserialize(contentAs = EsSearchResult.class)
  private Collection<SearchResult> results;
  private long totalHits;
  private int startedFrom;
  private Map<String, Object> what;
  private String index;
  private String entityType;
  private String fdSearchError;
  private byte[] json;

  public EsSearchRequestResult() {
  }

  public EsSearchRequestResult(EsSearchRequestResult results) {
    this();
    totalHits = results.getStartedFrom();
    totalHits = results.getTotalHits();
    this.results = results.getResults();

  }

  public EsSearchRequestResult(Collection<SearchResult> results) {
    this.results = results;
  }

  public EsSearchRequestResult(String fdSearchError) {
    this.fdSearchError = fdSearchError;
  }

  public EsSearchRequestResult(byte[] json) {
    this.json = json;
  }

  public EsSearchRequestResult(Map<String, Object> source) {
    this.what = source;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }

  public Collection<SearchResult> getResults() {
    return results;
  }

  public void setResults(Collection<SearchResult> results) {
    this.results = results;
  }

  public Map<String, Object> getRawResults() {
    if (json != null) {
      try {
        return JsonUtils.toMap(json);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return new HashMap<>();
  }

  public long getTotalHits() {
    return totalHits;
  }

  public void setTotalHits(long totalHits) {
    this.totalHits = totalHits;
  }

  public int getStartedFrom() {
    return startedFrom;
  }

  public void setStartedFrom(int startedFrom) {
    this.startedFrom = startedFrom;
  }

  public Map<String, Object> getWhat() {
    return what;
  }

  public void setWhat(Map<String, Object> what) {
    this.what = what;
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getFdSearchError() {
    return fdSearchError;
  }
}
