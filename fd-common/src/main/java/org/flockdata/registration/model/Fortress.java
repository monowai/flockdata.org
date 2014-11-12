/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.registration.model;


public interface Fortress {

    public abstract Long getId();

    public abstract String getName();

    public abstract void setName(String name);

    public abstract Company getCompany();

    public abstract void setCompany(Company ownedBy);

    /**
     * Are changes logged against this fortress accumulated or updated in the search engine
     *
     * @return boolean
     */
    public Boolean isAccumulatingChanges();

    public Boolean isSearchActive();

    public void setAccumulatingChanges(Boolean accumulateChanges);

    public void setSearchActive(Boolean ignoreSearchEngine);

    /**
     * @return default timzeone for this fortress
     */
    public String getTimeZone();

    public String getLanguageTag();

    String getCode();

    Boolean isEnabled();

    Boolean isSystem();

    String getIndexName();
}