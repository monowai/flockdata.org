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

package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.dao.SystemDaoI;
import com.auditbucket.registration.model.ISystem;
import com.auditbucket.registration.repo.neo4j.SystemRepository;
import com.auditbucket.registration.repo.neo4j.model.SystemId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:33 PM
 */
@Repository
public class SystemDaoImpl implements SystemDaoI {

    @Autowired
    SystemRepository sysRepo;

    public ISystem save(ISystem system) {
        return sysRepo.save((SystemId) system);
    }

    @Override
    public ISystem findOne(String name) {
        return null;
    }
}
