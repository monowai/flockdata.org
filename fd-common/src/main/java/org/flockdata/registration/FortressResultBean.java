/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.track.bean.SegmentResultBean;

/**
 * @author mholdsworth
 * @since 21/12/2013
 */
public class FortressResultBean implements Fortress, Serializable {
  private String code;
  private String name;
  private String rootIndex;
  private String timeZone;
  private String message;
  private Boolean enabled = Boolean.TRUE;
  private Boolean searchEnabled;
  private Boolean storeEnabled;
  private CompanyResultBean company;
  private SegmentResultBean defaultSegment;
  private boolean system;

  protected FortressResultBean() {

  }

  public FortressResultBean(Fortress fortress) {
    this();
    this.name = fortress.getName();
    this.code = fortress.getCode();
    this.rootIndex = fortress.getRootIndex();
    this.timeZone = fortress.getTimeZone();
    this.enabled = fortress.isEnabled();
    this.system = fortress.isSystem();
    this.searchEnabled = fortress.isSearchEnabled();
    this.company = new CompanyResultBean(fortress.getCompany());
    this.storeEnabled = fortress.isStoreEnabled();
    //this.defaultSegment= new SegmentResultBean(fortress.getDefaultSegment());
  }

  public String getName() {
    return name;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Boolean isStoreEnabled() {
    return storeEnabled;
  }

  @Override
  public Company getCompany() {
    return company;
  }

  @Override
  public Boolean isSearchEnabled() {
    return searchEnabled;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getRootIndex() {
    return rootIndex;
  }

  @Override
  public Segment getDefaultSegment() {
    return defaultSegment;
  }

  public Boolean isSystem() {
    return system;
  }

  public String getMessage() {
    return message;
  }
}
