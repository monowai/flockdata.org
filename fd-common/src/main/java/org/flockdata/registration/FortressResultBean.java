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

import org.flockdata.model.Fortress;
import org.flockdata.model.MetaFortress;

import java.io.Serializable;

/**
 * User: Mike Holdsworth
 * Since: 21/12/13
 */
public class FortressResultBean implements MetaFortress, Serializable{
    private String code;
    private String name;
    private String indexName;
    private String timeZone;
    private Boolean enabled=Boolean.TRUE;
    private Boolean searchEnabled;
    private Boolean storeEnabled;
    private String companyName;

    protected FortressResultBean() {

    }

    public FortressResultBean(Fortress fortress) {
        this();
        this.name = fortress.getName();
        this.code = fortress.getCode();
        this.indexName = fortress.getRootIndex();
        this.timeZone = fortress.getTimeZone();
        this.enabled = fortress.isEnabled();
        this.searchEnabled = fortress.isSearchEnabled();
        this.companyName = fortress.getCompany().getName();
        this.storeEnabled = fortress.isStoreEnabled();
    }

    public String getName() {
        return name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public boolean getSearchEnabled() {
        return searchEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isStoreEnabled() {
        return storeEnabled;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIndexName() {
        return indexName;
    }

}
