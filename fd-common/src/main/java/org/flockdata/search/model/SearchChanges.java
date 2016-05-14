/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.search.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.search.SearchChangesDeserializer;
import org.flockdata.track.bean.SearchChange;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: mike
 * Date: 23/05/14
 * Time: 12:10 PM
 */
@JsonDeserialize(using = SearchChangesDeserializer.class)
public class SearchChanges {

    Collection <SearchChange> searchChanges = new ArrayList<>();

    public SearchChanges(){}

    public SearchChanges(Collection<SearchChange> searchDocuments) {
        this();
        this.searchChanges = searchDocuments;
    }

    public SearchChanges(SearchChange change) {
        searchChanges.add(change);
    }

    public Collection<SearchChange> getChanges() {
        return searchChanges;
    }

    public void addChange(SearchChange change) {
        searchChanges.add(change);
    }
}
