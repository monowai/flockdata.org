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

package org.flockdata.graph.schema;

import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.data.Fortress;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.model.CompanyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Maintains company specific Schema details. Structure of the nodes that FD has established
 * based on Entities, DocumentTypes, Tags and Relationships
 *
 * @author mholdsworth
 * @tag neo4j, Schema, Administration
 * @since 3/04/2014
 */
@Repository
public class SchemaDaoNeo4j {

  private static final Logger logger = LoggerFactory.getLogger(SchemaDaoNeo4j.class);
  private DriverManager template;

  @Autowired
  public SchemaDaoNeo4j(DriverManager template) {
    this.template = template;
  }

//    Util.Result<Map<String, Object>> runQuery(String statement) {
//        return runQuery(statement, null);
//    }
//
//    // Just here to minimize the use of the template object
//    private Result<Map<String, Object>> runQuery(String statement, HashMap<String, Object> params) {
//        return template.query(statement, params);
//    }


  public Boolean ensureSystemConstraints(CompanyNode company) {

    //logger.debug("Creating system constraints for {} ", company.getName());
//        runQuery("create constraint on (t:Country) assert t.key is unique");
//        runQuery("create constraint on (t:CountryAlias) assert t.key is unique");
//        runQuery("create constraint on (t:State) assert t.key is unique");
//        runQuery("create constraint on (t:StateAlias) assert t.key is unique");
//        runQuery("create constraint on (t:City) assert t.key is unique");
//        runQuery("create constraint on (t:CityAlias) assert t.key is unique");
//        runQuery("create constraint on (t:Suburb) assert t.key is unique");
//        runQuery("create constraint on (t:SuburbAlias) assert t.key is unique");


    logger.debug("Created system constraints");
    return true;
  }


  //    @Cacheable(value = "labels", unless = "#result==null") // Caches the fact that a constraint has been created
  public String ensureUniqueIndex(String label) {
//
//        boolean quoted = CypherHelper.requiresQuoting(label);
//
//        String cLabel = quoted ? "`" + label : label;
//
//        runQuery("create constraint on (t:" + cLabel + (quoted ? "`" : "") + ") assert t.key is unique");
//        runQuery("create constraint on (t:" + cLabel + "Alias " + (quoted ? "`" : "") + ") assert t.key is unique");
//        return label;
    return null;
  }

  public Collection<String> getAllLabels() {
    return new ArrayList<>();
  }


  public void purge(Fortress fortress) {
//        HashMap<String, Object> params = new HashMap<>();
//        params.put("fortId", fortress.getId());
//
//        String modelRelationships =" match (m:Model)-[r:FORTRESS_MODEL]->(fort:Fortress) " +
//                "where id(fort)={fortId} " +
//                "delete r";
//        runQuery(modelRelationships, params);
//
//        String conceptRelationships = "match (fort:Fortress)-[fd:FORTRESS_DOC]-(a:DocType)-[dr]-(o)-[k]-(p)" +
//                "where id(fort)={fortId} " +
//                "and not (o:Model)  " +
//                "delete dr, k, o;";
//
//        // ToDo: Purge Unused Concepts!!
//        runQuery(conceptRelationships, params);
//
  }

}
