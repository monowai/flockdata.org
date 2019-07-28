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
import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.registration.FortressInputBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author mholdsworth
 * @tag Entity, Fortress
 */
@NodeEntity
@TypeAlias("Fortress")
public class FortressNode implements Fortress, Serializable {

  @GraphId
  Long id;

  @Indexed
  private String code;

  @Indexed
  private String name;

  //@Relationship(type = "OWNS", direction = Relationship.INCOMING)
  @RelatedTo(type = "OWNS", direction = Direction.INCOMING)
  @Fetch
  private CompanyNode company;


  @RelatedTo(type = "DEFAULT", direction = Direction.OUTGOING)
  @Fetch
  private FortressSegmentNode defaultSegment;

  private Boolean accumulatingChanges = false;
  private Boolean storeEnabled = Boolean.TRUE;
  private Boolean searchEnabled = true;
  private String timeZone;
  private String languageTag;
  private Boolean system = Boolean.FALSE;
  private Boolean enabled = Boolean.TRUE;

  @Indexed(unique = true)
  private String rootIndex = null;

  protected FortressNode() {
  }

  public FortressNode(FortressInputBean fortressInputBean, Company ownedBy) {
    this();
    getTimeZone();
    getLanguageTag();
    setFortressInput(fortressInputBean);
    setCompany(ownedBy);
    defaultSegment = new FortressSegmentNode(this);

  }

  public String getRootIndex() {
    return rootIndex;
  }

  public void setRootIndex(String rootIndex) {
    this.rootIndex = rootIndex;
  }

  FortressNode setFortressInput(FortressInputBean fortressInputBean) {
    setName(fortressInputBean.getName().trim());
    setSearchEnabled(fortressInputBean.isSearchEnabled());
    system = fortressInputBean.isSystem();
    enabled = fortressInputBean.getEnabled();
    storeEnabled = fortressInputBean.isStoreEnabled();
    if (fortressInputBean.getTimeZone() != null) {
      this.timeZone = fortressInputBean.getTimeZone();
      if (TimeZone.getTimeZone(timeZone) == null) {
        throw new IllegalArgumentException(fortressInputBean.getTimeZone() + " is not a valid TimeZone. If you don't know a timezone to set, leave this null and the system default will be used.");
      }
    }
    if (fortressInputBean.getLanguageTag() != null) {
      this.languageTag = fortressInputBean.getLanguageTag();
    } else {
      getLanguageTag();
    }


    return this;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    this.code = Fortress.code(name);
  }

  @JsonIgnore
  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = (CompanyNode) company;

  }

  public Boolean isStoreEnabled() {
    if (storeEnabled == null) {
      return Boolean.TRUE;
    }
    return storeEnabled;
  }

  @JsonIgnore
  public Boolean isStoreDisabled() {
    return !isStoreEnabled();
  }

  public void setStoreEnabled(Boolean enabled) {
    this.storeEnabled = enabled;
  }

  @JsonIgnore
  public Boolean isAccumulatingChanges() {
    // Reserved for future use
    return accumulatingChanges;
  }

  public Boolean isSearchEnabled() {
    return searchEnabled;
  }

  public FortressNode setSearchEnabled(Boolean searchEnabled) {
    if (searchEnabled != null) {
      this.searchEnabled = searchEnabled;
    }
    return this;
  }

  public void setAccumulatingChanges(Boolean addChanges) {
    this.accumulatingChanges = addChanges;
    if (addChanges) {
      searchEnabled = false;
    }
  }

  @Override
  public String toString() {
    return "Fortress{" +
        "id=" + id +
        ", code='" + code + '\'' +
        ", name='" + name + '\'' +
        ", searchEnabled=" + searchEnabled +
        ", storeEnabled=" + storeEnabled +
        ", rootIndex='" + rootIndex + '\'' +
        '}';
  }

  public String getTimeZone() {
    if (this.timeZone == null) {
      this.timeZone = TimeZone.getDefault().getID();
    }
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public String getLanguageTag() {
    if (this.languageTag == null) {
      this.languageTag = Locale.getDefault().toLanguageTag();
    }
    return this.languageTag;
  }

  public String getCode() {
    return code;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean isSystem() {
    return system;
  }

  @JsonIgnore
  public Segment getDefaultSegment() {
    return defaultSegment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FortressNode)) {
      return false;
    }

    FortressNode that = (FortressNode) o;

    if (code != null ? !code.equals(that.code) : that.code != null) {
      return false;
    }
    if (company != null ? !company.equals(that.company) : that.company != null) {
      return false;
    }
    if (rootIndex != null ? !rootIndex.equals(that.rootIndex) : that.rootIndex != null) {
      return false;
    }
    return !(id != null ? !id.equals(that.id) : that.id != null);

  }

  @Override
  public int hashCode() {
    int result = rootIndex != null ? rootIndex.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (company != null ? company.hashCode() : 0);
    return result;
  }

  public void setSystem(Boolean system) {
    this.system = system;
  }
}
