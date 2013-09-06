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

package com.auditbucket.engine.repo.neo4j.dao;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditEvent;
import com.auditbucket.dao.AuditEventDao;
import com.auditbucket.engine.repo.neo4j.AuditEventRepo;
import com.auditbucket.engine.repo.neo4j.model.AuditEventNode;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository
public class AuditEventDaoRepo implements AuditEventDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    AuditEventRepo eventRepo;

    @Override
    public AuditEvent findEvent(Company company, String eventCode) {
        return eventRepo.findCompanyEvent(company.getId(), eventCode);
    }

    @Override
    public AuditEvent createEvent(Company company, String eventCode) {
        AuditEventNode node = new AuditEventNode(company, eventCode);

        return template.save(node);
    }

    @Override
    public AuditEvent associate(AuditChange change, AuditEvent existingEvent, Company company) {
        return null;
    }

    @Override
    public Set<AuditEvent> findCompanyEvents(Long id) {
        return eventRepo.findCompanyEvents(id);
    }
}
