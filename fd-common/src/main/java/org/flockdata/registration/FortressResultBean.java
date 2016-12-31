/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;

import java.io.Serializable;

/**
 * @author mholdsworth
 * @since 21/12/2013
 */
public class FortressResultBean implements Fortress, Serializable {
    private String code;
    private String name;
    private String rootIndex;
    private String timeZone;
    private String message;
    private Boolean enabled = Boolean.TRUE;
    private Boolean searchEnabled;
    private Boolean storeEnabled;
    private CompanyResultBean company;
    private boolean system;

    protected FortressResultBean() {

    }

    public FortressResultBean(Fortress fortress) {
        this();
        this.name = fortress.getName();
        this.code = fortress.getCode();
        this.rootIndex = fortress.getRootIndex();
        this.timeZone = fortress.getTimeZone();
        this.enabled = fortress.isEnabled();
        this.system = fortress.isSystem();
        this.searchEnabled = fortress.isSearchEnabled();
        this.company = new CompanyResultBean(fortress.getCompany());
        this.storeEnabled = fortress.isStoreEnabled();
    }

    public String getName() {
        return name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Boolean isStoreEnabled() {
        return storeEnabled;
    }

    @Override
    public Company getCompany() {
        return company;
    }

    @Override
    public Boolean isSearchEnabled() {
        return searchEnabled;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return null;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRootIndex() {
        return rootIndex;
    }

    @Override
    @JsonIgnore
    public Segment getDefaultSegment() {
        return null;
    }

    public Boolean isSystem() {
        return system;
    }

    public String getMessage() {
        return message;
    }
}
