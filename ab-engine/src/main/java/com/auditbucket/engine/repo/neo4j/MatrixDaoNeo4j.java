package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.dao.MatrixDao;
import com.auditbucket.helper.NeoSyntaxHelper;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResult;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.*;

/**
 * Neo4j matrix queries
 */
@Repository
public class MatrixDaoNeo4j implements MatrixDao {

    @Autowired
    private Neo4jTemplate template;
    private Logger logger = LoggerFactory.getLogger(MatrixDaoNeo4j.class);

    @Override
    public MatrixResults getMatrix(Company company, MatrixInputBean input) {

        // DAT-109 enhancements
        String docIndexes = NeoSyntaxHelper.getLabels("meta", input.getDocuments());
        String conceptsFrom = NeoSyntaxHelper.getConcepts("tag1", input.getConcepts());
        String conceptsTo = NeoSyntaxHelper.getConcepts("tag2", input.getConcepts());
        String fromRlx = NeoSyntaxHelper.getRelationships(input.getFromRlxs());
        String toRlx = NeoSyntaxHelper.getRelationships(input.getToRlxs()); // ToDo: Can we have diff from and too?
        String conceptString = "";
        if (!conceptsFrom.equals(""))
            conceptString = "where ("+ conceptsFrom +")";
        if (!conceptsTo.equals("")) {
            conceptString = conceptString + " and ( "+ conceptsTo +") " ;
        }
        boolean docFilter = !(docIndexes.equals(":_MetaHeader") || docIndexes.equals(""));
        //ToDo: Restrict metaHeaders by Company
        String query = "match (meta:MetaHeader) "+(docFilter? "where  " + docIndexes: "")+
                " with meta " +
                "match t=(tag1:_Tag)-[" + fromRlx + "]-(meta)-[" + toRlx + "]-(tag2:_Tag) " +     // Concepts
                conceptString+
                "with tag1.name as tag1, tag2.name as tag2, count(t) as links " +
                "order by links desc, tag2 " +
                (input.getMinCount()>1?"where links >={linkCount} " : "") +
                "return tag1, collect(tag2) as tag2,  " +
                "collect( links) as occurrenceCount";

        Map<String, Object> params = new HashMap<>();
        params.put("linkCount", input.getMinCount());
        StopWatch watch = new StopWatch(input.toString());
        watch.start("Execute Matrix Query");
        Result<Map<String, Object>> result = template.query(query, params);
        watch.stop();
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
//                MatrixResult inverse = new MatrixResult(mr.getTo(), mr.getFrom(), mr.getCount());
                // Suppress inverse occurrences
//                if (!matrixResults.contains(inverse))
                    matrixResults.add(mr);
            }
        }
        //
        logger.info("Count {}, Performance {}", matrixResults.size(), watch.prettyPrint());
        return new MatrixResults(matrixResults);

    }


}
