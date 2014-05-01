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

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.SystemUserRepository;
import com.auditbucket.registration.repo.neo4j.model.SystemUserNode;
import com.auditbucket.registration.service.KeyGenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:40 PM
 */
@Repository
public class RegistrationNeo implements com.auditbucket.dao.RegistrationDao {
    @Autowired
    private SystemUserRepository suRepo;

    @Autowired
    Neo4jTemplate template;

    @Autowired
    KeyGenService keyGenService;

    SystemUser save(SystemUser systemUser) {
        return suRepo.save((SystemUserNode) systemUser);
    }

    public SystemUser findByApiKey(String apiKey){
        if ( apiKey == null )
            return null;
        return suRepo.findBySchemaPropertyValue("apiKey", apiKey);
    }

    public SystemUser findSysUserByName(String name) {
        SystemUser su = suRepo.getSystemUser(name);
        if (su !=null ){
            if (su.getApiKey()== null){
                // ToDo: Remove this in 0.94 - this is for upgrading only.
                su.setApiKey(keyGenService.getUniqueKey());
                su = save(su);
            }
            return su;
        }
        return null;
    }

    @Override
    public SystemUser save(Company company, String userName, String password) {
        SystemUser su = new SystemUserNode(userName, password, company, true);
        su.setApiKey(keyGenService.getUniqueKey());
        su = save(su);
        return su;
    }

    public FortressUser getFortressUser(String userName, String fortressName, String fortressUser) {
        return suRepo.getFortressUser(userName, fortressName, fortressUser);
    }


}
