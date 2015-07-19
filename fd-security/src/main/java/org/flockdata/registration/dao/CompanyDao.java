/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.registration.dao;


import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;

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
    Company update(Company systemUser);

    Company findByPropertyValue(String property, Object value);

    SystemUser getAdminUser(Long companyId, String name);

    Company create(String companyName, String uniqueKey);

    Collection<Company> findCompanies(Long sysUserId);

    Collection<Company> findCompanies(String userApiKey);

    Company create(Company company);
}
