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

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.flockdata.data.Company;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * @author mholdsworth
 * @tag Node, Company
 */
@NodeEntity
@TypeAlias(value = "FDCompany")
@Builder
@Data
public class CompanyNode implements Serializable, Company {
  @GraphId
  Long id;
  @Indexed
  String apiKey;
  @Indexed
  private String name;
  @Indexed(unique = true)
  private String code;

  public Long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "CompanyNode{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", code='" + code + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CompanyNode)) {
      return false;
    }

    CompanyNode that = (CompanyNode) o;

    if (apiKey != null ? !apiKey.equals(that.apiKey) : that.apiKey != null) {
      return false;
    }
    return !(id != null ? !id.equals(that.id) : that.id != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (apiKey != null ? apiKey.hashCode() : 0);
    return result;
  }


}
