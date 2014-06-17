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

import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.track.model.DocumentType;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:20 AM
 */
public interface DocumentTypeRepo extends GraphRepository<DocumentTypeNode> {
    @Query(elementClass = DocumentTypeNode.class,
            value =
                    "MATCH fortress<-[:FORTRESS_DOC]-documentType " +
                            "        where id(fortress)={0} and documentType.code ={1}" +
                            "       return documentType")
    DocumentTypeNode findFortressDocType(Long fortressId, String docKey);

    @Query(elementClass = DocumentTypeNode.class,
            value =
                    "MATCH (company:ABCompany)<-[:TAG_INDEX]-(tag:_TagLabel) " +
                            "        where id(company)={0} and tag.companyKey ={1}" +
                            "       return tag")
    DocumentTypeNode findCompanyTag(Long companyId, String companyKey);


    @Query(elementClass = DocumentTypeNode.class,
            value = "MATCH fortress<-[:FORTRESS_DOC]-documentTypes " +
                    "        where id(fortress)={0} " +
                    "       return documentTypes")
    Collection<DocumentType> getFortressDocumentsInUse(Long fortressId);

    @Query(elementClass = DocumentTypeNode.class,
            value = "match (docTypes:_DocType)-[*..2]-(company:ABCompany) " +
                    "where id(company) = {0} return docTypes")
    Collection<DocumentType> getCompanyDocumentsInUse(Long companyId);

    @Query (value = "match (fortress:Fortress)-[fd:FORTRESS_DOC]-(d:_DocType) " +
            "where id(fortress) = {0} delete fd")
    void purgeFortressDocuments(Long fortressId);
}
