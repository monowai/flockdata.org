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
import org.flockdata.data.Company;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.integration.search.EntityKeyQuery.EntityKeyGateway;
import org.flockdata.engine.matrix.EdgeResult;
import org.flockdata.engine.matrix.EdgeResults;
import org.flockdata.engine.matrix.FdNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.CypherHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.FlockServiceException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.QueryParams;
import org.flockdata.track.bean.MatrixInputBean;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;
import org.springframework.web.client.ResourceAccessException;

/**
 * Neo4j matrix queries
 *
 * @author mholdsworth
 * @tag Neo4j, Matrix, Query
 */
@Repository
public class MatrixDaoNeo4j implements MatrixDao {

  private final Neo4jTemplate template;
  private final FortressService fortressService;
  private Logger logger = LoggerFactory.getLogger(MatrixDaoNeo4j.class);
  private EntityKeyGateway entityKeyGateway;

  @Autowired
  public MatrixDaoNeo4j(Neo4jTemplate template, FortressService fortressService) {
    this.template = template;
    this.fortressService = fortressService;
  }

  public static Collection<? extends FdNode> setTargetTags(Collection<FdNode> fdNodes, Collection<Object> neoNodes) {
    for (Object neoNode : neoNodes) {
      FdNode fdNode = new FdNode((Node) neoNode);
      if (!fdNodes.contains(fdNode)) {
        fdNodes.add(fdNode);
      }
    }
    return fdNodes;
  }

  @Autowired(required = false)
    // Functional tests don't require gateways
  void setEntityKeyGateway(EntityKeyGateway entityKeyGateway) {
    this.entityKeyGateway = entityKeyGateway;
  }

  @Override
  public MatrixResults buildMatrix(Company company, MatrixInputBean input) throws FlockException {

    if (company == null) {
      throw new FlockException("Authorised company could not be identified");
    }

    if (input.getCypher() != null) {
      return buildMatrixFromCypher(company, input);
    }

    return buildMatrixWithSearch(company, input);
  }

