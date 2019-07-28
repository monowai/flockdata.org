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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.flockdata.registration.TagResultBean;

/**
 * Concept represents either an Entity or a Tag. It is used to record the structure of data passed to the service
 *
 * @author mholdsworth
 * @tag Contract, Concept, Entity, Tag
 * @since 19/06/2014
 */
public class ConceptInputBean {
  Collection<String> relationships = new HashSet<>();
  private String name;
  private boolean tag = true;
  private String description;

  private ConceptInputBean() {
  }

  public ConceptInputBean(String name) {
    this();
    this.name = name;
  }

  public ConceptInputBean(TagResultBean tagResultBean) {
    this(tagResultBean.getLabel());
    this.description = tagResultBean.getDescription();
  }

  public String getName() {
    return name;
  }

  public ConceptInputBean setName(String name) {
    this.name = name;
    return this;
  }

  public Collection<String> getRelationships() {
    return relationships;
  }

  public ConceptInputBean setRelationships(Set<String> relationships) {
    for (String relationship : relationships) {
      if (!this.relationships.contains(relationship)) {
        this.relationships.add(relationship);
      }
    }
    return this;
  }

  /**
   * If not a Tag then it is an Entity
   *
   * @return true if it's a tag
   */
  public boolean isTag() {
    return tag;
  }

  /**
   * Does this concept represent a tag or an entity?
   *
   * @param tag true==tag, false==entity
   * @return this
   */
  public ConceptInputBean setTag(boolean tag) {
    this.tag = tag;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConceptInputBean)) {
      return false;
    }

    ConceptInputBean that = (ConceptInputBean) o;

    if (tag != that.tag) {
      return false;
    }
    return !(name != null ? !name.equals(that.name) : that.name != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (tag ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ConceptInputBean{" +
        "name='" + name + '\'' +
        '}';
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
