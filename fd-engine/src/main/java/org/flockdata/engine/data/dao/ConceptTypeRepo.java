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

import org.flockdata.engine.data.graph.ConceptNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mholdsworth
 * @tag GraphRepository, Query, Neo4j, Tag
 * @since 16/06/2014
 */
public interface ConceptTypeRepo extends GraphRepository<ConceptNode> {

  @Query(value = "match ( c:Concept) where c.key ={0} return c")
  ConceptNode findByLabel(String key);

//    @Query( value =
//                    "MATCH (company:FDCompany) -[:OWNS]->(fortress:_Fortress)<-[:FORTRESS_DOC]-(doc:_DocType) " +
//                            " -[:HAS_CONCEPT]->(concept:_Concept)" +
//                            "        where id(company)={0} and doc.name in{1}" +
//                            "       return concept")
//    Relationship findRelationship(Company company, String conceptName, String relationship);
}
