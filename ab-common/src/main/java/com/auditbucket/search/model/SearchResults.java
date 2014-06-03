/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: mike
 * Date: 28/05/14
 * Time: 8:22 PM
 */
public class SearchResults {

    private ArrayList<SearchResult>searchResults = new ArrayList<>();

    public void addSearchResult(SearchResult result){
        searchResults.add(result);
    }

    public Collection<SearchResult> getSearchResults() {
        return searchResults;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return searchResults.isEmpty();
    }
}
