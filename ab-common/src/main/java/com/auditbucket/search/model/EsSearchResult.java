package com.auditbucket.search.model;

import java.util.ArrayList;
import java.util.Collection;

public class EsSearchResult<E> {

    private E results;
    private long totalHits;
    private int startedFrom;
    private Collection<SearchResult> searchResults;

    public EsSearchResult() {
    }

    public EsSearchResult(EsSearchResult results) {
        this();
        totalHits= results.getStartedFrom();
        totalHits = results.getTotalHits();
    }

    public E getResults() {
        return results;
    }

    public void setResults(E results) {
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

    public Collection<String> getMetaKeys() {
        Collection<String>metaKeys = new ArrayList<>();
        for (SearchResult searchResult : searchResults) {
            metaKeys.add(searchResult.getMetaKey());
        }
        return metaKeys;
    }
}
