package org.flockdata.engine.track.service;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.EntityTagOut;
import org.flockdata.model.Tag;
import org.flockdata.track.EntityTagFinder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Assumes you are wanting to get a hierarchy of tags back to a known root
 *
 * Created by mike on 20/08/15.
 */
@Repository
public class ChildToRootFinder implements EntityTagFinder {

    @Autowired
    Neo4jTemplate template;

    @Override
    public Iterable<EntityTag> getEntityTags(Entity entity) {
        String query = getSearchTagQuery(entity);
        Map<String, Object> params = getParams(entity);
        Result<Map<String, Object>> dbResults = template.query(query, params);
        Map<Long,EntityTag>indexed = new HashMap<>();
        Collection<EntityTag> results = new ArrayList<>();
        for (Map<String, Object> row : dbResults) {
            if ( row.containsKey("path")) {
                Iterable<Object> objects = (Iterable<Object>) row.get("path");
                for (Object o : objects) {
                    if ( o instanceof Node) {
                        // Skip
                    } else {
                        Relationship r = (Relationship)o;
                        Node end = r.getEndNode();  // Generally we want this one


                        if (!indexed.containsKey(end.getId())){
                            Tag tag = template.projectTo(end, Tag.class);
                            EntityTag entityTag = new EntityTagOut(entity, tag);
                            entityTag.setRelationship(tag.getLabel());
                            indexed.put(tag.getId(), entityTag);
                            results. add(entityTag);
                        } else if ( !indexed.containsKey(r.getStartNode().getId())){
                            Tag tag = template.projectTo(r.getStartNode(), Tag.class);
                            EntityTag entityTag = new EntityTagOut(entity, tag);
                            indexed.put(tag.getId(), entityTag);
                            entityTag.setRelationship(tag.getLabel());
                            results. add(entityTag);

                        }

                        //System.out.println(tag.getLabel() );

                        //System.out.println(r.getType().name());
                    }

                    //setFromNode(sourceTag, geoBeans, node);

                }
            }

        }
        return results;
    }

    private String getSearchTagQuery(Entity entity) {
        return "match (e:Entity) where id(e) = {id} with e match (e)-[]-(et:Tag) with et match path = ((et)<-[*1..5]-(o:Interest)) return path ";
    }

    private Map<String, Object> getParams(Entity entity) {
        Map<String,Object> args = new HashMap<>();
        args.put("id", entity.getId());
        return args;
    }

    private void process(Map<String, Object> row, ArrayList<EntityTag> results) {
        if ( row.isEmpty())
            return ;



    }

}
