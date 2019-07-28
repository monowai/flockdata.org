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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.data.ChangeEvent;

/**
 * View we return to the caller representing an event
 *
 * @author mike
 * @tag
 * @since 3/01/17
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeEventResultBean implements ChangeEvent {
  private String name;
  private String code;

  ChangeEventResultBean() {
  }

  ChangeEventResultBean(ChangeEvent changeEventInterface) {
    this();
    this.name = changeEventInterface.getName();
    this.code = changeEventInterface.getCode();
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }
}
