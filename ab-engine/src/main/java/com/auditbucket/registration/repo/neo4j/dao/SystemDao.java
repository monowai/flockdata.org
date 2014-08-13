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

package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.model.ISystem;
import com.auditbucket.registration.repo.neo4j.SystemRepository;
import com.auditbucket.registration.repo.neo4j.model.SystemNode;
import com.auditbucket.registration.service.KeyGenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:33 PM
 */
@Repository
@TypeAlias("ab.System")
public class SystemDao implements com.auditbucket.dao.SystemDao {

    @Autowired
    SystemRepository sysRepo;

    @Autowired
    KeyGenService keyGenService;

    public ISystem save(ISystem system) {
        return sysRepo.save((SystemNode) system);
    }

    @Override
    public ISystem findOne(String name) {
        return null;
    }
}
