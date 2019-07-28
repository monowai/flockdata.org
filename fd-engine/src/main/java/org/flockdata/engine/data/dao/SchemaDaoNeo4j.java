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
import java.util.Map;
import org.flockdata.data.Company;
import org.flockdata.data.Fortress;
import org.flockdata.helper.CypherHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

  private final Neo4jTemplate template;

  private Logger logger = LoggerFactory.getLogger(SchemaDaoNeo4j.class);

  @Autowired
  public SchemaDaoNeo4j(Neo4jTemplate template) {
    this.template = template;
  }

  Result<Map<String, Object>> runQuery(String statement) {
    return runQuery(statement, null);
  }

  // Just here to minimize the use of the template object
  private Result<Map<String, Object>> runQuery(String statement, HashMap<String, Object> params) {
    return template.query(statement, params);
  }


  public Boolean ensureSystemConstraints(Company company) {

    //logger.debug("Creating system constraints for {} ", company.getName());
    runQuery("create constraint on (t:Country) assert t.key is unique");
    runQuery("create constraint on (t:CountryAlias) assert t.key is unique");
    runQuery("create constraint on (t:State) assert t.key is unique");
    runQuery("create constraint on (t:StateAlias) assert t.key is unique");
    runQuery("create constraint on (t:City) assert t.key is unique");
    runQuery("create constraint on (t:CityAlias) assert t.key is unique");
    runQuery("create constraint on (t:Suburb) assert t.key is unique");
    runQuery("create constraint on (t:SuburbAlias) assert t.key is unique");

    // These are required for SDN 4
    // commented out in SDN3 because the Pojos prefer to create the indexes and constraints

//        runQuery("create constraint on (t:FDCompany) assert t.code is unique");
//        runQuery("create index on :FDCompany(name) ");
//        runQuery("create index on :FDCompany(apiKey) ");
//
//        runQuery("create constraint on (t:Tenant) assert t.code is unique");
//        runQuery("create index on :Tenant(name) ");
//        runQuery("create index on :Tenant(apiKey) ");
//
//
//        runQuery("create constraint on (t:Fortress) assert t.indexName is unique");
//        runQuery("create index on :Fortress(name) ");
//        runQuery("create index on :Fortress(code) ");
//
//        runQuery("create constraint on (t:ChangeEvent) assert t.code is unique");
//
//        runQuery("create constraint on (t:TagLabel) assert t.companyKey is unique");
//
//        runQuery("create constraint on (t:FortressUser) assert t.key is unique");
//        runQuery("create index on :FortressUser(code) ");
//
//        runQuery("create constraint on (t:TxRef) assert t.name is unique");
//
//        runQuery("create index on :Tag(code) ");
//        runQuery("create index on :Tag(key) ");
//
//        runQuery("create constraint on (t:SystemUser) assert t.login is unique");
//        runQuery("create index on :SystemUser(apiKey) ");
//
//        runQuery("create constraint on (t:Profile) assert t.profileKey is unique");
//
//        runQuery("create constraint on (t:Log) assert t.logKey is unique");
//
//        runQuery("create index on :Entity(key) ");
//        runQuery("create constraint on (t:Entity) assert t.callerKeyRef is unique");
//
//        runQuery("create index on :DocumentType(code) ");
//        runQuery("create constraint on (t:DocumentType) assert t.companyKey is unique");
//
//        runQuery("create constraint on (t:Concept) assert t.name is unique");
//
//        runQuery("create index on :EntityLog(sysWhen) ");
//        runQuery("create index on :EntityLog(fortressWhen) ");

    logger.debug("Created system constraints");
    return true;
  }


  @Cacheable(value = "labels", unless = "#result==null")
  // Caches the fact that a constraint has been created
  @Transactional
  public String ensureUniqueIndex(String label) {

    boolean quoted = CypherHelper.requiresQuoting(label);

    String cLabel = quoted ? "`" + label : label;

    runQuery("create constraint on (t:" + cLabel + (quoted ? "`" : "") + ") assert t.key is unique");
    runQuery("create constraint on (t:" + cLabel + "Alias " + (quoted ? "`" : "") + ") assert t.key is unique");
    return label;
  }

  public Collection<String> getAllLabels() {
    return new ArrayList<>();
  }


  @Transactional
  public void purge(Fortress fortress) {
    HashMap<String, Object> params = new HashMap<>();
    params.put("fortId", fortress.getId());

    String modelRelationships = " match (m:Model)-[r:FORTRESS_MODEL]->(fort:Fortress) " +
        "where id(fort)={fortId} " +
        "delete r";
    runQuery(modelRelationships, params);

    String conceptRelationships = "match (fort:Fortress)-[fd:FORTRESS_DOC]-(a:DocType)-[dr]-(o)-[k]-(p)" +
        "where id(fort)={fortId} " +
        "and not (o:Model)  " +
        "delete dr, k, o;";

    // ToDo: Purge Unused Concepts!!
    runQuery(conceptRelationships, params);

  }

}
