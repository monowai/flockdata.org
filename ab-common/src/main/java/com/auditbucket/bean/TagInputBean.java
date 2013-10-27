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

package com.auditbucket.bean;

import com.auditbucket.registration.model.Company;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 1:20 PM
 */
public class TagInputBean {

    @NotEmpty
    private Company company;
    @NotEmpty
    private String name;

    private TagInputBean() {
    }

    public TagInputBean(String tagName) {
        this();
        this.name = tagName;
    }

    public TagInputBean(Company company, String tagName) {
        this(tagName);
        this.company = company;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Long getId() {
        return null;
    }

    public String getCode() {
        return null;
    }

    public Company getCompany() {
        return company;
    }

}
