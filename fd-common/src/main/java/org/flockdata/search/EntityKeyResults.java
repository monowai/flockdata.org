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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;

public class EntityKeyResults {

  private Collection<String> results = new ArrayList<>();
  private int startedFrom;

  public EntityKeyResults() {
  }

  public EntityKeyResults(Collection<String> results) {
    this.results = results;
  }


  public Collection<String> getResults() {
    return results;
  }

  public void setResults(Collection<String> results) {
    this.results = results;
  }

  @JsonIgnore
  public long getTotalHits() {
    return results.size();
  }

  public int getStartedFrom() {
    return startedFrom;
  }

  public void setStartedFrom(int startedFrom) {
    this.startedFrom = startedFrom;
  }

  public void add(Object value) {
    if (value == null) {
      return;
    }
    results.add(value.toString());
  }
}
