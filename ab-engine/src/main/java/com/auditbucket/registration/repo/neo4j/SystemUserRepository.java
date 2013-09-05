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

import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.auditbucket.registration.repo.neo4j.model.SystemUserNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface SystemUserRepository extends GraphRepository<SystemUserNode> {

    @Query(elementClass = FortressUserNode.class,
            value = "start sysUser=node:sysUserName(name={0}) " +
                    "        match sysUser-[:administers]->company-[:owns]->fortress<-[:fortressUser]-fortressUser " +
                    "where fortressUser.name ={2} and fortress.name={1} return fortressUser")
    FortressUser getFortressUser(String userName, String fortressName, String fortressUser);

    @Query(value = "start sysUser=node:sysUserName(name={0}) return sysUser")
    SystemUserNode getSystemUser(String name);
}
