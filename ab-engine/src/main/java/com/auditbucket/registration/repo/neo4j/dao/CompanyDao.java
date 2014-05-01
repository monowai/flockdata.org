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
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;

import java.util.Collection;

/**
 * Company represents a unique organisation who interacts with the system
 * API To abstract interactions with underlying implementations
 *
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface CompanyDao {
    public Company update(Company systemUser);

    public CompanyUser save(CompanyUser companyUser);

    public Company findByPropertyValue(String property, Object value);

    public CompanyUser getCompanyUser(Long companyId, String userName);

    public Fortress getFortressByName(Long companyId, String fortressName);

    public SystemUser getAdminUser(Long companyId, String name);

    public Iterable<CompanyUser> getCompanyUsers(Long companyId);

    Company create(String companyName, String uniqueKey);

    Fortress getFortressByCode(Long companyId, String fortressCode);

    Collection<Company> findCompanies(Long sysUserId);

    Collection<Company> findCompanies(String userApiKey);
}
