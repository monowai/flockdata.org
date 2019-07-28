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

package org.flockdata.track.bean;

import org.flockdata.data.Alias;

/**
 * @author mholdsworth
 * @tag Alias, Contract
 * @since 20/05/2015
 */
public class AliasResultBean {
  private String name;
  private String description;

  AliasResultBean() {
  }

  public AliasResultBean(Alias alias) {
    this();
    this.name = alias.getName();
    this.description = alias.getDescription();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AliasResultBean)) {
      return false;
    }

    AliasResultBean that = (AliasResultBean) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    return !(description != null ? !description.equals(that.description) : that.description != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AliasResultBean{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
