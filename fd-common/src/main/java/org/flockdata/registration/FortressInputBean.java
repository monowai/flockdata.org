/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.registration;

import org.flockdata.model.MetaFortress;

import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Represents the input to create a fortress
 * Default behaviour is
 * not to accumulate each change as a separate document
 * index document changes in the search engine
 * <p>
 * Date: 15/06/13
 * Time: 12:04 PM
 */
public class FortressInputBean implements Serializable, MetaFortress {
    private String name;
    private Boolean searchEnabled = null;
    private Boolean storeEnabled = null;

    private String message = null;
    private String fortressKey = null;
    private String timeZone =  TimeZone.getDefault().getID();
    private String languageTag;
    private Boolean enabled = true;
    private Boolean system = false;
    private String code = null;

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
        this.searchEnabled = !ignoreSearchEngine;
        this.name = name;
        this.code = name;
    }

    public FortressInputBean(String name) {
        this.name = name;
        this.code = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    public FortressInputBean setName(String name) {
        this.name = name;
        if ( this.code ==null )
            this.code = name;
        return this;
    }

    public Boolean getSearchEnabled() {
        return searchEnabled;
    }

    public FortressInputBean setSearchEnabled(Boolean searchEnabled) {
        this.searchEnabled = searchEnabled;
        return this;
    }

    public void setMessage(String message) {
        this.message = message;

    }

    public FortressInputBean setFortressKey(String fortressKey) {
        this.fortressKey = fortressKey;
        return this;
    }

    public String getTimeZone() {
        return timeZone;
    }

    /**
     * setting an illegal timezone will fall back to GMT.
     *
     * @param timeZone timeZone you require
     */
    public FortressInputBean setTimeZone(String timeZone) {
        if (timeZone != null) {
            if (!TimeZone.getTimeZone(timeZone).getID().equals(timeZone))
                throw new IllegalArgumentException(timeZone + " was not recognized");
            this.timeZone = timeZone;
        }
        return this;
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

    public FortressInputBean setLanguageTag(String languageTag) {
        if (languageTag != null) {
            if ("und".equals(Locale.forLanguageTag(languageTag).toLanguageTag()))
                throw new IllegalArgumentException(languageTag + " was not recognized");
            this.languageTag = languageTag;
        }
        return this;
    }

    @Override
    public String toString() {
        return "FortressInputBean{" +
                "name='" + name + '\'' +
                ", searchEnabled=" + searchEnabled +
                '}';
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public FortressInputBean setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getSystem() {
        return system;
    }

    public FortressInputBean setSystem(Boolean system) {
        this.system = system;
        return this;
    }

    public Boolean getStoreEnabled() {
        return storeEnabled;
    }

    public FortressInputBean setStoreEnabled(Boolean enabled) {
        this.storeEnabled = enabled;
        return this;
    }

}
