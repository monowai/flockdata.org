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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author mholdsworth
 * @tag Contract, Query, Entity
 * @since 29/06/2015
 */
public class EntityResults {

  Collection<TrackResultBean> trackResults = new ArrayList<>();

  public void addResult(TrackResultBean trackResult) {
    if (!trackResults.contains(trackResult)) {
      trackResults.add(trackResult);
    }
  }

  public Collection<TrackResultBean> getTrackResults() {
    return trackResults;
  }


  public TrackResultBean getSingleResult() {
    if (trackResults.isEmpty()) {
      return null;
    }
    return getTrackResults().iterator().next();
  }
}
