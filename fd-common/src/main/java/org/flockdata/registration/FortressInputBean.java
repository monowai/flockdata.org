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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.track.bean.CompanyInputBean;

/**
 * Represents the input to create a fortress
 * Default behaviour is
 * not to accumulate each change as a separate document
 * index document changes in the search engine
 *
 * @tag Payload, Fortress
 * @since 15/06/2013
 */
public class FortressInputBean implements Serializable, Fortress {
  private String name;
  private Boolean searchEnabled = null;
  private Boolean storeEnabled = null;

  private String message = null;
  private String fortressKey = null;
  private String timeZone = TimeZone.getDefault().getID();
  private String languageTag;
  private Boolean enabled = true;
  private Boolean system = false;
  private String code = null;
  private Company company;

  protected FortressInputBean() {
  }

  /**
   * The fortress will *not* ignore the search engine
   * changes will not be accumulated
   *
   * @param name               CompanyNode unique name for the fortress
   * @param ignoreSearchEngine accumulate the changes in the search engine rather than update
   */
  public FortressInputBean(String name, boolean ignoreSearchEngine) {
    //this.accumulateChanges = ignoreSearchEngine;
    this.searchEnabled = !ignoreSearchEngine;
    this.name = name;
    this.code = name;
  }

  public FortressInputBean(String name) {
    this.name = name;
    this.code = name;
  }

  public FortressInputBean(String name, Company company) {
    this(name);
    this.company = company;
  }

  public String getName() {
    return name;
  }

  public FortressInputBean setName(String name) {
    this.name = name;
    if (this.code == null) {
      this.code = name;
    }
    return this;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  @JsonDeserialize(as = CompanyInputBean.class)
  public Company getCompany() {
    return company; // This is derived from the caller
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean isSearchEnabled() {
    return searchEnabled;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  public FortressInputBean setSearchEnabled(Boolean searchEnabled) {
    this.searchEnabled = searchEnabled;
    return this;
  }

  public void setMessage(String message) {
    this.message = message;

  }

  public FortressInputBean setFortressKey(String fortressKey) {
    this.fortressKey = fortressKey;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getTimeZone() {
    return timeZone;
  }

  /**
   * setting an illegal timezone will fall back to GMT.
   *
   * @param timeZone timeZone you require
   * @return this - method chaining
   */
  public FortressInputBean setTimeZone(String timeZone) {
    if (timeZone != null) {
      if (!TimeZone.getTimeZone(timeZone).getID().equals(timeZone)) {
        throw new IllegalArgumentException(timeZone + " was not recognized");
      }
      this.timeZone = timeZone;
    }
    return this;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * IETF BCP 47 language tag to use for this fortress.
   * Defaults to server system default.
   *
   * @return Language tag to be used for Locale conversions
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getLanguageTag() {
    return languageTag;
  }

  public FortressInputBean setLanguageTag(String languageTag) {
    if (languageTag != null) {
      if ("und".equals(Locale.forLanguageTag(languageTag).toLanguageTag())) {
        throw new IllegalArgumentException(languageTag + " was not recognized");
      }
      this.languageTag = languageTag;
    }
    return this;
  }

  @Override
  public String toString() {
    return "FortressInputBean{" +
        "name='" + name + '\'' +
        ", searchEnabled=" + searchEnabled +
        '}';
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean getEnabled() {
    return enabled;
  }

  public FortressInputBean setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean isSystem() {
    return system;
  }

  public FortressInputBean setSystem(Boolean system) {
    this.system = system;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Override
  public Boolean isStoreEnabled() {
    return storeEnabled;
  }

  @Override
  @JsonIgnore
  public String getRootIndex() {
    return null;
  }

  @Override
  @JsonIgnore
  public Segment getDefaultSegment() {
    return null;
  }

  public FortressInputBean setStoreEnabled(Boolean enabled) {
    this.storeEnabled = enabled;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FortressInputBean)) {
      return false;
    }

    FortressInputBean that = (FortressInputBean) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (searchEnabled != null ? !searchEnabled.equals(that.searchEnabled) : that.searchEnabled != null) {
      return false;
    }
    if (storeEnabled != null ? !storeEnabled.equals(that.storeEnabled) : that.storeEnabled != null) {
      return false;
    }
    if (fortressKey != null ? !fortressKey.equals(that.fortressKey) : that.fortressKey != null) {
      return false;
    }
    return code != null ? code.equals(that.code) : that.code == null;

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (searchEnabled != null ? searchEnabled.hashCode() : 0);
    result = 31 * result + (storeEnabled != null ? storeEnabled.hashCode() : 0);
    result = 31 * result + (fortressKey != null ? fortressKey.hashCode() : 0);
    result = 31 * result + (code != null ? code.hashCode() : 0);
    return result;
  }
}
