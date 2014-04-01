/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface MetaDataRepo extends GraphRepository<MetaHeaderNode> {

    @Query(value = "start company=node({1}) " +
            "   MATCH company-[:TX]->txTag " +
            "   where txTag.name = {0} " +
            "return txTag")
    TxRefNode findTxTag(String userTag, Long company);

    @Query(elementClass = MetaHeaderNode.class, value = "start tx=node({0}) " +
            "   MATCH tx-[:AFFECTED]->change<-[:LOGGED]-auditHeader " +
            "return auditHeader")
    Set<MetaHeader> findHeadersByTxRef(Long txRef);

    @Query(elementClass = MetaHeaderNode.class, value =
            "start fortress = node({0}) " +
                    " match fortress-[:TRACKS]->audit " +
                    " return audit ORDER BY audit.dateCreated ASC" +
                    " skip {1} limit 100 ")
    Set<MetaHeader> findHeadersFrom(Long fortressId, Long skip);

    @Query(elementClass = MetaHeaderNode.class, value =
                    " match (fortress)-[:TRACKS]->(header) where id(fortress)={0} " +
                    " and header.callerRef ={1}" +
                    " return header ")
    Iterable<MetaHeader> findByCallerRef(Long fortressId, String callerRef);

//    @Query(elementClass = MetaHeaderNode.class, value =
//            "start fortress = node({0}), docType=node({1}) " +
//                    " match fortress-[:TRACKS]->audit-[:CLASSIFIED_AS]->docType " +
//                    " return audit ORDER BY audit.dateCreated ASC" +
//                    " skip {2} limit 100 ")
//    Set<MetaHeader> findHeadersFrom(Long fortressId, Long docTypeId, Long skipTo);
}
