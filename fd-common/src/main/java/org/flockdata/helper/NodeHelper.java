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

package org.flockdata.helper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Centralise node identification
 *
 * @author mholdsworth
 * @since 16/08/2016
 */
public class NodeHelper {

  private static Collection<String> internalLabels = new ArrayList<>();

  static {
    internalLabels.add("tag"); // FD concept
    internalLabels.add("_tag"); // SDN prefix
    internalLabels.add("entity"); // FD entity
    internalLabels.add("_entity"); // SDN prefix
  }

  public static boolean isInternalLabel(String label) {
    return internalLabels.contains(label.toLowerCase());
  }
}
