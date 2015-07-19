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

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface RegistrationDao {
    org.flockdata.model.SystemUser findSysUserByName(String name);

    org.flockdata.model.SystemUser findByApiKey(String apiKey);

    org.flockdata.model.SystemUser save(Company company, String userName, String password);

}
