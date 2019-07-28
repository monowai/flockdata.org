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

package org.flockdata.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.FortressUser;

/**
 * @author mike
 * @tag
 * @since 1/01/17
 */
public class FortressUserResult implements FortressUser {
  private String code;
  private String name;

  FortressUserResult() {

  }

  public FortressUserResult(FortressUser fortressUserInterface) {
    this();
    this.code = fortressUserInterface.getCode();
    this.name = fortressUserInterface.getName();
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return null;
  }

  public String getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }
}
