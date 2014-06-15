package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.dao.MatrixDao;
import com.auditbucket.helper.NeoSyntaxHelper;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResult;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Neo4j matrix queries
 */
@Repository
public class MatrixDaoNeo4j implements MatrixDao {

    @Autowired
    private Neo4jTemplate template;

    @Override
    public MatrixResults getMatrix(Company company, MatrixInputBean input) {

        String docIndexes = NeoSyntaxHelper.getLabels(input.getDocuments());
        String concepts = NeoSyntaxHelper.getConcepts(input.getConcepts());
        String fromRlx = NeoSyntaxHelper.getRelationships(input.getFromRlxs());
        String toRlx = NeoSyntaxHelper.getRelationships(input.getToRlxs());

        //ToDo: Restrict metaHeaders by Company
        String query = "match (meta" + docIndexes + ") " +
                "with meta\n" +
                "match t=(tag1" + concepts + ")-[" + fromRlx + "]->(meta)<-[" + toRlx + "]-(tag2" + concepts + ") " +     // Concepts
                "with tag1.name as tag1, tag2.name as tag2, count(t) as links " +
                "order by links desc, tag2 " +
                "where links >={linkCount} " +
                "return tag1, collect(tag2) as tag2,  " +
                "collect( links) as occurrenceCount";

        Map<String, Object> params = new HashMap<>();
        params.put("linkCount", input.getMinCount());
        Result<Map<String, Object>> result = template.query(query, params);

        Iterator<Map<String, Object>> rows = result.iterator();
        Collection<MatrixResult> matrixResults = new ArrayList<>();
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            Collection<String> tag2 = (Collection<String>) row.get("tag2");
            Collection<Long> occ = (Collection<Long>) row.get("occurrenceCount");
            String conceptFrom = row.get("tag1").toString();

            Iterator<String> concept = tag2.iterator();
            Iterator<Long> occurrence = occ.iterator();
            while (concept.hasNext() && occurrence.hasNext()) {
                MatrixResult mr = new MatrixResult(conceptFrom, concept.next(), occurrence.next());
                MatrixResult inverse = new MatrixResult(mr.getTo(), mr.getFrom(), mr.getCount());
                // Suppress inverse occurrences
                if (!matrixResults.contains(inverse))
                    matrixResults.add(mr);
            }
        }
        //
        return new MatrixResults(matrixResults);

    }


}
