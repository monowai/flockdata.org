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

package org.flockdata.engine.data.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.flockdata.data.Tag;
import org.flockdata.helper.TagResultBuilder;
import org.flockdata.registration.TagResultBean;
import org.neo4j.cypher.internal.compiler.v2_3.commands.expressions.PathImpl;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Tag, Neo4j
 * @since 28/12/2015
 */
@Service
public class TagPathDao {
  private final Neo4jTemplate template;


  private Logger logger = LoggerFactory.getLogger(TagPathDao.class);

  @Autowired
  public TagPathDao(Neo4jTemplate template) {
    this.template = template;
  }

  public Collection<Map<String, Object>> getPaths(Tag tag, int length, String label) {
    String query = "match p=(t) -[*.." + length + "]->(targetTag:`" + label + "`) where id(t)= {0}   return p";
    Map<String, Object> params = new HashMap<>();
    params.put("0", tag.getId());
    Collection<Map<String, Object>> results = new ArrayList<>();
    Result<Map<String, Object>> qResults = template.query(query, params);
    for (Map<String, Object> row : qResults) {
      PathImpl paths = (PathImpl) row.get("p");

      Map<String, Object> path = new HashMap<>();
      results.add(path);
      Integer pathCount = 0;
      TagResultBean root = null;
      String relationship = null;
      for (Iterator<PropertyContainer> iterator = paths.iterator(); iterator.hasNext(); ) {
        PropertyContainer pc = iterator.next();
        if (pc instanceof Node) {
          TagResultBean tagResultBean = TagResultBuilder.make((Node) pc);
          path.put((pathCount++).toString(), tagResultBean);
          if (iterator.hasNext()) {
            Relationship rel = (Relationship) iterator.next(); // Get the relationship
            relationship = rel.getType().name();
            tagResultBean.setRelationship(relationship);
//                        logger.info("Ignoring {} for {}, tag {}", rel.getType().name(), pathCount--, tagResultBean);
          }
        } else {
          logger.info("What the hell am I doing here?");
        }


      }
    }
    return results;
  }

}
