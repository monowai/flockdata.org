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

package org.flockdata.engine.track.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.EntityTagOut;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.helper.CypherHelper;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * Assume the tag connected to the entity is a Term tag with a path to an Interest tag.
 * Neo4j then walks the path to return the path between the two.
 * When it gets back to a term then we are walking the second path etc.
 * <p>
 * Returns only this specific structure, but the approach can be reused for integration
 * <p>
 * In-progress custom version of how to retrieve a hierarchical structure of tags. Can do with
 * figuring out how to allow deployment outside of org.flockdata and ability to customize the
 * path being looked for
 *
 * @author mholdsworth
 * @since 20/08/2015
 */
@Repository
public class TaxonomyTags implements EntityTagFinder {

  private final
  Neo4jTemplate template;

  @Autowired
  public TaxonomyTags(Neo4jTemplate template) {
    this.template = template;
  }

  @Override
  public Iterable<EntityTag> getEntityTags(TrackResultBean trackResultBean) {
    String query = getSearchTagQuery();
    Map<String, Object> params = getParams((EntityNode) trackResultBean.getEntity());
    Result<Map<String, Object>> dbResults = template.query(query, params);
    Map<Long, EntityTag> terms = new HashMap<>();
    Collection<EntityTag> results = new ArrayList<>();

    EntityTag term = null;
    for (Map<String, Object> row : dbResults) {
      if (row.containsKey("node")) {
        //Iterator<Node> objects = ((Node) row.get("node")).iterator();
        Object o = row.get("node");
        if (o instanceof Node) {
          Node node = (Node) o;  // Generally we want this one

          String label = CypherHelper.getLabel(node.getLabels());

          // The tag connected tot he entity
          if (label.equals("Term")) {
            //term = null;
            term = terms.get(node.getId());
            if (term == null) {
              TagNode tag = template.projectTo(node, TagNode.class);

              // ETO is arbitrary
              term = new EntityTagOut((EntityNode) trackResultBean.getEntity(), tag);
              String relationship = "viewed";     // ToDo: Dynamic, not static
              term.setRelationship(relationship);
              terms.put(tag.getId(), term);
              results.add(term);
            }
          } else if (term != null) {
            label = CypherHelper.getLabel(node.getLabels());

            TagNode tag = template.projectTo(node, TagNode.class);
            Collection<Tag> codes = term.getTag().getSubTags(label.toLowerCase());
            if (codes == null) {
              codes = new ArrayList<>();
              ((TagNode) term.getTag()).addSubTag(label.toLowerCase(), codes);
            }
            if (!codes.contains(tag)) {
              codes.add(tag);
            }


          }

        }
      }

    }

    return results;
  }

  @Override
  public EntityTag.TAG_STRUCTURE getTagStructure() {
    return EntityTag.TAG_STRUCTURE.TAXONOMY;
  }

  private String getSearchTagQuery() {
    return "match (e:Entity) where id(e) = {id} with e match (e)-[]-(et:Tag) with et match path = ((et)-[*1..5]->(o:Interest))  with path unwind nodes(path) as node return node;";
  }

  private Map<String, Object> getParams(EntityNode entity) {
    Map<String, Object> args = new HashMap<>();
    args.put("id", entity.getId());
    return args;
  }


}
