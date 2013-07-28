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

package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.List;


public interface FortressRepository extends GraphRepository<FortressNode> {

    @Query(value = "start fortress=node({0}) match fortress-[:fortressUser]->fu where fu.name = {1} return fu")
    FortressUserNode getFortressUser(Long fortressId, String userName);

    @Query(elementClass = FortressNode.class, value = "start company=node({0}) match company-[:owns]->f return f")
    List<Fortress> findCompanyFortresses(Long companyID);

}
