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

package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;

import java.util.Collection;
import java.util.List;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface CompanyDao {
    public Company update(Company systemUser);

    public CompanyUser save(CompanyUser companyUser);

    public Company findByPropertyValue(String name, Object value);

    public CompanyUser getCompanyUser(Long id, String userName);

    public Fortress getFortressByName(Long id, String fortressName);

    public SystemUser getAdminUser(Long id, String name);

    public Iterable<CompanyUser> getCompanyUsers(String companyName);

    Company create(String companyName, String uniqueKey);

    Fortress getFortressByCode(Long id, String fortressCode);

    Collection<Company> findCompanies(Long id);
}
