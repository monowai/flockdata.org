/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
