/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.company.service;

import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.dao.RegistrationDao;
import org.flockdata.registration.model.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemUserServiceNeo4j implements org.flockdata.registration.service.SystemUserService {

    @Autowired
    RegistrationDao regDao;

    // TODO DAT-184 Gives error while enabling caching 
//    @Cacheable(value = "systemUsers", unless = "#result == null")
    public SystemUser findByLogin(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Login name cannot be null");
        }
        return regDao.findSysUserByName(name.toLowerCase());
    }

    public SystemUser save(RegistrationBean regBean) {
        return regDao.save(regBean.getCompany(), regBean.getName(), regBean.getLogin());
    }

    public SystemUser findByApiKey(String apiKey) {
        return regDao.findByApiKey(apiKey);
    }
}
