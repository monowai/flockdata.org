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

package com.auditbucket.core.registration.dao;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface CompanyDaoI {
    public ICompany save(ICompany systemUser);

    public ICompanyUser save(ICompanyUser companyUser);

    public ICompany findByPropertyValue(String name, Object value);

    public ICompanyUser getCompanyUser(Long id, String userName);

    public IFortress getFortress(Long id, String fortressName);

    public ISystemUser getAdminUser(Long id, String name);

    public Iterable<ICompanyUser> getCompanyUsers(String companyName);
}
