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

package org.flockdata.engine.repo.neo4j.dao;

import org.flockdata.engine.repo.neo4j.FortressRepository;
import org.flockdata.engine.repo.neo4j.FortressUserRepository;
import org.flockdata.engine.repo.neo4j.model.FortressNode;
import org.flockdata.engine.repo.neo4j.model.FortressUserNode;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.registration.service.KeyGenService;
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
public class FortressDaoNeo  {
    @Autowired
    private FortressRepository fortressRepo;

    @Autowired
    private FortressUserRepository fortressUserRepo;

    @Autowired
    private KeyGenService keyGenService;

    private Logger logger = LoggerFactory.getLogger(FortressDaoNeo.class);

    public Fortress save(Company company, FortressInputBean fortressInput) {
        FortressNode fortress = new FortressNode(fortressInput, company);
        fortress.setFortressKey(keyGenService.getUniqueKey());
        return fortressRepo.save(fortress);
    }

    public Fortress findByPropertyValue(String property, Object value) {
        return fortressRepo.findBySchemaPropertyValue(property, value);
    }

    public Fortress findOne(Long fortressId) {
//        logger.debug("Looking for {}", fortressId);
        return fortressRepo.findOne(fortressId);
    }

    @Autowired
    Neo4jTemplate template;

    public FortressUser getFortressUser(Long fortressId, String name) {
        //return fortressRepo.getFortressUser(fortressId, name);
        return fortressUserRepo.findBySchemaPropertyValue("key", fortressId + "." + name);
    }

    public List<Fortress> findFortresses(Long companyID) {
        return fortressRepo.findCompanyFortresses(companyID);
    }

    public FortressUser findOneUser(Long fortressUserId) {
        return fortressUserRepo.findOne(fortressUserId);
    }

    public FortressUser save(Fortress fortress, String fortressUserName) {
        return fortressUserRepo.save(new FortressUserNode(fortress, fortressUserName));
    }

    public void fetch(FortressUser fortressUser) {
        template.fetch(fortressUser);

    }

    public void delete(Fortress fortress) {
        template.delete(fortress);
    }

    public Fortress getFortressByName(Long companyId, String fortressName) {
        return fortressRepo.getFortressByName(companyId, fortressName);
    }

    public Fortress getFortressByCode(Long companyId, String fortressCode) {
        return fortressRepo.getFortressByCode(companyId, fortressCode);
    }


}
