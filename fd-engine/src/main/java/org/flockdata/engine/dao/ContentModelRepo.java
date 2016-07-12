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

import org.flockdata.model.Model;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:51 PM
 */
public interface ContentModelRepo extends GraphRepository<Model> {

    @Query( elementClass = Model.class, value="match (c:FDCompany)<-[COMPANY_MODEL]-(model:Model) with c,model " +
            " optional match (model)-[]-(d:DocType)-[]-(f:Fortress) " +
            " where id(c)={0} " +
            " return model order by lower(f.name), lower(d.name) " +
            " limit 100 ")
    Collection<Model> findCompanyModels(Long companyId);

    @Query( elementClass = Model.class, value=" match (c:FDCompany)-[:OWNS]->(f:Fortress)-[:FORTRESS_MODEL]-(model:Model)" +
            " where id(c)={0} " +
            " return model " +
            " limit 100 ")
    Collection<Model> findEntityModels(Long companyId);

    @Query( elementClass = Model.class, value=" match (model:Model {key:{0}})" +
            " return model " )
    Model findByKey(String key);

    @Query( elementClass = Model.class, value=" match (f:Fortress)<-[:FORTRESS_MODEL]-(model:Model)-[:DOCUMENT_MODEL]-(d:DocType)" +
            " where id(f)={0} and id(d)={1}" +
            " return model " )
    Model findTagModel(Long fortressId, Long documentId);

    @Query( elementClass = Model.class, value=" match (c:FDCompany)<-[:COMPANY_MODEL]-(model:Model{code:{1}})" +
            " where id(c)={0} " +
            " return model " )
    Model findTagModel(Long companyId, String code);

}
