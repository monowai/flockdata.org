/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.query;

import org.flockdata.dao.MatrixDao;
import org.flockdata.engine.query.endpoint.FdSearchGateway;
import org.flockdata.helper.CypherHelper;
import org.flockdata.query.EdgeResult;
import org.flockdata.query.KeyValue;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.model.Company;
import org.flockdata.search.model.MetaKeyResults;
import org.flockdata.search.model.QueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    FdSearchGateway searchGateway;

    @Override
    public MatrixResults getMatrix(Company company, MatrixInputBean input) {

        // DAT-109 enhancements
        MetaKeyResults metaKeyResults= null;

        if (input.getQueryString() !=null && !input.getQueryString().equals("") )
            metaKeyResults = searchGateway.metaKeys(new QueryParams(company, input));

        String docIndexes = CypherHelper.getLabels("entity", input.getDocuments());
        String conceptsFrom = CypherHelper.getConcepts("tag1", input.getConcepts());
        String conceptsTo = CypherHelper.getConcepts("tag2", input.getConcepts());
        String fromRlx = CypherHelper.getRelationships(input.getFromRlxs());
        String toRlx = CypherHelper.getRelationships(input.getToRlxs());
        String conceptString = "";
        if (!conceptsFrom.equals(""))
            conceptString = "where (" + conceptsFrom + ")";
        if (!conceptsTo.equals("")) {
            conceptString = conceptString + " and ( " + conceptsTo + ") ";
        }
        boolean docFilter = !(docIndexes.equals(":Entity") || docIndexes.equals(""));
        //ToDo: Restrict Entities by Company
        String entityFilter;
        if ( metaKeyResults == null )
            entityFilter =  (docFilter ? "where  " + docIndexes : "");
        else {
            entityFilter = " where entity.metaKey in [";
            boolean first = true;
            for (String s : metaKeyResults.getResults()) {
                if (first ) {
                    entityFilter = entityFilter + "\"" + s + "\"";
                    first = false;
                }
                else
                    entityFilter = entityFilter + ",\""+s+"\"";
            }
            entityFilter = entityFilter +"]";
        }

        String query = "match (entity:Entity) " +entityFilter+
                " with entity " +
                "match t=(tag1:Tag)-[" + fromRlx + "]-(entity)-[" + toRlx + "]-(tag2:Tag) " +     // Concepts
                conceptString +
                "with tag1, id(tag1) as tag1Id, tag2.name as tag2, id(tag2) as tag2Id, count(t) as links " +
//                "order by links desc, tag2 " +
                (input.getMinCount() > 1 ? "where links >={linkCount} " : "") +
                "return coalesce(tag1.name, tag1.code) as tag1, tag1Id, collect(tag2) as tag2, collect(tag2Id) as tag2Ids, " +
                "collect( links) as occurrenceCount";

        Map<String, Object> params = new HashMap<>();
//        if (metaKeyResults !=null )
//            params.put("keys", metaKeyResults.getResults().toArray());

        Collection<KeyValue> labels = new ArrayList<>();

        String conceptFmCol  = "tag1Id";
        String conceptToCol = "tag2Ids";
        // Does the caller want Keys or Values in the result set?
        if ( !input.isByKey()){
            conceptFmCol = "tag1";
            conceptToCol = "tag2";
        }

        params.put("linkCount", input.getMinCount());
        StopWatch watch = new StopWatch(input.toString());
        watch.start("Execute Matrix Query");
        Iterable<Map<String, Object>> result = template.query(query, params);
        watch.stop();
        Iterator<Map<String, Object>> rows = result.iterator();
        Collection<EdgeResult> edgeResults = new ArrayList<>();
        Map<String,Object> uniqueKeys = new HashMap<>();
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            Collection<Object> tag2 = (Collection<Object>) row.get(conceptToCol);
            Collection<Long> occ = (Collection<Long>) row.get("occurrenceCount");
            String conceptFrom = row.get(conceptFmCol).toString();
            KeyValue label = new KeyValue(row.get("tag1Id").toString(), row.get("tag1"));
            labels.add(label);

            Iterator<Object> concept = tag2.iterator();
            Iterator<Long> occurrence = occ.iterator();
            while (concept.hasNext() && occurrence.hasNext()) {
                Object conceptTo = concept.next();
                String conceptKey = conceptFrom + "/"+conceptTo;
                boolean selfRlx = conceptFrom.equals(conceptTo.toString());

                if ( !selfRlx) {
                    String inverseKey = conceptTo + "/" + conceptFrom;
                    if (!uniqueKeys.containsKey(inverseKey) && !uniqueKeys.containsKey(conceptKey)) {
                        EdgeResult mr = new EdgeResult(conceptFrom, conceptTo.toString(), occurrence.next());
                        edgeResults.add(mr);
                        if (input.isReciprocalExcluded())
                            uniqueKeys.put(conceptKey, true);
                    }
                }
            }
        }

        logger.info("Count {}, Performance {}", edgeResults.size(), watch.prettyPrint());
        MatrixResults results =  new MatrixResults(edgeResults);
        results.setNodes(labels);
        return results;

    }


}
