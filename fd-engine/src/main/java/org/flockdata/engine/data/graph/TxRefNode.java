/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Company;
import org.flockdata.data.TxRef;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author mholdsworth
 * @since 15/06/2013
 */
@NodeEntity
@TypeAlias("TxRef")
public class TxRefNode implements TxRef {

  @GraphId
  private Long id;

  //@Relationship( type = "TX", direction = Relationship.INCOMING)
  @Fetch
  @RelatedTo(type = "TX", direction = Direction.INCOMING)
  private CompanyNode company;

  @Indexed
  private String name;

  private TxRef.TxStatus txStatus = TxRef.TxStatus.TX_CREATED;

  private long txDate;

  protected TxRefNode() {
  }

  public TxRefNode(String tagName, CompanyNode company) {
    this.name = tagName;
    this.company = company;
    setStatus(TxStatus.TX_CREATED);

  }

  @Override
  public TxRef.TxStatus getTxStatus() {
    return txStatus;
  }

  @Override
  public long getTxDate() {
    return txDate;
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
