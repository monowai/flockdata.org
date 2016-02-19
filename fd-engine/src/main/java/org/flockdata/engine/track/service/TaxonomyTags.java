package org.flockdata.engine.track.service;

import org.flockdata.helper.TagHelper;
import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.EntityTagOut;
import org.flockdata.model.Tag;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.service.EntityService;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Assume the tag connected to the entity is a Term tag with a path to an Interest tag.
 * Neo4j then walks the path to return the path between the two.
 * When it gets back to a term then we are walking the second path etc.
 *
 * Returns only this specific structure, but the approach can be reused for integration

 * In-progress custom version of how to retrieve a hierarchical structure of tags. Can do with
 * figuring out how to allow deployment outside of org.flockdata and ability to customize the
 * path being looked for
 * <p>
 * Created by mike on 20/08/15.
 */
@Repository
public class TaxonomyTags implements EntityTagFinder {

    @Autowired
    Neo4jTemplate template;

    @Override
    public Iterable<EntityTag> getEntityTags(Entity entity) {
        String query = getSearchTagQuery();
        Map<String, Object> params = getParams(entity);
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

                    String label = TagHelper.getLabel(node.getLabels());

                    // The tag connected tot he entity
                    if (label.equals("Term")) {
                        //term = null;
                        term = terms.get(node.getId());
                        if ( term == null ) {
                            Tag tag = template.projectTo(node, Tag.class);

                            // ETO is arbitrary
                            term = new EntityTagOut(entity, tag);
                            String relationship = "viewed";     // ToDo: Dynamic, not static
                            term.setRelationship(relationship);
                            terms.put(tag.getId(), term);
                            results.add(term);
                        }
                    } else if ( term !=null ) {
                        label = TagHelper.getLabel(node.getLabels());

                        Tag tag = template.projectTo(node, Tag.class);
                        Collection<Tag> codes = term.getTag().getSubTags( label.toLowerCase());
                        if (codes == null) {
                            codes = new ArrayList<>();
                            term.getTag().addSubTag( label.toLowerCase(), codes);
                        }
                        if ( !codes.contains(tag)) {
                            codes.add(tag);
                        }


                    }

                }
            }

        }

        return results;
    }

    @Override
    public EntityService.TAG_STRUCTURE getTagStructure() {
        return EntityService.TAG_STRUCTURE.TAXONOMY;
    }

    private String getSearchTagQuery() {
        return "match (e:Entity) where id(e) = {id} with e match (e)-[]-(et:Tag) with et match path = ((et)-[*1..5]->(o:Interest))  with path unwind nodes(path) as node return node;";
    }

    private Map<String, Object> getParams(Entity entity) {
        Map<String, Object> args = new HashMap<>();
        args.put("id", entity.getId());
        return args;
    }


}
