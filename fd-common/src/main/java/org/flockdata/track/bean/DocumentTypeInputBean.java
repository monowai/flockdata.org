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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.data.Document;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;

/**
 * @author mholdsworth
 * @tag Contract, DocumentType
 * @since 10/10/2014
 */
public class DocumentTypeInputBean implements Document {
  private String name;
  private String code;

  private String geoQuery;
  private Document.VERSION versionStrategy = Document.VERSION.FORTRESS;
  private EntityTag.TAG_STRUCTURE tagStructure = EntityTag.TAG_STRUCTURE.DEFAULT;
  private Boolean searchEnabled; // If null default to fortress
  private Boolean storeEnabled; // If null default to fortress
  private Boolean trackEnabled;
  private Fortress fortress;

  DocumentTypeInputBean() {
  }

  public DocumentTypeInputBean(String docName) {
    this();
    if (docName == null || docName.trim().equals("")) {
      throw new IllegalArgumentException("DocumentType name is invalid");
    }
    this.name = docName;
    this.code = docName;
  }

  /**
   * Helps unit testing
   *
   * @param documentResultBean result of a previous request to create
   */
  public DocumentTypeInputBean(DocumentResultBean documentResultBean) {
    this.name = documentResultBean.getName();
  }

  public String getName() {
    return name;
  }

  public DocumentTypeInputBean setName(String name) {
    this.name = name;
    return this;
  }

  public String getCode() {
    return code;
  }

  public DocumentTypeInputBean setCode(String code) {
    this.code = code;
    return this;
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  @Override
  @JsonIgnore
  public Fortress getFortress() {
    return fortress;
  }

  // MKH - Overrides the default geo query path for this DocumentType. VULNERABLE!
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getGeoQuery() {
    // DAT-507
    return geoQuery;
  }

  public DocumentTypeInputBean setGeoQuery(final String geoQuery) {
    this.geoQuery = geoQuery;
    return this;
  }

  public Document.VERSION getVersionStrategy() {
    return versionStrategy;
  }

  public DocumentTypeInputBean setVersionStrategy(Document.VERSION versionStrategy) {
    this.versionStrategy = versionStrategy;
    return this;
  }

  public EntityTag.TAG_STRUCTURE getTagStructure() {
    return tagStructure;
  }

  public DocumentTypeInputBean setTagStructure(EntityTag.TAG_STRUCTURE tagStructure) {
    this.tagStructure = tagStructure;
    return this;
  }

  public DocumentTypeInputBean getName(final String name) {
    this.name = name;
    return this;
  }

  public DocumentTypeInputBean getCode(final String code) {
    this.code = code;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public DocumentTypeInputBean getVersionStrategy(final Document.VERSION versionStrategy) {
    this.versionStrategy = versionStrategy;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean isSearchEnabled() {
    return searchEnabled;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean isStoreEnabled() {
    return storeEnabled;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean isTrackEnabled() {
    return trackEnabled;
  }

  @Override
  public String toString() {
    return "DocumentTypeInputBean{" +
        "name='" + name + '\'' +
        ", code='" + code + '\'' +
        ", tagStructure=" + tagStructure +
        ", versionStrategy=" + versionStrategy +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DocumentTypeInputBean)) {
      return false;
    }

    DocumentTypeInputBean that = (DocumentTypeInputBean) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (code != null ? !code.equals(that.code) : that.code != null) {
      return false;
    }
    if (geoQuery != null ? !geoQuery.equals(that.geoQuery) : that.geoQuery != null) {
      return false;
    }
    if (versionStrategy != that.versionStrategy) {
      return false;
    }
    if (tagStructure != that.tagStructure) {
      return false;
    }
    if (searchEnabled != null ? !searchEnabled.equals(that.searchEnabled) : that.searchEnabled != null) {
      return false;
    }
    return storeEnabled != null ? storeEnabled.equals(that.storeEnabled) : that.storeEnabled == null;

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (geoQuery != null ? geoQuery.hashCode() : 0);
    result = 31 * result + (versionStrategy != null ? versionStrategy.hashCode() : 0);
    result = 31 * result + (tagStructure != null ? tagStructure.hashCode() : 0);
    result = 31 * result + (searchEnabled != null ? searchEnabled.hashCode() : 0);
    result = 31 * result + (storeEnabled != null ? storeEnabled.hashCode() : 0);
    return result;
  }
}
