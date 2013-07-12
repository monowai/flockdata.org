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

package com.auditbucket.registration.dao;

import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;

import java.util.List;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 6:31 PM
 */
public interface FortressDaoI {
    public IFortress save(IFortress fortress);

    public IFortress findByPropertyValue(String name, Object value);

    public IFortress findOne(Long id);

    public IFortressUser getFortressUser(Long id, String name);

    List<IFortress> findFortresses(Long companyID);

    IFortressUser findOneUser(Long id);

    IFortressUser save(IFortressUser fortressUser);
}
