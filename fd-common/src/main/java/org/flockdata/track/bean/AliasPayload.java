/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import org.flockdata.registration.AliasInputBean;

/**
 * Data necessary to create a single Alias for a tag
 *
 * @author mholdsworth
 * @tag Alias, Contract
 * @since 3/07/2015
 */
public class AliasPayload {
  String label;
  AliasInputBean aliasInput;
  Long tagId;

  AliasPayload() {
  }


  public AliasPayload(String label, Long tagId, AliasInputBean aliasInput) {
    this();
    this.label = label;
    this.aliasInput = aliasInput;
    this.tagId = tagId;

  }

  public String getLabel() {
    return label;
  }

  public AliasInputBean getAliasInput() {
    return aliasInput;
  }

  public Long getTagId() {
    return tagId;
  }
}
