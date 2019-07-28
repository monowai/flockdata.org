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

package org.flockdata.track;

import java.util.Collection;
import org.flockdata.data.Company;
import org.flockdata.registration.TagInputBean;

/**
 * @author mholdsworth
 * @since 19/06/2015
 */
public class TagPayload {

  private String tenant = "";
  private boolean ignoreRelationships;
  private Collection<TagInputBean> tags;
  private Company company;

  TagPayload() {
  }

  public TagPayload(Company company) {
    this();
    this.company = company;
  }

  public String getTenant() {
    return tenant;
  }

  public TagPayload setTenant(String tenant) {
    this.tenant = tenant;
    return this;
  }

  public boolean isIgnoreRelationships() {
    return ignoreRelationships;
  }

  public TagPayload setIgnoreRelationships(boolean ignoreRelationships) {
    this.ignoreRelationships = ignoreRelationships;
    return this;
  }

  public Collection<TagInputBean> getTags() {
    return tags;
  }

  public TagPayload setTags(Collection<TagInputBean> tags) {
    this.tags = tags;
    return this;
  }

  public Company getCompany() {
    return company;
  }

}
