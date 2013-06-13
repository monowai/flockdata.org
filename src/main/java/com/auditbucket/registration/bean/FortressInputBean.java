package com.auditbucket.registration.bean;

/**
 * User: mike
 * Date: 12/06/13
 * Time: 12:04 PM
 */
public class FortressInputBean {
    private String name;
    private Boolean accumulatingChanges = false;
    private Boolean ignoreSearchEngine = true;

    public FortressInputBean(String name, boolean accumulatingChanges) {
        this.accumulatingChanges = accumulatingChanges;
        this.name = name;
    }

    public FortressInputBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAccumulatingChanges() {
        return accumulatingChanges;
    }

    public void setAccumulatingChanges(Boolean accumulatingChanges) {
        this.accumulatingChanges = accumulatingChanges;
    }

    public Boolean getIgnoreSearchEngine() {
        return ignoreSearchEngine;
    }

    public void setIgnoreSearchEngine(Boolean ignoreSearchEngine) {
        this.ignoreSearchEngine = ignoreSearchEngine;
    }
}
