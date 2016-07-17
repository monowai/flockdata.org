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

package org.flockdata.search;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Encapsulate as yet not totally defined functionality.
 * Currently supports deleting collection of indexes between fd-engine and fd-search
 * <p>
 * Created by mike on 12/05/16.
 */
//@JsonDeserialize(using = AdminRequestDeserializer.class)
public class AdminRequest {

    private Collection<String> indexesToDelete ;

    @SuppressWarnings("WeakerAccess")
    public AdminRequest() {
    }

    public AdminRequest(String indexToDelete) {
        this();
        setIndexToDelete(indexToDelete);
    }

    public Collection<String> getIndexesToDelete() {
        return indexesToDelete;
    }

    @JsonIgnore
    private void setIndexToDelete(String indexToDelete) {
        indexesToDelete = new ArrayList<>();
        indexesToDelete.add(indexToDelete);

    }

    public void setIndexesToDelete(Collection<String>delete) {
        this.indexesToDelete = delete;
    }

    public void addIndexToDelete(String searchIndexToDelete) {
        if ( this.indexesToDelete == null)
            indexesToDelete = new ArrayList<>();
        indexesToDelete.add(searchIndexToDelete);
    }
}
