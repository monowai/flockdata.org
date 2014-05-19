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

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.FortressRepository;
import com.auditbucket.registration.repo.neo4j.FortressUserRepository;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.auditbucket.registration.service.KeyGenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:29 PM
 */
@Repository
public class FortressDaoNeo implements FortressDao {
    @Autowired
    private FortressRepository fortressRepo;

    @Autowired
    private FortressUserRepository fortressUserRepo;

    @Autowired
    private KeyGenService keyGenService;

    private Logger logger = LoggerFactory.getLogger(FortressDaoNeo.class);

    @Override
    public Fortress save(Company company, FortressInputBean fortressInput) {
        FortressNode fortress = new FortressNode(fortressInput, company);
        fortress.setFortressKey(keyGenService.getUniqueKey());
        return fortressRepo.save(fortress);
    }

    @Override
    public Fortress findByPropertyValue(String property, Object value) {
        return fortressRepo.findBySchemaPropertyValue(property, value);
    }

    @Override
    public Fortress findOne(Long fortressId) {
        logger.debug("Looking for {}", fortressId);
        return fortressRepo.findOne(fortressId);
    }

    @Autowired
    Neo4jTemplate template;

    @Override
    public FortressUser getFortressUser(Long fortressId, String name) {
        return fortressRepo.getFortressUser(fortressId, name);
    }

    @Override
    public List<Fortress> findFortresses(Long companyID) {
        return fortressRepo.findCompanyFortresses(companyID);
    }

    @Override
    public FortressUser findOneUser(Long fortressUserId) {
        return fortressUserRepo.findOne(fortressUserId);
    }

    @Override
    public FortressUser save(Fortress fortress, String fortressUserName) {
        return fortressUserRepo.save(new FortressUserNode(fortress, fortressUserName));
    }

    @Override
    public void fetch(FortressUser fortressUser) {
        template.fetch(fortressUser);

    }

    @Override
    public void delete(Fortress fortress) {
        template.delete(fortress);
    }


}
