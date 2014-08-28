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

package com.auditbucket.engine.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.DocumentType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Query parameter support functionality
 * Centralises methods that will support options to use on MatrixService etc.
 * <p/>
 * User: mike
 * Date: 14/06/14
 * Time: 9:43 AM
 */
@Service
public class QueryService {
    @Autowired
    FortressService fortressService;

    @Autowired
    TagTrackService tagService;

    @Autowired
    SchemaService schemaService;

    public Collection<DocumentType> getDocumentsInUse(Company abCompany, Collection<String> fortresses) throws DatagioException {
        ArrayList<DocumentType> docs = new ArrayList<>();

        // ToDo: Optimize via Cypher, not a java loop
        //match (f:Fortress) -[:FORTRESS_DOC]-(d) return f,d
        if (fortresses == null) {
            Collection<Fortress> forts = fortressService.findFortresses(abCompany);
            for (Fortress fort : forts) {
                docs.addAll(fortressService.getFortressDocumentsInUse(abCompany, fort.getCode()));
            }

        } else {
            for (String fortress : fortresses) {
                Collection<DocumentType> documentTypes = fortressService.getFortressDocumentsInUse(abCompany, fortress);
                docs.addAll(documentTypes);
            }
        }
        return docs;

    }

    public Set<DocumentType> getConceptsWithRelationships(Company company, Collection<String> documents) {
        return schemaService.findConcepts(company, documents, true);

    }

    public Set<DocumentType> getConcepts(Company company, Collection<String> documents, boolean withRelationships) {
        return schemaService.findConcepts(company, documents, withRelationships);

    }

    public Set<DocumentType> getConcepts(Company abCompany, Collection<String> documents) {
        //match (a:Orthopedic) -[r]-(:_Tag) return distinct type(r) as typeName  order by typeName;

        return getConcepts(abCompany, documents, false);
    }
}
