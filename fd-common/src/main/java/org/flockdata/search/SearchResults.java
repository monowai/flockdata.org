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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;

/**
 * Transfer object containing SearchResultInterface
 *
 * @author mholdsworth
 * @since 28/05/2014
 */
@Data
public class SearchResults {

  @JsonDeserialize(contentAs = EsSearchResult.class)
  private Collection<SearchResult> searchResults = new ArrayList<>();

  public SearchResults() {

  }

  public SearchResults(Collection<SearchResult> searchResults) {
    setSearchResults(searchResults);
  }

  public void addSearchResult(SearchResult result) {
    searchResults.add(result);
  }

  @JsonIgnore
  public boolean isEmpty() {
    return searchResults.isEmpty();
  }
}
