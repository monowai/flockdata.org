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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.registration.FortressResultBean;

/**
 * @author mike
 * @tag
 * @since 3/01/17
 */
public class SegmentResultBean implements Segment {
  private boolean isDefault = false;
  private String code;
  private FortressResultBean fortressResultBean;

  SegmentResultBean() {
  }

  ;

  public SegmentResultBean(Segment segment) {
    this();
    this.code = segment.getCode();
    this.fortressResultBean = new FortressResultBean(segment.getFortress());
    this.isDefault = segment.isDefault();
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  @JsonIgnore
  public Fortress getFortress() {
    return fortressResultBean;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  @Override
  @JsonIgnore
  public String getKey() {
    return null;
  }

  @Override
  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  @Override
  @JsonIgnore
  public Company getCompany() {
    return null;
  }
}
