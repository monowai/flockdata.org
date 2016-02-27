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

import org.flockdata.helper.JsonUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
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

    public byte[] getJson() {
        return json;
    }

    public void setJson(byte[] json) {
        this.json = json;
    }

    private byte[] json;

    public EsSearchResult (byte[] json){
        this.json = json;
    }

    public EsSearchResult(Map<String, Object> source) {
        this.what = source;
        this.kvResponse = true;
    }

    public Collection<SearchResult> getResults() {
        return results;
    }

    public Map<String,Object>getRawResults() {
        if ( json !=null ){
            try {
                return JsonUtils.toMap(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
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
