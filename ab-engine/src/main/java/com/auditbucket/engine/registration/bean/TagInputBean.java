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

package com.auditbucket.engine.registration.bean;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ITag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: Mike Holdsworth
 * Date: 26/06/13
 * Time: 1:20 PM
 */
public class TagInputBean implements ITag {

    @NotEmpty
    private ICompany company;
    @NotEmpty
    private String name;

    public TagInputBean() {
    }

    public TagInputBean(String tagName) {
        this();
        this.name = tagName;
    }

    public TagInputBean(ICompany company, String tagName) {
        this(tagName);
        this.company = company;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return null;
    }

    @Override
    public ICompany getCompany() {
        return company;
    }

    @Override
    public void setCompany(ICompany company) {
        this.company = company;
    }
}
