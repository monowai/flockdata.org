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

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:02 AM
 */
@NodeEntity
@TypeAlias("DocType")
public class DocumentTypeNode implements DocumentType {

    @GraphId
    Long id;

    @RelatedTo(elementClass = FortressNode.class, type = "FORTRESS_DOC", direction = Direction.INCOMING)
    private Fortress fortress;

    private String name;

    private String code;

    @Indexed(unique = true)
    private String companyKey;

    protected DocumentTypeNode() {
    }

    public DocumentTypeNode(Fortress fortress, String documentType) {
        this();
        this.name = documentType;
        this.code = documentType.toLowerCase().replaceAll("\\s", "");
        this.fortress = fortress;
        this.companyKey = fortress.getCompany().getId() + "." + documentType.toLowerCase().replaceAll("\\s", "");

    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public Fortress getFortress() {
        return fortress;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public String getCode() {
        return code;
    }

    @JsonIgnore
    /**
     * used to create a unique key index for a company+docType combo
     */
    public String getCompanyKey() {
        return companyKey;
    }
}
