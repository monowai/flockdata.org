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

package org.flockdata.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Tag;
import org.flockdata.track.bean.AliasResultBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.SearchChange;

/**
 * Represents data about a tag that requires indexing
 *
 * @author mholdsworth
 * @since 15/05/2016
 */
public class TagSearchChange implements SearchChange {

  private Map<String, Object> props = new HashMap<>(); // User defined properties
  private String code;
  private String name = null;
  private String type = Type.TAG.name();
  private String documentType;
  private String key = null;
  private String description;
  private Long id;
  private Long logId = null;
  private boolean delete = false;
  private String indexName;
  private boolean forceReindex = false;
  private boolean replyRequired = false;
  private String searchKey = null; // how to find this object in an es index

  private Collection<AliasResultBean> aliases = new ArrayList<>();
  private EntityKeyBean parent = null;
  private EntityTag.TAG_STRUCTURE tagStructure = null;

  TagSearchChange() {

  }

  public TagSearchChange(String indexName, Tag tag) {
    this();
    this.id = tag.getId();
    this.code = tag.getCode();
    this.name = tag.getName();
    this.documentType = tag.getLabel();
    this.key = tag.getKey();
    this.indexName = indexName;
    this.searchKey = key;
    if (tag.hasProperties()) {
      this.props = tag.getProperties();
    }
    aliases.addAll(tag.getAliases().stream().map(AliasResultBean::new).collect(Collectors.toList()));
  }

  @Override
  @JsonIgnore
  public boolean isType(Type type) {
    return getType().equals(type.name());
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getSearchKey() {
    return searchKey;
  }

  @Override
  public void setSearchKey(String key) {
    this.searchKey = key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Long getLogId() {
    return logId;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getIndexName() {
    return indexName;
  }

  @Override
  @JsonIgnore
  public String getFortressName() {
    return null;
  }

  @Override
  public String getDocumentType() {
    return documentType;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public TagSearchChange setDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public boolean isReplyRequired() {
    return replyRequired;
  }

  @Override
  public void setReplyRequired(boolean required) {

  }

  @Override
  public boolean isForceReindex() {
    return forceReindex;
  }

  @Override
  public Boolean isDelete() {
    return delete;
  }

  @Override
  public Map<String, Object> getProps() {
    return props;
  }

  @Override
  @JsonIgnore
  public EntityTag.TAG_STRUCTURE getTagStructure() {
    return tagStructure;
  }

  @Override
  public EntityKeyBean getParent() {
    return parent;
  }

  public Collection<AliasResultBean> getAliases() {
    return aliases;
  }
}
