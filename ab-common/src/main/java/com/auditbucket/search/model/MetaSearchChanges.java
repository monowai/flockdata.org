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

package com.auditbucket.search.model;

import com.auditbucket.track.model.SearchChange;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: mike
 * Date: 23/05/14
 * Time: 12:10 PM
 */

public class MetaSearchChanges {
    Collection <MetaSearchChange> searchChanges = new ArrayList<>();
    public MetaSearchChanges(){}
    public MetaSearchChanges(Collection<SearchChange> searchDocuments) {
        this();
        this.searchChanges = (Collection <MetaSearchChange>)(Collection)searchDocuments;
    }
    public Iterable<MetaSearchChange> getChanges() {
        return searchChanges;
    }

    public void addChange(MetaSearchChange change) {
        searchChanges.add(change);
    }
}
