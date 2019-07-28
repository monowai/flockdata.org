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

package org.flockdata.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Company;

/**
 * Callers view of an FD Company
 *
 * @author mike
 * @tag Contract
 * @tag
 * @since 1/01/17
 */
public class CompanyResultBean implements Company {

  private String apiKey;
  private String name;

  CompanyResultBean() {

  }

  public CompanyResultBean(Company company) {
    this();
    this.name = company.getName();
    this.apiKey = company.getApiKey();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @JsonIgnore
  public String getCode() {
    return null;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  @Override
  @JsonIgnore
  public String getApiKey() {
    return apiKey;
  }
}
