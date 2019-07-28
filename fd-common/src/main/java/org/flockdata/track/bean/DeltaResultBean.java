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

import java.util.Map;

/**
 *
 */
public class DeltaResultBean {
  private Map<String, Object> added;
  private Map<String, Object> changed;
  private Map<String, Object> removed;
  private Map<String, Object> unchanged;

  public Map getAdded() {
    return added;
  }

  public void setAdded(Map<String, Object> added) {
    this.added = added;
  }

  public Map getChanged() {
    return changed;
  }

  public void setChanged(Map<String, Object> changed) {
    this.changed = changed;
  }

  public Map getRemoved() {
    return removed;
  }

  public void setRemoved(Map<String, Object> removed) {
    this.removed = removed;
  }

  public Map<String, Object> getUnchanged() {
    return unchanged;
  }

  public void setUnchanged(Map<String, Object> unchanged) {
    this.unchanged = unchanged;
  }
}
