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
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.Alias;
import org.flockdata.data.Tag;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

/**
 * @author mholdsworth
 * @tag Node, Tag, EntityTag
 * @since 15/06/2013
 */
@NodeEntity // Only in place to support projection
@TypeAlias("Tag")
public class TagNode implements Tag {

  @GraphId
  Long id;
  @Transient
  Map<String, Collection<Tag>> subTags = new HashMap<>();
  @Indexed
  private String key;
  @Indexed
  private String code;
  @Labels
  private ArrayList<String> labels = new ArrayList<>();
  //@Relationship(type = "HAS_ALIAS")
  @RelatedTo(elementClass = AliasNode.class, type = "HAS_ALIAS")
  private Set<Alias> aliases = new HashSet<>();
  private DynamicProperties props = new DynamicPropertiesContainer();
  private String name;
  @Transient
  private Boolean isNew = false;

  protected TagNode() {
    labels.add("Tag");
    labels.add("_Tag"); // Required for SDN 3.x
  }

  public TagNode(TagInputBean tagInput) {
    this();
    setName(tagInput.getName());
    if (tagInput.getCode() == null) {
      setCode(getName());
    } else {
      setCode(tagInput.getCode());
    }

    this.key = TagHelper.parseKey(tagInput);
    if (tagInput.hasTagProperties()) {
      props = new DynamicPropertiesContainer(tagInput.getProperties());
    }
    String label = tagInput.getLabel();
    if (label != null) {
      this.labels.add(label);
    }
  }

  // Called only when creating a new Tag
  public TagNode(TagInputBean tagInput, String tagLabel) {
    this(tagInput);
    if (!labels.contains(tagLabel)) {
      labels.add(tagLabel);
    }
    isNew = true;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Long getId() {
    return id;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setId(Long id) {
    this.id = id;

  }

  @Override
  public String toString() {
    return "TagNode{" +
        "id=" + id +
        ", label='" + getLabel() + '\'' +
        ", code='" + code + '\'' +
        ", key='" + key + '\'' +
        '}';
  }

  @Override
  public String getKey() {
    return key;
  }

  public Object getProperty(String name) {
    return props.getProperty(name);
  }

  @Override
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public Map<String, Object> getProperties() {
    return props.asMap();
  }

  @Override
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public String getLabel() {
    return TagHelper.getLabel(labels);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TagNode)) {
      return false;
    }

    TagNode tagNode = (TagNode) o;

    if (id != null ? !id.equals(tagNode.id) : tagNode.id != null) {
      return false;
    }
    if (code != null ? !code.equals(tagNode.code) : tagNode.code != null) {
      return false;
    }
    if (key != null ? !key.equals(tagNode.key) : tagNode.key != null) {
      return false;
    }
    return !(name != null ? !name.equals(tagNode.name) : tagNode.name != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (key != null ? key.hashCode() : 0);
    result = 31 * result + (code != null ? code.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  public void addAlias(Alias newAlias) {
    aliases.add(newAlias);
  }

  @Override
  public Set<Alias> getAliases() {
    return aliases;
  }

  @Override
  @JsonIgnore
  public Boolean isNew() {
    return isNew;
  }

  @Override
  public void addProperty(String key, Object property) {
    props.setProperty(key, property);
    //getProperties().put(key, property);
  }

  @JsonIgnore
  public void addSubTag(String key, Collection<Tag> o) {
    subTags.put(key, o);
  }

  @Override
  @JsonIgnore
  public Map<String, Collection<Tag>> getSubTags() {
    return subTags;
  }

  @Override
  @JsonIgnore
  public Collection<Tag> getSubTags(String key) {
    return subTags.get(key);
  }

  @Override
  public boolean hasSubTags() {
    return (subTags != null && !subTags.isEmpty());
  }

  @Override
  public boolean hasProperties() {
    return (getProperties() != null && !getProperties().isEmpty());
  }
}
