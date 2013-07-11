/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.engine.repo.neo4j.model.AuditHeader;
import com.auditbucket.engine.repo.neo4j.model.TxRef;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface AuditHeaderRepo extends GraphRepository<AuditHeader> {

    @Query(elementClass = AuditHeader.class, value =
            "start fortress = node({0}), " +
                    " docType = node:documentTypeName(name={1}), " +
                    "   audit = node:callerRef(callerRef  ={2})  " +
                    "   match fortress-[:audit]->audit<-[:classifies]-docType " +
                    "  return audit")
    AuditHeader findByCallerRef(Long fortress, String docType, String callerRef);

    @Query(elementClass = AuditHeader.class, value = "start audit=node:auditKey(auditKey = {0} ) return audit")
    AuditHeader findByUID(String uid);

    @Query(elementClass = AuditHeader.class, value = "start n=node:company({1} ) " +
            "   MATCH company-[:validTag]->ct " +
            "   where ct.name in [{0}]" +
            "return ct")
    Set<IAuditHeader> findByUserTag(String userTag, Long id);

    //ToDo: which is more efficient?
    // start tag =node:tagName(name="6afe75a7-bf69-40c0-aeea-a2d74c0962de")

    @Query(value = "start company=node({1}) " +
            "   MATCH company-[:txTag]->txTag " +
            "   where txTag.name = {0} " +
            "return txTag")
    TxRef findTxTag(String userTag, Long company);


}
