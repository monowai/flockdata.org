package com.auditbucket.search.model;

public class EsSearchResult<E> {

    private E results;
    private long totalHits;
    private int startedFrom;

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
}
