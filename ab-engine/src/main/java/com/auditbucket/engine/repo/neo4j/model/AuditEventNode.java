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

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.audit.model.AuditEvent;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: Mike Holdsworth
 * Since: 6/09/13
 */
@NodeEntity
public class AuditEventNode implements AuditEvent {

    @GraphId
    private Long id;

    @Indexed(indexName = "eventCode")
    private String code;
    private String name;

    @RelatedTo(type = "COMPANY_EVENT", direction = Direction.INCOMING)
    private CompanyNode company;

    protected AuditEventNode() {
    }

    public AuditEventNode(Company company, String event) {
        this();
        this.company = (CompanyNode) company;
        this.code = event.toLowerCase();
        this.name = event;
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public String getCode() {
        return code;
    }

    @Override
    @JsonIgnore
    public Company getCompany() {
        return company;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
