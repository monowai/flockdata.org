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

package com.auditbucket.core.registration.repo.neo4j.dao;

import com.auditbucket.core.registration.dao.FortressDaoI;
import com.auditbucket.core.registration.repo.neo4j.FortressRepository;
import com.auditbucket.core.registration.repo.neo4j.FortressUserRepository;
import com.auditbucket.core.registration.repo.neo4j.model.Fortress;
import com.auditbucket.core.registration.repo.neo4j.model.FortressUser;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * User: mike
 * Date: 20/04/13
 * Time: 10:29 PM
 */
@Repository
public class FortressDaoImpl implements FortressDaoI {
    @Autowired
    private FortressRepository fortressRepo;
    @Autowired
    private FortressUserRepository fortressUserRepo;

    @Override
    public IFortress save(IFortress fortress) {
        return fortressRepo.save((Fortress) fortress);
    }

    @Override
    public IFortress findByPropertyValue(String name, Object value) {
        return fortressRepo.findByPropertyValue(name, value);
    }

    @Override
    public IFortress findOne(Long id) {
        return fortressRepo.findOne(id);
    }

    @Autowired
    Neo4jTemplate template;

    @Override
    public IFortressUser getFortressUser(Long id, String name) {
        IFortressUser fu = fortressRepo.getFortressUser(id, name);
        if (fu != null)
            template.fetch(fu.getFortress());
        return fu;
    }

    @Override
    public List<IFortress> findFortresses(Long companyID) {

//        TraversalDescription td = Traversal.description()
//                .breadthFirst()
//                .relationships( DynamicRelationshipType.withName("owns"), Direction.OUTGOING )
//                .evaluator( Evaluators.excludeStartPosition() );

        //return fortressRepo.findAllByTraversal(companyID, td );

        return fortressRepo.findCompanyFortresses(companyID);
    }

    @Override
    public IFortressUser findOneUser(Long id) {
        return fortressUserRepo.findOne(id);
    }

    @Override
    public IFortressUser save(IFortressUser fortressUser) {
        return fortressUserRepo.save((FortressUser) fortressUser);
    }


}
