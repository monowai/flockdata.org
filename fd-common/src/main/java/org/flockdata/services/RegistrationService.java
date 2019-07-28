/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.services;


import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.RegistrationBean;

/**
 * @author mholdsworth
 * @since 22/08/2014
 */
public interface RegistrationService {

  SystemUser registerSystemUser(RegistrationBean regBean) throws FlockException;

  SystemUser registerSystemUser(Company company, RegistrationBean regBean) throws FlockException;

  Company resolveCompany(String apiKey) throws FlockException;

  SystemUser getSystemUser(String apiKey);

  SystemUser getSystemUser();
}
