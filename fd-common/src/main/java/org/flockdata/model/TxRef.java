/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 9:11 AM
 */
@NodeEntity
@TypeAlias("TxRef")
public class TxRef {

    @GraphId
    private Long id;

    //@Relationship( type = "TX", direction = Relationship.INCOMING)
    @Fetch
    @RelatedTo( type = "TX", direction = Direction.INCOMING)
    private Company company;

//    @RelatedTo( type = "AFFECTED")
//    private Set<EntityNode> entities;

    @Indexed
    private String name;

    private TxRef.TxStatus txStatus = TxRef.TxStatus.TX_CREATED;

    private long txDate;

    public TxRef.TxStatus getTxStatus() {
        return txStatus;
    }

    public long getTxDate() {
        return txDate;
    }

    protected TxRef() {
    }

    public TxRef( String tagName, Company company) {
        this.name = tagName;
        this.company = company;
        setStatus(TxStatus.TX_CREATED);

    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public Company getCompany() {
        return company;
    }

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

    public enum TxStatus {
        TX_CREATED, TX_ROLLBACK, TX_COMMITTED
    }
}
