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

package org.flockdata.registration.service;


import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.RegistrationBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.SystemUser;

/**
 * User: mike
 * Date: 22/08/14
 * Time: 9:43 AM
 */
public interface RegistrationService {
    SystemUser registerSystemUser(Company company, RegistrationBean regBean) throws FlockException;

    SystemUser registerSystemUser(RegistrationBean regBean) throws FlockException;

    Company resolveCompany(String apiKey) throws FlockException;

    SystemUser getSystemUser(String s);
}
