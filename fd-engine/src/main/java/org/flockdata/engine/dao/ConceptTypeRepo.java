/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.engine.dao;

import org.flockdata.model.Concept;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 10:33 AM
 */
public interface ConceptTypeRepo extends GraphRepository<Concept> {

//    @Query( value =
//                    "MATCH (company:FDCompany) -[:OWNS]->(fortress:_Fortress)<-[:FORTRESS_DOC]-(doc:_DocType) " +
//                            " -[:HAS_CONCEPT]->(concept:_Concept)" +
//                            "        where id(company)={0} and doc.name in{1}" +
//                            "       return concept")
//    Relationship findRelationship(Company company, String conceptName, String relationship);
}
