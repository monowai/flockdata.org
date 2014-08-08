package com.auditbucket.search.model;

import java.util.Collection;

public class EsSearchResult {

    private Collection<SearchResult> results;
    private long totalHits;
    private int startedFrom;

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

}
