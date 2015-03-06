/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine;

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.search.model.EntitySearchSchema;

/**
 * For testing purposes
 * Created by mike on 6/03/15.
 */
public class SimpleFortress implements Fortress {
    FortressInputBean fib ;

    public SimpleFortress(){}

    public SimpleFortress(FortressInputBean fib, Company company) {
        this();
        this.fib = fib;
        setCompany(company);
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getName() {
        return fib.getName();
    }

    @Override
    public void setName(String name) {

    }

    private Company company;
    @Override
    public Company getCompany() {
        return company;
    }

    @Override
    public void setCompany(Company ownedBy) {
        this.company = ownedBy;
    }

    @Override
    public Boolean isAccumulatingChanges() {
        return true;
    }

    @Override
    public Boolean isSearchActive() {
        return fib.getSearchActive();
    }

    @Override
    public void setAccumulatingChanges(Boolean accumulateChanges) {

    }

    @Override
    public void setSearchActive(Boolean ignoreSearchEngine) {

    }

    @Override
    public String getTimeZone() {
        return fib.getTimeZone();
    }

    @Override
    public String getLanguageTag() {
        return fib.getLanguageTag();
    }

    @Override
    public String getCode() {
        return fib.getName();
    }

    @Override
    public Boolean isEnabled() {
        return fib.getEnabled();
    }

    @Override
    public Boolean isSystem() {
        return fib.getSystem();
    }

    @Override
    public String getIndexName() {
        return EntitySearchSchema.parseIndex(this);
    }

    @Override
    public Fortress setFortressInput(FortressInputBean fib) {
        this.fib = fib;
        return this;

    }
}
