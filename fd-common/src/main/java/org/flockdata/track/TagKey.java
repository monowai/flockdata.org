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

package org.flockdata.track;

import org.flockdata.data.Tag;

/**
 * Used in the location and caching of a Tag.
 * The properties that can are used to uniquely identify a tag
 *
 * @author mholdsworth
 * @since 25/03/2016
 */
public class TagKey {
  String label;
  String prefix;
  String code;
  Tag tag; // Carried in to help with cache eviction

  public TagKey(String label, String prefix, String code) {
    this.label = label;
    this.prefix = prefix;
    this.code = code;
  }

  public TagKey(Tag tag) {
    this.label = tag.getLabel();
    this.code = tag.getCode();
    this.tag = tag;
    // Figure out the prefix
    //this.prefix = tag.ge
  }

  public Tag getTag() {
    return tag;
  }

  public String getLabel() {
    return label;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getCode() {
    return code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TagKey)) {
      return false;
    }

    TagKey tagKey = (TagKey) o;

    if (label != null ? !label.equals(tagKey.label) : tagKey.label != null) {
      return false;
    }
    if (prefix != null ? !prefix.equals(tagKey.prefix) : tagKey.prefix != null) {
      return false;
    }
    return code != null ? code.equals(tagKey.code) : tagKey.code == null;

  }

  @Override
  public int hashCode() {
    int result = label != null ? label.hashCode() : 0;
    result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
    result = 31 * result + (code != null ? code.hashCode() : 0);
    return result;
  }
}
