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
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;

/**
 * @author mholdsworth
 * @tag Contract, DocumentType, Query
 * @since 29/08/2014
 */
public class DocumentResultBean {

  ArrayList<ConceptResultBean> concepts = new ArrayList<>();
  ArrayList<String> segments = null;
  private Long id;
  private String name;
  private Boolean searchEnabled;
  private Boolean storeEnabled;
  private Document.VERSION versionStrategy;

  DocumentResultBean() {
  }

  public DocumentResultBean(Document documentType) {
    this();
    if (documentType != null) {
      this.name = documentType.getName();
      this.id = documentType.getId();
      if (documentType.isSearchEnabled() != null) {
        this.searchEnabled = documentType.isSearchEnabled();
      }
      if (documentType.isStoreEnabled() != null) // Suppressed if it's not enabled
      {
        this.storeEnabled = documentType.isStoreEnabled();
      }
      this.versionStrategy = documentType.getVersionStrategy();

    }
  }

  public DocumentResultBean(Document documentType, Collection<Segment> segments) {
    this(documentType);
    if (segments != null) {
      this.segments = new ArrayList<>(segments.size());
      this.segments.addAll(segments.stream().map(Segment::getCode).collect(Collectors.toList()));
    }
  }

  public DocumentResultBean(Document document, Fortress fortress) {
    this(document);
    if (document.getVersionStrategy() == Document.VERSION.FORTRESS) {
      this.storeEnabled = fortress.isStoreEnabled();
    } else {
      this.storeEnabled = (document.getVersionStrategy() == Document.VERSION.ENABLE);
    }
    if (this.searchEnabled == null) {
      searchEnabled = fortress.isSearchEnabled();
    }

  }

  public String getName() {
    return name;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public ArrayList<ConceptResultBean> getConcepts() {
    return concepts;
  }

  @JsonIgnore
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void add(ConceptResultBean concept) {
    if (concepts == null) {
      concepts = new ArrayList<>();
    }
    concepts.add(concept);
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public ArrayList<String> getSegments() {
    return segments;
  }

  @Override
  public String toString() {
    return "DocumentResultBean{" +
        "id=" + id +
        ", name='" + name + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DocumentResultBean)) {
      return false;
    }

    DocumentResultBean that = (DocumentResultBean) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    return !(name != null ? !name.equals(that.name) : that.name != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  public void addSegment(Segment segment) {
    if (this.segments == null) {
      segments = new ArrayList<>();
    }
    this.segments.add(segment.getCode());
  }

  public Document.VERSION getVersionStrategy() {
    return versionStrategy;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean getSearchEnabled() {
    // Null defaults to the fortress
    return searchEnabled;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean getStoreEnabled() {
    // Null defaults to the fortress
    return storeEnabled;
  }
}
