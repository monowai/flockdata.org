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

package org.flockdata.engine.query.service;

import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Mike Holdsworth
 * @since 24/09/17
 */
@Service
public class EsHelper {

  /**
   * Walks the ES Raw result to extract the _source
   *
   * @param jsonMap all results
   * @return Source Document
   */
  public Map<String, Object> extractData(Map<String, Object> jsonMap) {
    if (!jsonMap.containsKey("hits")) {
      return null;
    }
    Map<String, Object> hitMap = (Map<String, Object>) jsonMap.get("hits");
    if (!hitMap.containsKey("hits")) {
      return null;
    }

    Collection<Map<String, Object>> hits = (Collection<Map<String, Object>>) hitMap.get("hits");
    if (hits.size() == 1) {
      for (Map<String, Object> hit : hits) {
        return (Map<String, Object>) hit.get("_source");
      }
    }
    return null;

  }
}
