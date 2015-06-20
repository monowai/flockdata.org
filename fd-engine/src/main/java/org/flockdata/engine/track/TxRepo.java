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

package org.flockdata.engine.track;

import org.flockdata.engine.track.model.TxRefNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface TxRepo extends GraphRepository<TxRefNode> {

    @Query(value = "   MATCH (company:FDCompany)-[:TX]->(txTag:TxRef) " +
            "   where id(company) = {1} and txTag.name = {0} " +
            "return txTag")
    TxRefNode findTxTag(String userTag, Long company);

}
