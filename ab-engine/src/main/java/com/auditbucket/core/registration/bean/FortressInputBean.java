/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.core.registration.bean;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * Represents the input to create a fortress
 * Default behaviour is
 * not to accumulate each change as a separate document
 * index document changes in the search engine
 * <p/>
 * Date: 12/06/13
 * Time: 12:04 PM
 */
public class FortressInputBean {
    private String name;
    private Boolean accumulateChanges = false;
    private Boolean ignoreSearchEngine = false;
    private String message = null;
    private String fortressKey = null;

    protected FortressInputBean() {
    }

    /**
     * implies that the fortress will *not* ignore the search engine
     *
     * @param name              Company unique name for the fortress
     * @param accumulateChanges accumulate the changes in the search engine rather than update
     */
    public FortressInputBean(String name, boolean accumulateChanges) {
        this.accumulateChanges = accumulateChanges;
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
