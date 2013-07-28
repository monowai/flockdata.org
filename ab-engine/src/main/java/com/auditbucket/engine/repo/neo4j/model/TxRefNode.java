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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.TxRef;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/06/13
 * Time: 9:34 AM
 */
@NodeEntity
public class TxRefNode implements TxRef {

    @GraphId
    private Long id;

    @Fetch
    @RelatedTo(elementClass = CompanyNode.class, type = "txTag", direction = Direction.INCOMING)
    private CompanyNode company;

    @RelatedTo(elementClass = AuditHeaderNode.class, type = "txIncludes")
    private Set<AuditHeader> auditHeaders;

    @Indexed(numeric = false, indexName = "tagName")
    private String name;

    private TxStatus txStatus = TxStatus.TX_CREATED;

    private long txDate;

    public TxStatus getTxStatus() {
        return txStatus;
    }

    public long getTxDate() {
        return txDate;
    }

    protected TxRefNode() {
    }

    public TxRefNode(@NotNull @NotBlank String tagName, @NotNull Company company) {
        this.name = tagName;
        this.company = (CompanyNode) company;
        setStatus(TxStatus.TX_CREATED);

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @JsonIgnore
    public Company getCompany() {
        return company;
    }

    @Override
    @JsonIgnore
    public Set<AuditHeader> getHeaders() {
        return auditHeaders;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return id;
    }

    public TxStatus commit() {
        return setStatus(TxStatus.TX_COMMITTED);
    }

    public TxStatus rollback() {
        return setStatus(TxStatus.TX_ROLLBACK);
    }

    private TxStatus setStatus(TxStatus txStatus) {
        TxStatus previous = this.txStatus;
        this.txStatus = txStatus;
        this.txDate = DateTime.now(DateTimeZone.UTC).getMillis();
        return previous;
    }

    @Override
    public String toString() {
        return "TxRefNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", txStatus=" + txStatus +
                '}';
    }
}
