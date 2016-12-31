/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search;

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
    private String index;
    private String entityType;
    private String fdSearchError;
    private byte[] json;

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

    public EsSearchResult(String fdSearchError) {
        this.fdSearchError = fdSearchError;
    }

    public EsSearchResult (byte[] json){
        this.json = json;
    }

    public EsSearchResult(Map<String, Object> source) {
        this.what = source;
        this.kvResponse = true;
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
