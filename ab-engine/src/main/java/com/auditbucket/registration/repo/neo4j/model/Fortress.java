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

package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.UUID;

@NodeEntity
public class Fortress implements IFortress {

    private UUID fortressKey;

    @GraphId
    Long id;

    @Indexed(indexName = "fortressName")
    String name;

    @Fetch
    @RelatedTo(type = "owns", direction = Direction.INCOMING)
    Company company;

    private Boolean accumulatingChanges = false;
    private Boolean ignoreSearchEngine = true;

    public Fortress() {
    }

    public Fortress(FortressInputBean fortressInputBean, ICompany ownedBy) {
        setName(fortressInputBean.getName());
        setIgnoreSearchEngine(fortressInputBean.getSearchActive());
        setCompany(ownedBy);
        fortressKey = UUID.randomUUID();
    }

    public String getFortressKey() {
        return fortressKey.toString();
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public ICompany getCompany() {
        return company;
    }

    @Override
    public void setCompany(ICompany ownedBy) {
        this.company = (Company) ownedBy;

    }

    public Boolean isAccumulatingChanges() {
        return accumulatingChanges;
    }

    @Override
    public Boolean isSearchActive() {
        return ignoreSearchEngine;
    }

    public void setIgnoreSearchEngine(Boolean ignoreSearchEngine) {
        if (ignoreSearchEngine != null)
            this.ignoreSearchEngine = ignoreSearchEngine;
    }

    public void setAccumulatingChanges(Boolean addChanges) {
        this.accumulatingChanges = addChanges;
        if (addChanges)
            ignoreSearchEngine = false;
    }

    @Override
    public String toString() {
        return "Fortress{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
