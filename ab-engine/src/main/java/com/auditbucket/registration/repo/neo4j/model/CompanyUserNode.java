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

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class CompanyUserNode implements CompanyUser {
    @GraphId
    Long id;

    @Indexed(indexName = "companyUserName")
    private String name = null;

    @RelatedTo(elementClass = CompanyNode.class, type = "works", direction = Direction.OUTGOING)
    Company company;

//    @RelatedTo (elementClass = SystemUserNode.class, type ="isA", direction = Direction.INCOMING)
//    private SystemUser systemUser;

    protected CompanyUserNode() {
    }

    public CompanyUserNode(String name, Company company) {
        setName(name);
        setCompany(company);
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Company getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase();
    }

}
