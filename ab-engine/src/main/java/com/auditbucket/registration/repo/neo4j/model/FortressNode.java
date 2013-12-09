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
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

@NodeEntity
@TypeAlias("ab.Fortress")
public class FortressNode implements Fortress {

    private UUID fortressKey;

    @GraphId
    Long id;

    @Indexed(indexName = "fortressCode")
    private String code;

    @Indexed(indexName = "fortressName")
    private String name;

    @RelatedTo(type = "owns", direction = Direction.INCOMING)
    @Fetch
    private CompanyNode company;

    private Boolean accumulatingChanges = false;
    private Boolean searchActive = true;
    private String timeZone;
    private String languageTag;

    protected FortressNode() {
        getTimeZone();
        getLanguageTag();
    }

    public FortressNode(FortressInputBean fortressInputBean, Company ownedBy) {
        this();
        setName(fortressInputBean.getName());
        setSearchActive(fortressInputBean.getSearchActive());
        setCompany(ownedBy);
        if (fortressInputBean.getTimeZone() != null) {
            this.timeZone = fortressInputBean.getTimeZone();
            if (TimeZone.getTimeZone(timeZone) == null)
                throw new IllegalArgumentException(fortressInputBean.getTimeZone() + " is not a valid TimeZone. If you don't know a timezone to set, leave this null and the system default will be used.");
        }
        if (fortressInputBean.getLanguageTag() != null)
            this.languageTag = fortressInputBean.getLanguageTag();
        else
            getLanguageTag();

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
        this.code = name.toLowerCase().replaceAll("\\s+", "");
    }

    @Override
    @JsonIgnore
    public Company getCompany() {
        return company;
    }

    @Override
    public void setCompany(Company ownedBy) {
        this.company = (CompanyNode) ownedBy;

    }

    @JsonIgnore
    public Boolean isAccumulatingChanges() {
        // Reserved for future use
        return accumulatingChanges;
    }

    @Override
    public Boolean isSearchActive() {
        return searchActive;
    }

    public void setSearchActive(Boolean searchActive) {
        if (searchActive != null)
            this.searchActive = searchActive;
    }

    public void setAccumulatingChanges(Boolean addChanges) {
        this.accumulatingChanges = addChanges;
        if (addChanges)
            searchActive = false;
    }

    @Override
    public String toString() {
        return "FortressNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public String getTimeZone() {
        if (this.timeZone == null)
            this.timeZone = TimeZone.getDefault().getID();
        return timeZone;
    }

    @Override
    public String getLanguageTag() {
        if (this.languageTag == null)
            this.languageTag = Locale.getDefault().toLanguageTag();
        return this.languageTag;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @JsonIgnore
    public String getCode() {
        return code;
    }
}
