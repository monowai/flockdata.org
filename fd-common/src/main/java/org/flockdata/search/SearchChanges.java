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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.track.bean.SearchChange;

/**
 * @author mholdsworth
 * @since 23/05/2014
 */
@JsonDeserialize(using = SearchChangesDeserializer.class)
public class SearchChanges {

  Collection<SearchChange> searchChanges = new ArrayList<>();

  public SearchChanges() {
  }

  public SearchChanges(Collection<SearchChange> searchDocuments) {
    this();
    this.searchChanges = searchDocuments;
  }

  public SearchChanges(SearchChange change) {
    searchChanges.add(change);
  }

  public Collection<SearchChange> getChanges() {
    return searchChanges;
  }

  public void addChange(SearchChange change) {
    searchChanges.add(change);
  }
}
