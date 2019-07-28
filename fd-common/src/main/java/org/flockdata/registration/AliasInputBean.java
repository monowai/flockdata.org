/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.registration;

/**
 * Payload representing alias attributes for a tag.
 * The AliasInput can be set in to a ColumnDefinitionInputBean or TagInputBean
 *
 * @author mholdsworth
 * @since 12/02/2015
 */
public class AliasInputBean {
  private String code;
  private String description;

  public AliasInputBean() {
  }

  public AliasInputBean(String code) {
    this();
    this.code = code;
  }

  public AliasInputBean(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public AliasInputBean setCode(String code) {
    this.code = code;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public AliasInputBean setDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AliasInputBean)) {
      return false;
    }

    AliasInputBean that = (AliasInputBean) o;

    return code.equals(that.code);

  }

  @Override
  public int hashCode() {
    int result = code.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "AliasInputBean{" +
        "code='" + code + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
