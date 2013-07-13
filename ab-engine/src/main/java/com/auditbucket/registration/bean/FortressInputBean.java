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

package com.auditbucket.registration.bean;

import org.hibernate.validator.constraints.NotEmpty;

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
    private Boolean searchActive = false;
    private String message = null;
    private String fortressKey = null;

    protected FortressInputBean() {
    }

    /**
     * The fortress will *not* ignore the search engine
     * changes will not be accumulated
     *
     * @param name              Company unique name for the fortress
     * @param accumulateChanges accumulate the changes in the search engine rather than update
     */
    public FortressInputBean(String name, boolean accumulateChanges) {
        this.accumulateChanges = accumulateChanges;
        searchActive = false;
        this.name = name;
    }

    /**
     * Setting AccumulateChanges to true will force the fortress to set searchActive to false
     *
     * @param name              company unique name for the fortress
     * @param accumulateChanges should the search engine accumulate each modification as a separate document?
     * @param searchActive      should this fortress use a search engine?
     */
    public FortressInputBean(String name, boolean accumulateChanges, boolean searchActive) {
        this(name);
        setAccumulateChanges(accumulateChanges);
        setSearchActive(searchActive);

    }


    public FortressInputBean(@NotEmpty String name) {
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
            this.searchActive = false;
    }

    public Boolean getSearchActive() {
        return searchActive;
    }

    public void setSearchActive(Boolean searchActive) {
        this.searchActive = searchActive;
    }

    public void setMessage(String message) {
        this.message = message;

    }

    public void setFortressKey(String fortressKey) {
        this.fortressKey = fortressKey;

    }
}
