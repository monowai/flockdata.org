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

package org.flockdata.search;

import java.util.Map;
import org.flockdata.data.AbstractEntityTag;
import org.flockdata.data.Entity;
import org.flockdata.data.Tag;

/**
 * @author mholdsworth
 * @since 22/08/2015
 */
public class SubTag extends AbstractEntityTag {
  private Long id;
  private Tag tag;
  private String relationship;
  private Map<String, Object> properties;

  public SubTag() {
  }

  public SubTag(Tag subTag, String label) {
    this();
    this.tag = subTag;
    this.relationship = label;
  }

  public SubTag(Tag subTag, String label, Map<String, Object> properties) {
    this(subTag, label);
    this.properties = properties;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public Entity getEntity() {
    return null;
  }

  @Override
  public Tag getTag() {
    return tag;
  }

  @Override
  public String getRelationship() {
    return relationship;
  }

  @Override
  public void setRelationship(String name) {
    this.relationship = name;
  }

  @Override
  public Boolean isReversed() {
    return false;
  }

  public Object getProperty(String key) {
    if (properties == null) {
      return null;
    }
    return properties.get(key);
  }

  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }


}
