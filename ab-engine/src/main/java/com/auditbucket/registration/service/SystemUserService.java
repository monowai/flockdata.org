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

package com.auditbucket.registration.service;

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.dao.RegistrationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemUserService {

    @Autowired
    RegistrationDao regDao;

    @Cacheable(value = "systemUsers", unless = "#result == null")
    public SystemUser findByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return regDao.findSysUserByName(name.toLowerCase());
    }

    public FortressUser findBy(String userName, String fortressName, String fortressUser) {
        return regDao.getFortressUser(userName, fortressName, fortressUser);
    }

    //@Transactional

    public SystemUser save(RegistrationBean regBean) {
        return regDao.save(regBean.getCompany(), regBean.getName(), regBean.getPassword());
    }


}
