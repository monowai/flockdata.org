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

import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.repo.neo4j.model.Company;
import com.auditbucket.registration.repo.neo4j.model.CompanyUser;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.SystemUser;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;


public interface CompanyRepository extends GraphRepository<Company> {

    @Query(elementClass = CompanyUser.class, value = "start company=node:companyName(name={0}) match company<-[:works]-companyUsers return companyUsers")
    Collection<ICompanyUser> getCompanyUsers(String companyName);

    @Query(elementClass = Fortress.class, value = "start company=node({0}) match company-[r:owns]->fortress where fortress.name ={1} return fortress")
    Fortress getFortress(Long companyId, String fortressName);

    @Query(elementClass = CompanyUser.class, value = "start company=node({0}) match company-[r:works]-companyUser where companyUser.name ={1} return companyUser")
    CompanyUser getCompanyUser(long ID, String userName);

    @Query(elementClass = SystemUser.class, value = "start company=node({0}) match company-[r:administers]-systemUser where systemUser.name ={1} return systemUser")
    SystemUser getAdminUser(long ID, String userName);


}
