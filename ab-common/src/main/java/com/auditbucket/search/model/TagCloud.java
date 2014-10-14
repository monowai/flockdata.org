package com.auditbucket.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO representing a TagCloud .
 * Example : {"colera":12,"Cancer":1};
 * User: nabil
 * Date: 12/10/2014
 */
public class TagCloud {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Integer> terms = new HashMap<>();

    public Map<String, Integer> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Integer> terms) {
        this.terms = terms;
    }

    public void addTerm(String term, Integer occurrence) {
        terms.put(term, occurrence);
    }
}
