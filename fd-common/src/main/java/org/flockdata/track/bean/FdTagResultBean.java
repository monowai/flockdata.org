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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Alias;
import org.flockdata.data.Tag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;

/**
 * Bit hokey - Used to carry a result that includes a server side tag object
 *
 * @author mike
 * @tag
 * @since 1/01/17
 */
public class FdTagResultBean extends TagResultBean {
  @JsonIgnore
  private Tag tag = null;

  private Map<FdTagResultBean, Collection<String>> targets = new HashMap<>();

  FdTagResultBean() {
  }

  public FdTagResultBean(TagInputBean tagInput, Tag startTag, boolean isNew) {
    this(tagInput, startTag);
    this.newTag = isNew;
  }

  public FdTagResultBean(TagInputBean tagInputBean, Tag tag) {

    this(tag);
    if (tag == null) {
      this.code = tagInputBean.getCode();
      this.name = tagInputBean.getName();
    }
    if (tagInputBean != null) {
      this.message = tagInputBean.setServiceMessage();
      this.description = tagInputBean.getDescription();
    }

  }

  public FdTagResultBean(Tag tag) {
    this();
    this.tag = tag;

    if (tag != null) {
      this.newTag = tag.isNew();
      this.code = tag.getCode();
      this.key = tag.getKey();
      this.name = tag.getName();
      this.label = tag.getLabel();
      if (code.equals(name)) {
        name = null;
      }
      this.properties = tag.getProperties();

      for (Alias alias : tag.getAliases()) {
        aliases.add(new AliasResultBean(alias));
      }
    }
  }

  public FdTagResultBean(TagInputBean tagInput) {
    this(tagInput, null);
  }

  public Tag getTag() {
    return tag;
  }

  public void setTag(Tag tag) {
    this.tag = tag;
  }

  @JsonIgnore
  public Map<FdTagResultBean, Collection<String>> getTargets() {
    return targets;
  }

  public void addTargetResult(String rlxName, FdTagResultBean targetTag) {
    Collection<String> relationships = targets.get(targetTag);
    if (relationships == null) {
      relationships = new ArrayList<>();
    }
    relationships.add(rlxName);
    targets.put(targetTag, relationships);
  }
}
