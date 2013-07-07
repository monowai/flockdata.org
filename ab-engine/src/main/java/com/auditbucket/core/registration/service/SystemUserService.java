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

package com.auditbucket.core.registration.service;

import com.auditbucket.core.registration.bean.RegistrationBean;
import com.auditbucket.core.registration.repo.neo4j.dao.RegistrationDaoImpl;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.model.ISystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemUserService {

    @Autowired
    RegistrationDaoImpl regDao;

    public ISystemUser findByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return regDao.findByPropertyValue("name", name.toLowerCase());
    }

    public IFortressUser findBy(String userName, String fortressName, String fortressUser) {
        return regDao.getFortressUser(userName, fortressName, fortressUser);
    }

    @Transactional
    public ISystemUser save(RegistrationBean regBean) {
        return regDao.save(regBean.getCompany(), regBean.getName(), regBean.getPassword());
    }
}
