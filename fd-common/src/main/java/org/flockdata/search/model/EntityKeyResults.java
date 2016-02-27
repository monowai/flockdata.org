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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collection;

public class EntityKeyResults {

    private Collection<String> results = new ArrayList<>();
    private int startedFrom;

    public EntityKeyResults() {
    }

    public EntityKeyResults(Collection<String> results) {
        this.results = results;
    }


    public Collection<String> getResults() {
        return results;
    }

    public void setResults(Collection<String> results) {
        this.results = results;
    }

    @JsonIgnore
    public long getTotalHits() {
        return results.size();
    }

    public void setStartedFrom(int startedFrom) {
        this.startedFrom = startedFrom;
    }

    public int getStartedFrom() {
        return startedFrom;
    }

    public void add(Object value) {
        if ( value == null )
            return;
        results.add(value.toString());
    }
}
