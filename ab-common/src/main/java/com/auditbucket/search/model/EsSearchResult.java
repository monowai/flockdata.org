package com.auditbucket.search.model;

import java.util.Collection;

public class EsSearchResult {

    private Collection<String> results;

    public EsSearchResult() {
    }

    public EsSearchResult(Collection<String> results) {
        this.results = results;
    }

    public Collection<String> getResults() {
        return results;
    }

    public void setResults(Collection<String> results) {
        this.results = results;
    }

}
