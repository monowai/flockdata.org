package com.auditbucket.registration.bean;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * User: mike
 * Date: 12/06/13
 * Time: 12:04 PM
 */
public class FortressInputBean {
    private String name;
    private Boolean accumulateChanges = false;
    private Boolean ignoreSearchEngine = true;
    private String message = null;
    private String fortressKey = null;

    protected FortressInputBean() {
    }

    public FortressInputBean(String name, boolean accumulateChanges) {
        this.accumulateChanges = accumulateChanges;
        if (accumulateChanges)
            ignoreSearchEngine = false;

        this.name = name;
    }

    /**
     * Setting AccumulateChanges to true will force the fortress to set ignoreSearchEngine to false
     *
     * @param name               company unique name for the fortress
     * @param accumulateChanges  should the search engine accumulate each modification as a separate document?
     * @param ignoreSearchEngine should this fortress use a search engine?
     */
    public FortressInputBean(String name, boolean accumulateChanges, boolean ignoreSearchEngine) {
        this(name);
        setAccumulateChanges(accumulateChanges);
        setIgnoreSearchEngine(ignoreSearchEngine);

    }


    public FortressInputBean(@NotNull @NotEmpty String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAccumulateChanges() {
        return accumulateChanges;
    }

    public void setAccumulateChanges(Boolean accumulateChanges) {
        this.accumulateChanges = accumulateChanges;
        if (this.accumulateChanges)
            this.ignoreSearchEngine = false;
    }

    public Boolean getIgnoreSearchEngine() {
        return ignoreSearchEngine;
    }

    public void setIgnoreSearchEngine(Boolean ignoreSearchEngine) {
        this.ignoreSearchEngine = ignoreSearchEngine;
    }

    public void setMessage(String message) {
        this.message = message;

    }

    public void setFortressKey(String fortressKey) {
        this.fortressKey = fortressKey;

    }
}
