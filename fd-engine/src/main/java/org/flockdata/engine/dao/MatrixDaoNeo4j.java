/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.engine.dao;

import org.flockdata.dao.MatrixDao;
import org.flockdata.engine.integration.search.EntityKeyQuery;
import org.flockdata.helper.CypherHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.query.*;
import org.flockdata.search.model.EntityKeyResults;
import org.flockdata.search.model.QueryParams;
import org.flockdata.track.service.FortressService;
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

    @Autowired(required = false) // Functional tests don't require gateways
    EntityKeyQuery.EntityKeyGateway searchKeyGateway;

    @Autowired
    FortressService fortressService;
    @Autowired
    SchemaDaoNeo4j schemaDaoNeo4j;

    @Override
    public MatrixResults buildMatrix(Company company, MatrixInputBean input) throws FlockException {

        if ( company == null )
            throw new FlockException("Authorised company could not be identified");

        if (input.getCypher() != null)
            return buildMatrixFromCypher(company, input);

        return buildMatrixWithSearch(company, input);
    }

    public MatrixResults buildMatrixWithSearch(Company company, MatrixInputBean input) throws FlockException {
        // DAT-109 enhancements
        EntityKeyResults entityKeyResults = null;

        if (input.getQueryString() == null)
            input.setQueryString("*");

        if (input.getSampleSize() > 0) {
            if (input.getSampleSize() > 3000)
                input.setSampleSize(3000); // Neo4j can't handle any more in it's where clause
            entityKeyResults = searchKeyGateway.keys(getQueryParams(company, input));
        }

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
        //ToDo: MultiTenant requires restriction by Company

        String entityFilter;
        if (entityKeyResults == null)
            entityFilter = (docFilter ? " where " + docIndexes : "");
        else {

            entityFilter = " where entity.key in [";
            boolean first = true;
            for (String s : entityKeyResults.getResults()) {
                if (first) {
                    entityFilter = entityFilter + "\"" + s + "\"";
                    first = false;
                } else
                    entityFilter = entityFilter + ",\"" + s + "\"";
            }
            entityFilter = entityFilter + "]";
        }
        String sumCol = ""; // Which user defined column against the entity to sum
        String sumVal = ""; // Where the total will be output


        if (input.isSumByCol()) {
            //sumCol = ", sum( entity.`props-value`) as sumValue ";
            sumCol = ", sum( entity.`" + input.getSumColumn() + "`) as sumValue ";
            sumVal = ", collect(sumValue) as sumValues";
        }

        String query = "match (entity:Entity) " + entityFilter +
                " with entity " +
                "match t=(tag1:Tag)-[" + fromRlx + " ]-(entity)-[" + toRlx + " ]-(tag2:Tag) " +     // Concepts
                conceptString +
                "with tag1, id(tag1) as tag1Id, tag2, id(tag2) as tag2Id, count(t) as links " + sumCol +

                (input.getMinCount() > 1 ? "where links >={linkCount} " : "") +
                "return coalesce(tag1.name, tag1.code) as tag1, tag1Id, collect(coalesce(tag2.name, tag2.code)) as tag2, collect(tag2Id) as tag2Ids, " +
                "collect( links) as occurrenceCount " + sumVal;

        Map<String, Object> params = new HashMap<>();
        if (entityKeyResults != null) {
            int count = 0;
            for (String s : entityKeyResults.getResults()) {
                params.put("key" + count++, s);
            }
        }

        Collection<FdNode> labels = new ArrayList<>();

        String conceptFmCol = "tag1Id";
        String conceptToCol = "tag2Ids";
        // Does the caller want Keys or Values in the result set?
        if (!input.isByKey()) {
            conceptFmCol = "tag1";
            conceptToCol = "tag2";
        }

        params.put("linkCount", input.getMinCount());
        StopWatch watch = new StopWatch(input.toString());
        watch.start("Execute Matrix Query");
        Iterable<Map<String, Object>> result = template.query(query, params);
        watch.stop();
        Iterator<Map<String, Object>> rows = result.iterator();
        EdgeResults edgeResults = new EdgeResults();
        Map<String, Object> uniqueKeys = new HashMap<>();
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            Collection<Object> tag2 = (Collection<Object>) row.get(conceptToCol);
            Collection<Object> occ;
            if (row.containsKey("sumValues"))
                occ = (Collection<Object>) row.get("sumValues");
            else
                occ = (Collection<Object>) row.get("occurrenceCount");

            String conceptFrom = row.get(conceptFmCol).toString();

            if (input.isByKey()) {
                // Edges will be indexed by Id. This will set the Name values in to the Node collection
                FdNode source = new FdNode(row.get("tag1Id").toString(), row.get("tag1"));
                Collection<Object> targetIds = (Collection<Object>) row.get("tag2Ids");
                Collection<Object> targetVals = (Collection<Object>) row.get("tag2");
                if (!labels.contains(source))
                    labels.add(source);
                setTargetTags(labels, targetIds, targetVals);
            }

            Iterator<Object> concept = tag2.iterator();
            Iterator<Object> occurrence = occ.iterator();
            while (concept.hasNext() && occurrence.hasNext()) {
                Object conceptTo = concept.next();
                String conceptKey = conceptFrom + "/" + conceptTo;
                boolean selfRlx = conceptFrom.equals(conceptTo.toString());

                if (!selfRlx) {
                    String inverseKey = conceptTo + "/" + conceptFrom;
                    if (!uniqueKeys.containsKey(inverseKey) && !uniqueKeys.containsKey(conceptKey)) {
                        Number value;

                        if (input.isSumByCol())
                            value = Double.parseDouble(occurrence.next().toString());
                        else
                            value = Long.parseLong(occurrence.next().toString());

                        EdgeResult mr = new EdgeResult(conceptFrom, conceptTo.toString(), value);
                        edgeResults.addResult(mr);
                        if (input.isReciprocalExcluded())
                            uniqueKeys.put(conceptKey, true);
                    }
                }
            }
        }

        logger.info("Count {}, Performance {}", edgeResults.get().size(), watch.prettyPrint());
        MatrixResults results = new MatrixResults(edgeResults.get());
        if (!labels.isEmpty())
            results.setNodes(labels);
        if (edgeResults.get().size() > input.getMaxEdges())
            throw new FlockException("Excessive amount of data was requested. Query cancelled " + edgeResults.get().size());

        results.setSampleSize(input.getSampleSize());
        if (entityKeyResults != null)
            results.setTotalHits(entityKeyResults.getTotalHits());
        return results;
    }

    private MatrixResults buildMatrixFromCypher(Company company, MatrixInputBean input) {
        // Make sure no destructive statements
        Result<Map<String, Object>> results = template.query(input.getCypher(), null);
        //match (s:Tag {key:'sports.icc'})-[]-(c:Division)-[r]-(d:Division) return c.code as source, d.code as target ;
        EdgeResults edgeResults = new EdgeResults();
        Map<String, Object> uniqueKeys = new HashMap<>();

        for (Map<String, Object> result : results) {

            String conceptFrom = result.get("source").toString();
            String conceptTo = result.get("target").toString();
            String conceptKey = conceptFrom + "/" + conceptTo;

            boolean selfRlx = conceptFrom.equals(conceptTo);
            if (!selfRlx) {
                String inverseKey = conceptTo + "/" + conceptFrom;
                if (!uniqueKeys.containsKey(inverseKey) && !uniqueKeys.containsKey(conceptKey)) {


                    edgeResults.addResult(new EdgeResult(conceptFrom, conceptTo, 0));
                    if (input.isReciprocalExcluded())
                        uniqueKeys.put(conceptKey, true);


                }
            }
        }


        MatrixResults matricResults = new MatrixResults();
        matricResults.setEdges(edgeResults.get());
        return matricResults;

    }

    public static Collection<? extends FdNode> setTargetTags(Collection<FdNode> labels, Collection<Object> ids, Collection<Object> names) {
        Iterator<Object> tagNames = names.iterator();
        for (Object id : ids) {
            FdNode kv = new FdNode(id.toString(), tagNames.next());
            if (!labels.contains(kv))
                labels.add(kv);
        }
        return labels;
    }

    private QueryParams getQueryParams(Company company, MatrixInputBean input) throws FlockException {
        // Fortresses come in as names - need to resolve to codes:
        QueryParams qp = new QueryParams(company, input);
        for (String fortressName : input.getFortresses()) {
            try {
                Fortress fortress = fortressService.findByName(company, fortressName);
                if (fortress != null)
                    qp.setFortress(fortress.getCode());
            } catch (NotFoundException e) {
                throw new FlockException("Unable to locate fortress " + fortressName);
            }
        }
        return qp;
    }


}
