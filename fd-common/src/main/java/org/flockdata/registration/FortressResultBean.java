/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.registration;

import org.flockdata.model.Fortress;
import org.flockdata.model.MetaFortress;

import java.io.Serializable;

/**
 * @author mholdsworth
 * @since 21/12/2013
 */
public class FortressResultBean implements MetaFortress, Serializable{
    private String code;
    private String name;
    private String indexName;
    private String timeZone;
    private String message;
    private Boolean enabled=Boolean.TRUE;
    private Boolean searchEnabled;
    private Boolean storeEnabled;
    private String companyName;
    private boolean system;

    protected FortressResultBean() {

    }

    public FortressResultBean(Fortress fortress) {
        this();
        this.name = fortress.getName();
        this.code = fortress.getCode();
        this.indexName = fortress.getRootIndex();
        this.timeZone = fortress.getTimeZone();
        this.enabled = fortress.isEnabled();
        this.system = fortress.isSystem();
        this.searchEnabled = fortress.isSearchEnabled();
        if ( fortress.getCompany()!=null)
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

    public boolean isSystem() {
        return system;
    }

    public String getMessage() {
        return message;
    }
}
