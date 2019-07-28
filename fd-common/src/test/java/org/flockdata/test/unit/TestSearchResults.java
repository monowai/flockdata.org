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

package org.flockdata.test.unit;

import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.EsSearchResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.SearchResult;
import org.flockdata.search.SearchResults;
import org.flockdata.track.bean.SearchChange;
import org.junit.Test;

/**
 * @author Mike Holdsworth
 * @since 23/09/17
 */
public class TestSearchResults {
  @Test
  public void searchResultsSerialize() throws Exception {
    SearchChange searchChange = new EntitySearchChange();
    searchChange.setName("anyName");
    SearchResult searchResult = EsSearchResult.builder()
        .code("code")
        .name("name")
        .description("description")
        .build();
    Collection<SearchResult> results = new ArrayList<>();
    results.add(searchResult);
    SearchResults searchResults = new SearchResults(results);

    byte[] bytes = JsonUtils.toJsonBytes(searchResults);
    assertNotNull(JsonUtils.toObject(bytes, SearchResults.class));

  }

  @Test
  public void queryParams() throws Exception {
    QueryParams qp = new QueryParams("*");
    assertThat(qp)
        .hasFieldOrProperty("matchAll")
        .hasFieldOrPropertyWithValue("matchAll", true);

  }
}
