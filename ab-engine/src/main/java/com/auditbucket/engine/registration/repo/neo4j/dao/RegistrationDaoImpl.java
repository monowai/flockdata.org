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

package com.auditbucket.engine.registration.repo.neo4j.dao;

import com.auditbucket.engine.registration.dao.RegistrationDaoI;
import com.auditbucket.engine.registration.repo.neo4j.SystemUserRepository;
import com.auditbucket.engine.registration.repo.neo4j.model.SystemUser;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:40 PM
 */
@Repository
public class RegistrationDaoImpl implements RegistrationDaoI {
    @Autowired
    private
    SystemUserRepository suRepo;

    @Override
    public ISystemUser save(ISystemUser systemUser) {
        return suRepo.save((SystemUser) systemUser);
    }

    public ISystemUser findByPropertyValue(String name, Object value) {
        return suRepo.findByPropertyValue(name, value);
    }

    @Override
    public ISystemUser save(ICompany company, String userName, String password) {
        SystemUser su = new SystemUser(userName, password, company, true);
        return save(su);
    }

    public IFortressUser getFortressUser(String userName, String fortressName, String fortressUser) {
        return suRepo.getFortressUser(userName, fortressName, fortressUser);
    }
}
