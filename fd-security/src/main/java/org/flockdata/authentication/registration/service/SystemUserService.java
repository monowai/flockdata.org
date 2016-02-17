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

package org.flockdata.authentication.registration.service;

import org.flockdata.model.SystemUser;
import org.flockdata.registration.RegistrationBean;

/**
 * User: mike
 * Date: 22/08/14
 * Time: 9:46 AM
 */
public interface SystemUserService {
    SystemUser findByLogin(String userEmail);

    SystemUser findByApiKey(String apiKey);

    SystemUser save(RegistrationBean regBean);
}
