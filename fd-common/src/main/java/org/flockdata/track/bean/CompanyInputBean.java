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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import org.flockdata.data.Company;

/**
 * @author mike
 * @tag
 * @since 31/12/16
 */
public class CompanyInputBean implements Company {
  private String name;
  private String code;

  public CompanyInputBean() {
    super();
  }

  public CompanyInputBean(String name) {
    this();
    setName(name);
    setCode(name);

  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  @Override
  @JsonIgnore
  public String getApiKey() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CompanyInputBean)) {
      return false;
    }
    CompanyInputBean that = (CompanyInputBean) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, code);
  }
}
