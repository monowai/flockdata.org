/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.model;

import java.util.Collection;
import java.util.Map;

public class EsSearchResult {

    private Collection<SearchResult> results;
    private long totalHits;
    private int startedFrom;
    private boolean kvResponse = false;
    private Map<String,Object>what;

    public EsSearchResult() {
    }

    public EsSearchResult(EsSearchResult results) {
        this();
        totalHits= results.getStartedFrom();
        totalHits = results.getTotalHits();
        this.results = results.getResults();

    }

    public EsSearchResult(Collection<SearchResult> results) {
        this.results = results;
    }

    public EsSearchResult(Map<String, Object> source) {
        this.what = source;
        this.kvResponse = true;
    }

    public Collection<SearchResult> getResults() {
        return results;
    }

    public void setResults(Collection<SearchResult> results) {
        this.results = results;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setStartedFrom(int startedFrom) {
        this.startedFrom = startedFrom;
    }

    public int getStartedFrom() {
        return startedFrom;
    }

    public Map<String, Object> getWhat() {
        return what;
    }

    public void setWhat(Map<String, Object> what) {
        this.what = what;
    }
}
