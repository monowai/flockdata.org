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

package org.flockdata.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.store.StoredContent;

/**
 * Tracks general Log event data that results in a change to an Entity
 *
 * @author mholdsworth
 * @tag Log
 * @since 15/04/2013
 */
public interface Log {
  @JsonIgnore
  Long getId();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  FortressUser getMadeBy();

  String getStorage();

  String getContentType();

  String getChecksum();

  boolean isMocked();

  ChangeEvent getEvent();

  String getComment();

  StoredContent getContent();

  EntityLog getEntityLog();

  Log getPreviousLog();

  String getFileName();
}
