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

package org.flockdata.graph.helper;

import org.flockdata.registration.TagResultBean;
import org.neo4j.graphdb.Node;

/**
 * @author mike
 * @tag
 * @since 3/01/17
 */
public class TagResultBuilder {

  public static TagResultBean make(Node pc) {

    String code = pc.getProperty("code").toString();
    String name = null;
    if (pc.hasProperty("name")) {
      name = pc.getProperty("name").toString();
    }
    String label = CypherHelper.getLabel(pc.getLabels());
    return new TagResultBean(code, name, label);

  }
}
