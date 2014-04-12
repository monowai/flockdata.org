/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import java.util.Locale;
import java.util.TimeZone;

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
    private String timeZone = null;
    private String languageTag;


    protected FortressInputBean() {
    }

    /**
     * The fortress will *not* ignore the search engine
     * changes will not be accumulated
     *
     * @param name               CompanyNode unique name for the fortress
     * @param ignoreSearchEngine accumulate the changes in the search engine rather than update
     */
    public FortressInputBean(String name, boolean ignoreSearchEngine) {
        //this.accumulateChanges = ignoreSearchEngine;
        this.searchActive = !ignoreSearchEngine;
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

    public String getTimeZone() {
        return timeZone;
    }

    /**
     * setting an illegal timezone will fall back to GMT.
     *
     * @param timeZone timeZone you require
     */
    public void setTimeZone(String timeZone) {
        if (timeZone != null) {
            if (!TimeZone.getTimeZone(timeZone).getID().equals(timeZone))
                throw new IllegalArgumentException(timeZone + " was not recognized");
            this.timeZone = timeZone;
        }
    }

    /**
     * IETF BCP 47 language tag to use for this fortress.
     * Defaults to server system default.
     *
     * @return Language tag to be used for Locale conversions
     */
    public String getLanguageTag() {
        return languageTag;
    }

    public void setLanguageTag(String languageTag) {
        if (languageTag != null) {
            if ("und".equals(Locale.forLanguageTag(languageTag).toLanguageTag()))
                throw new IllegalArgumentException(languageTag + " was not recognized");
            this.languageTag = languageTag;
        }
    }


}
