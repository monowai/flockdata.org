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

import java.util.Collection;
import org.flockdata.engine.data.graph.ModelNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * Neo4j, ContentModel, GraphRepository
 *
 * @author mholdsworth
 * @since 3/10/2014
 */
public interface ContentModelRepo extends GraphRepository<ModelNode> {

  @Query(elementClass = ModelNode.class, value = "match (c:FDCompany)<-[COMPANY_MODEL]-(model:Model) where id(c)={0} with c,model " +
      " optional match (model)-[]-(d:DocType)-[]-(f:Fortress) " +
      " return model order by lower(f.name), lower(d.name) " +
      " limit 100 ")
  Collection<ModelNode> findCompanyModels(Long companyId);

  @Query(elementClass = ModelNode.class, value = " match (c:FDCompany)-[:OWNS]->(f:Fortress)-[:FORTRESS_MODEL]-(model:Model)" +
      " where id(c)={0} " +
      " return model " +
      " limit 100 ")
  Collection<ModelNode> findEntityModels(Long companyId);

  @Query(elementClass = ModelNode.class, value = " match (model:Model {key:{0}})" +
      " return model ")
  ModelNode findByKey(String key);

  @Query(elementClass = ModelNode.class, value = " match (f:Fortress)<-[:FORTRESS_MODEL]-(model:Model)-[:DOCUMENT_MODEL]-(d:DocType)" +
      " where id(f)={0} and id(d)={1}" +
      " return model ")
  ModelNode findTagModel(Long fortressId, Long documentId);

  @Query(elementClass = ModelNode.class, value = " match (c:FDCompany)<-[:COMPANY_MODEL]-(model:Model{code:{1}})" +
      " where id(c)={0} " +
      " return model ")
  ModelNode findTagModel(Long companyId, String code);

}
