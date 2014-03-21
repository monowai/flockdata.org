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

package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditEvent;
import com.auditbucket.dao.AuditEventDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 5:07 PM
 */
@Transactional
@Service
public class AuditEventService {

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    AuditEventDao auditEventDao;

    /**
     * associates the change with the event name for the company. Creates if it does not exist
     *
     * @param eventCode - descriptive name of the event - duplicates for a company will not be created
     * @return created AuditEvent
     */
    @Transactional(propagation =  Propagation.SUPPORTS)
    public AuditEvent processEvent(String eventCode) {
        Company company = securityHelper.getCompany();
        return processEvent(company, eventCode);
    }

    @Transactional(propagation =  Propagation.SUPPORTS)
    public AuditEvent processEvent(Company company, String eventCode) {
        AuditEvent existingEvent = auditEventDao.findEvent(company, eventCode);
        if (existingEvent == null)
            existingEvent = auditEventDao.createEvent(company, eventCode);
        return existingEvent;
    }

    public Set<AuditEvent> getCompanyEvents(Long id) {
        return auditEventDao.findCompanyEvents(id);
    }
}
