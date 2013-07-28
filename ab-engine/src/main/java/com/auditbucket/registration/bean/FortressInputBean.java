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
 * Date: 15/06/13
 * Time: 12:04 PM
 */
public class FortressInputBean {
    private String name;
    private Boolean searchActive = false;
    private String message = null;
    private String fortressKey = null;

    protected FortressInputBean() {
    }

    /**
     * The fortress will *not* ignore the search engine
     * changes will not be accumulated
     *
     * @param name         CompanyNode unique name for the fortress
     * @param searchActive accumulate the changes in the search engine rather than update
     */
    public FortressInputBean(String name, boolean searchActive) {
        //this.accumulateChanges = searchActive;
        this.searchActive = searchActive;
        this.name = name;
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