  private MatrixResults buildMatrixWithSearch(Company company, MatrixInputBean input) throws FlockException {
    // DAT-109 enhancements
    EntityKeyResults entityKeyResults = null;

    if (input.getQueryString() == null) {
      input.setQueryString("*");
    }

    if (input.getSampleSize() > 0) {
      if (input.getSampleSize() > input.getMaxEdges()) {
        input.setSampleSize(input.getMaxEdges()); // Neo4j can't handle any more in it's where clause
      }
      try {
        entityKeyResults = entityKeyGateway.keys(getQueryParams(company, input));
      } catch (ResourceAccessException e) {
        throw new FlockServiceException("The search service is not currently available so we cannot execute your query");
      }
    }

    String docIndexes = CypherHelper.getLabels("entity", input.getDocuments());
    String conceptsFrom = CypherHelper.getConcepts("tag1", input.getConcepts());
    String conceptsTo = CypherHelper.getConcepts("tag2", input.getConcepts());

    String fromRlx = CypherHelper.getRelationships(input.getFromRlxs());
    String toRlx = CypherHelper.getRelationships(input.getToRlxs());
    String conceptString = "";
    if (!conceptsFrom.equals("")) {
      conceptString = "where (" + conceptsFrom + ")";
    }
    if (!conceptsTo.equals("")) {
      conceptString = conceptString + " and ( " + conceptsTo + ") ";
    }
    boolean docFilter = !(docIndexes.equals(":Entity") || docIndexes.equals(""));
    //ToDo: MultiTenant requires restriction by Company
    String entityFilter;
    if (entityKeyResults == null) {
      entityFilter = (docFilter ? " where " + docIndexes : "");
    } else {

      entityFilter = " where entity.key in {0}";
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
        "with tag1, tag2, count(t) as links " + sumCol +

        (input.getMinCount() > 1 ? "where links >={linkCount} " : "") +
        "return tag1, collect(tag2) as tag2, " +
        "collect( links) as occurrenceCount " + sumVal;

    Map<String, Object> params = new HashMap<>();

    if (entityKeyResults != null) {
      params.put("0", entityKeyResults.getResults());
    }
    params.put("linkCount", input.getMinCount());

    Collection<FdNode> nodes = new ArrayList<>();

    String conceptFmCol = "tag1";
    String conceptToCol = "tag2";
    // Does the caller want Keys or Values in the result set?
    if (!input.isByKey()) {
      conceptFmCol = "tag1";
      conceptToCol = "tag2";
    }

    StopWatch watch = new StopWatch(input.toString());
    watch.start("Execute Matrix Query");
    Iterable<Map<String, Object>> result = template.query(query, params);
    watch.stop();
    Iterator<Map<String, Object>> rows = result.iterator();
    EdgeResults edgeResults = new EdgeResults();
    Map<String, Object> uniqueKeys = new HashMap<>();
    while (rows.hasNext()) {
      if (edgeResults.getEdgeResults().size() > input.getMaxEdges()) {
        String message = "Excessive amount of data was requested " + edgeResults.getEdgeResults().size() + " vs. limit of " + input.getMaxEdges() + ". Try increasing the minimum occurrences, applying a search filter or reducing the sample size";
        logger.error(message);
        throw new FlockException(message);
      }

      Map<String, Object> row = rows.next();
      Collection<Object> tag2 = (Collection<Object>) row.get(conceptToCol);
      Collection<Object> occ;
      if (row.containsKey("sumValues")) {
        occ = (Collection<Object>) row.get("sumValues");
      } else {
        occ = (Collection<Object>) row.get("occurrenceCount");
      }

      Node conceptFrom = (Node) row.get(conceptFmCol);

      if (input.isByKey()) {
        // Edges will be indexed by Id. This will set the Name values in to the Node collection
        FdNode source = new FdNode(conceptFrom);
        //Collection<Object> targetIds = (Collection<Object>) row.get("tag2Ids");
        Collection<Object> targetVals = (Collection<Object>) row.get("tag2");
        if (!nodes.contains(source)) {
          nodes.add(source);
        }
        setTargetTags(nodes, targetVals);
      }

      Iterator<Object> concept = tag2.iterator();
      Iterator<Object> occurrence = occ.iterator();
      while (concept.hasNext() && occurrence.hasNext()) {
        Node conceptTo = (Node) concept.next();
        String conceptKey = conceptFrom.getId() + "/" + conceptTo.getId();
        boolean selfRlx = conceptFrom.getId() == conceptTo.getId();

        if (!selfRlx) {
          String inverseKey = conceptTo.getId() + "/" + conceptFrom.getId();
          if (!uniqueKeys.containsKey(inverseKey) && !uniqueKeys.containsKey(conceptKey)) {
            Number value;

            if (input.isSumByCol()) {
              value = Double.parseDouble(occurrence.next().toString());
            } else {
              value = Long.parseLong(occurrence.next().toString());
            }

            edgeResults.addResult(new EdgeResult(conceptFrom, conceptTo, value));
            if (input.isReciprocalExcluded()) {
              uniqueKeys.put(conceptKey, true);
            }
          }
        }
      }
    }

    logger.info("Count {}, Performance {}", edgeResults.getEdgeResults().size(), watch.prettyPrint());
    MatrixResults results = new MatrixResults(edgeResults);
    results.setNodes(nodes);

    results.setSampleSize(input.getSampleSize());
    if (entityKeyResults != null) {
      results.setTotalHits(entityKeyResults.getTotalHits());
    }
    return results;
  }

  private MatrixResults buildMatrixFromCypher(Company company, MatrixInputBean input) {
    // ToDo: Make sure no destructive statements
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
          if (input.isReciprocalExcluded()) {
            uniqueKeys.put(conceptKey, true);
          }


        }
      }
    }


    return new MatrixResults(edgeResults);

  }

  private QueryParams getQueryParams(Company company, MatrixInputBean input) throws FlockException {
    // Fortresses come in as names - need to resolve to codes:
    QueryParams qp = new QueryParams(company, input);
    for (String fortressName : input.getFortresses()) {
      try {
        FortressNode fortress = fortressService.findByName(company, fortressName);
        if (fortress != null) {
          qp.setFortress(fortress.getCode());
        }
      } catch (NotFoundException e) {
        throw new FlockException("Unable to locate fortress " + fortressName);
      }
    }
    return qp;
  }


}
