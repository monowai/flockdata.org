package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.dao.MatrixDao;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Neo4j matrix queries
 *
 */
@Repository
public class MatrixDaoNeo4j implements MatrixDao {

    @Autowired
    Neo4jTemplate template;

    @Override
    public Map<String, Map<String, Long>> getMatrix(Company company, String metaLabel) {
        if ( metaLabel == null )
            metaLabel = "MetaHeader";

        //ToDo: Restrict metaHeaders by Company
        String query = "match (meta:"+metaLabel+") // Things\n" +
                "with meta\n" +
                "match t=(tag1:Person)-[:writer|lead]->(meta)<-[r:contributor|lead|writer]-(tag2:Person) // Concepts\n" +
                "with tag1.name as tag1, tag2.name as tag2, count(t) as links order by links desc, tag2\n" +
                " where links >{linkCount} "+
                "return tag1, collect(tag2) as tag2,  collect( links) as occurrenceCount";

        Map<String,Object>params = new HashMap<>();
        params.put("linkCount", 2);
        Result<Map<String, Object>> result = template.query(query, params);

//        if (!((Result) result).iterator().hasNext())
//            return new ArrayList<>();

        Iterator<Map<String, Object>> rows = result.iterator();

        Map<String, Map<String,Long>> results = new HashMap<>();
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            Collection<String>tag2 = (Collection<String>) row.get("tag2");
            Collection<Long>occ = (Collection<Long>)row.get("occurrenceCount");
            String t = row.get("tag1").toString();
            Map<String,Long>occurrences = new HashMap<>(tag2.size());
            Iterator<String>i1 = tag2.iterator();
            Iterator<Long>i2 = occ.iterator();
            while (i1.hasNext() && i2.hasNext())
                occurrences.put(i1.next(), i2.next());

            results.put(t, occurrences);
        }
        //
        return results;

    }
}
